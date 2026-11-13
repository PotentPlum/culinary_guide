package nl.potentplum.mise.feature.curriculum

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.potentplum.mise.content.CurriculumIndex
import nl.potentplum.mise.content.CurriculumRepository
import nl.potentplum.mise.content.model.CheckItem
import nl.potentplum.mise.content.model.CheckItemType
import nl.potentplum.mise.content.model.CourseFile
import nl.potentplum.mise.content.model.CourseModule
import nl.potentplum.mise.content.model.CourseSummary
import nl.potentplum.mise.content.model.CurriculumStage
import nl.potentplum.mise.content.model.Exercise
import nl.potentplum.mise.content.model.ExerciseSummary
import nl.potentplum.mise.content.model.Lesson
import nl.potentplum.mise.core.navigation.CheckRoute
import nl.potentplum.mise.core.navigation.CourseRoute
import nl.potentplum.mise.core.navigation.ExerciseRoute
import nl.potentplum.mise.core.navigation.LessonRoute
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.data.db.entity.CheckAttemptEntity
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.repo.CurriculumProgressRepository
import nl.potentplum.mise.data.repo.EvidenceInput
import nl.potentplum.mise.domain.CheckResult
import nl.potentplum.mise.domain.CurriculumProgress
import nl.potentplum.mise.domain.CurriculumRules
import nl.potentplum.mise.domain.NextStep
import nl.potentplum.mise.domain.StepCount
import nl.potentplum.mise.ingest.ImageStore
import java.io.File
import javax.inject.Inject

/** A screen that is loading, shows something, or names an id that isn't in the curriculum. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data object NotFound : Loadable<Nothing>

    data class Loaded<T>(
        val value: T,
    ) : Loadable<T>
}

private fun <T> Flow<Loadable<T>>.state(scope: kotlinx.coroutines.CoroutineScope): StateFlow<Loadable<T>> =
    stateIn(scope, SharingStarted.WhileSubscribed(5_000), Loadable.Loading)

// ── Home ─────────────────────────────────────────────────────────────────────

data class CourseRow(
    val course: CourseSummary,
    val steps: StepCount,
)

data class StageSection(
    /** 1 to 5. */
    val number: Int,
    val stage: CurriculumStage,
    val courses: List<CourseRow>,
    val steps: StepCount,
)

data class CurriculumHome(
    val stages: List<StageSection>,
    val overall: StepCount,
    /** Null when every step is done. */
    val next: NextStep?,
)

@HiltViewModel
class CurriculumViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        progress: CurriculumProgressRepository,
    ) : ViewModel() {
        val state: StateFlow<Loadable<CurriculumHome>> =
            combine(flow { emit(curriculum.index()) }, progress.observeProgress()) { index, done ->
                Loadable.Loaded(home(index, done))
            }.state(viewModelScope)

        private fun home(
            index: CurriculumIndex,
            progress: CurriculumProgress,
        ): CurriculumHome {
            val stages =
                index.stages.mapIndexed { i, stage ->
                    val rows = index.coursesOf(stage).map { CourseRow(it, CurriculumRules.courseSteps(it, progress)) }
                    StageSection(i + 1, stage, rows, rows.fold(StepCount(0, 0)) { sum, row -> sum + row.steps })
                }
            return CurriculumHome(
                stages = stages,
                overall = stages.fold(StepCount(0, 0)) { sum, stage -> sum + stage.steps },
                next = CurriculumRules.nextStep(index, progress),
            )
        }
    }

// ── Course ───────────────────────────────────────────────────────────────────

data class CourseDetail(
    val summary: CourseSummary,
    /** Null for milestones and the capstone, which show their rubric only. */
    val file: CourseFile?,
    val stageNumber: Int,
    val stageTitle: String,
    val prerequisites: List<CourseSummary>,
    val progress: CurriculumProgress,
    val steps: StepCount,
    /** The best percent per module id, over all sittings. */
    val bestChecks: Map<String, Int>,
)

