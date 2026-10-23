package nl.potentplum.mise.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.CuratedFallback
import nl.potentplum.mise.domain.CuratedSuggestion
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.domain.SeasonPresentation
import nl.potentplum.mise.domain.SeasonStatus
import nl.potentplum.mise.domain.Suggestion
import nl.potentplum.mise.domain.TodaySuggester
import nl.potentplum.mise.domain.VegetableInSeason
import java.time.LocalDate
import javax.inject.Inject

/** A suggestion with the names its reason line needs. */
data class SuggestionUi(
    val suggestion: Suggestion,
    /** Display names of the reason's peak vegetables, in order. */
    val peakNames: List<String>,
) {
    val item: LibraryItem get() = suggestion.item
}

data class TodayUiState(
    val loading: Boolean = true,
    val libraryEmpty: Boolean = false,
    /** The Vanavond pin, when it is still in the library. */
    val tonight: LibraryItem? = null,
    val suggestions: List<SuggestionUi> = emptyList(),
    /** Curated links for the slots the library cannot fill, or below the empty state. */
    val curated: List<CuratedSuggestion> = emptyList(),
    val quickOnly: Boolean = false,
    /** "Nu op z'n best": at most 8 PEAK vegetables in the ranked order. */
    val peak: List<VegetableInSeason> = emptyList(),
    /** Vegetables available this month (PEAK or SHOULDER), for "Alle n groenten". */
    val availableCount: Int = 0,
)

sealed interface TodayEvent {
    data class AddedToShopping(
        val count: Int,
    ) : TodayEvent
}

/**
 * Vandaag (spec §6.2). The ranking runs off the main thread and only when the library or the day changes; dismissing,
 * pinning and the ≤ 30 min chip only re-pick from the cached ranking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val shopping: ShoppingRepository,
        private val settings: SettingsRepository,
        content: ContentRepository,
        clock: MiseClock,
    ) : ViewModel() {
        private val day = MutableStateFlow(clock.today())
        private val quickOnly = MutableStateFlow(false)
        private val vegetables = flow { emit(content.content().vegetables) }

        private data class Ranking(
            val day: LocalDate,
            val vegetables: List<Vegetable>,
            val library: List<LibraryItem>,
            val ranked: List<Suggestion>,
        )

        private val ranking =
            combine(vegetables, recipes.observeLibrary(), day) { vegetables, library, day ->
                Ranking(day, vegetables, library, TodaySuggester.rank(library, vegetables.associateBy { it.id }, day))
            }.flowOn(Dispatchers.Default)

        private val dayState = combine(settings.tonight, settings.todayDismissed, quickOnly, ::Triple)

        val state: StateFlow<TodayUiState> =
            combine(ranking, dayState) { ranking, (pin, dismissed, quick) ->
                val byId = ranking.vegetables.associateBy { it.id }
                val tonight = pin?.let { p -> ranking.library.firstOrNull { it.id == p.recipeId } }
                val picked = TodaySuggester.pick(ranking.ranked, dismissed, tonight?.id, quick)
                val month = ranking.day.monthValue
                val seasonal = SeasonPresentation.ranked(ranking.vegetables, month)
                val libraryEmpty = ranking.library.isEmpty()
                TodayUiState(
                    loading = false,
                    libraryEmpty = libraryEmpty,
                    tonight = tonight,
                    suggestions =
                        picked.map { suggestion ->
                            SuggestionUi(suggestion, suggestion.reason.peakVegIds.mapNotNull { byId[it]?.let(::shortName) })
                        },
                    curated =
                        CuratedFallback.pick(
                            ranking.vegetables,
                            ranking.day,
                            count = if (libraryEmpty) TodaySuggester.SLOTS else TodaySuggester.SLOTS - picked.size,
                            quickOnly = quick && !libraryEmpty,
                        ),
                    quickOnly = quick,
                    peak = seasonal.filter { it.status == SeasonStatus.PEAK }.take(PEAK_SHORTLIST),
                    availableCount = seasonal.count { it.status.isAvailable },
                )
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

        private val _events = Channel<TodayEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        /** The shell's day; a new day re-ranks (the pin and dismissals expire on their own). */
        fun onDay(today: LocalDate) {
            day.value = today
        }

        fun setQuickOnly(on: Boolean) {
            quickOnly.value = on
        }

        fun dismiss(recipeId: String) {
            viewModelScope.launch { settings.dismissToday(recipeId) }
        }

        fun pin(recipeId: String) {
            viewModelScope.launch { settings.pinTonight(recipeId) }
        }

        fun unpin() {
            viewModelScope.launch { settings.unpinTonight() }
        }

        /** The pinned recipe at its own servings (spec §6.14). */
        fun addTonightToShopping() {
            val id = state.value.tonight?.id ?: return
            viewModelScope.launch {
                val recipe = recipes.get(id) ?: return@launch
                _events.send(TodayEvent.AddedToShopping(shopping.addRecipe(recipe, recipes.scaler())))
            }
        }

        private companion object {
            const val PEAK_SHORTLIST = 8

            /** "Wortelen / peen" → "Wortelen". */
            fun shortName(vegetable: Vegetable): String = vegetable.nl.substringBefore(" / ")
        }
    }
