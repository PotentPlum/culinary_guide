package nl.potentplum.mise.feature.shopping

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Boodschappen (spec §6.14). Built in P1. */
@Composable
fun ShoppingScreen(onOverflowItem: (OverflowItem) -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.nav_shopping), onOverflowItem = onOverflowItem)
}
