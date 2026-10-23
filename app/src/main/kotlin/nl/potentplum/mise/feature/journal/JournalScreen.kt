package nl.potentplum.mise.feature.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.feature.quests.QuestHistory

enum class JournalSegment(
    val label: Int,
) {
    TIMELINE(R.string.journal_tab_timeline),
    QUESTS(R.string.journal_tab_quests),
    INSIGHTS(R.string.journal_tab_insights),
}

/** Dagboek (spec §6.13): Tijdlijn · Quests · Inzichten. Quests is live; Tijdlijn and Inzichten follow in P1. */
@Composable
fun JournalScreen(
    onOverflowItem: (OverflowItem) -> Unit,
    onOpenQuest: (String) -> Unit,
    onNewQuest: () -> Unit,
    initialSegment: JournalSegment = JournalSegment.TIMELINE,
) {
    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    Scaffold(topBar = { MiseTopAppBar(title = stringResource(R.string.nav_journal), onOverflowItem = onOverflowItem) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                JournalSegment.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = segment == entry,
                        onClick = { segment = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, JournalSegment.entries.size),
                    ) { Text(stringResource(entry.label)) }
                }
            }
            when (segment) {
                JournalSegment.QUESTS -> {
                    QuestHistory(onOpenQuest = onOpenQuest, onNewQuest = onNewQuest)
                }

                JournalSegment.TIMELINE, JournalSegment.INSIGHTS -> {
                    EmptyState(
                        title = stringResource(R.string.placeholder_under_construction),
                        icon = Icons.Outlined.Construction,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
