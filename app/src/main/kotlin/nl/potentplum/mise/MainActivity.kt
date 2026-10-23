package nl.potentplum.mise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.util.MiseClock
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var clock: MiseClock

    private val shell: ShellViewModel by viewModels()

    /** A share that arrived and is not handled yet; the shell consumes it. */
    private val pendingShare = MutableStateFlow<SharedContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingShare.value = ShareIntents.read(intent)
        setContent {
            val unchecked by shell.uncheckedShoppingCount.collectAsStateWithLifecycle()
            val share by pendingShare.collectAsStateWithLifecycle()
            MiseTheme {
                MiseAppShell(
                    today = clock.today(),
                    uncheckedShoppingCount = unchecked,
                    pendingShare = share,
                    onShareHandled = { pendingShare.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ShareIntents.read(intent)?.let { pendingShare.value = it }
    }
}
