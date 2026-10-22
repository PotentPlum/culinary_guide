package nl.potentplum.mise.ingest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedContentParserTest {
    @Test
    fun `a bare url`() =
        assertEquals(
            "https://www.ah.nl/allerhande/recept/R-R1187954",
            SharedContentParser.firstUrl("https://www.ah.nl/allerhande/recept/R-R1187954"),
        )

    @Test
    fun `chrome shares the title and the url`() =
        assertEquals(
            "https://www.leukerecepten.nl/recepten/pompoensoep/",
            SharedContentParser.firstUrl("Pompoensoep - Leuke Recepten\nhttps://www.leukerecepten.nl/recepten/pompoensoep/"),
        )

    @Test
    fun `instagram text with trailing punctuation`() =
        assertEquals(
            "https://www.instagram.com/p/Cx1y2z3/?igsh=abc",
            SharedContentParser.firstUrl("Bekijk deze post van @chef (https://www.instagram.com/p/Cx1y2z3/?igsh=abc)."),
        )

    @Test
    fun `the first of several urls wins`() =
        assertEquals("http://a.nl/x", SharedContentParser.firstUrl("zie http://a.nl/x en https://b.nl/y"))

    @Test
    fun `no url`() = assertNull(SharedContentParser.firstUrl("Gewoon wat tekst zonder link"))
}
