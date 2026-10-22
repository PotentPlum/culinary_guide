package nl.potentplum.mise.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Instellingen (spec §6.15). Built in P1. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    PlaceholderScreen(title = stringResource(R.string.menu_settings), onOverflowItem = onOverflowItem, onBack = onBack)
}
