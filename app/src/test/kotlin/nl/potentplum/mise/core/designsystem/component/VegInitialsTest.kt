package nl.potentplum.mise.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class VegInitialsTest {
    @Test
    fun `two words give two capitals`() = assertEquals("WA", vegInitials("Witte asperges"))

    @Test
    fun `one word gives a capital and a small letter`() = assertEquals("Pa", vegInitials("Pastinaak"))

    @Test
    fun `aliases after a slash are ignored`() = assertEquals("Wo", vegInitials("Wortelen / peen"))

    @Test
    fun `hyphenated names count as two words`() = assertEquals("PB", vegInitials("Pak-boi"))
}
