package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.core.designsystem.Contrast
import nl.potentplum.mise.core.designsystem.InstrumentSans
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.hexColor

enum class VegTokenLayout { Horizontal, Vertical }

/**
 * A vegetable's coloured circle with initials, always shown with its name (spec §5): 66 mostly green vegetables make
 * initials alone ambiguous. The initials are ink or white, whichever contrasts more with [color].
 */
@Composable
fun VegToken(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    layout: VegTokenLayout = VegTokenLayout.Horizontal,
    nameStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val circle = @Composable { VegCircle(name, color, size) }
    val label =
        @Composable {
            Text(
                text = name,
                style = nameStyle,
                maxLines = if (layout == VegTokenLayout.Vertical) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (layout == VegTokenLayout.Vertical) TextAlign.Center else TextAlign.Start,
            )
        }
    when (layout) {
        VegTokenLayout.Horizontal -> {
            Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                circle()
                label()
            }
        }

        VegTokenLayout.Vertical -> {
            Column(
                modifier.widthIn(max = size * 2.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                circle()
                label()
            }
        }
    }
}

@Composable
private fun VegCircle(
    name: String,
    color: Color,
    size: Dp,
) {
    // Sized in dp, not sp: the initials are decorative (the name is always beside them) and must stay inside the circle.
    val fontSize = with(LocalDensity.current) { (size * 0.38f).toSp() }
    Box(
        modifier =
            Modifier
                .size(size)
                .background(color, CircleShape)
                .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = vegInitials(name),
            color = Contrast.inkOrWhiteOn(color),
            style = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = fontSize),
        )
    }
}

/** "Witte asperges" → "WA", "Pastinaak" → "Pa", "Wortelen / peen" → "Wo". */
fun vegInitials(name: String): String {
    val words = name.substringBefore(" / ").split(' ', '-').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2).replaceFirstChar { it.uppercaseChar() }
        else -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun VegTokenPreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VegToken(name = "Witte asperges", color = hexColor("#EFE3CD"))
            VegToken(name = "Boerenkool", color = hexColor("#3E5A3A"))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VegToken(name = "Pompoen", color = hexColor("#D9822B"), layout = VegTokenLayout.Vertical, size = 48.dp)
                VegToken(name = "Wortelen / peen", color = hexColor("#E07B39"), layout = VegTokenLayout.Vertical, size = 48.dp)
            }
        }
    }
}
