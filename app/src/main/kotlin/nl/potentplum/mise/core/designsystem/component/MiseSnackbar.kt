package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * One snackbar host for the whole app, so a message survives navigation (e.g. "Recept verwijderd · Ongedaan maken"
 * shown on the library after leaving the deleted recipe). [show]'s callbacks run in the app-wide scope, so an undo
 * still completes after the screen that offered it is gone.
 */
class MiseSnackbar(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(
        message: String,
        actionLabel: String? = null,
        onAction: suspend () -> Unit = {},
        onDismiss: suspend () -> Unit = {},
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result =
                hostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) onAction() else onDismiss()
        }
    }
}

val LocalMiseSnackbar = staticCompositionLocalOf<MiseSnackbar> { error("MiseSnackbar not provided") }

/**
 * Bottom action bars on screen (a recipe's Kookmodus / Gekookt, cook mode's step buttons), by the top of each in root
 * coordinates. The one app-wide snackbar sits above the navigation bar, which is exactly where those bars are, so
 * [MiseSnackbarHost] lifts it above the highest one instead of covering its buttons.
 */
class SnackbarObstacles {
    internal val tops = mutableStateMapOf<Any, Float>()
}

/** Null outside the app shell (previews, component tests): then [avoidedBySnackbar] does nothing. */
val LocalSnackbarObstacles = staticCompositionLocalOf<SnackbarObstacles?> { null }

/** Marks a bottom action bar that a snackbar must not cover. */
fun Modifier.avoidedBySnackbar(): Modifier =
    composed {
        val obstacles = LocalSnackbarObstacles.current ?: return@composed Modifier
        val key = remember { Any() }
        DisposableEffect(obstacles, key) { onDispose { obstacles.tops.remove(key) } }
        Modifier.onGloballyPositioned { obstacles.tops[key] = it.boundsInRoot().top }
    }

/** The app's snackbar host, moved up when a bottom action bar ([avoidedBySnackbar]) would be under it. */
@Composable
fun MiseSnackbarHost(
    state: SnackbarHostState,
    obstacles: SnackbarObstacles,
    modifier: Modifier = Modifier,
) {
    var hostBottom by remember { mutableFloatStateOf(Float.NaN) }
    // Measured on an unmoved parent, so the lift never feeds back into its own measurement.
    Box(modifier.onGloballyPositioned { hostBottom = it.boundsInRoot().bottom }) {
        SnackbarHost(
            state,
            Modifier.offset {
                val highest = obstacles.tops.values.minOrNull()
                val lift = if (highest == null || hostBottom.isNaN()) 0f else (hostBottom - highest).coerceAtLeast(0f)
                IntOffset(0, -lift.roundToInt())
            },
        )
    }
}
