package nl.potentplum.mise.feature.cook

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.AttemptRepository
import nl.potentplum.mise.data.repo.Evaluation
import nl.potentplum.mise.ingest.ImageStore
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** The evaluation form (spec §6.12); kept in the SavedStateHandle so rotation and process death keep it. */
@Serializable
data class EvaluationForm(
    val score: Int? = null,
    val tasteScore: Int? = null,
    val notes: String = "",
    /** ISO date. */
    val cookedOn: String,
    val photoPath: String? = null,
    val saving: Boolean = false,
) {
    val date: LocalDate get() = LocalDate.parse(cookedOn)
}

sealed interface EvaluationEvent {
    data class Logged(
        val questCompleted: Boolean,
    ) : EvaluationEvent
}

/** One per recipe sheet; see [EvaluationSheet]. */
@HiltViewModel
class EvaluationViewModel
    @Inject
    constructor(
        private val attempts: AttemptRepository,
        private val images: ImageStore,
        private val clock: MiseClock,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val json = Json { ignoreUnknownKeys = true }

        val today: LocalDate get() = clock.today()

        val form: StateFlow<EvaluationForm> =
            savedState
                .getStateFlow<String?>(KEY_FORM, null)
                .map { it?.let { text -> json.decodeFromString(EvaluationForm.serializer(), text) } ?: blank() }
                .stateIn(viewModelScope, SharingStarted.Eagerly, blank())

        private val _events = Channel<EvaluationEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private fun blank() = EvaluationForm(cookedOn = clock.today().toString())

        private fun update(change: (EvaluationForm) -> EvaluationForm) {
            savedState[KEY_FORM] = json.encodeToString(EvaluationForm.serializer(), change(form.value))
        }

        fun setScore(value: Int?) = update { it.copy(score = value) }

        fun setTaste(value: Int?) = update { it.copy(tasteScore = value) }

        fun setNotes(text: String) = update { it.copy(notes = text) }

        fun setDate(date: LocalDate) = update { it.copy(cookedOn = minOf(date, clock.today()).toString()) }

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
                val photo = if (taken) images.finishCameraPhoto(file) else withContext(io) { file.delete() }.let { null }
                if (photo != null) replacePhoto(photo)
            }
        }

        fun picked(uri: Uri?) {
            if (uri == null) return
            viewModelScope.launch { images.importPhoto(uri)?.let(::replacePhoto) }
        }

        fun removePhoto() {
            val old = form.value.photoPath ?: return
            update { it.copy(photoPath = null) }
            viewModelScope.launch(io) { File(old).delete() }
        }

        private fun replacePhoto(path: String) {
            val old = form.value.photoPath
            update { it.copy(photoPath = path) }
            if (old != null) viewModelScope.launch(io) { File(old).delete() }
        }

        /** Vastleggen, or "Zonder cijfers vastleggen" with [withScores] false. */
        fun log(
            recipeId: String,
            withScores: Boolean,
        ) {
            val current = form.value
            if (current.saving) return
            update { it.copy(saving = true) }
            viewModelScope.launch {
                val attempt =
                    attempts.log(
                        recipeId,
                        Evaluation(
                            cookedOn = current.date,
                            score = current.score.takeIf { withScores },
                            tasteScore = current.tasteScore.takeIf { withScores },
                            notes = current.notes,
                            stagedPhotoPath = current.photoPath,
                        ),
                    )
                savedState[KEY_FORM] = null
                if (attempt != null) _events.send(EvaluationEvent.Logged(questCompleted = attempt.questId != null))
            }
        }

        /** Dismissing logs nothing; a staged photo is dropped. */
        fun discard() {
            form.value.photoPath?.let { path -> viewModelScope.launch(io) { File(path).delete() } }
            savedState[KEY_FORM] = null
        }

        private companion object {
            const val KEY_FORM = "evaluationForm"
            const val KEY_CAMERA = "evaluationCamera"
        }
    }
