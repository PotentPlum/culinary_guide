package nl.potentplum.mise.feature.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.ui.claudeErrorMessage

/** The Quest van vandaag card on Vandaag (spec §6.2, §6.11). */
@Composable
fun TodayQuestCard(
    onOpenQuest: (String) -> Unit,
    onRefine: () -> Unit,
    onAddKey: () -> Unit,
    onAllQuests: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayQuestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.opened.collect(onOpenQuest) }
    when (val current = state) {
        TodayQuestUiState.Loading, TodayQuestUiState.Hidden -> {
            Unit
        }

        TodayQuestUiState.NoKey -> {
            QuestCardFrame(modifier) {
                Text(stringResource(R.string.quest_card_no_key), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddKey) { Text(stringResource(R.string.quest_card_add_key)) }
                    TextButton(onClick = viewModel::hide) { Text(stringResource(R.string.quest_card_hide)) }
                }
            }
        }

        is TodayQuestUiState.Ready -> {
            QuestCardFrame(modifier) {
                ReadyContent(current, viewModel, onOpenQuest, onRefine, onAllQuests)
            }
        }
    }
}

@Composable
private fun QuestCardFrame(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow(stringResource(R.string.quest_daily_title))
            content()
        }
    }
}

@Composable
private fun ReadyContent(
    state: TodayQuestUiState.Ready,
    viewModel: TodayQuestViewModel,
    onOpenQuest: (String) -> Unit,
    onRefine: () -> Unit,
    onAllQuests: () -> Unit,
) {
    when (val gen = state.generation) {
        is QuestGenState.Generating -> {
            Text(questSummaryLine(gen.summary), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.quest_loading_msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.quest_generating_cancel)) }
            }
        }

        is QuestGenState.Failed -> {
            Text(claudeErrorMessage(gen.error), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::start, enabled = state.online) { Text(stringResource(R.string.quest_retry)) }
                TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_cancel)) }
            }
        }

        QuestGenState.Idle -> {
            val quest = state.quest
            if (quest != null) {
                Text(quest.text, style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DifficultyBadge(quest.difficulty)
                    Text(quest.skill, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenQuest(quest.id) }) { Text(stringResource(R.string.quest_open)) }
                    TextButton(onClick = onRefine) { Text(stringResource(R.string.quest_params_title)) }
                    TextButton(onClick = onAllQuests) { Text(stringResource(R.string.today_all_quests)) }
                }
            } else {
                Text(stringResource(R.string.quest_daily_body), style = MaterialTheme.typography.bodyMedium)
                if (!state.online) {
                    Text(
                        stringResource(R.string.quest_offline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::start, enabled = state.online) { Text(stringResource(R.string.quest_submit_btn)) }
                    TextButton(onClick = onRefine) { Text(stringResource(R.string.quest_params_title)) }
                    TextButton(onClick = onAllQuests) { Text(stringResource(R.string.today_all_quests)) }
                }
            }
        }
    }
}
