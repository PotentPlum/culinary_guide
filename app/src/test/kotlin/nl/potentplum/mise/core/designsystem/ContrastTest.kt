package nl.potentplum.mise.core.designsystem

import androidx.compose.ui.graphics.Color
import nl.potentplum.mise.core.designsystem.Contrast.GRAPHICS
import nl.potentplum.mise.core.designsystem.Contrast.LARGE_TEXT
import nl.potentplum.mise.core.designsystem.Contrast.TEXT
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text/indicator pair of the spec §5 table against its WCAG floor. */
class ContrastTest {
    private val scheme = MiseLightColorScheme
    private val status = MiseColors.Light

    private data class Pair(
        val name: String,
        val foreground: Color,
        val background: Color,
        val floor: Double,
    )

    private val surfaces =
        listOf("cream" to MisePalette.Cream, "card" to MisePalette.Card, "warm" to MisePalette.Warm)

    /** Status colours are specified on cream (spec §5) and appear on cream and cards, never on the warm surface. */
    private val statusSurfaces = surfaces.take(2)

    private val pairs: List<Pair> =
        buildList {
            for ((surface, background) in surfaces) {
                add(Pair("ink on $surface", scheme.onSurface, background, TEXT))
                add(Pair("ink-soft on $surface", scheme.onSurfaceVariant, background, TEXT))
                add(Pair("primary text on $surface", scheme.primary, background, TEXT))
                add(Pair("error on $surface", scheme.error, background, TEXT))
            }
            for ((surface, background) in statusSurfaces) {
                add(Pair("status peak on $surface", status.peak, background, GRAPHICS))
                add(Pair("shoulder outline on $surface", status.shoulderOutline, background, GRAPHICS))
                add(Pair("soon fg on $surface", status.soonFg, background, TEXT))
                add(Pair("last chance fg on $surface", status.lastChanceFg, background, TEXT))
                add(Pair("last chance fill on $surface", status.lastChanceFill, background, GRAPHICS))
            }
            add(Pair("terracotta large text on cream", scheme.tertiary, MisePalette.Cream, LARGE_TEXT))
            add(Pair("terracotta fill on cream", scheme.tertiary, MisePalette.Cream, GRAPHICS))
            add(Pair("onPrimary on primary", scheme.onPrimary, scheme.primary, TEXT))
            add(Pair("onPrimaryContainer on primary-soft", scheme.onPrimaryContainer, scheme.primaryContainer, TEXT))
            add(Pair("onTertiary on terracotta", scheme.onTertiary, scheme.tertiary, TEXT))
            add(Pair("onSecondaryContainer on warm", scheme.onSecondaryContainer, scheme.secondaryContainer, TEXT))
            add(Pair("onTertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer, TEXT))
            add(Pair("onError on error", scheme.onError, scheme.error, TEXT))
            add(Pair("onErrorContainer on errorContainer", scheme.onErrorContainer, scheme.errorContainer, TEXT))
            add(Pair("inverse", scheme.inverseOnSurface, scheme.inverseSurface, TEXT))
        }

    @Test
    fun `every pair reaches its floor`() {
        val failures =
            pairs
                .map { it to Contrast.ratio(it.foreground, it.background) }
                .filter { (pair, ratio) -> ratio < pair.floor }
                .map { (pair, ratio) -> "${pair.name}: %.2f < %.1f".format(ratio, pair.floor) }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `measured values match the spec table`() {
        assertEquals(6.4, Contrast.ratio(MisePalette.Primary, MisePalette.Cream), 0.1)
        assertEquals(2.7, Contrast.ratio(MisePalette.PrimarySoft, MisePalette.Cream), 0.1)
        assertEquals(4.3, Contrast.ratio(MisePalette.Accent, MisePalette.Cream), 0.1)
        assertEquals(6.6, Contrast.ratio(MisePalette.InkSoft, MisePalette.Cream), 0.1)
        assertEquals(5.6, Contrast.ratio(MisePalette.Ink, MisePalette.PrimarySoft), 0.1)
        assertEquals(4.9, Contrast.ratio(MisePalette.White, MisePalette.Accent), 0.1)
        assertEquals(5.1, Contrast.ratio(MisePalette.SoonFg, MisePalette.Cream), 0.1)
        assertEquals(7.9, Contrast.ratio(MisePalette.LastChanceFg, MisePalette.Cream), 0.1)
    }

    @Test
    fun `soft fills alone stay below the graphics floor, so they never carry meaning`() {
        assertTrue(Contrast.ratio(MisePalette.PrimarySoft, MisePalette.Cream) < GRAPHICS)
        assertTrue(Contrast.ratio(MisePalette.SoonFill, MisePalette.Cream) < GRAPHICS)
    }

    @Test
    fun `vegetable initials reach 4,5 to 1 with ink or white`() {
        for (vegetable in TestContent.content.vegetables) {
            val fill = hexColor(vegetable.color)
            val ratio = Contrast.ratio(Contrast.inkOrWhiteOn(fill), fill)
            assertTrue("${vegetable.id}: %.2f".format(ratio), ratio >= TEXT)
        }
    }

    @Test
    fun `hex colours parse`() {
        assertEquals(MisePalette.Primary, hexColor("#3F5E2A"))
        assertEquals(MisePalette.Ink, hexColor("#1e1a14"))
    }
}
