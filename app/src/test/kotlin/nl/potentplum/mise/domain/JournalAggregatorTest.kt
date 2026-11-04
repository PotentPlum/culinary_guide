package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.BadgeRule
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Spec §12. The first cases are ported from `web/lib/profileAggregation.test.ts@e76778c`, adapted to the three
 * disciplines (no "material"); the rest cover the seasonal metric and every badge rule at and around its threshold.
 */
class JournalAggregatorTest {
    private val content = TestContent.content
    private val today = LocalDate.of(2026, 10, 22)
    private var next = 0

    private fun attempt(
        on: LocalDate = today,
        techniques: List<String> = emptyList(),
        score: Int? = null,
        cuisines: List<String> = emptyList(),
        peak: List<String> = emptyList(),
    ) = AttemptEntity(
        id = "a${next++}",
        sourceType = AttemptSourceType.RECIPE,
        cookedAt =
            on
                .atTime(18, 0)
                .atZone(MiseClock.ZONE)
                .toInstant()
                .toEpochMilli(),
        score = score,
        techniques = techniques,
        cuisines = cuisines,
        vegIds = peak,
        peakVegIds = peak,
    )

    private fun insights(attempts: List<AttemptEntity>) = JournalAggregator.insights(attempts, questsCompleted = 0, content, today)

    // ── Ported from the web app ──────────────────────────────────────────────

    @Test
    fun `empty attempts return sensible defaults`() {
        val stats = insights(emptyList())
        assertEquals(listOf(DisciplineId.MECHANICAL, DisciplineId.THERMAL, DisciplineId.FLAVOR), stats.disciplines.map { it.discipline })
        stats.disciplines.forEach {
            assertEquals(0, it.count)
            assertNull(it.avgScore)
        }
        assertTrue(stats.topCuisines.isEmpty())
        assertTrue(stats.topTechniques.isEmpty())
        assertEquals(3, stats.neglected.size)
        assertEquals(0, stats.totalAttempts)
    }

    @Test
    fun `counts discipline frequency and average execution score`() {
        val stats =
            insights(
                listOf(
                    attempt(techniques = listOf("julienne", "brunoise"), score = 8),
                    attempt(techniques = listOf("julienne"), score = 6),
                    attempt(techniques = listOf("sauteren"), score = 7),
                    attempt(techniques = listOf("roux", "hollandaise"), score = 9),
                    // A cookware guide has no discipline and never matches.
                    attempt(techniques = listOf("koekenpan"), score = 2),
                ),
            )
        val byId = stats.disciplines.associateBy { it.discipline }
        assertEquals(2, byId.getValue(DisciplineId.MECHANICAL).count)
        assertEquals(7.0, byId.getValue(DisciplineId.MECHANICAL).avgScore!!, 1e-9)
        assertEquals(1, byId.getValue(DisciplineId.THERMAL).count)
        assertEquals(7.0, byId.getValue(DisciplineId.THERMAL).avgScore!!, 1e-9)
        assertEquals(1, byId.getValue(DisciplineId.FLAVOR).count)
        assertEquals(9.0, byId.getValue(DisciplineId.FLAVOR).avgScore!!, 1e-9)
    }

    @Test
    fun `average ignores attempts without a score`() {
        val stats = insights(listOf(attempt(techniques = listOf("julienne"), score = 8), attempt(techniques = listOf("julienne"))))
        val mechanical = stats.disciplines.first { it.discipline == DisciplineId.MECHANICAL }
        assertEquals(2, mechanical.count)
        assertEquals(8.0, mechanical.avgScore!!, 1e-9)
    }

    @Test
    fun `neglected disciplines are those without an attempt in the last 30 days`() {
        val stats =
            insights(
                listOf(
                    attempt(on = today.minusDays(5), techniques = listOf("julienne")),
                    attempt(on = today.minusDays(40), techniques = listOf("sauteren")),
                ),
            )
        assertFalse(DisciplineId.MECHANICAL in stats.neglected)
        assertTrue(DisciplineId.THERMAL in stats.neglected)
        assertTrue(DisciplineId.FLAVOR in stats.neglected)
    }

    @Test
    fun `top cuisines sort by count and keep five`() {
        val attempts = (0 until 15).map { attempt(cuisines = listOf("keuken-${it % 7}")) }
        val top = insights(attempts).topCuisines
        assertEquals(5, top.size)
        assertEquals(listOf(3, 2, 2, 2, 2), top.map { it.count })
        assertEquals("keuken-0", top.first().value)
    }

