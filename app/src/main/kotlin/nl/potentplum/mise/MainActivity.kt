package nl.potentplum.mise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.util.MiseClock
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var clock: MiseClock

    private val shell: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val unchecked by shell.uncheckedShoppingCount.collectAsStateWithLifecycle()
            MiseTheme { MiseAppShell(today = clock.today(), uncheckedShoppingCount = unchecked) }
        }
    }
}
