package nl.potentplum.mise.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/** The colour tokens of spec §5, measured on cream. */
object MisePalette {
    val Primary = Color(0xFF3F5E2A)

    /** 2.7:1 on cream: fills only, never alone. */
    val PrimarySoft = Color(0xFF7A9C5C)

    /** Terracotta, 4.3:1 on cream: fills and text ≥ 18 sp only. */
    val Accent = Color(0xFFB95128)
    val Cream = Color(0xFFF4EFE6)
    val Card = Color(0xFFFBF8F1)
    val Warm = Color(0xFFEAE2D0)
    val Ink = Color(0xFF1E1A14)
    val InkSoft = Color(0xFF5A5347)
    val White = Color(0xFFFFFFFF)

    /** Ink @ 10 % and 22 % on cream, flattened so they read the same on every surface. */
    val Line = Ink.copy(alpha = 0.10f).compositeOver(Cream)
    val Outline = Ink.copy(alpha = 0.22f).compositeOver(Cream)

    /** "Komt eraan": the fill is 2.5:1 and never carries meaning alone; the fg is 5.1:1. */
    val SoonFill = Color(0xFFC98B2A)
    val SoonFg = Color(0xFF8A5C12)

    /** "Laatste kans". */
    val LastChanceFill = Color(0xFF99514A)
    val LastChanceFg = Color(0xFF6E3A35)

    val Error = Color(0xFF9E2B25)
    val ErrorContainer = Color(0xFFF6DAD5)
    val OnErrorContainer = Color(0xFF5C1712)
    val TertiaryContainer = Color(0xFFF3DCCF)
}

/** Status colours that Material 3 has no role for (spec §5). `PeakBar` and the calendar use these, never a vegetable's accent. */
@Immutable
data class MiseColors(
    val peak: Color,
    /** A shoulder cell or dot is an outline in [shoulderOutline] filled with [shoulderFill], readable as "half" without colour. */
    val shoulderOutline: Color,
    val shoulderFill: Color,
    val soonFill: Color,
    val soonFg: Color,
    val lastChanceFill: Color,
    val lastChanceFg: Color,
    /** Hairline for cards and empty cells. */
    val line: Color,
    val inkSoft: Color,
) {
    companion object {
        val Light =
            MiseColors(
                peak = MisePalette.Primary,
                shoulderOutline = MisePalette.Primary,
                shoulderFill = MisePalette.PrimarySoft,
                soonFill = MisePalette.SoonFill,
                soonFg = MisePalette.SoonFg,
                lastChanceFill = MisePalette.LastChanceFill,
                lastChanceFg = MisePalette.LastChanceFg,
                line = MisePalette.Line,
                inkSoft = MisePalette.InkSoft,
            )
    }
}

val LocalMiseColors = staticCompositionLocalOf { MiseColors.Light }

/** Light scheme from the token table (spec §5). Dynamic colour is never used. */
val MiseLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = MisePalette.Primary,
        onPrimary = MisePalette.Card,
        primaryContainer = MisePalette.PrimarySoft,
        onPrimaryContainer = MisePalette.Ink,
        inversePrimary = MisePalette.PrimarySoft,
        secondary = MisePalette.InkSoft,
        onSecondary = MisePalette.Card,
        secondaryContainer = MisePalette.Warm,
        onSecondaryContainer = MisePalette.Ink,
        tertiary = MisePalette.Accent,
        onTertiary = MisePalette.White,
        tertiaryContainer = MisePalette.TertiaryContainer,
        onTertiaryContainer = MisePalette.Ink,
        background = MisePalette.Cream,
        onBackground = MisePalette.Ink,
        surface = MisePalette.Cream,
        onSurface = MisePalette.Ink,
        surfaceVariant = MisePalette.Warm,
        onSurfaceVariant = MisePalette.InkSoft,
        surfaceTint = MisePalette.Primary,
        inverseSurface = MisePalette.Ink,
        inverseOnSurface = MisePalette.Cream,
        error = MisePalette.Error,
        onError = MisePalette.White,
        errorContainer = MisePalette.ErrorContainer,
        onErrorContainer = MisePalette.OnErrorContainer,
        outline = MisePalette.Outline,
        outlineVariant = MisePalette.Line,
        scrim = Color.Black,
        surfaceBright = MisePalette.Card,
        surfaceDim = MisePalette.Warm,
        surfaceContainerLowest = MisePalette.Card,
        surfaceContainerLow = MisePalette.Card,
        surfaceContainer = MisePalette.Cream,
        surfaceContainerHigh = MisePalette.Warm,
        surfaceContainerHighest = MisePalette.Warm,
    )
