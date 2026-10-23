package nl.potentplum.mise.feature.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.MiseSearchField
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.difficultyLabel
import nl.potentplum.mise.core.ui.claudeErrorMessage
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.TimeLimit
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.TextMatch

/** The Verfijn quest form (spec §6.11), pre-filled from a vegetable or technique. */
@Composable
fun QuestRefineScreen(
    onBack: () -> Unit,
    onOpenQuest: (String) -> Unit,
    onAddKey: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: QuestRefineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.opened.collect(onOpenQuest) }
    val params = state.params
    val options = state.options
    val busy = state.generation is QuestGenState.Generating
    Scaffold(
        topBar = { MiseTopAppBar(title = stringResource(R.string.quest_params_title), onOverflowItem = onOverflowItem, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.quest_page_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MiseTheme.colors.inkSoft,
            )

            FormSection(stringResource(R.string.quest_input_label)) {
                Choices(
                    listOf(IngredientMode.SURPRISE, IngredientMode.SEASONAL, IngredientMode.CUSTOM),
                    params.ingredientMode,
                    { ingredientModeLabel(it) },
                ) { mode -> viewModel.update { it.copy(ingredientMode = mode) } }
                when (params.ingredientMode) {
                    IngredientMode.SEASONAL -> {
                        PickerField(
                            label = stringResource(R.string.quest_seasonal_label),
                            value = params.vegId?.let { id -> options.vegetables.firstOrNull { it.id == id }?.nl },
                            placeholder = stringResource(R.string.quest_seasonal_label),
                            options = options.vegetables.map { it.id to it.nl },
                        ) { id -> viewModel.update { it.copy(vegId = id) } }
                    }

                    IngredientMode.CUSTOM -> {
                        OutlinedTextField(
                            value = params.ingredient,
                            onValueChange = viewModel::setIngredient,
                            label = { Text(stringResource(R.string.quest_input_label)) },
                            placeholder = { Text(stringResource(R.string.quest_input_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = state.quantityText,
                                onValueChange = viewModel::setQuantity,
                                label = { Text(stringResource(R.string.quest_quantity_label)) },
                                placeholder = { Text(stringResource(R.string.quest_quantity_placeholder)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            PickerField(
                                label = stringResource(R.string.quest_unit_label),
                                value = params.unit?.let { id -> options.units.firstOrNull { it.id == id }?.plural },
                                placeholder = stringResource(R.string.quest_no_unit),
                                options = options.units.map { it.id to it.plural },
                                modifier = Modifier.weight(1f),
                                allowNone = true,
                            ) { id -> viewModel.update { it.copy(unit = id) } }
                        }
                    }

                    IngredientMode.SURPRISE -> {
                        Unit
                    }
                }
            }

            FormSection(stringResource(R.string.quest_type_label)) {
                Choices(
                    QuestType.entries,
                    params.questType,
                    { questTypeLabel(it) },
                ) { type -> viewModel.update { it.copy(questType = type) } }
                if (params.questType == QuestType.COMPONENT) {
                    Text(
                        stringResource(R.string.quest_type_component_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                }
            }

            FormSection(stringResource(R.string.quest_difficulty_label)) {
                Choices(
                    listOf(null) + Difficulty.entries,
                    params.difficulty,
                    { it?.let { d -> difficultyLabel(d) } ?: stringResource(R.string.level_auto) },
                ) { difficulty -> viewModel.update { it.copy(difficulty = difficulty) } }
            }

            PickerField(
                label = stringResource(R.string.quest_technique_label),
                value = params.techniqueId?.let { id -> options.techniques.firstOrNull { it.id == id }?.nl },
                placeholder = stringResource(R.string.quest_technique_placeholder),
                options = options.techniques.map { it.id to it.nl },
                allowNone = true,
            ) { id -> viewModel.update { it.copy(techniqueId = id) } }

            PickerField(
                label = stringResource(R.string.quest_cuisine_label),
                value = params.cuisine,
                placeholder = stringResource(R.string.quest_cuisine_placeholder),
                options = options.cuisines.map { it to it },
                allowNone = true,
            ) { cuisine -> viewModel.update { it.copy(cuisine = cuisine) } }

            FormSection(stringResource(R.string.quest_time_limit_label)) {
                Choices(listOf<TimeLimit?>(null) + TimeLimit.entries, params.timeLimit, { timeLimitLabel(it) }) { limit ->
                    viewModel.update { it.copy(timeLimit = limit) }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Switch) { viewModel.update { it.copy(simpleAndSmall = !it.simpleAndSmall) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quest_simple_label), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.quest_simple_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                }
                Switch(checked = params.simpleAndSmall, onCheckedChange = null)
            }

            GenerateSection(state, busy, viewModel, onAddKey)
        }
    }
}

@Composable
private fun GenerateSection(
    state: QuestRefineUiState,
    busy: Boolean,
    viewModel: QuestRefineViewModel,
    onAddKey: () -> Unit,
) {
    when (val gen = state.generation) {
        is QuestGenState.Generating -> {
            Text(questSummaryLine(gen.summary), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.quest_generating_cancel)) }
        }

        is QuestGenState.Failed -> {
            Text(claudeErrorMessage(gen.error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        QuestGenState.Idle -> {
            Unit
        }
    }
    if (!busy) {
        Button(onClick = viewModel::generate, enabled = state.canGenerate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quest_submit_btn))
        }
    }
    // The button is disabled with an explanation when there is no key or no network (spec §6.11).
    if (!state.hasKey) {
        Text(stringResource(R.string.quest_card_no_key), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        OutlinedButton(onClick = onAddKey) { Text(stringResource(R.string.quest_card_add_key)) }
    } else if (!state.online) {
        Text(stringResource(R.string.quest_offline), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun <T> Choices(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value)) })
        }
    }
}

/** A field that opens a searchable list; [allowNone] adds the placeholder as the "nothing chosen" option. */
@Composable
private fun PickerField(
    label: String,
    value: String?,
    placeholder: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    allowNone: Boolean = false,
    onPick: (String?) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(value ?: placeholder, maxLines = 1, modifier = Modifier.weight(1f))
        }
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        val filtered = options.filter { TextMatch.contains(it.second, query) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (options.size > SEARCH_THRESHOLD) MiseSearchField(query, { query = it }, placeholder = label, description = label)
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        if (allowNone) {
                            item {
                                PickerRow(placeholder) {
                                    onPick(null)
                                    open = false
                                }
                            }
                        }
                        items(filtered, key = { it.first }) { (id, name) ->
                            PickerRow(name) {
                                onPick(id)
                                open = false
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

private const val SEARCH_THRESHOLD = 8

@Composable
private fun PickerRow(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
    )
}
