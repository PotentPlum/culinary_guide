package nl.potentplum.mise.feature.curriculum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.Block
import nl.potentplum.mise.content.model.CourseStatus
import nl.potentplum.mise.content.model.ListItem
import nl.potentplum.mise.content.model.SupportLevel
import nl.potentplum.mise.core.designsystem.JetBrainsMono
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.entity.Band

/**
 * Turns the curriculum's inline marks into styles: `**bold**`, `*italic*` and `` `code` `` (spec §6.17). Unpaired
 * marks stay as they are, so a stray asterisk never hides text.
 */
fun inlineMarks(text: String): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val (mark, style) =
                when {
                    text.startsWith("**", i) -> "**" to SpanStyle(fontWeight = FontWeight.SemiBold)
                    text[i] == '*' -> "*" to SpanStyle(fontStyle = FontStyle.Italic)
                    text[i] == '`' -> "`" to SpanStyle(fontFamily = JetBrainsMono)
                    else -> null to null
                }
            if (mark != null && style != null) {
                val end = text.indexOf(mark, i + mark.length)
                if (end > i + mark.length) {
                    withStyle(style) { append(text, i + mark.length, end) }
                    i = end + mark.length
                    continue
                }
            }
            append(text[i])
            i++
        }
    }

/** Lesson text: paragraphs, lists and tables, in reading order. */
@Composable
fun Blocks(
    blocks: List<Block>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is Block.Paragraph -> MarkedText(block.text, MaterialTheme.typography.bodyLarge)
                is Block.Items -> ListBlock(block.items)
                is Block.Table -> TableBlock(block)
            }
        }
    }
}

@Composable
private fun MarkedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(text) { inlineMarks(text) }
    Text(annotated, style = style, modifier = modifier)
}

@Composable
private fun ListBlock(items: List<ListItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var number = 0
        items.forEach { item ->
            val bullet =
                if (item.marker.isEmpty()) {
                    "•"
                } else {
                    if (item.level == 0) number++
                    if (item.level == 0) "$number." else "–"
                }
            Row(Modifier.padding(start = (item.level * 20).dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(bullet, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                MarkedText(item.text, MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Tables scroll sideways; each column is wide enough to read without wrapping every word. */
@Composable
private fun TableBlock(table: Block.Table) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MiseTheme.colors.line),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
            TableRow(table.header, header = true)
            table.rows.forEach { row ->
                HorizontalDivider(color = MiseTheme.colors.line)
                TableRow(row, header = false)
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    header: Boolean,
) {
    Row {
        cells.forEach { cell ->
            val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
            MarkedText(cell, style, Modifier.width(180.dp).padding(horizontal = 10.dp, vertical = 6.dp))
        }
    }
}

/** "Concept" on a course whose safety items a second reader hasn't all logged (spec §0 D10). */
@Composable
fun DraftLabel(modifier: Modifier = Modifier) {
    // Outlined, not filled: most courses are drafts, so a loud badge on every card would drown the titles.
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MiseTheme.colors.line),
        modifier = modifier,
    ) {
        Text(
            androidx.compose.ui.res
                .stringResource(R.string.curriculum_draft),
            style = MaterialTheme.typography.labelMedium,
            color = MiseTheme.colors.inkSoft,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

fun CourseStatus?.isDraft(): Boolean = this == CourseStatus.DRAFT

val Band.labelRes: Int
    get() =
        when (this) {
            Band.NOT_YET -> R.string.curriculum_band_not_yet
            Band.DEVELOPING -> R.string.curriculum_band_developing
            Band.COMPETENT -> R.string.curriculum_band_competent
            Band.EXCELLENT -> R.string.curriculum_band_excellent
        }

val SupportLevel.labelRes: Int
    get() =
        when (this) {
            SupportLevel.RECIPE -> R.string.curriculum_support_recipe
            SupportLevel.FRAMEWORK -> R.string.curriculum_support_framework
            SupportLevel.BRIEF -> R.string.curriculum_support_brief
        }

/** "12" or "2,5", the Dutch way. */
fun hoursLabel(hours: Double): String = if (hours % 1.0 == 0.0) hours.toInt().toString() else hours.toString().replace('.', ',')

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun BlocksPreview() {
    MiseTheme {
        Blocks(
            listOf(
                Block.Paragraph("Heat the pan **before** the oil goes in; *never* crowd it. Use `EGG-1.M2`."),
                Block.Items(listOf(ListItem(0, "1.", "Crack the egg"), ListItem(0, "1.", "Season"), ListItem(1, "", "Salt"))),
                Block.Table(listOf("Stage", "Sign"), listOf(listOf("Soft", "Runny yolk"), listOf("Hard", "Firm yolk"))),
            ),
            Modifier.padding(16.dp),
        )
    }
}
