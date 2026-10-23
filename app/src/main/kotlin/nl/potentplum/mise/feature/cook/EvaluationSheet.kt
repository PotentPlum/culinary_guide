package nl.potentplum.mise.feature.cook

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.ScoreScale
import nl.potentplum.mise.core.util.DutchDates
import nl.potentplum.mise.core.util.MiseClock
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The evaluation sheet, "Gekookt" (spec §6.12), shown by every screen that ends a cook: recipe detail, cook mode and the quest
 * screen. Logging completes the recipe's quest wherever it is opened from; dismissing logs nothing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EvaluationSheet(
    recipeId: String,
    onDismiss: () -> Unit,
    onLogged: () -> Unit,
    viewModel: EvaluationViewModel = hiltViewModel(key = "evaluation-$recipeId"),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EvaluationEvent.Logged -> {
                    val message = if (event.questCompleted) R.string.quest_completed_msg else R.string.evaluation_logged
                    snackbar.show(resources.getString(message))
                    onLogged()
                }
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::picked)
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val dismiss = {
        viewModel.discard()
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.evaluation_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            ScoreScale(stringResource(R.string.evaluation_label_score), form.score, viewModel::setScore)
            ScoreScale(stringResource(R.string.evaluation_label_taste), form.tasteScore, viewModel::setTaste)
            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::setNotes,
                label = { Text(stringResource(R.string.evaluation_label_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.evaluation_photo_label), style = MaterialTheme.typography.titleSmall)
            val photo = form.photoPath
            if (photo != null) {
                Box {
                    AsyncImage(
                        model = File(photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                    )
                    FilledTonalIconButton(onClick = viewModel::removePhoto, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.evaluation_photo_remove))
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { camera.launch(viewModel.cameraTarget()) }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.evaluation_photo_camera), Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.evaluation_photo_gallery), Modifier.padding(start = 6.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.evaluation_date_label, DutchDates.day(form.date.toEpochMillis())),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { pickingDate = true }) {
                    Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.evaluation_date_change), Modifier.padding(start = 6.dp))
                }
            }
            Button(onClick = { viewModel.log(recipeId, withScores = true) }, enabled = !form.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.evaluation_submit_btn))
            }
            TextButton(
                onClick = { viewModel.log(recipeId, withScores = false) },
                enabled = !form.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.evaluation_skip_btn)) }
        }
    }

    if (pickingDate) {
        DateDialog(
            selected = form.date,
            today = viewModel.today,
            onPick = {
                viewModel.setDate(it)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(
    selected: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // The picker works in UTC midnights.
    val latest = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latest

                    override fun isSelectableYear(year: Int) = year <= today.year
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    ) { DatePicker(state) }
}

private fun LocalDate.toEpochMillis(): Long = atTime(12, 0).atZone(MiseClock.ZONE).toInstant().toEpochMilli()
