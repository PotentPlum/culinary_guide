package nl.potentplum.mise.feature.cook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.designsystem.component.TimerChipState
import nl.potentplum.mise.core.navigation.CookRoute
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.settings.CookSession
import nl.potentplum.mise.data.settings.CookTimer
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.DetectedDuration
import nl.potentplum.mise.domain.DurationDetector
import nl.potentplum.mise.domain.TechniqueMentions
import nl.potentplum.mise.feature.recipes.IngredientGroupLines
import nl.potentplum.mise.feature.recipes.IngredientLine
import nl.potentplum.mise.platform.ClockTimers
import javax.inject.Inject
import kotlin.math.ceil

data class CookStep(
    val index: Int,
    /** The step group's name, shown as an eyebrow. */
    val groupName: String?,
    val text: String,
    val durations: List<DetectedDuration>,
    val techniques: List<Technique>,
)

/** A timer started from cook mode, as the strip at the top shows it. */
data class RunningTimer(
    val timer: CookTimer,
    val chip: TimerChipState,
)

data class CookUiState(
    val loading: Boolean = true,
    val title: String = "",
    val steps: List<CookStep> = emptyList(),
    /** 0-based. */
    val stepIndex: Int = 0,
    val servings: Int? = null,
    val ingredients: List<IngredientGroupLines> = emptyList(),
    val timers: List<RunningTimer> = emptyList(),
    /** False when no app takes `ACTION_SET_TIMER`: the chips are hidden. */
    val timersSupported: Boolean = true,
    /** The one-time explanation of hidden timer chips. */
    val showTimerHint: Boolean = false,
) {
    val notFound: Boolean get() = !loading && steps.isEmpty() && title.isEmpty()
    val isLastStep: Boolean get() = steps.isNotEmpty() && stepIndex == steps.lastIndex
}

sealed interface CookEvent {
    data class TimerStarted(
        val seconds: Int,
    ) : CookEvent

    data object TimerFailed : CookEvent
}

