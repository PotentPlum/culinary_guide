package nl.potentplum.mise.core.designsystem

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * A string resource with its inline `<i>`/`<b>` markup as an [AnnotatedString] (spec §5): `<i>` becomes Instrument Serif
 * italic, `<b>` semibold. Headlines stay one string, so TalkBack reads one sentence and wrapping works at any font scale.
 */
@Composable
@ReadOnlyComposable
fun styledStringResource(
    @StringRes id: Int,
): AnnotatedString = LocalResources.current.getText(id).toStyledAnnotatedString()

private val Italic = SpanStyle(fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic)
private val Bold = SpanStyle(fontWeight = FontWeight.SemiBold)

fun CharSequence.toStyledAnnotatedString(): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        append(source.toString())
        if (source !is Spanned) return@buildAnnotatedString
        for (span in source.getSpans(0, source.length, StyleSpan::class.java)) {
            val start = source.getSpanStart(span)
            val end = source.getSpanEnd(span)
            when (span.style) {
                Typeface.ITALIC -> addStyle(Italic, start, end)
                Typeface.BOLD -> addStyle(Bold, start, end)
                Typeface.BOLD_ITALIC -> addStyle(Italic.merge(Bold), start, end)
            }
        }
    }
}
