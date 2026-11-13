package nl.potentplum.mise.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.potentplum.mise.BuildConfig
import nl.potentplum.mise.content.model.Competency
import nl.potentplum.mise.content.model.CourseFile
import nl.potentplum.mise.content.model.CourseSummary
import nl.potentplum.mise.content.model.CurriculumIndexFile
import nl.potentplum.mise.content.model.CurriculumStage
import nl.potentplum.mise.core.util.IoDispatcher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** The curriculum's structure (spec §7.1): small enough to keep in memory, unlike the course texts. */
class CurriculumIndex(
    val stages: List<CurriculumStage>,
    val courses: List<CourseSummary>,
    val competencies: List<Competency>,
) {
    val coursesById: Map<String, CourseSummary> = courses.associateBy { it.id }
    val competenciesById: Map<String, Competency> = competencies.associateBy { it.id }

    fun course(id: String): CourseSummary? = coursesById[id]

    fun coursesOf(stage: CurriculumStage): List<CourseSummary> = stage.courseIds.mapNotNull(coursesById::get)

    companion object {
        /** "EGG-1.M2.L1", "EGG-1.M2.X1" and "EGG-1.M2" all belong to course "EGG-1". */
        fun courseIdOf(unitId: String): String = unitId.substringBefore('.')

        /** "EGG-1.M2.L1" → "EGG-1.M2". */
        fun moduleIdOf(unitId: String): String = unitId.split('.').take(2).joinToString(".")
    }
}

interface CurriculumRepository {
    /** Loads the index once, off the main thread. */
    suspend fun index(): CurriculumIndex

    /** One course's lessons, exercises and knowledge checks; null when the course has no content (or doesn't exist). */
    suspend fun course(id: String): CourseFile?
}

/** Parses the curriculum assets; strict like [ContentParser] (unknown keys and versions throw in debug builds and tests). */
class CurriculumParser(
    private val strict: Boolean,
    private val onWarning: (String) -> Unit = {},
) {
    private val json = Json { ignoreUnknownKeys = !strict }

    fun index(text: String): CurriculumIndex {
        val file = decode(INDEX, text, CurriculumIndexFile.serializer())
        return CurriculumIndex(file.stages, file.courses, file.competencies)
    }

    fun course(
        id: String,
        text: String,
    ): CourseFile = decode(coursePath(id), text, CourseFile.serializer())

    private fun <T> decode(
        file: String,
        text: String,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val version =
            json
                .parseToJsonElement(text)
                .jsonObject["schemaVersion"]
                ?.jsonPrimitive
                ?.int
        if (version != SUPPORTED_SCHEMA_VERSION) {
            val message = "$file has schemaVersion $version; this build supports $SUPPORTED_SCHEMA_VERSION"
            if (strict) throw UnsupportedContentException(message) else onWarning(message)
        }
        return json.decodeFromString(deserializer, text)
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val INDEX = "curriculum/index.json"

        fun coursePath(id: String): String = "curriculum/courses/$id.json"
    }
}

@Singleton
class AssetCurriculumRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    ) : CurriculumRepository {
        private val parser = CurriculumParser(strict = BuildConfig.DEBUG, onWarning = { Timber.w(it) })
        private val mutex = Mutex()
        private var index: CurriculumIndex? = null

        /** The courses last opened; a course file is a few hundred kilobytes, so only a handful stay in memory. */
        private val courses = LinkedHashMap<String, CourseFile>()

        override suspend fun index(): CurriculumIndex =
            index ?: mutex.withLock {
                index ?: withContext(dispatcher) { parser.index(read(CurriculumParser.INDEX)) }.also { index = it }
            }

        override suspend fun course(id: String): CourseFile? {
            if (index().course(id)?.hasContent != true) return null
            return mutex.withLock {
                courses.remove(id)?.also { courses[id] = it }
                    ?: withContext(dispatcher) { parser.course(id, read(CurriculumParser.coursePath(id))) }.also {
                        courses[id] = it
                        if (courses.size > CACHED_COURSES) courses.remove(courses.keys.first())
                    }
            }
        }

        private fun read(path: String): String =
            context.assets
                .open(path)
                .bufferedReader()
                .use { it.readText() }

        private companion object {
            const val CACHED_COURSES = 4
        }
    }
