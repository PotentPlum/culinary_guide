package nl.potentplum.mise.core.designsystem.component

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.core.designsystem.MiseTheme

/**
 * A row of buttons that never wraps a label (spec §15.3): side by side while every button fits on one line at its natural
 * width, otherwise stacked full width. In a row, [alignment] places the group and [fill] gives the spare width to the first
 * button. In a stack, the button nearest to [alignment] goes on top (`End`: the last child, which is the primary action in
 * an end-aligned row such as "Niet nu · Dit kook ik vanavond"; `Start`: the first).
 */
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.End,
    fill: Boolean = false,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val needed = natural.sum() + gap * (measurables.size - 1).coerceAtLeast(0)
        val width = constraints.maxWidth
        if (!constraints.hasBoundedWidth || needed <= width) {
            val spare = if (fill && constraints.hasBoundedWidth) width - needed else 0
            val placeables =
                measurables.mapIndexed { index, measurable ->
                    val w = natural[index] + if (index == 0) spare else 0
                    measurable.measure(Constraints(minWidth = w, maxWidth = w))
                }
            val rowWidth = if (constraints.hasBoundedWidth) width else needed
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(rowWidth, height) {
                var x = alignment.align(placeables.sumOf { it.width } + gap * (placeables.size - 1), rowWidth, LayoutDirection.Ltr)
                placeables.forEach {
                    it.placeRelative(x, (height - it.height) / 2)
                    x += it.width + gap
                }
            }
        } else {
            val ordered = if (alignment == Alignment.End) measurables.reversed() else measurables
            val placeables = ordered.map { it.measure(Constraints.fixedWidth(width)) }
            val height = placeables.sumOf { it.height } + gap * (placeables.size - 1)
            layout(width, height) {
                var y = 0
                placeables.forEach {
                    it.placeRelative(0, y)
                    y += it.height + gap
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 220)
@Composable
private fun ActionRowPreview() {
    MiseTheme {
        ActionRow {
            TextButton(onClick = {}) { Text("Niet nu") }
            Button(onClick = {}) { Text("Dit kook ik vanavond") }
        }
    }
}
