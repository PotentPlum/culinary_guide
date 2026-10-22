package nl.potentplum.mise.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.1 contrast, the same formula as `tools/validate_data.py`. */
object Contrast {
    const val TEXT = 4.5
    const val LARGE_TEXT = 3.0
    const val GRAPHICS = 3.0

    fun ratio(
        foreground: Color,
        background: Color,
    ): Double {
        val fg = relativeLuminance(foreground.compositeOver(background))
        val bg = relativeLuminance(background)
        return (max(fg, bg) + 0.05) / (min(fg, bg) + 0.05)
    }

    /** Ink or white, whichever contrasts more with [background] (vegetable fills, spec §5). */
    fun inkOrWhiteOn(background: Color): Color =
        if (ratio(MisePalette.Ink, background) >= ratio(MisePalette.White, background)) MisePalette.Ink else MisePalette.White

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}

/** Parses `#RRGGBB` from the content assets. */
fun hexColor(hex: String): Color {
    require(hex.length == 7 && hex[0] == '#') { "expected #RRGGBB, got $hex" }
    return Color(0xFF000000 or hex.substring(1).toLong(16))
}
