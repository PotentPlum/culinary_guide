package nl.potentplum.mise.domain

import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientScalerTest {
    private val units = TestContent.content.units
    private val parser = IngredientParser(units)
    private val scaler = IngredientScaler(units)

    private fun scaled(
        line: String,
        ratio: Double,
    ) = scaler.format(parser.parse(line), ratio)

    @Test
    fun `unscaled lines render from their structured fields`() {
        assertEquals("150 g boter, in blokjes", scaled("150 g boter, in blokjes", 1.0))
        assertEquals("1,5 el olijfolie", scaled("1.5 el olijfolie", 1.0))
        assertEquals("2–3 tenen knoflook", scaled("2-3 teentjes knoflook", 1.0))
        assertEquals("1 teen knoflook", scaled("1 teentje knoflook", 1.0))
        assertEquals("zout en peper", scaled("zout en peper", 1.0))
        assertEquals("peper naar smaak", scaled("peper naar smaak", 1.0))
        assertEquals("snufje nootmuskaat", scaled("snufje nootmuskaat", 2.0))
    }

    @Test
    fun `counts round to the nearest half, at least a half`() {
        assertEquals("6 uien", scaled("3 uien", 2.0))
        assertEquals("1,5 ui", scaled("1 ui", 1.5))
        assertEquals("0,5 ui", scaled("1 ui", 0.25))
        assertEquals("2 eieren", scaled("3 eieren", 0.6))
    }

    @Test
    fun `grams and millilitres round to whole numbers below 50 and to 5 above`() {
        assertEquals("33 g boter", scaled("100 g boter", 1.0 / 3))
        assertEquals("165 g bloem", scaled("250 g bloem", 2.0 / 3))
        assertEquals("75 ml room", scaled("50 ml room", 1.5))
        assertEquals("1 g zout", scaled("2 g zout", 0.25))
    }

    @Test
    fun `from 1000 g or ml the unit switches to kg or l with one decimal`() {
        assertEquals("1,5 kg pompoen", scaled("500 g pompoen", 3.0))
        assertEquals("1,2 l bouillon", scaled("800 ml bouillon", 1.5))
        assertEquals("1 kg aardappelen", scaled("250 g aardappelen", 4.0))
        assertEquals("750 g aardappelen", scaled("1,5 kg aardappelen", 0.5))
    }

    @Test
    fun `spoons round to the nearest half`() {
        assertEquals("1,5 el honing", scaled("1 el honing", 1.5))
        assertEquals("0,5 tl zout", scaled("1 tl zout", 0.4))
    }

    @Test
    fun `ranges scale both ends`() {
        assertEquals("4–6 tenen knoflook", scaled("2-3 tenen knoflook", 2.0))
    }

    @Test
    fun `a pack size in the note never scales`() {
        assertEquals("2 blikken tomaten, à 400 g", scaled("1 blik à 400 g tomaten", 2.0))
    }

    @Test
    fun `unparsed lines multiply every number`() {
        assertEquals("Saus 4x zo veel", scaled("Saus 2x zo veel", 2.0))
        assertEquals("Voor 1,5 personen", IngredientScaler.scaleText("Voor ½ personen", 3.0))
        assertEquals("ongeveer 0,3 kopje", IngredientScaler.scaleText("ongeveer 1 kopje", 1.0 / 3))
    }

    @Test
    fun `the ratio needs a known base`() {
        assertEquals(1.5, scaler.ratio(4, 6)!!, 1e-9)
        assertNull(scaler.ratio(null, 4))
        assertNull(scaler.ratio(0, 4))
    }
}
