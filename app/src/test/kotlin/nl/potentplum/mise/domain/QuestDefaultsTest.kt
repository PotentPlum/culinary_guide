package nl.potentplum.mise.domain

import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestDefaultsTest {
    private val content = TestContent.content
    private val today = LocalDate.of(2026, 10, 22)

    private fun millis(date: LocalDate) =
        date
            .atTime(19, 0)
            .atZone(MiseClock.ZONE)
            .toInstant()
            .toEpochMilli()

    private fun attempt(
        daysAgo: Long,
        vegIds: List<String> = emptyList(),
        techniques: List<String> = emptyList(),
    ) = AttemptEntity(
        id = "a$daysAgo${vegIds.hashCode()}${techniques.hashCode()}",
        sourceType = AttemptSourceType.RECIPE,
        cookedAt = millis(today.minusDays(daysAgo)),
        vegIds = vegIds,
        techniques = techniques,
    )

    private fun quest(
        day: Int,
        difficulty: Difficulty = Difficulty.INTERMEDIATE,
        score: Int? = null,
        completed: Boolean = true,
    ) = PastQuest(
        createdAt = millis(today.minusDays(100L - day)),
        completedAt = if (completed) millis(today.minusDays(100L - day)) + 1 else null,
        difficulty = difficulty,
        skill = "skill $day",
        title = "titel $day",
        score = score,
    )

    private val octoberPeak =
        SeasonPresentation.ranked(content.vegetables, 10).filter { it.status == SeasonStatus.PEAK }.map { it.vegetable.id }

    @Test
    fun `candidates are PEAK vegetables in the ranked order, minus those cooked in the last 60 days`() {
        assertEquals(octoberPeak, QuestDefaults.vegetableCandidates(today, content, emptyList()))
        val cooked = octoberPeak.take(2)
        val attempts = listOf(attempt(10, cooked.take(1)), attempt(60, cooked.drop(1)), attempt(61, octoberPeak.drop(2).take(1)))
        assertEquals(octoberPeak.drop(2), QuestDefaults.vegetableCandidates(today, content, attempts))
    }

    @Test
    fun `fewer than three fresh PEAK vegetables fall back to all PEAK`() {
        val attempts = listOf(attempt(5, octoberPeak.dropLast(2)))
        assertEquals(octoberPeak, QuestDefaults.vegetableCandidates(today, content, attempts))
    }

    @Test
    fun `no PEAK vegetables fall back to everything in season`() {
        val noPeak =
            Content(
                content.vegetables.map { veg -> if (10 in veg.peak) veg.copy(peak = veg.peak - 10, shoulder = veg.shoulder + 10) else veg },
                content.techniques,
                content.facets,
                content.disciplines,
                content.archetypes,
                content.units,
            )
        val available = SeasonPresentation.ranked(noPeak.vegetables, 10).filter { it.status.isAvailable }.map { it.vegetable.id }
        assertTrue(available.size >= octoberPeak.size)
        assertEquals(available, QuestDefaults.vegetableCandidates(today, noPeak, emptyList()))
    }

    @Test
    fun `focus is the most neglected discipline`() {
        // Nothing cooked: all neglected, ties keep the disciplines.json order.
        assertEquals(DisciplineId.MECHANICAL, QuestDefaults.focusDiscipline(today, content, emptyList()))
        // Flavor was never practised.
        val some = listOf(attempt(10, techniques = listOf("julienne")), attempt(40, techniques = listOf("sauteren")))
        assertEquals(DisciplineId.FLAVOR, QuestDefaults.focusDiscipline(today, content, some))
        // Both neglected: the one practised longest ago.
        val old =
            listOf(
                attempt(1, techniques = listOf("julienne")),
                attempt(31, techniques = listOf("sauteren")),
                attempt(45, techniques = listOf("pekelen")),
            )
        assertEquals(DisciplineId.FLAVOR, QuestDefaults.focusDiscipline(today, content, old))
    }

    @Test
    fun `without neglect the focus is the discipline with the fewest attempts in 30 days`() {
        val attempts =
            listOf(
                attempt(1, techniques = listOf("julienne")),
                attempt(2, techniques = listOf("brunoise")),
                attempt(3, techniques = listOf("sauteren", "pekelen")),
                attempt(4, techniques = listOf("schroeien")),
                attempt(5, techniques = listOf("zout-timing")),
                // Cookware guides and unknown ids match nothing.
                attempt(6, techniques = listOf("nope")),
            )
        assertEquals(DisciplineId.MECHANICAL, QuestDefaults.focusDiscipline(today, content, attempts.drop(1)))
        val moreCutsAndFlavour =
            attempts.dropLast(1) + attempt(7, techniques = listOf("batonnet")) + attempt(8, techniques = listOf("pekelen"))
        assertEquals(DisciplineId.THERMAL, QuestDefaults.focusDiscipline(today, content, moreCutsAndFlavour))
    }

    @Test
    fun `difficulty adapts to the last five scored completions`() {
        assertEquals(Difficulty.BEGINNER, QuestDefaults.difficulty(emptyList()))
        assertEquals(Difficulty.BEGINNER, QuestDefaults.difficulty(listOf(quest(1, score = null), quest(2, score = 10, completed = false))))

        val good = (1..5).map { quest(it, Difficulty.INTERMEDIATE, score = 8) }
        assertEquals(Difficulty.ADVANCED, QuestDefaults.difficulty(good))
        assertEquals(Difficulty.ADVANCED, QuestDefaults.difficulty(good.map { it.copy(difficulty = Difficulty.ADVANCED) }))

        val poor = (1..5).map { quest(it, Difficulty.INTERMEDIATE, score = if (it == 5) 6 else 4) }
        assertEquals(Difficulty.BEGINNER, QuestDefaults.difficulty(poor)) // average 4.4
        assertEquals(Difficulty.BEGINNER, QuestDefaults.difficulty(poor.map { it.copy(difficulty = Difficulty.BEGINNER) }))

        val middling = listOf(quest(1, Difficulty.BEGINNER, 6), quest(2, Difficulty.ADVANCED, 6))
        assertEquals(Difficulty.ADVANCED, QuestDefaults.difficulty(middling)) // the most recent level, unchanged

        // Only the last five count: an old failure doesn't pull the average down.
        val withOld = good + quest(0, Difficulty.INTERMEDIATE, score = 2)
        assertEquals(Difficulty.ADVANCED, QuestDefaults.difficulty(withOld))
        assertEquals(Difficulty.INTERMEDIATE, QuestDefaults.difficulty(good.drop(1) + quest(6, Difficulty.INTERMEDIATE, score = 2)))
    }

    @Test
    fun `history is the ten newest quests`() {
        val quests = (1..12).map { quest(it) }.shuffled()
        assertEquals((12 downTo 3).map { "titel $it" }, QuestDefaults.history(quests).map { it.title })
    }

    @Test
    fun `today's vegetable is stable within a day and moves on the next`() {
        val first = QuestDefaults.choose(today, content, emptyList(), emptyList())
        assertEquals(first, QuestDefaults.choose(today, content, emptyList(), emptyList()))
        assertTrue(first.vegId in first.vegCandidates)
        assertNotEquals(first.vegId, QuestDefaults.choose(today.plusDays(1), content, emptyList(), emptyList()).vegId)
        assertNotEquals(
            first.vegId,
            QuestDefaults.choose(today, content, emptyList(), emptyList(), pick = today.toEpochDay().toInt() + 1).vegId,
        )
        assertEquals(Difficulty.BEGINNER, first.difficulty)
    }
}
