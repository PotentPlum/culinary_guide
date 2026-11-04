package nl.potentplum.mise.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.AttemptRow
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.repo.AttemptRepository
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.domain.DisciplineStats
import nl.potentplum.mise.domain.JournalAggregator
import nl.potentplum.mise.domain.JournalInsights
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class TimelineFilter { ALL, RECIPES, QUESTS }

data class TimelineMonth(
    val month: YearMonth,
    val entries: List<AttemptRow>,
)

data class JournalUiState(
    val loading: Boolean = true,
    val content: Content? = null,
    val today: LocalDate = LocalDate.now(MiseClock.ZONE),
    val filter: TimelineFilter = TimelineFilter.ALL,
    /** Newest month first. */
    val timeline: List<TimelineMonth> = emptyList(),
    val hasAny: Boolean = false,
    val insights: JournalInsights? = null,
)

sealed interface JournalEvent {
    data class Deleted(
        val attempt: AttemptEntity,
    ) : JournalEvent
}

/** Dagboek › Tijdlijn and Inzichten (spec §6.13, §12). Quests has its own view model. */
@HiltViewModel
class JournalViewModel
    @Inject
    constructor(
        private val attempts: AttemptRepository,
        quests: QuestRepository,
        content: ContentRepository,
        private val clock: MiseClock,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val _events = Channel<JournalEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private val filter = savedState.getStateFlow(KEY_FILTER, TimelineFilter.ALL.name).map(TimelineFilter::valueOf)

        val state: StateFlow<JournalUiState> =
            combine(
                flow { emit(content.content()) },
                attempts.observeTimeline(),
                quests.observeAll().map { all -> all.count { it.status == QuestStatus.COMPLETED } },
                filter,
            ) { content, rows, completed, filter ->
                val today = clock.today()
                val shown =
                    rows.filter { row ->
                        when (filter) {
                            TimelineFilter.ALL -> true
                            TimelineFilter.RECIPES -> row.attempt.sourceType == AttemptSourceType.RECIPE
                            TimelineFilter.QUESTS -> row.attempt.sourceType == AttemptSourceType.QUEST
                        }
                    }
                JournalUiState(
                    loading = false,
                    content = content,
                    today = today,
                    filter = filter,
                    timeline =
                        shown
                            .groupBy { YearMonth.from(DisciplineStats.cookedOn(it.attempt)) }
                            .map { (month, entries) -> TimelineMonth(month, entries) }
                            .sortedByDescending { it.month },
                    hasAny = rows.isNotEmpty(),
                    insights = JournalAggregator.insights(rows.map { it.attempt }, completed, content, today),
                )
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

        fun setFilter(value: TimelineFilter) {
            savedState[KEY_FILTER] = value.name
        }

        fun update(
            id: String,
            score: Int?,
            tasteScore: Int?,
            notes: String,
            cookedOn: LocalDate,
            removePhoto: Boolean,
        ) {
            viewModelScope.launch { attempts.update(id, score, tasteScore, notes, cookedOn, removePhoto) }
        }

        fun delete(id: String) {
            viewModelScope.launch { attempts.deleteForUndo(id)?.let { _events.send(JournalEvent.Deleted(it)) } }
        }

        suspend fun restore(attempt: AttemptEntity) = attempts.restore(attempt)

        fun discardPhoto(attempt: AttemptEntity) = attempts.discardPhoto(attempt)

        private companion object {
            const val KEY_FILTER = "timelineFilter"
        }
    }
