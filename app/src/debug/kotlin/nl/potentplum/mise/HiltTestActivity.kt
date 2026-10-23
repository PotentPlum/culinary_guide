package nl.potentplum.mise

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/** Host for Compose UI tests that need Hilt ViewModels (debug builds only). */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
