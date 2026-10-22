package nl.potentplum.mise.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** "Mise — een kooksalmanak". Light only in P1 (spec §0 A7); dynamic colour is off. */
@Composable
fun MiseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMiseColors provides MiseColors.Light) {
        MaterialTheme(
            colorScheme = MiseLightColorScheme,
            typography = MiseTypography,
            shapes = MiseShapes,
            content = content,
        )
    }
}

object MiseTheme {
    val colors: MiseColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMiseColors.current
}
