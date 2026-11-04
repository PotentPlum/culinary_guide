package nl.potentplum.mise.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar

/**
 * "Op boodschappenlijst" (spec §6.8, §6.14): the recipe's ingredients scaled to [servings] (null = the recipe's own), all
 * ticked. Untick what is already in the house and add the rest; the snackbar confirms and the sheet closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToShoppingSheet(
    recipeId: String,
    servings: Int?,
    onDismiss: () -> Unit,
    viewModel: AddToShoppingViewModel = hiltViewModel(key = "add-to-shopping-$recipeId-$servings"),
) {
    LaunchedEffect(viewModel) { viewModel.open(recipeId, servings) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddToShoppingEvent.Added -> {
                    snackbar.show(resources.getQuantityString(R.plurals.shopping_added, event.count, event.count))
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.recipe_add_to_shopping),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (state.lineCount > 0) {
                    TextButton(onClick = viewModel::toggleAll) {
                        Text(stringResource(if (state.allSelected) R.string.shopping_pick_none else R.string.shopping_pick_all))
                    }
                }
            }
            servings?.let {
                Text(
                    pluralStringResource(R.plurals.shopping_pick_servings, it, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiseTheme.colors.inkSoft,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (!state.loading && state.lineCount == 0) {
                Text(stringResource(R.string.shopping_pick_empty), modifier = Modifier.padding(horizontal = 16.dp))
            }
            LazyColumn(Modifier.weight(1f, fill = false)) {
                state.groups.forEachIndexed { index, group ->
                    group.name?.let { name ->
                        item(key = "group-$index") { Eyebrow(name, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)) }
                    }
                    group.lines.forEach { line ->
                        item(key = line.id) {
                            val checked = line.id !in state.excluded
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = { viewModel.toggle(line.id) })
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(line.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
            Button(
                onClick = viewModel::add,
                enabled = state.selectedCount > 0 && !state.adding,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(pluralStringResource(R.plurals.shopping_pick_add, state.selectedCount, state.selectedCount))
            }
        }
    }
}
