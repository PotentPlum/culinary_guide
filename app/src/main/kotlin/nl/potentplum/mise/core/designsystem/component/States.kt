package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme

/** A friendly empty screen or section: icon, serif title, explanation, and optional actions below. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        actions()
    }
}

/**
 * A screen that is still reading its data: a spinner in the middle, shown only after [delayMillis] so that fast loads
 * don't flash it. Without it the screen stays blank, which reads as broken.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    delayMillis: Long = 300,
) {
    var visible by remember { mutableStateOf(delayMillis <= 0) }
    LaunchedEffect(delayMillis) {
        delay(delayMillis)
        visible = true
    }
    val description = stringResource(R.string.common_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (visible) CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

/** A failed operation returns to a stable state with a retry button (spec §15.5). */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    EmptyState(
        title = stringResource(R.string.common_error_title),
        body = message,
        icon = Icons.Outlined.ErrorOutline,
        modifier = modifier,
    ) {
        if (onRetry != null) Button(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun StatesPreview() {
    MiseTheme {
        Column {
            EmptyState(
                title = "Nog geen recepten",
                body = "Voeg je eerste recept toe via een link of een foto van een kookboek.",
                icon = Icons.AutoMirrored.Outlined.MenuBook,
            ) { Button(onClick = {}) { Text("Eerste recept toevoegen") } }
            ErrorState(message = "Kon de pagina niet bereiken. Controleer je verbinding.", onRetry = {})
        }
    }
}
