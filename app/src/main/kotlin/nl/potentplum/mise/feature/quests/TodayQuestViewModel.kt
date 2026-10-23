package nl.potentplum.mise.feature.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.quest.QuestGenerator
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.core.network.NetworkStatus
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import javax.inject.Inject

sealed interface TodayQuestUiState {
    data object Loading : TodayQuestUiState

    /** Verbergen was chosen; Instellingen or adding a key brings it back. */
    data object Hidden : TodayQuestUiState

    data object NoKey : TodayQuestUiState

    data class Ready(
        /** Today's quest once generated; null before. */
        val quest: QuestEntity?,
        val generation: QuestGenState,
        val online: Boolean,
    ) : TodayQuestUiState
}

/** The Quest van vandaag card (spec §6.11): one tap, a cancellable generation, at most one daily quest. */
@HiltViewModel
class TodayQuestViewModel
    @Inject
    constructor(
        private val generator: QuestGenerator,
        private val settings: SettingsRepository,
        quests: QuestRepository,
        claude: ClaudeService,
        network: NetworkStatus,
        content: ContentRepository,
    ) : ViewModel() {
        private val generation = QuestGeneration(viewModelScope, generator, content)
        private val _opened = Channel<String>(Channel.BUFFERED)

        /** The new quest's id: the result screen opens. */
        val opened = _opened.receiveAsFlow()

        val state: StateFlow<TodayQuestUiState> =
            combine(
                settings.settings.map { it.showQuestCard },
                claude.hasKey,
                network.online,
                quests.observeDaily(),
                generation.state,
            ) { show, hasKey, online, quest, gen ->
                when {
                    !show -> TodayQuestUiState.Hidden
                    !hasKey -> TodayQuestUiState.NoKey
                    else -> TodayQuestUiState.Ready(quest, gen, online)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayQuestUiState.Loading)

        /** Generates today's quest; a second tap on the same day regenerates and replaces it. */
        fun start() = generation.start(params = { generator.dailyParams() }, onDone = { _opened.send(it.id) })

        fun cancel() = generation.cancel()

        fun dismissError() = generation.dismissError()

        fun hide() {
            viewModelScope.launch { settings.setShowQuestCard(false) }
        }
    }
