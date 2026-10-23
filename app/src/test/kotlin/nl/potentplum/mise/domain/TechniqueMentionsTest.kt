package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Test

class TechniqueMentionsTest {
    private val mentions = TechniqueMentions(TestContent.content.techniques)

    private fun ids(text: String) = mentions.find(text).map { it.id }

    @Test
    fun `stems and doubled vowels`() {
        assertEquals(setOf("smoren", "smor", "smoor"), TechniqueMentions.forms("smoren"))
        assertEquals(setOf("blancheren", "blancher", "blancheer"), TechniqueMentions.forms("blancheren"))
        assertEquals(setOf("pekelen", "pekel", "pekeel"), TechniqueMentions.forms("pekelen"))
        assertEquals(setOf("vlees braden"), TechniqueMentions.forms("vlees braden"))
        assertEquals(setOf("roux"), TechniqueMentions.forms("roux"))
    }

    @Test
    fun `imperatives, infinitives and accents match, in text order`() {
        val names = TestContent.content.techniques.associate { it.id to it.nl }
        val found = mentions.find("Blancheer de bonen, laat ze sudderen en snijd de wortel julienne.")
        assertEquals(listOf("Blancheren", "Sudderen", "Julienne"), found.map { it.nl })
        assertEquals(listOf("Sauteren"), mentions.find("Sauteer de ui in boter.").map { names.getValue(it.id) })
    }

    @Test
    fun `words that only contain a name do not match, and cookware is skipped`() {
        assertEquals(emptyList<String>(), ids("Gebruik een kookwekker."))
        assertEquals(emptyList<String>(), ids("Zet een koekenpan op het vuur."))
        assertEquals(3, mentions.find("Smoor, blancheer, sauteer en pekel.").size)
    }
}
