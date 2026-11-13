package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.CheckItem
import nl.potentplum.mise.content.model.CheckItemType
import nl.potentplum.mise.data.db.entity.Band
import nl.potentplum.mise.testing.TestCurriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumRulesTest {
    private val index = TestCurriculum.index

    private fun item(
        id: String,
        safety: Boolean = false,
    ) = CheckItem(id, CheckItemType.SCENARIO, "X-1.M1.L1", safety, "q", emptyList(), "a", "w")

    @Test
    fun `a check passes at 80 percent with every safety item right`() {
        val items = (1..10).map { item("Q$it", safety = it == 1) }
        val eight = (1..8).associate { "Q$it" to true }
        assertTrue(CheckResult.of(items, eight).passed)
        assertEquals(80, CheckResult.of(items, eight).percent)

        val seven = (1..7).associate { "Q$it" to true }
        assertFalse(CheckResult.of(items, seven).passed)

        val nineWithoutSafety = (2..10).associate { "Q$it" to true }
        val result = CheckResult.of(items, nineWithoutSafety)
        assertEquals(90, result.percent)
        assertFalse(result.passed)
        assertEquals(listOf("Q1"), result.wrongItemIds)
    }

    @Test
    fun `percent rounds down and an empty check never passes`() {
        val items = (1..6).map { item("Q$it") }
        val five = (1..5).associate { "Q$it" to true }
        assertEquals(83, CheckResult.of(items, five).percent)
        assertFalse(CheckResult.of(emptyList(), emptyMap()).passed)
        // 4 of 5 is exactly 80 %.
        assertTrue(CheckResult.of(items.take(5), (1..4).associate { "Q$it" to true }).passed)
    }

    @Test
    fun `an exercise counts as done from Competent up`() {
        val progress = CurriculumProgress(bestBands = mapOf("A" to Band.DEVELOPING, "B" to Band.COMPETENT, "C" to Band.EXCELLENT))
        assertFalse(progress.exerciseDone("A"))
        assertTrue(progress.exerciseDone("B"))
        assertTrue(progress.exerciseDone("C"))
        assertFalse(progress.exerciseDone("D"))
    }

    @Test
    fun `the next step starts with the first lesson and follows lessons, exercises, then the check`() {
        val first = index.coursesOf(index.stages.first()).first { it.hasContent }
        val module = first.modules.first()
        val step = CurriculumRules.nextStep(index, CurriculumProgress())
        assertEquals(NextStep.Lesson(first, module.lessons.first().id, module.lessons.first().title), step)

        val lessonsDone = CurriculumProgress(doneLessons = module.lessons.mapTo(mutableSetOf()) { it.id })
        val afterLessons = CurriculumRules.nextStep(index, lessonsDone)
        if (module.exercises.isNotEmpty()) {
            assertEquals(module.exercises.first().id, (afterLessons as NextStep.Exercise).id)
        }
        val exercisesDone = lessonsDone.copy(bestBands = module.exercises.associate { it.id to Band.COMPETENT })
        val afterExercises = CurriculumRules.nextStep(index, exercisesDone)
        if (module.checkItems > 0) assertEquals(module, (afterExercises as NextStep.Check).module)
    }

    @Test
    fun `everything done leaves no next step and full step counts`() {
        val courses = index.courses.filter { it.hasContent }
        val modules = courses.flatMap { it.modules }
        val all =
            CurriculumProgress(
                doneLessons = modules.flatMapTo(mutableSetOf()) { m -> m.lessons.map { it.id } },
                passedChecks = modules.mapTo(mutableSetOf()) { it.id },
                bestBands = modules.flatMap { it.exercises }.associate { it.id to Band.EXCELLENT },
            )
        assertNull(CurriculumRules.nextStep(index, all))
        val steps = CurriculumRules.steps(index.courses, all)
        assertTrue(steps.complete)
        assertEquals(StepCount(0, steps.total), CurriculumRules.steps(index.courses, CurriculumProgress()))
        // Milestones count no steps.
        assertEquals(StepCount(0, 0), CurriculumRules.courseSteps(index.course("MS-S1")!!, all))
    }
}
