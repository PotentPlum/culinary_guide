package nl.potentplum.mise.feature.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.ScoreScale
import nl.potentplum.mise.core.util.DutchDates
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.AttemptRow
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.domain.DisciplineStats
import nl.potentplum.mise.domain.ScoreSteps
import nl.potentplum.mise.feature.cook.DateDialog
import java.io.File
import java.time.LocalDate

/** Dagboek › Tijdlijn (spec §6.13): attempts by month, newest first; tap to edit or open the recipe. */
@Composable
fun JournalTimeline(
    state: JournalUiState,
    onFilter: (TimelineFilter) -> Unit,
    onEdit: (AttemptRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val months = stringArrayResource(R.array.months_long)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.hasAny) {
            item(key = "filters") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TimelineFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { onFilter(filter) },
                            label = {
                                Text(
                                    stringResource(
                                        when (filter) {
                                            TimelineFilter.ALL -> R.string.journal_filter_all
                                            TimelineFilter.RECIPES -> R.string.journal_filter_recipes
                                            TimelineFilter.QUESTS -> R.string.journal_filter_quests
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (!state.loading && state.timeline.isEmpty()) {
            item(key = "empty") { EmptyState(title = stringResource(R.string.journal_empty)) }
        }
        state.timeline.forEach { month ->
            item(key = "month-${month.month}") {
                Eyebrow(
                    stringResource(R.string.journal_month, months[month.month.monthValue - 1], month.month.year),
                    Modifier.padding(top = 8.dp),
                )
            }
            items(month.entries, key = { it.attempt.id }) { row -> TimelineEntry(row, onClick = { onEdit(row) }) }
        }
    }
}

@Composable
private fun TimelineEntry(
    row: AttemptRow,
    onClick: () -> Unit,
) {
    val attempt = row.attempt
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            attempt.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.recipeTitle ?: stringResource(R.string.journal_deleted_recipe), style = MaterialTheme.typography.titleMedium)
                val kind =
                    stringResource(
                        if (attempt.sourceType ==
                            AttemptSourceType.QUEST
                        ) {
                            R.string.journal_kind_quest
                        } else {
                            R.string.journal_kind_recipe
                        },
                    )
                Text(
                    stringResource(R.string.quest_meta, DutchDates.day(attempt.cookedAt), kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
                scoreLine(attempt.score, attempt.tasteScore)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                attempt.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 4) }
            }
        }
    }
}

@Composable
private fun scoreLine(
    score: Int?,
    taste: Int?,
): String? {
    val steps = stringArrayResource(R.array.score_steps)
    val execution = stringResource(R.string.evaluation_label_score)
    val tasteLabel = stringResource(R.string.evaluation_label_taste)
    return when {
        score != null && taste != null -> {
            stringResource(
                R.string.recipe_attempt_scores,
                execution,
                steps[ScoreSteps.stepOf(score)],
                tasteLabel,
                steps[ScoreSteps.stepOf(taste)],
            )
        }

        score != null -> {
            stringResource(R.string.recipe_attempt_score, execution, steps[ScoreSteps.stepOf(score)])
        }

        taste != null -> {
            stringResource(R.string.recipe_attempt_score, tasteLabel, steps[ScoreSteps.stepOf(taste)])
        }

        else -> {
            null
        }
    }
}

/** Edits a journal entry: scores, notes, date and the photo; or deletes it, or opens its recipe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    row: AttemptRow,
    today: LocalDate,
    onSave: (score: Int?, taste: Int?, notes: String, cookedOn: LocalDate, removePhoto: Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenRecipe: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val attempt = row.attempt
    var score by rememberSaveable(attempt.id) { mutableStateOf(attempt.score) }
    var taste by rememberSaveable(attempt.id) { mutableStateOf(attempt.tasteScore) }
    var notes by rememberSaveable(attempt.id) { mutableStateOf(attempt.notes.orEmpty()) }
    var date by rememberSaveable(attempt.id) { mutableStateOf(DisciplineStats.cookedOn(attempt).toString()) }
    var removePhoto by rememberSaveable(attempt.id) { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val cookedOn = LocalDate.parse(date)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                row.recipeTitle ?: stringResource(R.string.journal_deleted_recipe),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            ScoreScale(stringResource(R.string.evaluation_label_score), score, { score = it })
            ScoreScale(stringResource(R.string.evaluation_label_taste), taste, { taste = it })
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.evaluation_label_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            attempt.photoPath?.let { path ->
                if (!removePhoto) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { removePhoto = !removePhoto }) {
                    Checkbox(checked = removePhoto, onCheckedChange = { removePhoto = it })
                    Text(stringResource(R.string.evaluation_photo_remove))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.evaluation_date_label,
                        DutchDates.day(cookedOn.atStartOfDay(MiseClock.ZONE).toInstant().toEpochMilli()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { pickingDate = true }) {
                    Icon(Icons.Outlined.Event, contentDescription = null)
                    Text(stringResource(R.string.evaluation_date_change), Modifier.padding(start = 6.dp))
                }
            }
            Button(onClick = { onSave(score, taste, notes, cookedOn, removePhoto) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recipes_edit_save_btn))
            }
            if (onOpenRecipe != null && attempt.recipeId != null) {
                OutlinedButton(onClick = { onOpenRecipe(attempt.recipeId) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.journal_open_recipe))
                }
            }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.journal_delete_entry)) }
        }
    }
    if (pickingDate) {
        DateDialog(
            selected = cookedOn,
            today = today,
            onPick = {
                date = it.toString()
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}
