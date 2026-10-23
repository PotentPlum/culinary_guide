package nl.potentplum.mise.feature.season

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.FilterPillOption
import nl.potentplum.mise.core.designsystem.component.FilterPills
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.component.MiseSearchField
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.PeakBar
import nl.potentplum.mise.core.designsystem.component.SeasonBadge
import nl.potentplum.mise.core.designsystem.component.SeasonBadgeKind
import nl.potentplum.mise.core.designsystem.component.StatusDot
import nl.potentplum.mise.core.designsystem.component.VegToken
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.domain.CalendarGrouping
import nl.potentplum.mise.domain.CalendarSection
import nl.potentplum.mise.domain.SeasonFilter
import nl.potentplum.mise.domain.VegetableInSeason

/** Seizoen (spec §6.3–6.4): the Nu list and the Kalender, with one search field for both. */
@Composable
fun SeasonScreen(
    onVegetable: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { MiseTopAppBar(title = stringResource(R.string.nav_seasonal), onOverflowItem = onOverflowItem) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeToggle(state.mode, viewModel::setMode)
                MiseSearchField(
                    query = state.query,
                    onQuery = viewModel::setQuery,
                    placeholder = stringResource(R.string.controls_search_placeholder),
                    description = stringResource(R.string.controls_search_aria_label),
                )
            }
            if (state.loading) return@Column
            when (state.mode) {
                SeasonMode.NOW -> {
                    NowList(state, viewModel::setFilter, onVegetable)
                }

                SeasonMode.CALENDAR -> {
                    CalendarView(
                        sections = state.calendar,
                        currentMonth = state.month,
                        grouping = state.grouping,
                        onGrouping = viewModel::setGrouping,
                        onVegetable = onVegetable,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    mode: SeasonMode,
    onMode: (SeasonMode) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SeasonMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = mode == entry,
                onClick = { onMode(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, SeasonMode.entries.size),
            ) {
                Text(stringResource(if (entry == SeasonMode.NOW) R.string.season_mode_now else R.string.nav_calendar))
            }
        }
    }
}

@Composable
private fun NowList(
    state: SeasonUiState,
    onFilter: (SeasonFilter) -> Unit,
    onVegetable: (String) -> Unit,
) {
    val options =
        listOf(
            FilterPillOption(SeasonFilter.AVAILABLE, stringResource(R.string.filter_available), state.counts[SeasonFilter.AVAILABLE]),
            FilterPillOption(SeasonFilter.PEAK, stringResource(R.string.filter_peak), state.counts[SeasonFilter.PEAK]),
            FilterPillOption(SeasonFilter.SOON, stringResource(R.string.filter_soon), state.counts[SeasonFilter.SOON]),
        )
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "hero") {
            HeroHeader(
                title = styledStringResource(R.string.hero_title),
                subtitle = stringResource(R.string.hero_hero_sub),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item(key = "filters") {
            FilterPills(options, state.filter, onFilter, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        if (state.rows.isEmpty()) {
            item(key = "empty") { EmptyState(title = stringResource(R.string.empty_state)) }
        }
        items(state.rows, key = { it.vegetable.id }) { row ->
            VegetableRow(row, state.month, onClick = { onVegetable(row.vegetable.id) })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun VegetableRow(
    row: VegetableInSeason,
    month: Int,
    onClick: () -> Unit,
) {
    val vegetable = row.vegetable
    Surface(onClick = onClick, color = Color.Transparent) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VegToken(vegetable.nl, hexColor(vegetable.color), nameStyle = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.padding(start = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    vegetable.family.nl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusDot(row.status)
                if (row.lastChance) SeasonBadge(SeasonBadgeKind.LastChance)
                if (row.shortSeason) SeasonBadge(SeasonBadgeKind.ShortSeason)
            }
            PeakBar(vegetable.peak, vegetable.shoulder, Modifier.padding(start = 52.dp), currentMonth = month, compact = true)
        }
    }
}

@Composable
private fun CalendarView(
    sections: List<CalendarSection>,
    currentMonth: Int,
    grouping: CalendarGrouping,
    onGrouping: (CalendarGrouping) -> Unit,
    onVegetable: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        HeroHeader(
            title = styledStringResource(R.string.calendar_title),
            subtitle = stringResource(R.string.calendar_hero_sub),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        FilterPills(
            options =
                listOf(
                    FilterPillOption(CalendarGrouping.FAMILY, stringResource(R.string.calendar_group_family)),
                    FilterPillOption(CalendarGrouping.ALPHABETICAL, stringResource(R.string.calendar_group_az)),
                ),
            selected = grouping,
            onSelect = onGrouping,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (sections.all { it.vegetables.isEmpty() }) {
            EmptyState(title = stringResource(R.string.empty_state))
        } else {
            CalendarGrid(sections, currentMonth, onVegetable)
        }
    }
}
