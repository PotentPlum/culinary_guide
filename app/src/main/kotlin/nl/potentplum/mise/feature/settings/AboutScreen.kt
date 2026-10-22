package nl.potentplum.mise.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Over Mise (spec §6.15): version, sources, licences, privacy. Built in P1. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    PlaceholderScreen(title = stringResource(R.string.menu_about), onOverflowItem = onOverflowItem, onBack = onBack)
}
