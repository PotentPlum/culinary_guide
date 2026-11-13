package nl.potentplum.mise.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Both layouts decide from a child's min intrinsic width: for text, the width of its longest word. When that does not fit,
// they change shape instead of letting a word break mid-way (spec §15.3).

/**
 * Children of equal width and height side by side, as long as each child's longest word fits its share; otherwise they
 * stack, each full width. For a few tiles or choices in a row (stats, score steps) at any font scale.
 */
@Composable
fun EvenRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val count = measurables.size.coerceAtLeast(1)
        val width = constraints.maxWidth
        val share = ((width - gap * (count - 1)) / count).coerceAtLeast(0)
        val fits = constraints.hasBoundedWidth && measurables.all { it.minIntrinsicWidth(Constraints.Infinity) <= share }
        if (fits) {
            val height = measurables.maxOfOrNull { it.maxIntrinsicHeight(share) } ?: 0
            val placeables = measurables.map { it.measure(Constraints.fixed(share, height)) }
            layout(width, height) {
                placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (share + gap), 0) }
            }
        } else {
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = width, minHeight = 0)) }
            val height = placeables.sumOf { it.height } + gap * (placeables.size - 1).coerceAtLeast(0)
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

/**
 * [main] with [trailing] (a badge, a short button) at its end, centred vertically, while the longest word of [main] still
 * fits beside it; otherwise [trailing] goes under [main], at the start.
 */
@Composable
fun TrailingRow(
    main: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
) {
    Layout(contents = listOf(main, trailing), modifier = modifier) { (mainMeasurables, trailingMeasurables), constraints ->
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val trailingWidth = trailingMeasurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val mainMin = mainMeasurables.maxOfOrNull { it.minIntrinsicWidth(Constraints.Infinity) } ?: 0
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val beside = !constraints.hasBoundedWidth || trailingWidth == 0 || mainMin + gap + trailingWidth <= width
        if (beside) {
            val trailingPlaceables = trailingMeasurables.map { it.measure(loose) }
            val used = trailingPlaceables.sumOf { it.width }
            val mainWidth = (width - used - if (used > 0) gap else 0).coerceAtLeast(0)
            val mainPlaceables = mainMeasurables.map { it.measure(Constraints(minWidth = mainWidth, maxWidth = mainWidth)) }
            val height = maxOf(mainPlaceables.maxOfOrNull { it.height } ?: 0, trailingPlaceables.maxOfOrNull { it.height } ?: 0)
            layout(width, height) {
                mainPlaceables.forEach { it.placeRelative(0, Alignment.CenterVertically.align(it.height, height)) }
                var x = width - used
                trailingPlaceables.forEach {
                    it.placeRelative(x, Alignment.CenterVertically.align(it.height, height))
                    x += it.width
                }
            }
        } else {
            val mainPlaceables = mainMeasurables.map { it.measure(Constraints(minWidth = width, maxWidth = width)) }
            val trailingPlaceables = trailingMeasurables.map { it.measure(loose) }
            val mainHeight = mainPlaceables.sumOf { it.height }
            val height = mainHeight + (trailingPlaceables.maxOfOrNull { it.height } ?: 0) + gap / 2
            layout(width, height) {
                var y = 0
                mainPlaceables.forEach {
                    it.placeRelative(0, y)
                    y += it.height
                }
                var x = Alignment.Start.align(0, width, LayoutDirection.Ltr)
                trailingPlaceables.forEach {
                    it.placeRelative(x, mainHeight + gap / 2)
                    x += it.width
                }
            }
        }
    }
}
