package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonPresentationTest {
    private val vegetables = TestContent.content.vegetables

    @Test
    fun `september has 43 vegetables at peak`() {
        val counts = SeasonPresentation.counts(vegetables, 9)
        assertEquals(43, counts[SeasonFilter.PEAK])
        assertEquals(49, counts[SeasonFilter.AVAILABLE])
        assertEquals(7, counts[SeasonFilter.SOON])
        assertEquals(43, SeasonPresentation.filtered(vegetables, 9, SeasonFilter.PEAK).size)
    }

    @Test
    fun `the available filter is peak plus shoulder`() {
        for (month in 1..12) {
            val counts = SeasonPresentation.counts(vegetables, month)
            val shoulder = vegetables.count { Seasonality.statusFor(it, month) == SeasonStatus.SHOULDER }
            assertEquals("month $month", counts.getValue(SeasonFilter.PEAK) + shoulder, counts[SeasonFilter.AVAILABLE])
        }
    }

    @Test
    fun `laatste kans is peak now and gone next month`() {
        fun lastChance(month: Int) =
            SeasonPresentation
                .ranked(vegetables, month)
                .filter { it.lastChance }
                .map { it.vegetable.id }
                .toSet()
        assertEquals(setOf("cucumber", "corn"), lastChance(9))
        assertEquals(setOf("runner-beans", "aubergine", "artichoke", "pointed-cabbage", "sweet-potato"), lastChance(10))
        assertEquals(emptySet<String>(), lastChance(1))
    }

    @Test
    fun `laatste kans needs peak and not shoulder next month`() {
        val asparagus = TestContent.content.vegetable("asparagus-white")!!
        // Peak May–June, shoulder April: June is the last chance, May is not.
        assertTrue(SeasonPresentation.present(asparagus, 6).lastChance)
        assertFalse(SeasonPresentation.present(asparagus, 5).lastChance)
        val green = TestContent.content.vegetable("asparagus-green")!!
        // Green asparagus stays available (shoulder) in July.
        assertFalse(SeasonPresentation.present(green, 6).lastChance)
    }

    @Test
    fun `kort seizoen is at most three available months`() {
        val short = vegetables.filter { SeasonPresentation.present(it, 1).shortSeason }.map { it.id }.toSet()
        assertEquals(setOf("asparagus-white", "corn", "yacon", "sweet-potato"), short)
    }

    @Test
    fun `ranked order puts badges first within peak`() {
        val september = SeasonPresentation.ranked(vegetables, 9)
        // Corn is Laatste kans and Kort seizoen, cucumber only Laatste kans.
        assertEquals(listOf("corn", "cucumber"), september.take(2).map { it.vegetable.id })
        val october = SeasonPresentation.ranked(vegetables, 10)
        assertEquals("sweet-potato", october.first().vegetable.id)
        assertEquals(5, october.takeWhile { it.lastChance }.size)
    }

    @Test
    fun `ranked order follows the status then badges then season length then name`() {
        for (month in 1..12) {
            val ranked = SeasonPresentation.ranked(vegetables, month)
            assertEquals(vegetables.size, ranked.size)
            ranked.zipWithNext().forEach { (a, b) ->
                assertTrue("month $month: ${a.vegetable.id} before ${b.vegetable.id}", SeasonPresentation.order.compare(a, b) <= 0)
            }
            val statuses = ranked.map { it.status }
            val peakEnd = statuses.lastIndexOf(SeasonStatus.PEAK)
            val firstShoulder = statuses.indexOf(SeasonStatus.SHOULDER)
            if (peakEnd >= 0 && firstShoulder >= 0) assertTrue(peakEnd < firstShoulder)
        }
    }

    @Test
    fun `peak vegetables without badges are ordered by season length then dutch name`() {
        val plain = SeasonPresentation.ranked(vegetables, 9).filter { it.status == SeasonStatus.PEAK && !it.lastChance && !it.shortSeason }
        plain.zipWithNext().forEach { (a, b) ->
            assertTrue(a.availableMonths <= b.availableMonths)
            if (a.availableMonths == b.availableMonths) {
                assertTrue(SeasonPresentation.dutchCollator().compare(a.vegetable.nl, b.vegetable.nl) <= 0)
            }
        }
    }

    @Test
    fun `dutch collation ignores case and sorts accented letters with their base letter`() {
        val sorted = listOf("Zuring", "andijvie", "Émincé", "Biet").sortedWith(SeasonPresentation.dutchCollator())
        assertEquals(listOf("andijvie", "Biet", "Émincé", "Zuring"), sorted)
    }
}