@HiltViewModel
class CourseViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        progress: CurriculumProgressRepository,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<CourseRoute>().id

        private val loaded =
            flow {
                val index = curriculum.index()
                emit(index to index.course(id)?.let { curriculum.course(it.id) })
            }

        val state: StateFlow<Loadable<CourseDetail>> =
            combine(loaded, progress.observeProgress(), progress.observeChecks()) { (index, file), done, checks ->
                val summary = index.course(id) ?: return@combine Loadable.NotFound
                val stageIndex = index.stages.indexOfFirst { id in it.courseIds }
                Loadable.Loaded(
                    CourseDetail(
                        summary = summary,
                        file = file,
                        stageNumber = stageIndex + 1,
                        stageTitle =
                            index.stages
                                .getOrNull(stageIndex)
                                ?.titleNl
                                .orEmpty(),
                        prerequisites = summary.prerequisites.mapNotNull(index::course),
                        progress = done,
                        steps = CurriculumRules.courseSteps(summary, done),
                        bestChecks = bestChecks(checks),
                    ),
                )
            }.state(viewModelScope)
    }

internal fun bestChecks(checks: List<CheckAttemptEntity>): Map<String, Int> =
    checks.groupBy { it.moduleId }.mapValues { (_, rows) -> rows.maxOf { if (it.total == 0) 0 else it.correct * 100 / it.total } }

// ── Lesson ───────────────────────────────────────────────────────────────────

data class LessonDetail(
    val course: CourseSummary,
    val module: CourseModule,
    val lesson: Lesson,
    val competencyTitles: Map<String, String>,
    val done: Boolean,
    /** The next lesson in the same module, if any. */
    val nextLessonId: String?,
)

/** Finds a module's course file and the module itself. */
private suspend fun CurriculumRepository.module(unitId: String): Triple<CurriculumIndex, CourseSummary, CourseModule>? {
    val index = index()
    val summary = index.course(CurriculumIndex.courseIdOf(unitId)) ?: return null
    val module = course(summary.id)?.modules?.firstOrNull { it.id == CurriculumIndex.moduleIdOf(unitId) } ?: return null
    return Triple(index, summary, module)
}

@HiltViewModel
class LessonViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        private val progress: CurriculumProgressRepository,
        savedState: SavedStateHandle,
    ) : ViewModel() {
        private val id = savedState.toRoute<LessonRoute>().id

        val state: StateFlow<Loadable<LessonDetail>> =
            combine(flow { emit(curriculum.module(id)) }, progress.observeProgress()) { found, done ->
                val (index, course, module) = found ?: return@combine Loadable.NotFound
                val at = module.lessons.indexOfFirst { it.id == id }
                if (at < 0) return@combine Loadable.NotFound
                val lesson = module.lessons[at]
                Loadable.Loaded(
                    LessonDetail(
                        course = course,
                        module = module,
                        lesson = lesson,
                        competencyTitles = lesson.competencies.associate { it.id to (index.competenciesById[it.id]?.title ?: it.id) },
                        done = done.lessonDone(id),
                        nextLessonId = module.lessons.getOrNull(at + 1)?.id,
                    ),
                )
            }.state(viewModelScope)

        fun setDone(done: Boolean) {
            viewModelScope.launch { progress.setLessonDone(id, done) }
        }
    }

// ── Exercise ─────────────────────────────────────────────────────────────────

data class ExerciseDetail(
    val course: CourseSummary,
    val summary: ExerciseSummary,
    val exercise: Exercise,
    val competencyTitles: Map<String, String>,
    val evidence: List<EvidenceEntity>,
)

/** The evidence form; kept in [SavedStateHandle] so a camera round trip or process death keeps it. */
data class EvidenceForm(
    val band: Band? = null,
    val notes: String = "",
    val photoPath: String? = null,
    val saving: Boolean = false,
)

sealed interface ExerciseEvent {
    data object Logged : ExerciseEvent
}

