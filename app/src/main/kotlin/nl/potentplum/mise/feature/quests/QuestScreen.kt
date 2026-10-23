package nl.potentplum.mise.feature.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MenuEntry
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.ui.claudeErrorMessage
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.feature.recipes.RecipeBody
import nl.potentplum.mise.feature.recipes.RecipeDetailUiState
import nl.potentplum.mise.feature.recipes.Section

/** A quest and its full recipe (spec §6.11): Start koken, Quest voltooid, Andere quest, ⋮ Bewaar recept. */
@Composable
fun QuestScreen(
    onBack: () -> Unit,
    onCook: (recipeId: String) -> Unit,
    onCompleted: (recipeId: String) -> Unit,
    onReplaced: (questId: String) -> Unit,
    onTechnique: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: QuestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val savedMessage = stringResource(R.string.quest_recipe_saved)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is QuestEvent.Replaced -> onReplaced(event.id)
                QuestEvent.SavedToLibrary -> snackbar.show(savedMessage)
            }
        }
    }
    val quest = state.quest
    val recipe = state.recipe
    val saveEntry = MenuEntry(stringResource(R.string.quest_save_recipe), viewModel::saveToLibrary)
    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = recipe?.title ?: stringResource(R.string.quest_page_title),
                onOverflowItem = onOverflowItem,
                onBack = onBack,
                menuEntries = if (recipe != null && !recipe.inLibrary) listOf(saveEntry) else emptyList(),
            )
        },
        bottomBar = {
            if (quest != null && recipe != null) QuestActionBar(state, viewModel, { onCook(recipe.id) }, { onCompleted(recipe.id) })
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.notFound || quest == null || recipe == null -> {
                    EmptyState(title = stringResource(R.string.error_not_found))
                }

                else -> {
                    RecipeBody(
                        state =
                            RecipeDetailUiState(
                                loading = false,
                                recipe = recipe,
                                ingredients = state.ingredients,
                                techniques = state.techniques,
                            ),
                        recipe = recipe,
                        onServings = null,
                        onToggle = null,
                        onAddToShopping = null,
                        onTechnique = onTechnique,
                        leading = { item(key = "quest") { QuestHeader(quest, state.vegetableName) } },
                        trailing = {
                            if (quest.tips.isNotEmpty()) {
                                item(key = "quest-tips") {
                                    Section(stringResource(R.string.quest_label_recipe_tips)) {
                                        quest.tips.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestHeader(
    quest: QuestEntity,
    vegetableName: String?,
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val status =
            stringResource(
                if (quest.status ==
                    QuestStatus.COMPLETED
                ) {
                    R.string.quest_status_completed
                } else {
                    R.string.quest_status_open
                },
            )
        Eyebrow(stringResource(R.string.quest_meta, stringResource(R.string.quest_page_title), status))
        Text(quest.text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DifficultyBadge(quest.difficulty)
            Labeled(stringResource(R.string.quest_label_skill), quest.skill)
            if (quest.timeEstimateMinutes > 0) {
                Labeled(stringResource(R.string.quest_label_time), stringResource(R.string.minutes_short, quest.timeEstimateMinutes))
            }
            if (quest.dishType.isNotEmpty()) Labeled(stringResource(R.string.quest_label_output_type), quest.dishType)
            vegetableName?.let { Labeled(stringResource(R.string.quest_ingredient_seasonal), it) }
        }
        if (quest.why.isNotEmpty()) {
            Text(stringResource(R.string.quest_label_why), style = MaterialTheme.typography.titleMedium)
            Text(quest.why, style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Labeled(
    label: String,
    value: String,
) {
    Column {
        Eyebrow(label)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuestActionBar(
    state: QuestUiState,
    viewModel: QuestViewModel,
    onCook: () -> Unit,
    onCompleted: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            when (val gen = state.generation) {
                is QuestGenState.Generating -> {
                    Text(questSummaryLine(gen.summary), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.quest_generating_cancel)) }
                }

                is QuestGenState.Failed -> {
                    Text(claudeErrorMessage(gen.error), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::another, enabled = state.online) { Text(stringResource(R.string.quest_retry)) }
                        TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_cancel)) }
                    }
                }

                QuestGenState.Idle -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onCook, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.quest_start_cooking)) }
                        OutlinedButton(onClick = onCompleted, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.quest_completed_btn))
                        }
                    }
                    TextButton(onClick = viewModel::another, enabled = state.online) {
                        Text(stringResource(R.string.quest_try_another_btn))
                    }
                    if (!state.online) {
                        Text(
                            stringResource(R.string.quest_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiseTheme.colors.inkSoft,
                        )
                    }
                }
            }
        }
    }
}
