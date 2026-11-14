package nl.potentplum.mise.core.designsystem.component

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/** How many composables currently want each view's screen kept on. Main thread only, like composition. */
private val holders = WeakHashMap<View, Int>()

/**
 * Keeps the screen on while this is in the composition (`FLAG_KEEP_SCREEN_ON` through the view). Counted per view: every
 * screen shares the activity's view, so the recipe page in Kookmodus closing behind Stap voor stap must not turn it off.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        holders[view] = (holders[view] ?: 0) + 1
        view.keepScreenOn = true
        onDispose {
            val left = (holders[view] ?: 1) - 1
            if (left > 0) holders[view] = left else holders.remove(view)
            view.keepScreenOn = left > 0
        }
    }
}
