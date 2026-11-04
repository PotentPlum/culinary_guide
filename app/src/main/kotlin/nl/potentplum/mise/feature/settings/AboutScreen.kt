package nl.potentplum.mise.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.BuildConfig
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem

/** A bundled font and its SIL Open Font License text in `assets/licenses`. */
private enum class FontLicence(
    val label: Int,
    val asset: String,
) {
    InstrumentSerif(R.string.about_font_instrument_serif, "licenses/OFL-InstrumentSerif.txt"),
    InstrumentSans(R.string.about_font_instrument_sans, "licenses/OFL-InstrumentSans.txt"),
    JetBrainsMono(R.string.about_font_jetbrains_mono, "licenses/OFL-JetBrainsMono.txt"),
}

/** Over Mise (spec §6.15): version, data sources, privacy (§15.1), the threat-model note (§9) and open-source licences. */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    var licence by remember { mutableStateOf<FontLicence?>(null) }
    Scaffold(topBar = {
        MiseTopAppBar(title = stringResource(R.string.menu_about), onOverflowItem = onOverflowItem, onBack = onBack)
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.brand_title), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.brand_sub), style = MaterialTheme.typography.titleMedium, color = MiseTheme.colors.inkSoft)
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)

            Paragraph(stringResource(R.string.about_sources_title), stringResource(R.string.about_sources_body))
            Paragraph(stringResource(R.string.about_privacy_title), stringResource(R.string.about_privacy_body))
            Paragraph(stringResource(R.string.about_security_title), stringResource(R.string.about_security_body))

            Eyebrow(stringResource(R.string.about_licences_title))
            Text(stringResource(R.string.about_fonts_body), style = MaterialTheme.typography.bodyMedium)
            FontLicence.entries.forEach { font ->
                TextButton(onClick = { licence = font }) { Text(stringResource(font.label)) }
            }
            Text(stringResource(R.string.about_libraries_body), style = MaterialTheme.typography.bodyMedium)
            stringArrayResource(R.array.about_libraries).forEach { library ->
                Text(library, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            }
        }
    }

    licence?.let { font ->
        val context = LocalContext.current
        val text = remember(font) { context.readAsset(font.asset) }
        AlertDialog(
            onDismissRequest = { licence = null },
            title = { Text(stringResource(font.label)) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(text, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { licence = null }) { Text(stringResource(R.string.detail_close)) } },
        )
    }
}

@Composable
private fun Paragraph(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Eyebrow(title)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Context.readAsset(path: String): String =
    runCatching {
        assets.open(path).bufferedReader().use { it.readText() }
    }.getOrDefault("")
