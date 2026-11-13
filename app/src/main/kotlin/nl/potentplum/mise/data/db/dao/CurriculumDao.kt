package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.CheckAttemptEntity
import nl.potentplum.mise.data.db.entity.EvidenceEntity
import nl.potentplum.mise.data.db.entity.LessonProgressEntity

/** Curriculum progress (spec §6.17). The whole-table reads and deletes serve export and restore (spec §13). */
@Dao
interface CurriculumDao {
    @Query("SELECT * FROM curriculum_lesson")
    fun observeLessons(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM curriculum_lesson ORDER BY completedAt")
    suspend fun lessons(): List<LessonProgressEntity>

    @Upsert
    suspend fun upsertLessons(lessons: List<LessonProgressEntity>)

    @Query("DELETE FROM curriculum_lesson WHERE lessonId = :lessonId")
    suspend fun deleteLesson(lessonId: String)

    @Query("SELECT * FROM curriculum_check ORDER BY takenAt")
    fun observeChecks(): Flow<List<CheckAttemptEntity>>

    @Query("SELECT * FROM curriculum_check ORDER BY takenAt")
    suspend fun checks(): List<CheckAttemptEntity>

    @Upsert
    suspend fun upsertChecks(checks: List<CheckAttemptEntity>)

    @Query("SELECT * FROM curriculum_evidence ORDER BY loggedAt DESC")
    fun observeEvidence(): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM curriculum_evidence WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC")
    fun observeEvidenceFor(exerciseId: String): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM curriculum_evidence ORDER BY loggedAt")
    suspend fun evidence(): List<EvidenceEntity>

    @Upsert
    suspend fun upsertEvidence(evidence: List<EvidenceEntity>)

    @Query("DELETE FROM curriculum_evidence WHERE id = :id")
    suspend fun deleteEvidence(id: String)

    @Query("DELETE FROM curriculum_lesson")
    suspend fun deleteAllLessons()

    @Query("DELETE FROM curriculum_check")
    suspend fun deleteAllChecks()

    @Query("DELETE FROM curriculum_evidence")
    suspend fun deleteAllEvidence()
}
