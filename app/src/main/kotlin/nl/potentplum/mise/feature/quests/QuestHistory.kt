package nl.potentplum.mise.feature.quests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.difficultyLabel
import nl.potentplum.mise.core.ui.claudeErrorMessage
import nl.potentplum.mise.core.util.DutchDates
import nl.potentplum.mise.data.db.dao.QuestWithTitle
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.domain.Difficulty

/** Dagboek › Quests (spec §6.13): filters, the list, Nieuwe quest. */
@Composable
fun QuestHistory(
    onOpenQuest: (String) -> Unit,
    onNewQuest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuestHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val deletedMessage = stringResource(R.string.quest_deleted)
    val undo = stringResource(R.string.common_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is QuestHistoryEvent.Opened -> onOpenQuest(event.id)
                is QuestHistoryEvent.Deleted -> snackbar.show(deletedMessage, undo, onAction = { viewModel.restore(event.deleted) })
            }
        }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "new") {
            Button(onClick = onNewQuest, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.quest_new)) }
        }
        item(key = "generation") { GenerationRow(state, viewModel) }
        if (state.hasAny) {
            item(key = "filters") { Filters(state.filters, viewModel) }
        }
        if (!state.loading && state.quests.isEmpty()) {
            item(key = "empty") { EmptyState(title = stringResource(R.string.quest_history_empty)) }
        }
        items(state.quests, key = { it.quest.id }) { item ->
            QuestRow(
                item = item,
                canRetry = state.online && state.generation !is QuestGenState.Generating,
                onOpen = { onOpenQuest(item.quest.id) },
                onRetry = { viewModel.retry(item.quest.id) },
                onDelete = { viewModel.delete(item.quest.id) },
            )
        }
    }
}

@Composable
private fun GenerationRow(
    state: QuestHistoryUiState,
    viewModel: QuestHistoryViewModel,
) {
    when (val gen = state.generation) {
        is QuestGenState.Generating -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(questSummaryLine(gen.summary), style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.quest_generating_cancel)) }
            }
        }

        is QuestGenState.Failed -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(claudeErrorMessage(gen.error), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_done)) }
            }
        }

        QuestGenState.Idle -> {
            Unit
        }
    }
}

@Composable
private fun Filters(
    filters: QuestFilters,
    viewModel: QuestHistoryViewModel,
) {
    val all = stringResource(R.string.quest_filter_all)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ChipRow(listOf(null, QuestStatus.OPEN, QuestStatus.COMPLETED), filters.status, viewModel::setStatus) { status ->
            when (status) {
                null -> all
                QuestStatus.OPEN -> stringResource(R.string.quest_status_open)
                QuestStatus.COMPLETED -> stringResource(R.string.quest_status_completed)
            }
        }
        ChipRow(listOf<QuestType?>(null) + QuestType.entries, filters.type, viewModel::setType) {
            it?.let { t ->
                questTypeLabel(t)
            } ?: all
        }
        ChipRow(listOf<Difficulty?>(null) + Difficulty.entries, filters.difficulty, viewModel::setDifficulty) {
            it?.let { d -> difficultyLabel(d) } ?: all
        }
    }
}

@Composable
private fun <T> ChipRow(
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    // One scrolling line per filter, so the list stays in view.
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value -> FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value)) }) }
    }
}

@Composable
private fun QuestRow(
    item: QuestWithTitle,
    canRetry: Boolean,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    val quest = item.quest
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(quest.text, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft, maxLines = 2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DifficultyBadge(quest.difficulty)
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
                    Text(
                        stringResource(R.string.quest_meta, DutchDates.day(quest.createdAt), status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                }
            }
            Box {
                var menu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { menu = true },
                ) { Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.quest_row_more, item.title)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.quest_retry)) }, enabled = canRetry, onClick = {
                        menu = false
                        onRetry()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.quest_delete)) }, onClick = {
                        menu = false
                        onDelete()
                    })
                }
            }
        }
    }
}
