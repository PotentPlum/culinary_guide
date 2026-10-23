package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A short list whose items reorder by long-pressing and dragging the handle each item gets as a [Modifier]. Dragging is
 * never the only way: screens also offer move-up/move-down actions (spec §15.3).
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (item: T, index: Int, dragHandle: Modifier) -> Unit,
) {
    val heights = remember { mutableStateMapOf<Any, Int>() }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            val itemKey = key(item)
            key(itemKey) {
                val dragging = draggingKey == itemKey
                val handle =
                    Modifier.pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingKey = itemKey
                                offset = 0f
                            },
                            onDragEnd = { draggingKey = null },
                            onDragCancel = { draggingKey = null },
                            onDrag = { change, amount ->
                                change.consume()
                                offset += amount.y
                                val from = currentItems.indexOfFirst { key(it) == itemKey }
                                val spacing = 12.dp.toPx()
                                val next = currentItems.getOrNull(from + 1)?.let { heights[key(it)] }
                                val previous = currentItems.getOrNull(from - 1)?.let { heights[key(it)] }
                                if (next != null && offset > (next + spacing) / 2) {
                                    currentOnMove(from, from + 1)
                                    offset -= next + spacing
                                } else if (previous != null && offset < -(previous + spacing) / 2) {
                                    currentOnMove(from, from - 1)
                                    offset += previous + spacing
                                }
                            },
                        )
                    }
                Box(
                    Modifier
                        .onSizeChanged { heights[itemKey] = it.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) offset else 0f
                            shadowElevation = if (dragging) 8.dp.toPx() else 0f
                        },
                ) { content(item, index, handle) }
            }
        }
    }
}

/** Moves the element at [from] to [to]. */
fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
