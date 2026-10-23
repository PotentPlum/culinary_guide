package nl.potentplum.mise.feature.season

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

/** The Kalender year grid: no horizontal scrolling at any width or font scale. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarGrid(
    sections: List<CalendarSection>,
    currentMonth: Int,
    onVegetable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MonthHeader(currentMonth)
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
                    CalendarRow(vegetable, currentMonth, onClick = { onVegetable(vegetable.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(currentMonth: Int) {
    val initials = stringArrayResource(R.array.months_initial)
    val description = stringResource(R.string.calendar_current_month, monthName(currentMonth))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(Modifier.weight(NAME_WEIGHT))
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
    onClick: () -> Unit,
) {
    val description =
        stringResource(R.string.calendar_row_description, vegetable.nl, monthList(vegetable.peak), monthList(vegetable.shoulder))
    val colors = MiseTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }.padding(horizontal = 16.dp)
            .testTag("calendar-row-${vegetable.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            vegetable.nl,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(NAME_WEIGHT).padding(end = 8.dp),
        )
        Row(Modifier.weight(MONTHS_WEIGHT).height(48.dp)) {
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
                        ).padding(horizontal = 1.dp, vertical = 14.dp)
                        .testTag("calendar-cell-${vegetable.id}-$month"),
                ) { Box(Modifier.fillMaxSize().then(cell)) }
            }
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
