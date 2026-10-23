package nl.potentplum.mise.feature.season

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.domain.CalendarGrouping
import nl.potentplum.mise.domain.CalendarLayout
import nl.potentplum.mise.domain.CalendarSection
import nl.potentplum.mise.domain.SeasonFilter
import nl.potentplum.mise.domain.SeasonPresentation
import nl.potentplum.mise.domain.VegetableInSeason
import nl.potentplum.mise.domain.VegetableSearch
import javax.inject.Inject

enum class SeasonMode { NOW, CALENDAR }

data class SeasonUiState(
    val loading: Boolean = true,
    val month: Int = 1,
    val mode: SeasonMode = SeasonMode.NOW,
    val query: String = "",
    val filter: SeasonFilter = SeasonFilter.AVAILABLE,
    val counts: Map<SeasonFilter, Int> = emptyMap(),
    /** The Nu list: search and filter applied, in the §11.1 order. */
    val rows: List<VegetableInSeason> = emptyList(),
    val grouping: CalendarGrouping = CalendarGrouping.FAMILY,
    val calendar: List<CalendarSection> = emptyList(),
)

@HiltViewModel
class SeasonViewModel
    @Inject
    constructor(
        content: ContentRepository,
        clock: MiseClock,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val month = clock.currentMonth()
        private val vegetables = MutableStateFlow<List<Vegetable>?>(null)
        private val mode = savedState.getStateFlow(KEY_MODE, SeasonMode.NOW.name)
        private val query = savedState.getStateFlow(KEY_QUERY, "")
        private val filter = savedState.getStateFlow(KEY_FILTER, SeasonFilter.AVAILABLE.name)
        private val grouping = savedState.getStateFlow(KEY_GROUPING, CalendarGrouping.FAMILY.name)

        init {
            viewModelScope.launch { vegetables.value = content.content().vegetables }
        }

        val state: StateFlow<SeasonUiState> =
            combine(vegetables, mode, query, filter, grouping) { all, mode, query, filter, grouping ->
                if (all == null) return@combine SeasonUiState(month = month)
                val found = all.filter { VegetableSearch.matches(it, query) }
                val selected = SeasonFilter.valueOf(filter)
                val calendarGrouping = CalendarGrouping.valueOf(grouping)
                SeasonUiState(
                    loading = false,
                    month = month,
                    mode = SeasonMode.valueOf(mode),
                    query = query,
                    filter = selected,
                    counts = SeasonPresentation.counts(found, month),
                    rows = SeasonPresentation.filtered(found, month, selected),
                    grouping = calendarGrouping,
                    calendar = CalendarLayout.sections(found, calendarGrouping),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeasonUiState(month = month))

        fun setMode(value: SeasonMode) {
            savedState[KEY_MODE] = value.name
        }

        fun setQuery(value: String) {
            savedState[KEY_QUERY] = value
        }

        fun setFilter(value: SeasonFilter) {
            savedState[KEY_FILTER] = value.name
        }

        fun setGrouping(value: CalendarGrouping) {
            savedState[KEY_GROUPING] = value.name
        }

        private companion object {
            const val KEY_MODE = "mode"
            const val KEY_QUERY = "query"
            const val KEY_FILTER = "filter"
            const val KEY_GROUPING = "grouping"
        }
    }
