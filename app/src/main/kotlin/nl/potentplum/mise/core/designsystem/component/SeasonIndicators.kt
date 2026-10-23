package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseColors
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.SeasonStatus

/** The §5 label of a status. ENDING and OFF are both "Buiten seizoen". */
@Composable
fun seasonStatusLabel(status: SeasonStatus): String =
    stringResource(
        when (status) {
            SeasonStatus.PEAK -> R.string.status_peak
            SeasonStatus.SHOULDER -> R.string.status_shoulder
            SeasonStatus.SOON -> R.string.status_soon
            SeasonStatus.ENDING, SeasonStatus.OFF -> R.string.status_off
        },
    )

/** Text colour of a status label: always a ≥ 4.5:1 `*-fg` variant (spec §5). */
fun MiseColors.statusForeground(status: SeasonStatus): Color =
    when (status) {
        SeasonStatus.PEAK, SeasonStatus.SHOULDER -> peak
        SeasonStatus.SOON -> soonFg
        SeasonStatus.ENDING, SeasonStatus.OFF -> inkSoft
    }

/**
 * A status dot with its label (colour is never the only signal). Peak is filled; shoulder is a primary outline with a
 * soft fill; soon an outline in the soon fg with the soon fill; out of season a hollow ring.
 */
@Composable
fun StatusDot(
    status: SeasonStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val colors = MiseTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) {
            val stroke = 1.5.dp.toPx()
            val radius = size.minDimension / 2
            val inner = radius - stroke / 2
            when (status) {
                SeasonStatus.PEAK -> {
                    drawCircle(colors.peak, radius)
                }

                SeasonStatus.SHOULDER -> {
                    drawCircle(colors.shoulderFill, inner)
                    drawCircle(colors.shoulderOutline, inner, style = Stroke(stroke))
                }

                SeasonStatus.SOON -> {
                    drawCircle(colors.soonFill, inner)
                    drawCircle(colors.soonFg, inner, style = Stroke(stroke))
                }

                SeasonStatus.ENDING, SeasonStatus.OFF -> {
                    drawCircle(colors.inkSoft, inner, style = Stroke(stroke))
                }
            }
        }
        if (showLabel) {
            Text(
                text = seasonStatusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                color = colors.statusForeground(status),
            )
        }
    }
}

enum class SeasonBadgeKind { LastChance, ShortSeason }

/** "Laatste kans" or "Kort seizoen" as an outlined pill. */
@Composable
fun SeasonBadge(
    kind: SeasonBadgeKind,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val (label, color) =
        when (kind) {
            SeasonBadgeKind.LastChance -> stringResource(R.string.badge_last_chance) to colors.lastChanceFg
            SeasonBadgeKind.ShortSeason -> stringResource(R.string.badge_short_season) to colors.peak
        }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = color,
        border = BorderStroke(1.dp, color),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun SeasonIndicatorsPreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SeasonStatus.entries.forEach { StatusDot(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeasonBadge(SeasonBadgeKind.LastChance)
                SeasonBadge(SeasonBadgeKind.ShortSeason)
            }
        }
    }
}
