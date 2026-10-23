package nl.potentplum.mise.core.designsystem.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
