package nl.potentplum.mise.feature.quests

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** The Verfijn quest form (spec §6.11), pre-filled from a vegetable or technique. Built with the Quests section of P1. */
@Composable
fun QuestRefineScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    PlaceholderScreen(title = stringResource(R.string.quest_params_title), onOverflowItem = onOverflowItem, onBack = onBack)
}
