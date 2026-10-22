package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTextStyles
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.styledStringResource
import java.util.Locale

private val Dutch: Locale = Locale.forLanguageTag("nl-NL")

/** Mono meta label in uppercase. The resource stays in sentence case and TalkBack reads that (spec §5). */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MiseTheme.colors.inkSoft,
) {
    Text(
        text = text.uppercase(Dutch),
        style = MiseTextStyles.eyebrow,
        color = color,
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
    )
}

/** A screen's editorial header: eyebrow, serif headline with italic emphasis, optional subtitle. */
@Composable
fun HeroHeader(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (eyebrow != null) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun HeroHeaderPreview() {
    MiseTheme {
        HeroHeader(
            title = styledStringResource(R.string.today_hero_title),
            eyebrow = "Nederland · Oktober 2026",
            subtitle = "Lokale groenten, gegroeid op Nederlandse bodem.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
