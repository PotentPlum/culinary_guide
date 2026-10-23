package nl.potentplum.mise.feature.recipes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.MiseSearchField
import nl.potentplum.mise.core.designsystem.component.RecipeImage
import nl.potentplum.mise.core.designsystem.component.ReorderableColumn
import nl.potentplum.mise.core.designsystem.component.moved
import nl.potentplum.mise.domain.TextMatch

/** Edit mode (spec §6.8): every field of the recipe, saved with Opslaan; leaving with changes asks first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect(onSaved) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (state.dirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.edit_new_title else R.string.recipes_edit_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = close,
                    ) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.recipes_edit_cancel_btn)) }
                },
                actions = {
                    TextButton(onClick = viewModel::save) {
                        Text(stringResource(R.string.recipes_edit_save_btn))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val editor = state.editor ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorFields(editor, state, viewModel)
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) { Text(stringResource(R.string.edit_discard_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.edit_keep_editing)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorFields(
    editor: RecipeEditorState,
    state: RecipeEditUiState,
    viewModel: RecipeEditViewModel,
) {
    val update = viewModel::update
    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::setImage) }

    // Image.
    if (editor.imagePath != null) RecipeImage(editor.imagePath, null, Modifier.fillMaxWidth().height(180.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(stringResource(if (editor.imagePath == null) R.string.edit_image_add else R.string.edit_image_replace))
        }
        if (editor.imagePath != null) TextButton(onClick = viewModel::removeImage) { Text(stringResource(R.string.edit_image_remove)) }
    }

    // Title, description, numbers, source.
    val titleError = state.showErrors && !editor.hasTitle
    OutlinedTextField(
        value = editor.title,
        onValueChange = { value -> update { it.copy(title = value) } },
        label = { Text(stringResource(R.string.recipes_edit_label_title)) },
        isError = titleError,
        supportingText = if (titleError) ({ Text(stringResource(R.string.edit_title_required)) }) else null,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = editor.description,
        onValueChange = { value -> update { it.copy(description = value) } },
        label = { Text(stringResource(R.string.recipes_edit_label_desc)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(stringResource(R.string.recipes_label_servings), editor.servings) { v -> update { it.copy(servings = v) } }
        NumberField(stringResource(R.string.recipes_label_prep_time), editor.prepMinutes) { v -> update { it.copy(prepMinutes = v) } }
        NumberField(stringResource(R.string.recipes_label_cook_time), editor.cookMinutes) { v -> update { it.copy(cookMinutes = v) } }
        NumberField(stringResource(R.string.recipes_label_total_time), editor.totalMinutes) { v -> update { it.copy(totalMinutes = v) } }
    }
    OutlinedTextField(
        value = editor.sourceLabel,
        onValueChange = { value -> update { it.copy(sourceLabel = value) } },
        label = { Text(stringResource(R.string.recipes_edit_label_source)) },
        placeholder = { Text(stringResource(R.string.recipes_placeholder_source)) },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = editor.sourceUrl,
        onValueChange = { value -> update { it.copy(sourceUrl = value) } },
        label = { Text(stringResource(R.string.edit_source_url)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    // Ingredient and step groups.
    GroupsEditor(
        title = stringResource(R.string.recipes_section_ingredients),
        linesLabel = stringResource(R.string.recipes_edit_label_ingredients),
        groups = editor.ingredientGroups,
        error = state.showErrors && !editor.hasIngredients,
        onChange = { groups -> update { it.copy(ingredientGroups = groups) } },
    )
    GroupsEditor(
        title = stringResource(R.string.recipes_section_instructions),
        linesLabel = stringResource(R.string.recipes_edit_label_instructions),
        groups = editor.stepGroups,
        error = state.showErrors && !editor.hasSteps,
        onChange = { groups -> update { it.copy(stepGroups = groups) } },
    )

    // Extra fields.
    Heading(stringResource(R.string.recipe_extra_title))
    editor.extraFields.forEach { field ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = field.label,
                onValueChange = { v ->
                    update { s ->
                        s.copy(
                            extraFields =
                                s.extraFields.map {
                                    if (it.id ==
                                        field.id
                                    ) {
                                        it.copy(label = v)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                },
                label = { Text(stringResource(R.string.edit_extra_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = field.value,
                onValueChange = { v ->
                    update { s ->
                        s.copy(
                            extraFields =
                                s.extraFields.map {
                                    if (it.id ==
                                        field.id
                                    ) {
                                        it.copy(value = v)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                },
                label = { Text(stringResource(R.string.edit_extra_value)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { update { s -> s.copy(extraFields = s.extraFields.filterNot { it.id == field.id }) } }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.edit_extra_delete))
            }
        }
    }
    TextButton(onClick = { update { it.copy(extraFields = it.extraFields + EditableField()) } }) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Text(stringResource(R.string.edit_extra_add), modifier = Modifier.padding(start = 6.dp))
    }

    // Tags.
    Heading(stringResource(R.string.recipes_edit_label_tags))
    TagChips(state.options.dishTypeTitle, state.options.dishTypes.map { it.value to it.nl }, setOfNotNull(editor.dishType)) { value ->
        update { it.copy(dishType = if (it.dishType == value) null else value) }
    }
    TagChips(state.options.titles.cuisine, state.options.cuisines.map { it.value to it.nl }, editor.cuisines) { value ->
        update { it.copy(cuisines = it.cuisines.toggle(value)) }
    }
    TagChips(
        state.options.titles.mainIngredient,
        state.options.mainIngredients.map { it.value to it.nl },
        editor.mainIngredients,
    ) { value ->
        update { it.copy(mainIngredients = it.mainIngredients.toggle(value)) }
    }
    TechniquePicker(state, editor) { value -> update { it.copy(techniques = it.techniques.toggle(value)) } }

    // Notes.
    OutlinedTextField(
        value = editor.notes,
        onValueChange = { value -> update { it.copy(notes = value) } },
        label = { Text(stringResource(R.string.recipe_notes_title)) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValue(text.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(0.47f),
    )
}

/** Groups with an optional header and their lines; reorder by dragging the handle or with the ⋮ actions. */
@Composable
private fun GroupsEditor(
    title: String,
    linesLabel: String,
    groups: List<EditableGroup>,
    error: Boolean,
    onChange: (List<EditableGroup>) -> Unit,
) {
    Heading(title)
    ReorderableColumn(groups, key = { it.id }, onMove = { from, to -> onChange(groups.moved(from, to)) }) { group, index, handle ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DragIndicator,
                        contentDescription = stringResource(R.string.edit_drag_handle),
                        modifier = handle.padding(end = 8.dp),
                    )
                    OutlinedTextField(
                        value = group.name,
                        onValueChange = { v -> onChange(groups.map { if (it.id == group.id) it.copy(name = v) else it }) },
                        placeholder = { Text(stringResource(R.string.edit_group_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    GroupMenu(
                        canMoveUp = index > 0,
                        canMoveDown = index < groups.lastIndex,
                        onMoveUp = { onChange(groups.moved(index, index - 1)) },
                        onMoveDown = { onChange(groups.moved(index, index + 1)) },
                        onDelete = { onChange(groups.filterNot { it.id == group.id }.ifEmpty { listOf(EditableGroup()) }) },
                    )
                }
                OutlinedTextField(
                    value = group.text,
                    onValueChange = { v -> onChange(groups.map { if (it.id == group.id) it.copy(text = v) else it }) },
                    label = { Text(linesLabel) },
                    isError = error && index == 0,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (error) {
        Text(
            stringResource(R.string.edit_missing_parts),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    TextButton(onClick = { onChange(groups + EditableGroup()) }) {
        Icon(Icons.Outlined.Add, contentDescription = null)
        Text(stringResource(R.string.edit_add_group), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun GroupMenu(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.menu_more)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        if (canMoveUp) {
            DropdownMenuItem(text = { Text(stringResource(R.string.edit_move_up)) }, onClick = {
                open = false
                onMoveUp()
            })
        }
        if (canMoveDown) {
            DropdownMenuItem(text = { Text(stringResource(R.string.edit_move_down)) }, onClick = {
                open = false
                onMoveDown()
            })
        }
        DropdownMenuItem(text = { Text(stringResource(R.string.edit_delete_group)) }, onClick = {
            open = false
            onDelete()
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value in selected, onClick = { onToggle(value) }, label = { Text(label) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TechniquePicker(
    state: RecipeEditUiState,
    editor: RecipeEditorState,
    onToggle: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val techniques = state.options.techniques
    Text(stringResource(R.string.recipes_filter_technique), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        techniques.filter { it.id in editor.techniques }.forEach { technique ->
            FilterChip(selected = true, onClick = { onToggle(technique.id) }, label = { Text(technique.nl) })
        }
        OutlinedButton(onClick = { open = true }) { Text(stringResource(R.string.edit_techniques_pick)) }
    }
    if (open) {
        var query by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.edit_techniques_pick)) },
            text = {
                Column {
                    MiseSearchField(
                        query = query,
                        onQuery = { query = it },
                        placeholder = stringResource(R.string.techniques_search_placeholder),
                        description = stringResource(R.string.techniques_search_aria_label),
                    )
                    LazyColumn(Modifier.height(320.dp)) {
                        items(techniques.filter { TextMatch.contains(it.nl, query) }, key = { it.id }) { technique ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = technique.id in editor.techniques, onCheckedChange = { onToggle(technique.id) })
                                Text(technique.nl)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_done)) } },
        )
    }
}
