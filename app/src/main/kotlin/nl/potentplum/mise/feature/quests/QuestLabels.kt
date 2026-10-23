package nl.potentplum.mise.feature.quests

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.difficultyLabel
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.TimeLimit

@Composable
fun questTypeLabel(type: QuestType): String =
    stringResource(
        when (type) {
            QuestType.MAALTIJD -> R.string.quest_type_maaltijd
            QuestType.LUNCH -> R.string.quest_type_lunch
            QuestType.BIJGERECHT -> R.string.quest_type_bijgerecht
            QuestType.SAUS -> R.string.quest_type_saus
            QuestType.BOUILLON -> R.string.quest_type_bouillon
            QuestType.COMPONENT -> R.string.quest_type_component
            QuestType.CLAUDE_CHOICE -> R.string.quest_type_claude
        },
    )

@Composable
fun ingredientModeLabel(mode: IngredientMode): String =
    stringResource(
        when (mode) {
            IngredientMode.CUSTOM -> R.string.quest_ingredient_custom
            IngredientMode.SEASONAL -> R.string.quest_ingredient_seasonal
            IngredientMode.SURPRISE -> R.string.quest_ingredient_surprise
        },
    )

@Composable
fun timeLimitLabel(limit: TimeLimit?): String =
    stringResource(
        when (limit) {
            null -> R.string.quest_time_limit_any
            TimeLimit.UNDER_15 -> R.string.quest_time_limit15
            TimeLimit.UNDER_30 -> R.string.quest_time_limit30
            TimeLimit.UNDER_60 -> R.string.quest_time_limit60
            TimeLimit.OVER_60 -> R.string.quest_time_limit_over60
        },
    )

/** "Seizoensgroente: pastinaak · Focus: Mechanisch Meesterschap · Gemiddeld" (spec §6.11). */
@Composable
fun questSummaryLine(summary: QuestPlanSummary): String {
    val ingredient =
        when (summary.mode) {
            IngredientMode.SEASONAL -> summary.ingredient?.let { stringResource(R.string.quest_chosen_seasonal, it) }
            IngredientMode.CUSTOM -> summary.ingredient?.let { stringResource(R.string.quest_chosen_custom, it) }
            IngredientMode.SURPRISE -> stringResource(R.string.quest_chosen_surprise)
        }
    val focus =
        summary.technique?.let { stringResource(R.string.quest_chosen_technique, it) }
            ?: summary.focus?.let { stringResource(R.string.quest_chosen_focus, it) }
    return listOfNotNull(ingredient, focus, difficultyLabel(summary.difficulty)).joinToString(" · ")
}
