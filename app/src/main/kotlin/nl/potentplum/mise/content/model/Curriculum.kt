package nl.potentplum.mise.content.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The culinary curriculum (spec §6.17, §7.1; `curriculum/07-app-architecture.md`), generated from `curriculum/` by
 * `tools/export_curriculum.py`. Its prose is English: the one exception to the Dutch-only content rule (spec §0 D7).
 */

/** Mirrors `data/schemas/curriculum-index.schema.json`. */
@Serializable
data class CurriculumIndexFile(
    val schemaVersion: Int,
    val stages: List<CurriculumStage>,
    val courses: List<CourseSummary>,
    val competencies: List<Competency>,
)

@Serializable
data class CurriculumStage(
    val id: String,
    val title: String,
    /** The stage's Dutch name, e.g. "Fundament"; the UI shows this one. */
    val titleNl: String,
    val question: String,
    val courseIds: List<String>,
)

@Serializable
enum class CourseKind { COURSE, MILESTONE, CAPSTONE }

/** A lesson file is a draft until a second reader has logged every safety item of its course (curriculum Phase 6 §3). */
@Serializable
enum class CourseStatus { DRAFT, FINAL }

@Serializable
enum class SupportLevel { RECIPE, FRAMEWORK, BRIEF }

@Serializable
data class CourseSummary(
    val id: String,
    val stage: String,
    val kind: CourseKind,
    val title: String,
    val hours: Double,
    val purpose: String,
    val prerequisites: List<String>,
    /** Null for milestones and the capstone, which have no lesson file. */
    val status: CourseStatus?,
    /** Whether `curriculum/courses/<id>.json` exists. */
    val hasContent: Boolean,
    val safetyItems: Int,
    val safetyItemsRead: Int,
    val modules: List<ModuleSummary>,
    /** Milestones and the capstone only. */
    val rubric: List<RubricRow>,
)

@Serializable
data class ModuleSummary(
    val id: String,
    val title: String,
    val hours: Double,
    val lessons: List<LessonSummary>,
    val exercises: List<ExerciseSummary>,
    val checkItems: Int,
    val safetyCheckItems: Int,
)

@Serializable
data class LessonSummary(
    val id: String,
    val title: String,
    val hours: Double,
    val practiceSessions: Int,
    val objectives: String,
)

@Serializable
data class ExerciseSummary(
    val id: String,
    val title: String,
    val kind: String,
    val support: SupportLevel,
)

@Serializable
data class RubricRow(
    val id: String,
    val criterion: String,
    val competencies: List<String>,
    val evidence: List<String>,
    /** Share of 100; null for a food-safety gate (pass or fail). */
    val weight: Int?,
)

@Serializable
data class Competency(
    val id: String,
    val title: String,
)

/** Mirrors `data/schemas/curriculum-course.schema.json`. */
@Serializable
data class CourseFile(
    val schemaVersion: Int,
    val id: String,
    val introduction: List<Block>,
    val modules: List<CourseModule>,
)

@Serializable
data class CourseModule(
    val id: String,
    val title: String,
    /** The lesson file's note on the knowledge check; shown when the module has no items. */
    val note: List<Block>,
    val lessons: List<Lesson>,
    val exercises: List<Exercise>,
    val items: List<CheckItem>,
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val prerequisites: List<String>,
    val competencies: List<CompetencyLevel>,
    val blocks: List<Block>,
)

@Serializable
data class Exercise(
    val id: String,
    val title: String,
    val description: String,
    val competencies: List<CompetencyLevel>,
    val blocks: List<Block>,
)

@Serializable
data class CompetencyLevel(
    val id: String,
    val level: Int,
)

@Serializable
enum class CheckItemType { MULTIPLE_CHOICE, SCENARIO, CALCULATION, IMAGE_IDENTIFICATION, ORDERING }

@Serializable
data class CheckItem(
    val id: String,
    val type: CheckItemType,
    val lessonId: String,
    val safety: Boolean,
    val question: String,
    /** Multiple choice only. */
    val options: List<CheckOption>,
    /** An option key for multiple choice; otherwise the model answer, which the learner compares with their own. */
    val answer: String,
    val why: String,
)

@Serializable
data class CheckOption(
    val key: String,
    val text: String,
)

/** Lesson text as blocks; strings keep the inline marks `**bold**`, `*italic*` and `` `code` ``. */
@Serializable
sealed interface Block {
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val text: String,
    ) : Block

    @Serializable
    @SerialName("list")
    data class Items(
        val items: List<ListItem>,
    ) : Block

    @Serializable
    @SerialName("table")
    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : Block
}

@Serializable
data class ListItem(
    val level: Int,
    /** Empty for a bullet; "1." for a numbered item. */
    val marker: String,
    val text: String,
)
