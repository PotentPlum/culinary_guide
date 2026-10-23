package nl.potentplum.mise.feature.recipes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import nl.potentplum.mise.claude.ClaudeRecipeReader
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.ErrorState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.RecipeImage
import nl.potentplum.mise.core.ui.claudeErrorMessage

/** "Camera" and "Foto's kiezen", and photos shared into Mise (spec §6.9). */
@Composable
fun PhotoImportScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onEdit: (draftId: String, highlightMissing: Boolean) -> Unit,
    onAddKey: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: PhotoImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(ClaudeRecipeReader.MAX_PHOTOS)) {
            viewModel.add(it)
        }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::cameraResult)
    val openCamera = { camera.launch(viewModel.cameraTarget()) }
    val openPicker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(viewModel) {
        if (viewModel.openCameraFirst) {
            viewModel.cameraStarted()
            openCamera()
        }
        viewModel.events.collect { event ->
            when (event) {
                is ImportEvent.Saved -> onSaved(event.id)
                is ImportEvent.Edit -> onEdit(event.draftId, event.highlightMissing)
                is ImportEvent.OpenExisting -> Unit
            }
        }
    }
    val preview = state.phase as? PhotoPhase.Preview
    BackHandler(enabled = preview != null, onBack = viewModel::backToPhotos)

    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = preview?.draft?.title ?: stringResource(R.string.photo_import_title),
                onOverflowItem = onOverflowItem,
                onBack = if (preview != null) viewModel::backToPhotos else onBack,
            )
        },
        bottomBar = {
            if (preview != null || state.phase == PhotoPhase.Saving) {
                ImportPreviewBar(enabled = preview != null, onSave = viewModel::save, onEdit = viewModel::edit)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val phase = state.phase) {
                is PhotoPhase.Preview -> ImportPreview(phase.draft, phase.ingredients)
                PhotoPhase.Saving -> Unit
                else -> PhotoForm(state, viewModel, openCamera, openPicker, onAddKey)
            }
        }
    }
}

@Composable
private fun PhotoForm(
    state: PhotoImportUiState,
    viewModel: PhotoImportViewModel,
    onCamera: () -> Unit,
    onPicker: () -> Unit,
    onAddKey: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.recipes_page_subtitle_photo),
            style = MaterialTheme.typography.bodyMedium,
            color = MiseTheme.colors.inkSoft,
        )
        if (!state.hasKey) {
            EmptyState(title = stringResource(R.string.claude_error_no_key)) {
                Button(onClick = onAddKey) { Text(stringResource(R.string.photo_import_add_key)) }
            }
        }
        if (state.photos.isNotEmpty()) {
            Eyebrow(stringResource(R.string.photo_import_count, state.photos.size, ClaudeRecipeReader.MAX_PHOTOS))
            PhotoStrip(
                photos = state.photos,
                enabled = state.phase == PhotoPhase.Idle || state.phase is PhotoPhase.Failed,
                onMove = viewModel::move,
                onRemove = viewModel::remove,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCamera, enabled = state.canAdd, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Text(stringResource(R.string.photo_import_add_camera), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onPicker, enabled = state.canAdd, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Text(stringResource(R.string.photo_import_add_gallery), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(stringResource(R.string.recipes_photo_hint), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        when (val phase = state.phase) {
            PhotoPhase.Reading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.recipes_photo_loading_msg), modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.common_cancel)) }
                }
            }

            is PhotoPhase.Failed -> {
                ErrorState(message = claudeErrorMessage(phase.error), onRetry = viewModel::read.takeIf { state.canRead })
            }

            else -> {
                Button(onClick = viewModel::read, enabled = state.canRead, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recipes_photo_submit_btn))
                }
            }
        }
    }
}

/** The photos in reading order, each with move and remove buttons (labelled for TalkBack). */
@Composable
private fun PhotoStrip(
    photos: List<String>,
    enabled: Boolean,
    onMove: (index: Int, delta: Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        photos.forEachIndexed { index, path ->
            val number = index + 1
            Card(Modifier.width(132.dp)) {
                RecipeImage(path, null, Modifier.fillMaxWidth().height(148.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onMove(index, -1) }, enabled = enabled && index > 0) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.photo_import_move_left, number),
                        )
                    }
                    IconButton(onClick = { onRemove(index) }, enabled = enabled) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.photo_import_remove, number))
                    }
                    IconButton(onClick = { onMove(index, 1) }, enabled = enabled && index < photos.lastIndex) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = stringResource(R.string.photo_import_move_right, number),
                        )
                    }
                }
            }
        }
    }
}
