package nl.potentplum.mise.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.CurriculumDao
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.data.db.entity.CheckAttemptEntity
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.db.entity.LessonProgressEntity
import nl.potentplum.mise.domain.CheckResult
import nl.potentplum.mise.domain.CurriculumProgress
import nl.potentplum.mise.ingest.ImageStore
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What the evidence form hands over (spec §6.17). */
data class EvidenceInput(
    val exerciseId: String,
    val loggedOn: LocalDate,
    val band: Band,
    val notes: String? = null,
    /** A photo staged in the import folder; moved to the evidence folder when logged. */
    val stagedPhotoPath: String? = null,
)

/** The learner's curriculum progress (spec §6.17, §7.2). */
@Singleton
class CurriculumProgressRepository
    @Inject
    constructor(
        private val dao: CurriculumDao,
        private val images: ImageStore,
        private val clock: MiseClock,
    ) {
        fun observeProgress(): Flow<CurriculumProgress> =
            combine(dao.observeLessons(), dao.observeChecks(), dao.observeEvidence()) { lessons, checks, evidence ->
                CurriculumProgress(
                    doneLessons = lessons.mapTo(mutableSetOf()) { it.lessonId },
                    passedChecks = checks.filter { it.passed }.mapTo(mutableSetOf()) { it.moduleId },
                    bestBands = evidence.groupBy { it.exerciseId }.mapValues { (_, rows) -> rows.maxOf { it.band } },
                )
            }

        fun observeChecks(): Flow<List<CheckAttemptEntity>> = dao.observeChecks()

        fun observeEvidence(exerciseId: String): Flow<List<EvidenceEntity>> = dao.observeEvidenceFor(exerciseId)

        suspend fun setLessonDone(
            lessonId: String,
            done: Boolean,
        ) {
            if (done) dao.upsertLessons(listOf(LessonProgressEntity(lessonId, clock.nowMillis()))) else dao.deleteLesson(lessonId)
        }

        suspend fun saveCheck(
            moduleId: String,
            result: CheckResult,
        ) {
            dao.upsertChecks(
                listOf(
                    CheckAttemptEntity(
                        id = UUID.randomUUID().toString(),
                        moduleId = moduleId,
                        takenAt = clock.nowMillis(),
                        total = result.total,
                        correct = result.correct,
                        safetyTotal = result.safetyTotal,
                        safetyCorrect = result.safetyCorrect,
                        passed = result.passed,
                        wrongItemIds = result.wrongItemIds,
                    ),
                ),
            )
        }

        suspend fun logEvidence(input: EvidenceInput): String {
            val id = UUID.randomUUID().toString()
            val photoPath = input.stagedPhotoPath?.let { images.saveEvidencePhoto(id, it) }
            dao.upsertEvidence(
                listOf(
                    EvidenceEntity(
                        id = id,
                        exerciseId = input.exerciseId,
                        loggedAt = loggedAt(input.loggedOn),
                        band = input.band,
                        notes = input.notes?.trim()?.takeIf { it.isNotEmpty() },
                        photoPath = photoPath,
                    ),
                ),
            )
            return id
        }

        suspend fun deleteEvidence(id: String) {
            dao.deleteEvidence(id)
            images.evidencePhoto(id).delete()
        }

        /** Today's evidence keeps the time of logging, so entries of one day stay in order; another day is logged at noon. */
        private fun loggedAt(day: LocalDate): Long =
            if (day == clock.today()) {
                clock.nowMillis()
            } else {
                day
                    .atTime(12, 0)
                    .atZone(ZONE)
                    .toInstant()
                    .toEpochMilli()
            }

        private companion object {
            val ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")
        }
    }
