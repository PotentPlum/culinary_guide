package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/**
 * One of a few views (Tijdlijn · Quests · Inzichten): a segmented button row while every label fits its segment on one
 * line; without the selected segment's check mark when that makes it fit (large text); and only when even that fails (the
 * largest text on a small phone) a wrapping row of filter chips, so no label is ever split.
 */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    labels: List<String>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.labelLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widest =
            remember(labels, style, density) { labels.maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width } }
        // Per segment: 12dp padding each side and the border, plus the 18dp check icon of the selected one and its 8dp gap.
        val share = maxWidth / options.size
        val withCheck = with(density) { widest <= (share - 54.dp).toPx() }
        val withoutCheck = with(density) { widest <= (share - 28.dp).toPx() }
        if (withCheck || withoutCheck) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = { if (withCheck) SegmentedButtonDefaults.Icon(option == selected) },
                    ) { Text(labels[index], maxLines = 1, softWrap = false) }
                }
            }
        } else {
            FlowRow(
                Modifier.fillMaxWidth().semantics { selectableGroup() },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEachIndexed { index, option ->
                    FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(labels[index]) })
                }
            }
        }
    }
}
