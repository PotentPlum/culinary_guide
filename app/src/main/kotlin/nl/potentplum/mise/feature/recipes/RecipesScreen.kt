package nl.potentplum.mise.feature.recipes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.PlaceholderScreen

/** Recepten (spec §6.7): the library. Built in P1. */
@Composable
fun RecipesScreen(onOverflowItem: (OverflowItem) -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.nav_recipes), onOverflowItem = onOverflowItem)
}
