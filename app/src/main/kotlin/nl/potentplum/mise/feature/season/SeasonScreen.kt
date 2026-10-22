package nl.potentplum.mise.feature.season

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Seizoen (spec §6.3–6.4): Nu and Kalender. Built in P1. */
@Composable
fun SeasonScreen(onOverflowItem: (OverflowItem) -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.nav_seasonal), onOverflowItem = onOverflowItem)
}
