package nl.potentplum.mise.domain

import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class TodaySuggesterTest {
    private val content = TestContent.content
    private val vegetables = content.vegetablesById
    private val today = LocalDate.of(2026, 10, 22)

    private fun cookedDaysAgo(days: Long) =
        today
            .minusDays(days)
            .atTime(19, 0)
            .atZone(MiseClock.ZONE)
            .toInstant()
            .toEpochMilli()

    private fun item(
        id: String,
        vegIds: Set<String> = emptySet(),
        daysAgo: Long? = null,
        cooked: Int = if (daysAgo == null) 0 else 1,
        favorite: Boolean = false,
        minutes: Int? = 45,
        title: String = id,
    ) = LibraryItem(
        id = id,
        title = title,
        sourceLabel = "",
        imagePath = null,
        totalMinutes = minutes,
        isFavorite = favorite,
        createdAt = 0,
        cookedCount = cooked,
        lastCookedAt = daysAgo?.let(::cookedDaysAgo),
        ingredientTexts = emptyList(),
        vegIds = vegIds,
        cuisines = emptySet(),
        dishTypes = emptySet(),
        mainIngredients = emptySet(),
        techniques = emptySet(),
    )

    private fun rank(vararg items: LibraryItem) = TodaySuggester.rank(items.toList(), vegetables, today)

    private fun score(item: LibraryItem) = rank(item).single().score

    @Test
    fun `season score counts PEAK, the badge bonus and SHOULDER`() {
        // October: pumpkin PEAK, sweet potato PEAK with Laatste kans and Kort seizoen, parsnip SHOULDER, asparagus off.
        val tolerance = 1e-9
        assertEquals(1.0, score(item("a", setOf("pumpkin"), daysAgo = 400)) - 0.6, tolerance)
        assertEquals(1.5, score(item("b", setOf("sweet-potato"), daysAgo = 400)) - 0.6, tolerance)
        assertEquals(0.5, score(item("c", setOf("parsnip"), daysAgo = 400)) - 0.6, tolerance)
        assertEquals(0.0, score(item("d", setOf("asparagus-white"), daysAgo = 400)) - 0.6, tolerance)
        assertEquals(3.0, score(item("e", setOf("pumpkin", "sweet-potato", "parsnip", "unknown"), daysAgo = 400)) - 0.6, tolerance)
    }

    @Test
    fun `recency excludes the last week, halves the last month and rewards a long pause`() {
        val veg = setOf("pumpkin")
        assertEquals(emptyList<Suggestion>(), rank(item("a", veg, daysAgo = 0), item("b", veg, daysAgo = 7)))
        assertEquals(0.5, score(item("a", veg, daysAgo = 8)), 1e-9)
        assertEquals(0.7, score(item("a", veg, daysAgo = 30, favorite = true)), 1e-9)
        assertEquals(1.0 + 0.6 * 45 / 90, score(item("a", veg, daysAgo = 45)), 1e-9)
        assertEquals(1.6, score(item("a", veg, daysAgo = 90)), 1e-9)
        assertEquals(1.6, score(item("a", veg, daysAgo = 365)), 1e-9)
        assertEquals(1.3, score(item("a", veg)), 1e-9)
        assertEquals(1.7, score(item("a", veg, favorite = true)), 1e-9)
    }

    @Test
    fun `ties go to the fewest times cooked, then the title`() {
        val ranked =
            rank(
                item("1", daysAgo = 90, cooked = 3, title = "Appeltaart"),
                item("2", daysAgo = 120, cooked = 1, title = "Zuurkool"),
                item("3", daysAgo = 100, cooked = 1, title = "Éénpansgerecht"),
                item("4", daysAgo = 100, cooked = 1, title = "andijviestamppot"),
            )
        assertEquals(listOf("4", "3", "2", "1"), ranked.map { it.item.id })
    }

    @Test
    fun `the reason names up to two peak vegetables, the pause and the favourite`() {
        val reason =
            rank(item("a", setOf("parsnip", "pumpkin", "kale", "sweet-potato"), daysAgo = 44, favorite = true)).single().reason
        // Laatste kans first, then the ranked order; SHOULDER parsnip never counts.
        assertEquals(listOf("sweet-potato", "pumpkin"), reason.peakVegIds)
        assertEquals(6, reason.weeksNotCooked)
        assertEquals(false, reason.neverCooked)
        assertTrue(reason.favourite)

        val fresh = rank(item("b")).single().reason
        assertEquals(SuggestionReason(emptyList(), null, neverCooked = true, favourite = false), fresh)
        assertEquals(null, rank(item("c", daysAgo = 20)).single().reason.weeksNotCooked)
    }

    @Test
    fun `pick skips dismissed recipes and the pin, and refills from below`() {
        val ranked = rank(*(1..6).map { item("r$it", setOf("pumpkin"), daysAgo = 30L + it * 10) }.toTypedArray())
        assertEquals(listOf("r6", "r5", "r4"), TodaySuggester.pick(ranked, emptySet(), null, quickOnly = false).map { it.item.id })
        assertEquals(
            listOf("r6", "r4", "r2"),
            TodaySuggester.pick(ranked, setOf("r5", "r3"), pinnedId = "r1", quickOnly = false).map { it.item.id },
        )
        assertEquals(listOf("r1"), TodaySuggester.pick(ranked, setOf("r2", "r3", "r4", "r5"), "r6", false).map { it.item.id })
    }

    @Test
    fun `the quick chip keeps recipes of at most 30 minutes and hides those without a time`() {
        val ranked =
            rank(
                item("slow", setOf("kale"), minutes = 31),
                item("unknown", setOf("kale"), minutes = null),
                item("quick", minutes = 30),
                item("quicker", minutes = 10, daysAgo = 20),
            )
        assertEquals(listOf("quick", "quicker"), TodaySuggester.pick(ranked, emptySet(), null, quickOnly = true).map { it.item.id })
        assertEquals(3, TodaySuggester.pick(ranked, emptySet(), null, quickOnly = false).size)
    }

    @Test
    fun `the ranking is the same for the same day`() {
        val library = (1..40).map { n -> item("r$n", setOf(content.vegetables[n % 60].id), daysAgo = (n * 7L).takeIf { n % 3 != 0 }) }
        val first = TodaySuggester.rank(library, vegetables, today)
        assertEquals(first, TodaySuggester.rank(library.shuffled(), vegetables, today))
        assertNotEquals(first.map { it.item.id }, TodaySuggester.rank(library, vegetables, LocalDate.of(2026, 5, 1)).map { it.item.id })
    }

    @Test
    fun `curated links fill the empty slots from peak vegetables, varying by day`() {
        val october = CuratedFallback.pick(content.vegetables, today, 3)
        // Only beetroot and leek have curated links and peak in October.
        assertEquals(setOf("beetroot", "leek"), october.map { it.vegetable.id }.toSet())
        assertTrue(october.all { it.recipe in it.vegetable.curatedRecipes })
        assertEquals(october, CuratedFallback.pick(content.vegetables, today, 3))
        assertNotEquals(october, CuratedFallback.pick(content.vegetables, today.plusDays(1), 3))
        assertEquals(emptyList<CuratedSuggestion>(), CuratedFallback.pick(content.vegetables, today, 0))
        val quick = CuratedFallback.pick(content.vegetables, today, 3, quickOnly = true)
        assertTrue(quick.all { it.recipe.timeMinutes <= 30 })
    }

    @Test
    fun `a thousand recipes rank quickly`() {
        val library =
            (1..1_000).map { n ->
                item(
                    "r$n",
                    content.vegetables
                        .shuffled()
                        .take(n % 5)
                        .map { it.id }
                        .toSet(),
                    daysAgo = (n % 200L).takeIf { n % 4 != 0 },
                )
            }
        TodaySuggester.rank(library, vegetables, today)
        val millis = measureTimeMillis { TodaySuggester.rank(library, vegetables, today) }
        assertTrue("$millis ms", millis < 250)
    }
}
