package nl.potentplum.mise.content

import nl.potentplum.mise.content.model.Block
import nl.potentplum.mise.content.model.CheckItemType
import nl.potentplum.mise.content.model.CourseKind
import nl.potentplum.mise.testing.TestCurriculum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CurriculumParserTest {
    private val index = TestCurriculum.index

    @Test
    fun `the index parses strictly with the validator's counts`() {
        assertEquals(5, index.stages.size)
        assertEquals(50, index.courses.size)
        assertEquals(45, index.courses.count { it.hasContent })
        assertEquals(4, index.courses.count { it.kind == CourseKind.MILESTONE })
        assertEquals(1, index.courses.count { it.kind == CourseKind.CAPSTONE })
        assertEquals(listOf("Fundament", "Kerntechnieken", "Integratie", "Gevorderd", "Meesterschap"), index.stages.map { it.titleNl })
        assertEquals(index.courses.size, index.stages.sumOf { it.courseIds.size })
    }

    @Test
    fun `every course file parses strictly and matches its summary`() {
        var items = 0
        var safety = 0
        index.courses.filter { it.hasContent }.forEach { summary ->
            val course = TestCurriculum.course(summary.id)
            assertEquals(summary.id, course.id)
            assertEquals(summary.modules.map { it.id }, course.modules.map { it.id })
            course.modules.zip(summary.modules).forEach { (module, moduleSummary) ->
                assertEquals(moduleSummary.lessons.map { it.id }, module.lessons.map { it.id })
                assertEquals(moduleSummary.exercises.map { it.id }, module.exercises.map { it.id })
                assertEquals(moduleSummary.checkItems, module.items.size)
                module.items.forEach { item ->
                    assertEquals(item.id, CurriculumIndex.moduleIdOf(item.id), CurriculumIndex.moduleIdOf(item.lessonId))
                    if (item.type == CheckItemType.MULTIPLE_CHOICE) {
                        assertTrue(item.id, item.options.size >= 2 && item.options.any { it.key == item.answer })
                    } else {
                        assertTrue(item.id, item.options.isEmpty() && item.answer.isNotBlank())
                    }
                }
            }
            items += course.modules.sumOf { it.items.size }
            safety += course.modules.sumOf { m -> m.items.count { it.safety } }
        }
        assertEquals(1394, items)
        assertEquals(278, safety)
    }

    @Test
    fun `blocks keep their kinds`() {
        val blocks = TestCurriculum.course("EGG-1").modules.flatMap { m -> m.lessons.flatMap { it.blocks } }
        assertTrue(blocks.any { it is Block.Paragraph })
        assertTrue(blocks.any { it is Block.Items })
    }

    @Test
    fun `milestones and the capstone have a rubric and no content`() {
        val capstone = index.course("CAP-5")!!
        assertTrue(capstone.rubric.isNotEmpty())
        assertTrue(!capstone.hasContent)
        assertNull(capstone.status)
    }

    @Test
    fun `ids map to their course and module`() {
        assertEquals("EGG-1", CurriculumIndex.courseIdOf("EGG-1.M2.L1"))
        assertEquals("EGG-1.M2", CurriculumIndex.moduleIdOf("EGG-1.M2.X1"))
        assertEquals("EGG-1.M2", CurriculumIndex.moduleIdOf("EGG-1.M2"))
    }

    @Test
    fun `a newer schema version is refused in strict mode and warned about otherwise`() {
        val text = """{"schemaVersion": 2, "stages": [], "courses": [], "competencies": []}"""
        try {
            CurriculumParser(strict = true).index(text)
            fail("expected UnsupportedContentException")
        } catch (_: UnsupportedContentException) {
        }
        val warnings = mutableListOf<String>()
        CurriculumParser(strict = false, onWarning = { warnings += it }).index(text)
        assertEquals(1, warnings.size)
    }
}
