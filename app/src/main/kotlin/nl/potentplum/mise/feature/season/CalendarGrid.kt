package nl.potentplum.mise.feature.season

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.designsystem.MiseTextStyles
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.monthList
import nl.potentplum.mise.core.designsystem.component.monthName
import nl.potentplum.mise.domain.CalendarSection

/** The name column takes a third of the width; the twelve months share the rest, so the year always fits (spec §6.4). */
private const val NAME_WEIGHT = 1f
private const val MONTHS_WEIGHT = 2f
private const val NAME_COLUMN_SHARE = NAME_WEIGHT / (NAME_WEIGHT + MONTHS_WEIGHT)

/**
 * The Kalender year grid: no horizontal scrolling at any width or font scale. Names sit in a column beside the months and
 * wrap between words; when the longest word of any name does not fit that column (a small phone, large text), every name
 * goes on its own line above a full-width month bar instead, so no name is cut short or split mid-word.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarGrid(
    sections: List<CalendarSection>,
    currentMonth: Int,
    onVegetable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val nameStyle = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val longestWord =
        remember(sections, nameStyle, density) {
            sections
                .flatMap { it.vegetables }
                .flatMap { it.nl.split(' ') }
                .distinct()
                .maxOfOrNull { measurer.measure(it, nameStyle, maxLines = 1, softWrap = false).size.width } ?: 0
        }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val nameColumn = with(density) { ((maxWidth - 32.dp) * NAME_COLUMN_SHARE - 8.dp).toPx() }
        val stacked = longestWord > nameColumn
        CalendarColumns(sections, currentMonth, stacked, onVegetable)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarColumns(
    sections: List<CalendarSection>,
    currentMonth: Int,
    stacked: Boolean,
    onVegetable: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MonthHeader(currentMonth, stacked)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(Modifier.fillMaxSize()) {
            sections.forEach { section ->
                if (section.family != null) {
                    stickyHeader(key = "family-${section.family}") {
                        Eyebrow(
                            section.family,
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .semantics { heading() },
                        )
                    }
                }
                items(section.vegetables, key = { it.id }) { vegetable ->
                    CalendarRow(vegetable, currentMonth, stacked, onClick = { onVegetable(vegetable.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: Int,
    stacked: Boolean,
) {
    val initials = stringArrayResource(R.array.months_initial)
    val description = stringResource(R.string.calendar_current_month, monthName(currentMonth))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        if (!stacked) Box(Modifier.weight(NAME_WEIGHT))
        Row(Modifier.weight(MONTHS_WEIGHT)) {
            initials.forEachIndexed { index, letter ->
                val now = index + 1 == currentMonth
                Text(
                    text = letter,
                    style = MiseTextStyles.meta.copy(fontWeight = if (now) FontWeight.Medium else FontWeight.Normal),
                    color = if (now) MaterialTheme.colorScheme.onSurface else MiseTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(if (now) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier),
                )
            }
        }
    }
}

@Composable
private fun CalendarRow(
    vegetable: Vegetable,
    currentMonth: Int,
    stacked: Boolean,
    onClick: () -> Unit,
) {
    val description =
        stringResource(R.string.calendar_row_description, vegetable.nl, monthList(vegetable.peak), monthList(vegetable.shoulder))
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }.padding(horizontal = 16.dp)
            .testTag("calendar-row-${vegetable.id}")
    if (stacked) {
        Column(rowModifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(vegetable.nl, style = MaterialTheme.typography.bodyMedium)
            MonthCells(vegetable, currentMonth, Modifier.fillMaxWidth().height(28.dp), cellPadding = 6.dp)
        }
    } else {
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                vegetable.nl,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(NAME_WEIGHT).padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            )
            MonthCells(vegetable, currentMonth, Modifier.weight(MONTHS_WEIGHT).height(48.dp), cellPadding = 14.dp)
        }
    }
}

@Composable
private fun MonthCells(
    vegetable: Vegetable,
    currentMonth: Int,
    modifier: Modifier,
    cellPadding: Dp,
) {
    val colors = MiseTheme.colors
    Row(modifier) {
        for (month in 1..12) {
            val shape = RoundedCornerShape(3.dp)
            val cell =
                when (month) {
                    in vegetable.peak -> {
                        Modifier.background(colors.peak, shape)
                    }

                    in vegetable.shoulder -> {
                        Modifier.background(colors.shoulderFill, shape).border(1.dp, colors.shoulderOutline, shape)
                    }

                    else -> {
                        Modifier.border(1.dp, colors.line, shape)
                    }
                }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (month == currentMonth) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier,
                    ).padding(horizontal = 1.dp, vertical = cellPadding)
                    .testTag("calendar-cell-${vegetable.id}-$month"),
            ) { Box(Modifier.fillMaxSize().then(cell)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360, heightDp = 400)
@Composable
private fun CalendarGridPreview() {
    val sample =
        Vegetable(
            id = "asparagus-white",
            nl = "Witte asperges",
            nameEn = "White asparagus",
            family = nl.potentplum.mise.content.model.Family.STENGEL,
            color = "#EFE3CD",
            accent = "#C9B58A",
            peak = listOf(5, 6),
            shoulder = listOf(4),
            blurb = "",
            storage = "",
            prep = "",
            technique = "",
            pairings = emptyList(),
            substitutions = emptyList(),
            curatedRecipes = emptyList(),
        )
    MiseTheme { CalendarGrid(listOf(CalendarSection("Stengel", listOf(sample))), currentMonth = 10, onVegetable = {}) }
}
