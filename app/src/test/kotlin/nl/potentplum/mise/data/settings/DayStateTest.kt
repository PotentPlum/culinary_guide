package nl.potentplum.mise.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayStateTest {
    private val day = LocalDate.of(2026, 12, 31)

    @Test
    fun `day state is valid only on its own date, also across the year boundary`() {
        assertTrue(TonightPin("2026-12-31", "r").isValidOn(day))
        assertFalse(TonightPin("2026-12-31", "r").isValidOn(day.plusDays(1)))
        assertFalse(TodayDismissed("2026-12-30", listOf("r")).isValidOn(day))
    }

    @Test
    fun `a cook session expires after exactly 24 hours`() {
        val session = CookSession(recipeId = "r", stepIndex = 0, updatedAt = 1_000)
        assertFalse(session.isExpired(1_000 + CookSession.MAX_AGE_MILLIS))
        assertTrue(session.isExpired(1_000 + CookSession.MAX_AGE_MILLIS + 1))
    }
}
