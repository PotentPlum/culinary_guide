package nl.potentplum.mise.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationDetectorTest {
    private fun seconds(text: String) = DurationDetector.detect(text).map { it.seconds }

    @Test
    fun `Dutch durations`() {
        assertEquals(listOf(600), seconds("Laat 10 minuten sudderen."))
        assertEquals(listOf(60), seconds("Bak 1 minuut aan elke kant."))
        assertEquals(listOf(1_500), seconds("Rooster 25 min in de oven."))
        assertEquals(listOf(7_200), seconds("Laat 2 uur rijzen."))
        assertEquals(listOf(5_400), seconds("Stoof 1,5 uur op laag vuur."))
        assertEquals(listOf(30), seconds("Blancheer 30 seconden."))
        assertEquals(listOf(10_800), seconds("Marineer 3 u in de koelkast."))
    }

    @Test
    fun `English durations`() {
        assertEquals(listOf(1_200), seconds("Simmer for 20 minutes."))
        assertEquals(listOf(3_600), seconds("Rest for 1 hour."))
        assertEquals(listOf(45), seconds("Microwave 45 sec."))
        assertEquals(listOf(300), seconds("Cook 5 mins, stirring."))
    }

    @Test
    fun `a range uses the upper bound`() {
        assertEquals(listOf(600), seconds("Kook 8-10 minuten."))
        assertEquals(listOf(720), seconds("Bak 10–12 min."))
        assertEquals(listOf(2_700), seconds("Rooster 35 tot 45 minuten."))
        assertEquals(listOf(10_800), seconds("Braise 2 to 3 hours."))
    }

    @Test
    fun `several durations in one step, each once`() {
        assertEquals(listOf(300, 1_200), seconds("Fruit 5 min, voeg de bouillon toe en kook 20 minuten. Proef na 5 minuten."))
        val text = "Kook de pasta 9 minuten."
        val found = DurationDetector.detect(text).single()
        assertEquals("9 minuten", text.substring(found.range))
    }

    @Test
    fun `temperatures, quantities and words are not durations`() {
        assertEquals(emptyList<Int>(), seconds("Verwarm de oven voor op 200 °C."))
        assertEquals(emptyList<Int>(), seconds("Voeg 200 g bloem en 250 ml melk toe."))
        assertEquals(emptyList<Int>(), seconds("Snijd 2 uien en 1 ui in ringen."))
        assertEquals(emptyList<Int>(), seconds("Gebruik minder zout voor 4 personen."))
        assertEquals(emptyList<Int>(), seconds("Snijd in stukken van 2 cm."))
        assertEquals(listOf(1_500), seconds("Bak 25 minuten op 180°C met 50 g boter."))
    }

    @Test
    fun `nonsense lengths are dropped`() {
        assertEquals(emptyList<Int>(), seconds("Laat 0 minuten staan."))
        assertEquals(emptyList<Int>(), seconds("Bewaar tot 48 uur."))
    }
}