/**
 * Kookmodus (spec §6.10). Everything that should survive leaving or process death lives in the one cook session (§7.3): the
 * step, servings, ingredient ticks and started timers. Opening cook mode for another recipe replaces that session.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CookViewModel
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val settings: SettingsRepository,
        private val timers: ClockTimers,
        private val clock: MiseClock,
        content: ContentRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedState.toRoute<CookRoute>()
        val recipeId = route.recipeId

        private val tools = flow { emit(TechniqueMentions(content.content().techniques) to recipes.scaler()) }
        private val session = settings.cookSession.map { it?.takeIf { s -> s.recipeId == recipeId } }

        /** Ticks every 15 s while a timer runs, so "nog ~9 min" counts down. */
        private val now =
            session
                .map { it?.timers.orEmpty().isNotEmpty() }
                .distinctUntilChanged()
                .flatMapLatest { running ->
                    if (!running) {
                        flowOf(clock.nowMillis())
                    } else {
                        flow {
                            while (true) {
                                emit(clock.nowMillis())
                                delay(TICK_MILLIS)
                            }
                        }
                    }
                }.flowOn(Dispatchers.Default)

        /** Whether the last step was reached in this visit; only then does leaving ask "Heb je dit gekookt?". */
        val reachedEnd: StateFlow<Boolean> = savedState.getStateFlow(KEY_REACHED_END, false)

        val state: StateFlow<CookUiState> =
            combine(tools, recipes.observe(recipeId), session, now, settings.settings) { (mentions, scaler), recipe, session, now, prefs ->
                if (recipe == null) return@combine CookUiState(loading = false)
                val steps = steps(recipe, mentions)
                val servings = recipe.servings?.let { session?.servings ?: route.servings ?: it }
                val ratio = scaler.ratio(recipe.servings, servings ?: 0) ?: 1.0
                val checked = session?.checkedIngredientIds.orEmpty().toSet()
                val supported = timers.supported
                CookUiState(
                    loading = false,
                    title = recipe.title,
                    steps = steps,
                    stepIndex = (session?.stepIndex ?: 0).coerceIn(0, (steps.size - 1).coerceAtLeast(0)),
                    servings = servings,
                    ingredients =
                        recipe.ingredientGroups.map { group ->
                            IngredientGroupLines(
                                group.name,
                                group.items.map {
                                    IngredientLine(
                                        it.id,
                                        scaler.format(it.parsed, ratio),
                                        it.id in checked,
                                        it.parsed.parsed,
                                    )
                                },
                            )
                        },
                    timers = session?.timers.orEmpty().map { RunningTimer(it, chip(it, now)) },
                    timersSupported = supported,
                    showTimerHint = !supported && !prefs.clockTimerHintShown && steps.any { it.durations.isNotEmpty() },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookUiState())

        private val _events = Channel<CookEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            viewModelScope.launch {
                var seen = false
                session.collect { current ->
                    if (current != null) {
                        seen = true
                    } else if (seen) {
                        ended = true
                    }
                }
            }
            // One open session: start this recipe's, unless it is already the open one.
            viewModelScope.launch {
                val current = settings.cookSession.first()
                if (current?.recipeId != recipeId) {
                    val recipe = recipes.get(recipeId) ?: return@launch
                    settings.saveCookSession(CookSession(recipeId, 0, recipe.servings?.let { route.servings ?: it }, updatedAt = 0))
                } else {
                    val steps = recipes.get(recipeId)?.steps?.size ?: 0
                    if (steps > 0 && current.stepIndex >= steps - 1) savedState[KEY_REACHED_END] = true
                }
            }
        }

        fun goTo(index: Int) {
            val steps = state.value.steps
            if (steps.isEmpty()) return
            val target = index.coerceIn(0, steps.lastIndex)
            if (target == steps.lastIndex) savedState[KEY_REACHED_END] = true
            update { it.copy(stepIndex = target) }
        }

        fun toggleIngredient(ingredientId: String) =
            update { session ->
                val ticks = session.checkedIngredientIds.toMutableSet()
                if (!ticks.add(ingredientId)) ticks.remove(ingredientId)
                session.copy(checkedIngredientIds = ticks.toList())
            }

        /** Starts a Clock app timer for [step] (0-based) and records it for the strip. */
        fun startTimer(
            step: Int,
            seconds: Int,
            message: String,
        ) {
            if (!timers.start(seconds, message)) {
                viewModelScope.launch { _events.send(CookEvent.TimerFailed) }
                return
            }
            val timer = CookTimer(step, seconds, clock.nowMillis())
            update { it.copy(timers = it.timers + timer) }
            viewModelScope.launch { _events.send(CookEvent.TimerStarted(seconds)) }
        }

        fun showTimers() {
            timers.showTimers()
        }

        fun dismissTimerHint() {
            viewModelScope.launch { settings.setClockTimerHintShown() }
        }

        /**
         * Changes this recipe's session. Never starts one: [init] does, and once an attempt has cleared it (the evaluation
         * logged from here), the pager settling back on step 1 must not bring it back.
         */
        private fun update(change: (CookSession) -> CookSession) {
            viewModelScope.launch {
                if (ended) return@launch
                settings.updateCookSession { session -> if (session?.recipeId == recipeId) change(session) else session }
            }
        }

        /** Set once this recipe's session has been seen and then cleared (an attempt was logged); cook mode is done. */
        private var ended = false

        private fun chip(
            timer: CookTimer,
            now: Long,
        ): TimerChipState {
            val left = timer.startedAt + timer.lengthSeconds * 1_000L - now
            return if (left <= 0) {
                TimerChipState.Done(timer.stepIndex + 1)
            } else {
                TimerChipState.Running(timer.stepIndex + 1, ceil(left / 60_000.0).toInt())
            }
        }

        private companion object {
            const val KEY_REACHED_END = "reachedEnd"
            const val TICK_MILLIS = 15_000L

            fun steps(
                recipe: Recipe,
                mentions: TechniqueMentions,
            ): List<CookStep> {
                var index = 0
                return recipe.stepGroups.flatMap { group ->
                    group.steps.map { step ->
                        CookStep(index++, group.name, step.text, DurationDetector.detect(step.text), mentions.find(step.text))
                    }
                }
            }
        }
    }
