package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonalMatcherTest {
    private val matcher = SeasonalMatcher(TestContent.content.vegetables)

    private fun named(vararg names: String) = matcher.vegetableIds(names.map { IngredientRef(it, it) })

    private fun text(vararg lines: String) = matcher.vegetableIds(lines.map { IngredientRef(null, it) })

    @Test
    fun `structured names match singular and plural`() {
        assertEquals(setOf("pumpkin"), named("pompoen"))
        assertTrue("carrot" in named("wortel"))
        assertTrue("carrot" in named("wortelen"))
        assertTrue("tomato" in named("tomaten"))
        assertTrue("radish" in named("radijzen"))
        assertTrue("shallot" in named("sjalotten"))
        assertTrue("asparagus-white" in named("witte asperges"))
    }

    @Test
    fun `every name of a slash vegetable counts`() {
        assertTrue("carrot" in named("peen"))
        assertTrue("green-beans" in named("sperziebonen"))
        assertTrue("chard" in named("snijbiet"))
    }

    @Test
    fun `unparsed lines match whole words only`() {
        assertTrue("leek" in text("2 stengels prei, in ringen"))
        assertTrue("pumpkin" !in text("1 tl pompoenpitten"))
        assertTrue("potato" !in text("zoete aardappelpuree"))
    }

    @Test
    fun `diacritics and case are ignored`() {
        assertTrue("corn" in named("suikermais"))
        assertTrue("corn" in text("1 blik SUIKERMAÏS"))
    }

    @Test
    fun `nothing matches nothing`() {
        assertEquals(emptySet<String>(), named("bloem", "boter", "melk"))
    }

    @Test
    fun `prep time buckets`() {
        assertEquals(PrepTimeBucket.UNDER_15, PrepTimeBucket.of(10))
        assertEquals(PrepTimeBucket.UNDER_30, PrepTimeBucket.of(15))
        assertEquals(PrepTimeBucket.UNDER_60, PrepTimeBucket.of(59))
        assertEquals(PrepTimeBucket.OVER_60, PrepTimeBucket.of(60))
        assertEquals(null, PrepTimeBucket.of(null))
    }
}
