package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTextStyles
import nl.potentplum.mise.core.designsystem.MiseTheme

/**
 * Twelve month cells in the status colours, never the vegetable's accent (spec §5): peak filled, shoulder outlined with a
 * soft fill, other months a hairline. [currentMonth] is marked with an ink underline (and a bold letter when not compact).
 */
@Composable
fun PeakBar(
    peak: List<Int>,
    shoulder: List<Int>,
    modifier: Modifier = Modifier,
    currentMonth: Int? = null,
    compact: Boolean = false,
) {
    val colors = MiseTheme.colors
    val ink = MaterialTheme.colorScheme.onSurface
    val description = peakBarDescription(peak, shoulder)
    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (compact) 10.dp else 14.dp),
        ) {
            val gap = 2.dp.toPx()
            val stroke = 1.dp.toPx()
            val cell = (size.width - gap * 11) / 12
            val marker = 2.dp.toPx()
            val cellHeight = size.height - marker - stroke
            val radius = CornerRadius(2.dp.toPx())
            for (month in 1..12) {
                val left = (month - 1) * (cell + gap)
                val topLeft = Offset(left + stroke / 2, stroke / 2)
                val cellSize = Size(cell - stroke, cellHeight - stroke)
                when (month) {
                    in peak -> {
                        drawRoundRect(colors.peak, Offset(left, 0f), Size(cell, cellHeight), radius)
                    }

                    in shoulder -> {
                        drawRoundRect(colors.shoulderFill, topLeft, cellSize, radius)
                        drawRoundRect(colors.shoulderOutline, topLeft, cellSize, radius, style = Stroke(stroke))
                    }

                    else -> {
                        drawRoundRect(colors.line, topLeft, cellSize, radius, style = Stroke(stroke))
                    }
                }
                if (month == currentMonth) {
                    drawRect(ink, Offset(left, size.height - marker), Size(cell, marker))
                }
            }
        }
        if (!compact) {
            val initials = stringArrayResource(R.array.months_initial)
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                initials.forEachIndexed { index, letter ->
                    val now = index + 1 == currentMonth
                    Text(
                        text = letter,
                        style = MiseTextStyles.meta.copy(fontWeight = if (now) FontWeight.Medium else FontWeight.Normal),
                        color = if (now) ink else colors.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** "Op z'n best: mei, juni. Verkrijgbaar: april." with month names in full (spec §15.3). */
@Composable
fun peakBarDescription(
    peak: List<Int>,
    shoulder: List<Int>,
): String = stringResource(R.string.peakbar_description, monthList(peak), monthList(shoulder))

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 320)
@Composable
private fun PeakBarPreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PeakBar(peak = listOf(5, 6), shoulder = listOf(4), currentMonth = 10)
            PeakBar(peak = listOf(9, 10, 11, 12, 1), shoulder = listOf(8, 2), currentMonth = 10, compact = true)
        }
    }
}
