package nl.potentplum.mise.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * The learner's progress through the curriculum (spec §6.17, §7.2). Rows refer to curriculum ids ("EGG-1.M2.L1"), which
 * never change (curriculum README, Conventions), so a content update keeps the progress.
 */

/** The four bands of the curriculum's rubrics (assessment §3), used here for the learner's own judgement. */
enum class Band { NOT_YET, DEVELOPING, COMPETENT, EXCELLENT }

/** A lesson the learner marked as done. */
@Entity(tableName = "curriculum_lesson")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val completedAt: Long,
)

/** One sitting of a module's knowledge check. Passed = at least 80 % and every safety item correct (assessment §4.1). */
@Entity(tableName = "curriculum_check", indices = [Index("moduleId", "takenAt")])
data class CheckAttemptEntity(
    @PrimaryKey val id: String,
    val moduleId: String,
    val takenAt: Long,
    val total: Int,
    val correct: Int,
    val safetyTotal: Int,
    val safetyCorrect: Int,
    val passed: Boolean,
    val wrongItemIds: List<String> = emptyList(),
)

/** Evidence logged for an exercise: the learner's own band, notes and an optional photo. */
@Entity(tableName = "curriculum_evidence", indices = [Index("exerciseId", "loggedAt")])
data class EvidenceEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val loggedAt: Long,
    val band: Band,
    val notes: String? = null,
    val photoPath: String? = null,
)
