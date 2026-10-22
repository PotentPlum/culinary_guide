package nl.potentplum.mise.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import nl.potentplum.mise.R

/** Display and headline only (≥ 22 sp); italic for emphasis words. */
val InstrumentSerif =
    FontFamily(
        Font(R.font.instrument_serif_regular, FontWeight.Normal),
        Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
    )

/** Title, body and label. */
val InstrumentSans =
    FontFamily(
        Font(R.font.instrument_sans_regular, FontWeight.Normal),
        Font(R.font.instrument_sans_medium, FontWeight.Medium),
        Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    )

/** Meta labels only: eyebrows, month letters, counts (≥ 11 sp). */
val JetBrainsMono =
    FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    )

private fun serif(
    size: Int,
    lineHeight: Int,
) = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = lineHeight.sp)

private fun sans(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = InstrumentSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val MiseTypography =
    Typography(
        displayLarge = serif(57, 64),
        displayMedium = serif(45, 52),
        displaySmall = serif(36, 44),
        headlineLarge = serif(34, 40),
        headlineMedium = serif(28, 34),
        headlineSmall = serif(24, 30),
        titleLarge = sans(FontWeight.SemiBold, 20, 26),
        titleMedium = sans(FontWeight.SemiBold, 16, 22, 0.1),
        titleSmall = sans(FontWeight.Medium, 14, 20, 0.1),
        bodyLarge = sans(FontWeight.Normal, 16, 24),
        bodyMedium = sans(FontWeight.Normal, 14, 20),
        bodySmall = sans(FontWeight.Normal, 12, 16),
        labelLarge = sans(FontWeight.Medium, 14, 20, 0.1),
        labelMedium = sans(FontWeight.Medium, 12, 16, 0.3),
        labelSmall = sans(FontWeight.Medium, 11, 16, 0.3),
    )

/** Mono meta styles. The uppercase is applied in the composable, so TalkBack reads words (spec §5). */
object MiseTextStyles {
    val eyebrow =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.12.em,
        )
    val meta =
        TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.04.em,
        )
}