    @Test
    fun `top techniques count attempts per technique and keep ten`() {
        val attempts =
            listOf(attempt(techniques = listOf("julienne", "roux")), attempt(techniques = listOf("julienne"))) +
                (1..12).map { attempt(techniques = listOf("t$it")) }
        val top = insights(attempts).topTechniques
        assertEquals(10, top.size)
        assertEquals(CountedValue("julienne", 2), top.first())
    }

    @Test
    fun `header stats count this month and in total`() {
        val stats =
            JournalAggregator.insights(
                listOf(
                    attempt(on = today),
                    attempt(on = today.withDayOfMonth(1)),
                    attempt(on = today.minusMonths(1)),
                    attempt(on = today.minusYears(1)),
                ),
                questsCompleted = 4,
                content = content,
                today = today,
            )
        assertEquals(2, stats.attemptsThisMonth)
        assertEquals(4, stats.totalAttempts)
        assertEquals(4, stats.questsCompleted)
    }

    // ── Dit seizoen ──────────────────────────────────────────────────────────

    @Test
    fun `seasons start on the first of March, June, September and December`() {
        assertEquals(LocalDate.of(2026, 3, 1), CookingSeason.startOf(LocalDate.of(2026, 3, 1)))
        assertEquals(LocalDate.of(2026, 3, 1), CookingSeason.startOf(LocalDate.of(2026, 5, 31)))
        assertEquals(LocalDate.of(2026, 6, 1), CookingSeason.startOf(LocalDate.of(2026, 8, 31)))
        assertEquals(LocalDate.of(2026, 9, 1), CookingSeason.startOf(LocalDate.of(2026, 11, 30)))
        assertEquals(LocalDate.of(2026, 12, 1), CookingSeason.startOf(LocalDate.of(2026, 12, 1)))
        assertEquals(LocalDate.of(2025, 12, 1), CookingSeason.startOf(LocalDate.of(2026, 1, 15)))
        assertEquals(LocalDate.of(2025, 12, 1), CookingSeason.startOf(LocalDate.of(2026, 2, 28)))
        assertEquals(CookingSeason.WINTER, CookingSeason.of(1))
        assertEquals(CookingSeason.HERFST, CookingSeason.of(10))
    }

    @Test
    fun `the season count uses distinct peak vegetables since the season started`() {
        val march = LocalDate.of(2026, 3, 1)
        val attempts =
            listOf(
                attempt(on = march, peak = listOf("leek")),
                attempt(on = march.plusDays(3), peak = listOf("leek", "rhubarb")),
                // The last day of winter does not count in spring.
                attempt(on = march.minusDays(1), peak = listOf("kale")),
            )
        val season = JournalAggregator.season(attempts, content, march.plusDays(10))
        assertEquals(CookingSeason.LENTE, season.season)
        assertEquals(setOf("leek", "rhubarb"), season.cookedVegIds.toSet())
    }

    @Test
    fun `winter wraps from December into January`() {
        val january = LocalDate.of(2027, 1, 10)
        val attempts =
            listOf(
                attempt(on = LocalDate.of(2026, 12, 1), peak = listOf("kale")),
                attempt(on = LocalDate.of(2026, 11, 30), peak = listOf("pumpkin")),
                attempt(on = LocalDate.of(2027, 1, 5), peak = listOf("leek")),
            )
        val season = JournalAggregator.season(attempts, content, january)
        assertEquals(CookingSeason.WINTER, season.season)
        assertEquals(setOf("kale", "leek"), season.cookedVegIds.toSet())
    }

    @Test
    fun `nog te proberen is PEAK this month, not cooked this season, in the ranked order`() {
        val cooked =
            SeasonPresentation
                .ranked(content.vegetables, 10)
                .first { it.status == SeasonStatus.PEAK }
                .vegetable.id
        val season = JournalAggregator.season(listOf(attempt(on = today, peak = listOf(cooked))), content, today)
        val expected =
            SeasonPresentation
                .ranked(content.vegetables, 10)
                .filter { it.status == SeasonStatus.PEAK && it.vegetable.id != cooked }
                .map { it.vegetable.id }
        assertEquals(expected, season.toTry)
        assertFalse(cooked in season.toTry)
        // Laatste kans comes first.
        val first = SeasonPresentation.present(content.vegetable(season.toTry.first())!!, 10)
        val anyLastChance = expected.any { SeasonPresentation.present(content.vegetable(it)!!, 10).lastChance }
        if (anyLastChance) assertTrue(first.lastChance)
    }

