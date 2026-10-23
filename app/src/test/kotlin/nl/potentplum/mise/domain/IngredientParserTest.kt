package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientParserTest {
    private val parser = IngredientParser(TestContent.content.units)

    private data class Case(
        val line: String,
        val quantity: Double?,
        val quantityMax: Double? = null,
        val unit: String? = null,
        val name: String? = null,
        val note: String? = null,
    )

    private val cases =
        listOf(
            // Plain counts and decimals.
            Case("3 uien", 3.0, name = "uien"),
            Case("1,5 el olijfolie", 1.5, unit = "el", name = "olijfolie"),
            Case("1.5 dl room", 1.5, unit = "dl", name = "room"),
            // Fractions.
            Case("½ citroen", 0.5, name = "citroen"),
            Case("1½ tl zout", 1.5, unit = "tl", name = "zout"),
            Case("1 ½ tl komijn", 1.5, unit = "tl", name = "komijn"),
            Case("1/2 bosje peterselie", 0.5, unit = "bos", name = "peterselie"),
            Case("1 1/2 kg aardappelen", 1.5, unit = "kg", name = "aardappelen"),
            Case("¾ l bouillon", 0.75, unit = "l", name = "bouillon"),
            // Ranges.
            Case("2-3 tenen knoflook", 2.0, 3.0, "teen", "knoflook"),
            Case("2 – 3 el kappertjes", 2.0, 3.0, "el", "kappertjes"),
            Case("2 à 3 sjalotten", 2.0, 3.0, name = "sjalotten"),
            Case("4 tot 6 takjes tijm", 4.0, 6.0, "takje", "tijm"),
            // Unit aliases, glued and with a dot.
            Case("200g bloem", 200.0, unit = "g", name = "bloem"),
            Case("500 gram pompoen", 500.0, unit = "g", name = "pompoen"),
            Case("2 eetlepels honing", 2.0, unit = "el", name = "honing"),
            Case("1 el. mosterd", 1.0, unit = "el", name = "mosterd"),
            Case("250ml melk", 250.0, unit = "ml", name = "melk"),
            Case("1 ons spek", 1.0, unit = "ons", name = "spek"),
            Case("2 stuks prei", 2.0, unit = "stuks", name = "prei"),
            Case("1 krop sla", 1.0, unit = "krop", name = "sla"),
            // A unit-looking start of a word is not a unit.
            Case("2 groene paprika's", 2.0, name = "groene paprika's"),
            Case("1 limoen", 1.0, name = "limoen"),
            // Notes after a comma and in parentheses.
            Case("150 g boter, in blokjes", 150.0, unit = "g", name = "boter", note = "in blokjes"),
            Case("2 uien, gesnipperd, in ringen", 2.0, name = "uien", note = "gesnipperd, in ringen"),
            Case("1 blik tomaten (400 g)", 1.0, unit = "blik", name = "tomaten", note = "400 g"),
            // Pack sizes are notes, so they never scale.
            Case("1 blik à 400 g tomaten", 1.0, unit = "blik", name = "tomaten", note = "à 400 g"),
            Case("2 pakjes a 250 g spinazie", 2.0, unit = "pak", name = "spinazie", note = "a 250 g"),
            // Number words.
            Case("een snufje zout", 1.0, unit = "snuf", name = "zout"),
            Case("twee eieren", 2.0, name = "eieren"),
            Case("een halve rode ui", 1.0, name = "halve rode ui"),
            Case("anderhalve liter water", 1.5, unit = "l", name = "water"),
            // No quantity.
            Case("zout en peper", null, name = "zout en peper"),
            Case("peper naar smaak", null, unit = "naar-smaak", name = "peper"),
            Case("snufje nootmuskaat", null, unit = "snuf", name = "nootmuskaat"),
            Case("Verse koriander, grof gehakt", null, name = "verse koriander", note = "grof gehakt"),
            Case("een paar takjes rozemarijn", null, name = "een paar takjes rozemarijn"),
        )

    @Test
    fun `table of ingredient lines`() {
        val failures =
            cases.mapNotNull { case ->
                val result = parser.parse(case.line)
                val actual = Case(case.line, result.quantity, result.quantityMax, result.unit, result.name, result.note)
                if (actual == case && result.parsed) null else "${case.line}\n  expected $case\n  actual   $actual parsed=${result.parsed}"
            }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `unparseable lines stay text`() {
        listOf("Voor 4 personen: zie tip 2", "Saus 2x zo veel", "").forEach { line ->
            val result = parser.parse(line)
            assertEquals(line, false, result.parsed)
            assertEquals(line.trim(), result.text)
        }
    }

    @Test
    fun `the text is kept as written, whitespace collapsed`() {
        assertEquals("150 g boter, in blokjes", parser.parse("  150  g boter,  in blokjes ").text)
    }

    @Test
    fun `numbers parse in every notation`() {
        assertEquals(0.5, IngredientParser.number("1/2")!!, 1e-9)
        assertEquals(2.25, IngredientParser.number("2 1/4")!!, 1e-9)
        assertEquals(1.0 / 3, IngredientParser.number("⅓")!!, 1e-9)
        assertEquals(2.5, IngredientParser.number("2,5")!!, 1e-9)
        assertEquals(null, IngredientParser.number("1/0"))
    }
}
