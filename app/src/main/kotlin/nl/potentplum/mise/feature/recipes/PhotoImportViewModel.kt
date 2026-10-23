package nl.potentplum.mise.feature.recipes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeRecipeReader
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.core.navigation.PhotoImportRoute
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeDrafts
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.ingest.ImageStore
import java.io.File
import javax.inject.Inject

sealed interface PhotoPhase {
    data object Idle : PhotoPhase

    data object Reading : PhotoPhase

    data class Failed(
        val error: ClaudeError,
    ) : PhotoPhase

    /** Claude's recipe in read view with Bewaren and Aanpassen (spec §6.9). */
    data class Preview(
        val draft: Recipe,
        val ingredients: List<IngredientGroupLines>,
    ) : PhotoPhase

    data object Saving : PhotoPhase
}

data class PhotoImportUiState(
    /** Downscaled JPEGs in the import folder, in reading order. */
    val photos: List<String> = emptyList(),
    val phase: PhotoPhase = PhotoPhase.Idle,
    val hasKey: Boolean = true,
) {
    val canAdd: Boolean get() = photos.size < ClaudeRecipeReader.MAX_PHOTOS && phase == PhotoPhase.Idle
    val canRead: Boolean get() = photos.isNotEmpty() && hasKey && (phase == PhotoPhase.Idle || phase is PhotoPhase.Failed)
}

/**
 * Recipe from photos (spec §6.9): 1–5 photos from the picker, the camera or a share → reorder or remove → Lees recept →
 * Claude vision → preview → Bewaren. The first photo becomes the recipe image; the rest are deleted.
 */
@HiltViewModel
class PhotoImportViewModel
    @Inject
    constructor(
        private val reader: ClaudeRecipeReader,
        private val recipes: RecipeRepository,
        private val drafts: RecipeDrafts,
        private val images: ImageStore,
        claude: ClaudeService,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedState.toRoute<PhotoImportRoute>()
        private val photos = savedState.getStateFlow(KEY_PHOTOS, emptyList<String>())
        private val phase = MutableStateFlow<PhotoPhase>(PhotoPhase.Idle)
        private val _events = Channel<ImportEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()
        private var job: Job? = null

        /** A draft handed to edit mode owns the first photo from then on; it is moved into place when saved there. */
        private var handedOff: String? = null

        val state: StateFlow<PhotoImportUiState> =
            combine(photos, phase, claude.hasKey) { photos, phase, hasKey -> PhotoImportUiState(photos, phase, hasKey) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, PhotoImportUiState())

        /** True once, for a camera entry: the screen opens the camera on first show. */
        val openCameraFirst: Boolean get() = route.camera && savedState.get<Boolean>(KEY_STARTED) != true

        init {
            if (savedState.get<Boolean>(KEY_STARTED) != true && route.shared.isNotEmpty()) add(route.shared.map { it.toUri() })
        }

        fun cameraStarted() {
            savedState[KEY_STARTED] = true
        }

        /** Picked or shared photos; anything beyond five is ignored. */
        fun add(uris: List<Uri>) {
            savedState[KEY_STARTED] = true
            viewModelScope.launch {
                for (uri in uris) {
                    if (photos.value.size >= ClaudeRecipeReader.MAX_PHOTOS) break
                    images.importPhoto(uri)?.let { setPhotos(photos.value + it) }
                }
            }
        }

        /** A file for `TakePicture`; its path survives process death so the result can still be picked up. */
        fun cameraTarget(): Uri {
            val file = images.newImportFile()
            savedState[KEY_CAMERA] = file.absolutePath
            return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }

        fun cameraResult(taken: Boolean) {
            val path = savedState.get<String>(KEY_CAMERA) ?: return
            savedState[KEY_CAMERA] = null
            viewModelScope.launch {
                val file = File(path)
                if (!taken || photos.value.size >= ClaudeRecipeReader.MAX_PHOTOS) {
                    withContext(io) { file.delete() }
                    return@launch
                }
                images.finishCameraPhoto(file)?.let { setPhotos(photos.value + it) }
            }
        }

        fun move(
            index: Int,
            delta: Int,
        ) {
            val target = index + delta
            val list = photos.value
            if (index !in list.indices || target !in list.indices) return
            setPhotos(list.toMutableList().apply { add(target, removeAt(index)) })
        }

        fun remove(index: Int) {
            val list = photos.value
            if (index !in list.indices) return
            val path = list[index]
            setPhotos(list.filterIndexed { i, _ -> i != index })
            viewModelScope.launch(io) { File(path).delete() }
        }

        /** Lees recept: one Claude call with every photo. Cancelling returns to the photos and records nothing. */
        fun read() {
            if (!state.value.canRead) return
            phase.value = PhotoPhase.Reading
            job =
                viewModelScope.launch {
                    phase.value =
                        try {
                            val jpegs = withContext(io) { photos.value.map { File(it).readBytes() } }
                            val draft = reader.fromPhotos(jpegs).copy(imagePath = photos.value.first())
                            review(draft)
                        } catch (e: ClaudeError) {
                            PhotoPhase.Failed(e)
                        }
                }
        }

        fun cancel() {
            job?.cancel()
            phase.value = PhotoPhase.Idle
        }

        /** Back from the preview to the photos. */
        fun backToPhotos() {
            if (phase.value is PhotoPhase.Preview) phase.value = PhotoPhase.Idle
        }

        fun save() {
            val draft = (phase.value as? PhotoPhase.Preview)?.draft ?: return
            phase.value = PhotoPhase.Saving
            viewModelScope.launch {
                val image =
                    withContext(io) {
                        val target = images.recipeImage(draft.id)
                        target.parentFile?.mkdirs()
                        File(photos.value.first()).copyTo(target, overwrite = true).absolutePath
                    }
                val saved = recipes.save(draft.copy(imagePath = image))
                deletePhotos()
                _events.send(ImportEvent.Saved(saved.id))
            }
        }

        fun edit() {
            val draft = (phase.value as? PhotoPhase.Preview)?.draft ?: return
            handOff(draft, highlightMissing = false)
        }

        /** A complete recipe previews in read view; a missing title, ingredients or steps open edit mode (spec §6.9). */
        private suspend fun review(draft: Recipe): PhotoPhase {
            if (ImportReview.isComplete(draft)) return PhotoPhase.Preview(draft, ImportReview.lines(recipes.scaler(), draft))
            handOff(draft, highlightMissing = true)
            return PhotoPhase.Idle
        }

        private fun handOff(
            draft: Recipe,
            highlightMissing: Boolean,
        ) {
            drafts.put(draft)
            handedOff = draft.imagePath
            _events.trySend(ImportEvent.Edit(draft.id, highlightMissing))
        }

        private fun setPhotos(list: List<String>) {
            savedState[KEY_PHOTOS] = list
        }

        private suspend fun deletePhotos() =
            withContext(io) {
                photos.value.forEach { File(it).delete() }
                setPhotos(emptyList())
            }

        /** Leaving the import deletes its photos, except the one an open draft still needs. */
        override fun onCleared() {
            val leftovers = photos.value.filter { it != handedOff }
            Thread { leftovers.forEach { File(it).delete() } }.start()
        }

        private companion object {
            const val KEY_PHOTOS = "photos"
            const val KEY_CAMERA = "camera"
            const val KEY_STARTED = "started"
        }
    }