@HiltViewModel
class ExerciseViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        private val progress: CurriculumProgressRepository,
        private val images: ImageStore,
        private val clock: MiseClock,
        private val savedState: SavedStateHandle,
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val id = savedState.toRoute<ExerciseRoute>().id

        val state: StateFlow<Loadable<ExerciseDetail>> =
            combine(flow { emit(curriculum.module(id)) }, progress.observeEvidence(id)) { found, evidence ->
                val (index, course, module) = found ?: return@combine Loadable.NotFound
                val exercise = module.exercises.firstOrNull { it.id == id } ?: return@combine Loadable.NotFound
                val summary =
                    course.modules
                        .firstOrNull { it.id == module.id }
                        ?.exercises
                        ?.firstOrNull { it.id == id } ?: return@combine Loadable.NotFound
                Loadable.Loaded(
                    ExerciseDetail(
                        course = course,
                        summary = summary,
                        exercise = exercise,
                        competencyTitles = exercise.competencies.associate { it.id to (index.competenciesById[it.id]?.title ?: it.id) },
                        evidence = evidence,
                    ),
                )
            }.state(viewModelScope)

        val form: StateFlow<EvidenceForm> =
            combine(
                savedState.getStateFlow<String?>(KEY_BAND, null),
                savedState.getStateFlow(KEY_NOTES, ""),
                savedState.getStateFlow<String?>(KEY_PHOTO, null),
                savedState.getStateFlow(KEY_SAVING, false),
            ) { band, notes, photo, saving -> EvidenceForm(band?.let(Band::valueOf), notes, photo, saving) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, EvidenceForm())

        private val _events = Channel<ExerciseEvent>(Channel.BUFFERED)
        val events: Flow<ExerciseEvent> = _events.receiveAsFlow()

        fun setBand(band: Band) {
            savedState[KEY_BAND] = band.name
        }

        fun setNotes(text: String) {
            savedState[KEY_NOTES] = text
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
                val photo = if (taken) images.finishCameraPhoto(file) else withContext(io) { file.delete() }.let { null }
                if (photo != null) replacePhoto(photo)
            }
        }

        fun picked(uri: Uri?) {
            if (uri == null) return
            viewModelScope.launch { images.importPhoto(uri)?.let(::replacePhoto) }
        }

        fun removePhoto() {
            val old = savedState.get<String>(KEY_PHOTO) ?: return
            savedState[KEY_PHOTO] = null
            viewModelScope.launch(io) { File(old).delete() }
        }

        private fun replacePhoto(path: String) {
            val old = savedState.get<String>(KEY_PHOTO)
            savedState[KEY_PHOTO] = path
            if (old != null) viewModelScope.launch(io) { File(old).delete() }
        }

        fun log() {
            val current = form.value
            val band = current.band ?: return
            if (current.saving) return
            savedState[KEY_SAVING] = true
            viewModelScope.launch {
                progress.logEvidence(EvidenceInput(id, clock.today(), band, current.notes, current.photoPath))
                savedState[KEY_BAND] = null
                savedState[KEY_NOTES] = ""
                savedState[KEY_PHOTO] = null
                savedState[KEY_SAVING] = false
                _events.send(ExerciseEvent.Logged)
            }
        }

        fun delete(evidenceId: String) {
            viewModelScope.launch { progress.deleteEvidence(evidenceId) }
        }

        private companion object {
            const val KEY_BAND = "evidenceBand"
            const val KEY_NOTES = "evidenceNotes"
            const val KEY_PHOTO = "evidencePhoto"
            const val KEY_SAVING = "evidenceSaving"
            const val KEY_CAMERA = "evidenceCamera"
        }
    }

// ── Knowledge check ──────────────────────────────────────────────────────────

/** The learner's answer to one item: an option key, or their own verdict on a model answer. */
sealed interface ItemAnswer {
    val right: Boolean

    data class Choice(
        val key: String,
        override val right: Boolean,
    ) : ItemAnswer

    data class SelfMarked(
        override val right: Boolean,
    ) : ItemAnswer
}

data class CheckSession(
    val course: CourseSummary,
    val module: CourseModule,
    val position: Int,
    val answers: Map<String, ItemAnswer>,
    /** Items whose model answer is shown (non-multiple-choice). */
    val revealed: Set<String>,
    /** Set once the learner finishes; the sitting is saved at that moment. */
    val result: CheckResult?,
) {
    val items: List<CheckItem> get() = module.items
    val current: CheckItem? get() = items.getOrNull(position)
}

