package nl.potentplum.mise.feature.journal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Dagboek (spec §6.13): Tijdlijn · Quests · Inzichten. Built in P1. */
@Composable
fun JournalScreen(onOverflowItem: (OverflowItem) -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.nav_journal), onOverflowItem = onOverflowItem)
}
