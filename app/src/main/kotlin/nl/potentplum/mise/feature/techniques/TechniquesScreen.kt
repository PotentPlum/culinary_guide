package nl.potentplum.mise.feature.techniques

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Technieken (spec §6.6). Built in P1. */
@Composable
fun TechniquesScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    PlaceholderScreen(title = stringResource(R.string.nav_techniques), onOverflowItem = onOverflowItem, onBack = onBack)
}
