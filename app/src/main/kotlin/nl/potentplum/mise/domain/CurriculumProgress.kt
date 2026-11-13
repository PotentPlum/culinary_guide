package nl.potentplum.mise.domain

import nl.potentplum.mise.content.CurriculumIndex
import nl.potentplum.mise.content.model.CheckItem
import nl.potentplum.mise.content.model.CourseSummary
import nl.potentplum.mise.content.model.ModuleSummary
import nl.potentplum.mise.data.db.entity.Band

/** What the learner has done so far (spec §6.17): everything the progress rules need, and nothing else. */
data class CurriculumProgress(
    val doneLessons: Set<String> = emptySet(),
    /** Module ids whose knowledge check was passed at least once. */
    val passedChecks: Set<String> = emptySet(),
    /** The best band logged per exercise id. */
    val bestBands: Map<String, Band> = emptyMap(),
) {
    fun lessonDone(id: String): Boolean = id in doneLessons

    fun checkPassed(moduleId: String): Boolean = moduleId in passedChecks

    /** An exercise is done once evidence is logged at Competent or above. */
    fun exerciseDone(id: String): Boolean = bestBands[id]?.let { it >= Band.COMPETENT } == true
}

/** Steps done out of the steps a module, course or stage has: lessons, knowledge checks and exercises each count as one. */
data class StepCount(
    val done: Int,
    val total: Int,
) {
    val complete: Boolean get() = total > 0 && done == total
    val started: Boolean get() = done > 0
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total

    operator fun plus(other: StepCount): StepCount = StepCount(done + other.done, total + other.total)
}

/** The learner's next open step, in curriculum order. */
sealed interface NextStep {
    val course: CourseSummary

    data class Lesson(
        override val course: CourseSummary,
        val id: String,
        val title: String,
    ) : NextStep

    data class Exercise(
        override val course: CourseSummary,
        val id: String,
        val title: String,
    ) : NextStep

    data class Check(
        override val course: CourseSummary,
        val module: ModuleSummary,
    ) : NextStep
}

object CurriculumRules {
    /** Assessment §4.1: a knowledge check is passed at 80 % with every safety item correct. */
    const val PASS_PERCENT = 80

    fun moduleSteps(
        module: ModuleSummary,
        progress: CurriculumProgress,
    ): StepCount {
        val check = if (module.checkItems > 0) 1 else 0
        val done =
            module.lessons.count { progress.lessonDone(it.id) } +
                module.exercises.count { progress.exerciseDone(it.id) } +
                if (check == 1 && progress.checkPassed(module.id)) 1 else 0
        return StepCount(done, module.lessons.size + module.exercises.size + check)
    }

    /** Milestones and the capstone have no content in the app yet, so they have no steps. */
    fun courseSteps(
        course: CourseSummary,
        progress: CurriculumProgress,
    ): StepCount =
        if (!course.hasContent) {
            StepCount(0, 0)
        } else {
            course.modules.fold(StepCount(0, 0)) { sum, module -> sum + moduleSteps(module, progress) }
        }

    fun steps(
        courses: List<CourseSummary>,
        progress: CurriculumProgress,
    ): StepCount = courses.fold(StepCount(0, 0)) { sum, course -> sum + courseSteps(course, progress) }

    /**
     * The first open step in curriculum order: within a module the lessons, then the exercises, then the check.
     * Null when everything is done.
     */
    fun nextStep(
        index: CurriculumIndex,
        progress: CurriculumProgress,
    ): NextStep? {
        for (stage in index.stages) {
            for (course in index.coursesOf(stage)) {
                if (!course.hasContent) continue
                for (module in course.modules) {
                    module.lessons.firstOrNull { !progress.lessonDone(it.id) }?.let { return NextStep.Lesson(course, it.id, it.title) }
                    module.exercises
                        .firstOrNull {
                            !progress.exerciseDone(
                                it.id,
                            )
                        }?.let { return NextStep.Exercise(course, it.id, it.title) }
                    if (module.checkItems > 0 && !progress.checkPassed(module.id)) return NextStep.Check(course, module)
                }
            }
        }
        return null
    }
}

/** The outcome of one sitting of a knowledge check. */
data class CheckResult(
    val total: Int,
    val correct: Int,
    val safetyTotal: Int,
    val safetyCorrect: Int,
    val wrongItemIds: List<String>,
) {
    /** Whole percent, rounded down, so 79.9 % never shows as a pass. */
    val percent: Int get() = if (total == 0) 0 else correct * 100 / total
    val passed: Boolean get() = total > 0 && correct * 100 >= CurriculumRules.PASS_PERCENT * total && safetyCorrect == safetyTotal

    companion object {
        /** [answers] holds, per item id, whether the learner had it right; an unanswered item counts as wrong. */
        fun of(
            items: List<CheckItem>,
            answers: Map<String, Boolean>,
        ): CheckResult {
            val right = items.filter { answers[it.id] == true }
            return CheckResult(
                total = items.size,
                correct = right.size,
                safetyTotal = items.count { it.safety },
                safetyCorrect = right.count { it.safety },
                wrongItemIds = items.filter { answers[it.id] != true }.map { it.id },
            )
        }
    }
}