@HiltViewModel
class CheckViewModel
    @Inject
    constructor(
        curriculum: CurriculumRepository,
        private val progress: CurriculumProgressRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val moduleId = savedState.toRoute<CheckRoute>().moduleId

        // Answers are kept as "id|C|key", "id|S|1" so they survive process death in the saved state.
        private val answerLines = savedState.getStateFlow(KEY_ANSWERS, arrayListOf<String>())

        val state: StateFlow<Loadable<CheckSession>> =
            combine(
                flow { emit(curriculum.module(moduleId)) },
                savedState.getStateFlow(KEY_POSITION, 0),
                answerLines,
                savedState.getStateFlow(KEY_REVEALED, arrayListOf<String>()),
                savedState.getStateFlow(KEY_FINISHED, false),
            ) { found, position, lines, revealed, finished ->
                val (_, course, module) = found ?: return@combine Loadable.NotFound
                if (module.items.isEmpty()) return@combine Loadable.NotFound
                val answers = decode(module.items, lines)
                Loadable.Loaded(
                    CheckSession(
                        course = course,
                        module = module,
                        position = position.coerceIn(0, module.items.lastIndex),
                        answers = answers,
                        revealed = revealed.toSet(),
                        result = if (finished) CheckResult.of(module.items, answers.mapValues { it.value.right }) else null,
                    ),
                )
            }.state(viewModelScope)

        private fun session(): CheckSession? = (state.value as? Loadable.Loaded)?.value

        fun choose(key: String) {
            val item = session()?.current ?: return
            if (item.type != CheckItemType.MULTIPLE_CHOICE || hasAnswer(item.id)) return
            savedState[KEY_ANSWERS] = ArrayList(answerLines.value + "${item.id}|C|$key")
        }

        fun reveal() {
            val item = session()?.current ?: return
            val revealed = savedState.get<ArrayList<String>>(KEY_REVEALED) ?: arrayListOf()
            if (item.id !in revealed) savedState[KEY_REVEALED] = ArrayList(revealed + item.id)
        }

        fun selfMark(right: Boolean) {
            val item = session()?.current ?: return
            if (item.type == CheckItemType.MULTIPLE_CHOICE || hasAnswer(item.id)) return
            savedState[KEY_ANSWERS] = ArrayList(answerLines.value + "${item.id}|S|${if (right) 1 else 0}")
        }

        fun next() {
            val session = session() ?: return
            if (session.position < session.items.lastIndex) savedState[KEY_POSITION] = session.position + 1
        }

        fun finish() {
            val session = session() ?: return
            if (session.result != null) return
            savedState[KEY_FINISHED] = true
            val result = CheckResult.of(session.items, session.answers.mapValues { it.value.right })
            viewModelScope.launch { progress.saveCheck(moduleId, result) }
        }

        fun restart() {
            savedState[KEY_ANSWERS] = arrayListOf<String>()
            savedState[KEY_REVEALED] = arrayListOf<String>()
            savedState[KEY_POSITION] = 0
            savedState[KEY_FINISHED] = false
        }

        private fun hasAnswer(itemId: String) = answerLines.value.any { it.startsWith("$itemId|") }

        private companion object {
            const val KEY_POSITION = "checkPosition"
            const val KEY_ANSWERS = "checkAnswers"
            const val KEY_REVEALED = "checkRevealed"
            const val KEY_FINISHED = "checkFinished"

            fun decode(
                items: List<CheckItem>,
                lines: List<String>,
            ): Map<String, ItemAnswer> {
                val byId = items.associateBy { it.id }
                return lines
                    .mapNotNull { line ->
                        val (id, kind, value) = line.split('|').takeIf { it.size == 3 } ?: return@mapNotNull null
                        val item = byId[id] ?: return@mapNotNull null
                        id to
                            when (kind) {
                                "C" -> ItemAnswer.Choice(value, value == item.answer)
                                else -> ItemAnswer.SelfMarked(value == "1")
                            }
                    }.toMap()
            }
        }
    }
