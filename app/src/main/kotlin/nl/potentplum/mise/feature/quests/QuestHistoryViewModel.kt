package nl.potentplum.mise.feature.quests

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.quest.QuestGenerator
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.core.network.NetworkStatus
import nl.potentplum.mise.data.db.dao.QuestWithTitle
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.repo.DeletedQuest
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.QuestParams
import javax.inject.Inject

data class QuestFilters(
    val status: QuestStatus? = null,
    val type: QuestType? = null,
    val difficulty: Difficulty? = null,
)

data class QuestHistoryUiState(
    val loading: Boolean = true,
    val quests: List<QuestWithTitle> = emptyList(),
    /** Whether there are any quests at all, so the empty state can tell "none yet" from "none match". */
    val hasAny: Boolean = false,
    val filters: QuestFilters = QuestFilters(),
    val generation: QuestGenState = QuestGenState.Idle,
    val online: Boolean = true,
)

sealed interface QuestHistoryEvent {
    data class Opened(
        val id: String,
    ) : QuestHistoryEvent

    data class Deleted(
        val deleted: DeletedQuest,
    ) : QuestHistoryEvent
}

/** Dagboek › Quests (spec §6.13): filters, reopen, retry with the same parameters, delete with undo. */
@HiltViewModel
class QuestHistoryViewModel
    @Inject
    constructor(
        private val quests: QuestRepository,
        generator: QuestGenerator,
        content: ContentRepository,
        network: NetworkStatus,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val generation = QuestGeneration(viewModelScope, generator, content)
        private val _events = Channel<QuestHistoryEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private val status = savedState.getStateFlow<String?>(KEY_STATUS, null)
        private val type = savedState.getStateFlow<String?>(KEY_TYPE, null)
        private val difficulty = savedState.getStateFlow<String?>(KEY_DIFFICULTY, null)
        private val filters =
            combine(status, type, difficulty) { status, type, difficulty ->
                QuestFilters(status?.let(QuestStatus::valueOf), type?.let(QuestType::valueOf), difficulty?.let(Difficulty::valueOf))
            }

        val state: StateFlow<QuestHistoryUiState> =
            combine(quests.observeHistory(), filters, generation.state, network.online) { all, filters, gen, online ->
                QuestHistoryUiState(
                    loading = false,
                    quests =
                        all.filter { (quest) ->
                            (filters.status == null || quest.status == filters.status) &&
                                (filters.type == null || quest.questType == filters.type) &&
                                (filters.difficulty == null || quest.difficulty == filters.difficulty)
                        },
                    hasAny = all.isNotEmpty(),
                    filters = filters,
                    generation = gen,
                    online = online,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestHistoryUiState())

        fun setStatus(value: QuestStatus?) {
            savedState[KEY_STATUS] = value?.name
        }

        fun setType(value: QuestType?) {
            savedState[KEY_TYPE] = value?.name
        }

        fun setDifficulty(value: Difficulty?) {
            savedState[KEY_DIFFICULTY] = value?.name
        }

        /** Opnieuw: a new quest with the same parameters; it never replaces today's quest. */
        fun retry(id: String) {
            val quest =
                state.value.quests
                    .firstOrNull { it.quest.id == id }
                    ?.quest ?: return
            generation.start(
                params = { QuestParams.of(quest).copy(isDaily = false) },
                onDone = { _events.send(QuestHistoryEvent.Opened(it.id)) },
            )
        }

        fun cancel() = generation.cancel()

        fun dismissError() = generation.dismissError()

        fun delete(id: String) {
            viewModelScope.launch { quests.deleteForUndo(id)?.let { _events.send(QuestHistoryEvent.Deleted(it)) } }
        }

        /** Runs in the snackbar's app-wide scope, so it completes after this screen is gone. */
        suspend fun restore(deleted: DeletedQuest) = quests.restore(deleted)

        private companion object {
            const val KEY_STATUS = "status"
            const val KEY_TYPE = "type"
            const val KEY_DIFFICULTY = "difficulty"
        }
    }