    // ── Badges ───────────────────────────────────────────────────────────────

    private fun progress(
        rule: BadgeRule,
        attempts: List<AttemptEntity>,
    ) = BadgeEvaluator.progress(rule, attempts, content)

    @Test
    fun `discipline badge counts matching attempts around its threshold`() {
        val rule = BadgeRule.Techniques(discipline = DisciplineId.MECHANICAL, minAttempts = 8)
        assertEquals(BadgeProgress.MissingAttempts(8), progress(rule, emptyList()))
        assertEquals(BadgeProgress.MissingAttempts(1), progress(rule, List(7) { attempt(techniques = listOf("julienne")) }))
        assertEquals(BadgeProgress.Earned, progress(rule, List(8) { attempt(techniques = listOf("julienne")) }))
        // Other disciplines don't count.
        assertEquals(BadgeProgress.MissingAttempts(8), progress(rule, List(9) { attempt(techniques = listOf("roux")) }))
    }

    @Test
    fun `technique-id badge counts attempts with any listed technique`() {
        val rule = BadgeRule.Techniques(techniqueIds = listOf("schroeien", "gratineren"), minAttempts = 5)
        val four =
            List(2) { attempt(techniques = listOf("schroeien")) } + List(2) { attempt(techniques = listOf("gratineren", "schroeien")) }
        assertEquals(BadgeProgress.MissingAttempts(1), progress(rule, four + attempt(techniques = listOf("sauteren"))))
        assertEquals(BadgeProgress.Earned, progress(rule, four + attempt(techniques = listOf("gratineren"))))
    }

    @Test
    fun `minimum average score is checked on non-null execution scores`() {
        val rule = BadgeRule.Techniques(discipline = DisciplineId.FLAVOR, minAttempts = 6, minAvgScore = 7.0)

        fun six(score: Int?) = List(6) { attempt(techniques = listOf("roux"), score = score) }
        assertEquals(BadgeProgress.MissingScore(null, 7.0), progress(rule, six(null)))
        assertEquals(BadgeProgress.MissingScore(6.0, 7.0), progress(rule, six(6)))
        assertEquals(BadgeProgress.Earned, progress(rule, six(null).take(5) + attempt(techniques = listOf("roux"), score = 8)))
        assertEquals(BadgeProgress.Earned, progress(rule, six(7)))
        // Enough attempts come first.
        assertEquals(BadgeProgress.MissingAttempts(1), progress(rule, six(10).take(5)))
    }

    @Test
    fun `distinct peak vegetables around the threshold`() {
        val rule = BadgeRule.DistinctPeakVegetables(min = 15)
        val ids = content.vegetables.map { it.id }
        assertEquals(
            BadgeProgress.MissingPeakVegetables(1),
            progress(
                rule,
                ids.take(14).map { attempt(peak = listOf(it)) } + attempt(peak = listOf(ids[0])),
            ),
        )
        assertEquals(BadgeProgress.Earned, progress(rule, listOf(attempt(peak = ids.take(10)), attempt(peak = ids.subList(5, 15)))))
    }

    @Test
    fun `peak months count distinct calendar months with a peak vegetable`() {
        val rule = BadgeRule.PeakMonths(min = 12)
        val elevenMonths = (1..11).map { attempt(on = LocalDate.of(2026, it, 10), peak = listOf("leek")) }
        // December without a peak vegetable doesn't count; the same month in another year doesn't either.
        val noPeak = attempt(on = LocalDate.of(2026, 12, 10))
        val sameMonth = attempt(on = LocalDate.of(2025, 1, 10), peak = listOf("kale"))
        assertEquals(BadgeProgress.MissingPeakMonths(1), progress(rule, elevenMonths + noPeak + sameMonth))
        assertEquals(BadgeProgress.Earned, progress(rule, elevenMonths + attempt(on = LocalDate.of(2025, 12, 3), peak = listOf("kale"))))
    }

    @Test
    fun `every archetype in the assets is evaluated`() {
        val badges = BadgeEvaluator.evaluate(content.archetypes, emptyList(), content)
        assertEquals(content.archetypes.map { it.id }, badges.map { it.archetype.id })
        assertTrue(badges.none { it.earned })
    }
}
