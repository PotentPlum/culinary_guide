package nl.potentplum.mise.feature.shopping

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MenuEntry
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.domain.ShoppingListLine

/** Boodschappen (spec §6.14): one-handed in the shop. Combined lines A–Z with source chips, then "Los". */
@Composable
fun ShoppingScreen(
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val context = LocalContext.current
    val removedMessage = stringResource(R.string.shopping_removed)
    val undo = stringResource(R.string.common_undo)
    val shareTitle = stringResource(R.string.shopping_share_title)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingEvent.Removed -> snackbar.show(removedMessage, undo, onAction = { viewModel.restore(event.removed) })
            }
        }
    }
    val menu =
        buildList {
            if (state.hasChecked) add(MenuEntry(stringResource(R.string.shopping_clear_checked), viewModel::clearChecked))
            if (!state.list.isEmpty) add(MenuEntry(stringResource(R.string.shopping_clear_all), viewModel::clearAll))
        }

    Scaffold(
        topBar = {
            MiseTopAppBar(
                // The tab says "Lijstje" (room); the screen says what it is.
                title = stringResource(R.string.shopping_title),
                onOverflowItem = onOverflowItem,
                menuEntries = menu,
                actions = {
                    if (!state.list.isEmpty) {
                        IconButton(onClick = {
                            val send =
                                Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, ShoppingViewModel.shareText(shareTitle, state.list))
                            context.startActivity(Intent.createChooser(send, shareTitle))
                        }) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.shopping_share)) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "add") { AddItemField(onAdd = viewModel::addManual) }
            if (!state.loading && state.list.isEmpty) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.shopping_empty_title),
                        body = stringResource(R.string.shopping_empty_sub),
                        icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    )
                }
            }
            if (state.list.combined.isNotEmpty()) {
                // One recipe: name it once under the heading. Several: each line shows which recipes it is for.
                val sources =
                    state.list.combined
                        .flatMap { it.recipes }
                        .distinct()
                item(key = "combined-header") {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Eyebrow(stringResource(R.string.shopping_section_recipes))
                        sources.singleOrNull()?.let {
                            Text(
                                stringResource(R.string.shopping_single_recipe, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MiseTheme.colors.inkSoft,
                            )
                        }
                    }
                }
                items(state.list.combined, key = { it.key }) { line ->
                    LineRow(
                        line,
                        showRecipes = sources.size > 1,
                        onToggle = { viewModel.toggle(line) },
                        onDelete = { viewModel.delete(line) },
                    )
                }
            }
            if (state.list.manual.isNotEmpty()) {
                item(key = "manual-header") { Eyebrow(stringResource(R.string.shopping_section_manual), Modifier.padding(top = 12.dp)) }
                items(state.list.manual, key = { it.key }) { line ->
                    LineRow(line, onToggle = { viewModel.toggle(line) }, onDelete = { viewModel.delete(line) })
                }
            }
        }
    }
}

@Composable
private fun AddItemField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.shopping_add_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = submit, enabled = text.isNotBlank()) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.shopping_add))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Swipe or ✕ to delete (with undo, spec §15.3); tap anywhere on the row to tick. */
@Composable
private fun LineRow(
    line: ShoppingListLine,
    showRecipes: Boolean = true,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        backgroundContent = {},
        onDismiss = { onDelete() },
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = line.checked, onValueChange = { onToggle() }, role = Role.Checkbox)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = line.checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (line.checked) TextDecoration.LineThrough else null,
                        color = if (line.checked) MiseTheme.colors.inkSoft else MaterialTheme.colorScheme.onSurface,
                    )
                    if (showRecipes && line.recipes.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            line.recipes.forEach { title ->
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MiseTheme.colors.inkSoft,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.shopping_delete_item, line.text))
                }
            }
        }
    }
}
