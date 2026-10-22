package nl.potentplum.mise.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreStepsTest {
    @Test
    fun `steps are stored as 2 4 6 8 10`() {
        assertEquals(listOf(2, 4, 6, 8, 10), (0 until ScoreSteps.COUNT).map(ScoreSteps::valueOf))
    }

    @Test
    fun `stored values round trip`() {
        for (step in 0 until ScoreSteps.COUNT) assertEquals(step, ScoreSteps.stepOf(ScoreSteps.valueOf(step)))
    }

    @Test
    fun `legacy values show the nearest step`() {
        val expected = mapOf(1 to 0, 2 to 0, 3 to 1, 4 to 1, 5 to 2, 6 to 2, 7 to 3, 8 to 3, 9 to 4, 10 to 4)
        for ((value, step) in expected) assertEquals("value $value", step, ScoreSteps.stepOf(value))
    }

    @Test
    fun `out of range values are clamped`() {
        assertEquals(0, ScoreSteps.stepOf(0))
        assertEquals(4, ScoreSteps.stepOf(11))
    }
}
