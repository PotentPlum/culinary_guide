package nl.potentplum.mise.feature.curriculum

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.FilterPillOption
import nl.potentplum.mise.core.designsystem.component.FilterPills
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.util.DutchDates
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import java.io.File

/**
 * An exercise (spec §6.17): the brief, then evidence the learner logs in one of the four bands with notes and a photo.
 * This is the curriculum's own assessment; quests and the journal score stay as they are (spec §0 D8).
 */
@Composable
fun ExerciseScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: ExerciseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExerciseEvent.Logged -> snackbar.show(resources.getString(R.string.curriculum_evidence_logged))
            }
        }
    }
    val title =
        (state as? Loadable.Loaded)
            ?.value
            ?.exercise
            ?.id
            .orEmpty()
    CurriculumScaffold(title, state, onBack, onOverflowItem) { detail ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(listOf(detail.course.id, stringResource(detail.summary.support.labelRes)).joinToString(" · "))
                Text(detail.exercise.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                Text(
                    stringResource(R.string.curriculum_language_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            }
            if (detail.exercise.description.isNotBlank()) {
                Text(inlineMarks(detail.exercise.description), style = MaterialTheme.typography.bodyLarge)
            }
            Blocks(detail.exercise.blocks)
            Competencies(detail.exercise.competencies, detail.competencyTitles)
            EvidenceForm(form, viewModel)
            if (detail.evidence.isEmpty()) {
                Text(
                    stringResource(R.string.curriculum_evidence_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
            } else {
                detail.evidence.forEach { EvidenceCard(it, onDelete = { viewModel.delete(it.id) }) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EvidenceForm(
    form: EvidenceForm,
    viewModel: ExerciseViewModel,
) {
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::picked)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.curriculum_evidence_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.curriculum_evidence_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MiseTheme.colors.inkSoft,
        )
        Text(stringResource(R.string.curriculum_evidence_band), style = MaterialTheme.typography.titleSmall)
        FilterPills(
            options = Band.entries.map { FilterPillOption<Band?>(it, stringResource(it.labelRes)) },
            selected = form.band,
            onSelect = { it?.let(viewModel::setBand) },
        )
        OutlinedTextField(
            value = form.notes,
            onValueChange = viewModel::setNotes,
            label = { Text(stringResource(R.string.curriculum_evidence_notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
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
        Button(onClick = viewModel::log, enabled = form.band != null && !form.saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.curriculum_evidence_save))
        }
    }
}

@Composable
private fun EvidenceCard(
    evidence: EvidenceEntity,
    onDelete: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.Transparent, border = BorderStroke(1.dp, MiseTheme.colors.line)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(evidence.band.labelRes), style = MaterialTheme.typography.titleSmall)
                    Text(DutchDates.day(evidence.loggedAt), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.curriculum_evidence_delete))
                }
            }
            evidence.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            evidence.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}
