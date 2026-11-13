package nl.potentplum.mise.feature.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
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

/** Dagboek (spec §6.13): Tijdlijn · Quests · Inzichten. */
@Composable
fun JournalScreen(
    onOverflowItem: (OverflowItem) -> Unit,
    onOpenQuest: (String) -> Unit,
    onNewQuest: () -> Unit,
    onRecipe: (String) -> Unit = {},
    onVegetable: (String) -> Unit = {},
    onAllVegetables: () -> Unit = {},
    onTechnique: (String) -> Unit = {},
    onToday: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel(),
    initialSegment: JournalSegment = JournalSegment.TIMELINE,
    /** A segment another screen asked for ("Alle quests" on Vandaag); cleared through [onSegmentRequestHandled]. */
    requestedSegment: JournalSegment? = null,
    onSegmentRequestHandled: () -> Unit = {},
) {
    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = LocalMiseSnackbar.current
    val deletedMessage = stringResource(R.string.journal_entry_deleted)
    val undo = stringResource(R.string.common_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is JournalEvent.Deleted -> {
                    snackbar.show(
                        deletedMessage,
                        undo,
                        onAction = { viewModel.restore(event.attempt) },
                        onDismiss = { viewModel.discardPhoto(event.attempt) },
                    )
                }
            }
        }
    }
    LaunchedEffect(requestedSegment) {
        if (requestedSegment != null) {
            segment = requestedSegment
            onSegmentRequestHandled()
        }
    }
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

                JournalSegment.TIMELINE -> {
                    JournalTimeline(state, onFilter = viewModel::setFilter, onEdit = { editing = it.attempt.id }, onToday = onToday)
                }

                JournalSegment.INSIGHTS -> {
                    JournalInsightsView(state, onVegetable = onVegetable, onAllVegetables = onAllVegetables, onTechnique = onTechnique)
                }
            }
        }
    }

    val row = editing?.let { id -> state.timeline.flatMap { it.entries }.firstOrNull { it.attempt.id == id } }
    if (row != null) {
        EditEntrySheet(
            row = row,
            today = state.today,
            onSave = { score, taste, notes, cookedOn, removePhoto ->
                viewModel.update(row.attempt.id, score, taste, notes, cookedOn, removePhoto)
                editing = null
            },
            onDelete = {
                viewModel.delete(row.attempt.id)
                editing = null
            },
            onOpenRecipe =
                row.recipeTitle?.let {
                    { id: String ->
                        editing = null
                        onRecipe(id)
                    }
                },
            onDismiss = { editing = null },
        )
    }
}
