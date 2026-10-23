package nl.potentplum.mise.feature.quests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.quest.QuestGenerator
import nl.potentplum.mise.claude.quest.QuestPlan
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.QuestParams

/** "What was chosen", shown while a quest is generating (spec §6.11): ingredient · focus or technique · difficulty. */
data class QuestPlanSummary(
    val mode: IngredientMode,
    val ingredient: String?,
    val focus: String?,
    val technique: String?,
    val difficulty: Difficulty,
) {
    companion object {
        fun of(
            plan: QuestPlan,
            content: Content,
        ): QuestPlanSummary =
            QuestPlanSummary(
                mode = plan.params.ingredientMode,
                ingredient =
                    when (plan.params.ingredientMode) {
                        IngredientMode.CUSTOM -> {
                            plan.params.ingredient.trim()
                        }

                        IngredientMode.SEASONAL -> {
                            plan.params.vegId
                                ?.let(content::vegetable)
                                ?.nl
                                ?.lowercase()
                        }

                        IngredientMode.SURPRISE -> {
                            null
                        }
                    },
                focus = plan.focus?.let { focus -> content.disciplines.firstOrNull { it.id == focus }?.nl },
                technique =
                    plan.params.techniqueId
                        ?.let(content::technique)
                        ?.nl,
                difficulty = plan.difficulty,
            )
    }
}

sealed interface QuestGenState {
    data object Idle : QuestGenState

    data class Generating(
        val summary: QuestPlanSummary,
    ) : QuestGenState

    data class Failed(
        val error: ClaudeError,
    ) : QuestGenState
}

/** One cancellable generation at a time, shared by the Vandaag card, the result screen, Verfijn and history. */
class QuestGeneration(
    private val scope: CoroutineScope,
    private val generator: QuestGenerator,
    private val content: ContentRepository,
) {
    private val _state = MutableStateFlow<QuestGenState>(QuestGenState.Idle)
    val state: StateFlow<QuestGenState> = _state.asStateFlow()
    private var job: Job? = null

    /** [params] is resolved lazily, so the daily defaults are read at the moment of the tap. */
    fun start(
        params: suspend () -> QuestParams,
        onDone: suspend (QuestEntity) -> Unit,
    ) {
        if (_state.value is QuestGenState.Generating) return
        job =
            scope.launch {
                try {
                    val plan = generator.plan(params())
                    _state.value = QuestGenState.Generating(QuestPlanSummary.of(plan, content.content()))
                    val quest = generator.generate(plan)
                    _state.value = QuestGenState.Idle
                    onDone(quest)
                } catch (e: ClaudeError) {
                    _state.value = QuestGenState.Failed(e)
                }
            }
    }

    /** Annuleren: back to idle; nothing is recorded (spec §6.11). */
    fun cancel() {
        job?.cancel()
        _state.value = QuestGenState.Idle
    }

    fun dismissError() {
        if (_state.value is QuestGenState.Failed) _state.value = QuestGenState.Idle
    }
}
