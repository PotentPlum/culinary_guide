package nl.potentplum.mise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.claude.enrich.EnrichAllEstimate
import nl.potentplum.mise.claude.enrich.EnrichmentQueue
import nl.potentplum.mise.data.db.dao.UsageTotals
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.data.settings.Settings
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.ClaudeKeyState
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import javax.inject.Inject

data class SettingsUiState(
    val key: ClaudeKeyState = ClaudeKeyState(ClaudeKeyStatus.NONE),
    val settings: Settings = Settings(),
    val usage: UsageTotals = UsageTotals(0, 0, 0),
    val testingKey: Boolean = false,
    /** Set while the "Ingrediënten aanvullen" confirmation is open. */
    val enrichEstimate: EnrichAllEstimate? = null,
)

sealed interface SettingsEvent {
    data class KeyTested(
        val result: KeyCheck?,
    ) : SettingsEvent

    data object KeyDeleted : SettingsEvent

    data object NothingToEnrich : SettingsEvent

    data class EnrichQueued(
        val count: Int,
    ) : SettingsEvent
}

/** Instellingen (spec §6.15): the Claude section and "Ingrediënten aanvullen". Backups have their own view model. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val secrets: SecretStore,
        private val claude: ClaudeService,
        private val enrichment: EnrichmentQueue,
    ) : ViewModel() {
        private val testing = MutableStateFlow(false)
        private val estimate = MutableStateFlow<EnrichAllEstimate?>(null)
        private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        val state: StateFlow<SettingsUiState> =
            combine(secrets.keyState, settingsRepository.settings, claude.thisMonth(), testing, estimate) {
                key,
                settings,
                usage,
                testing,
                estimate,
                ->
                SettingsUiState(key, settings, usage, testing, estimate)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        fun setQuality(quality: Quality) {
            viewModelScope.launch { settingsRepository.setQuality(quality) }
        }

        fun setShowQuestCard(show: Boolean) {
            viewModelScope.launch { settingsRepository.setShowQuestCard(show) }
        }

        fun testKey() {
            if (testing.value) return
            viewModelScope.launch {
                testing.value = true
                val result =
                    try {
                        claude.testStoredKey()
                    } finally {
                        testing.value = false
                    }
                _events.send(SettingsEvent.KeyTested(result))
            }
        }

        fun deleteKey() {
            viewModelScope.launch {
                secrets.deleteClaudeKey()
                _events.send(SettingsEvent.KeyDeleted)
            }
        }

        /** Shows the cost first (spec §6.15); nothing is sent until [confirmEnrich]. */
        fun prepareEnrich() {
            viewModelScope.launch {
                val result = enrichment.estimateAll()
                if (result.recipeIds.isEmpty()) _events.send(SettingsEvent.NothingToEnrich) else estimate.value = result
            }
        }

        fun confirmEnrich() {
            val ids = estimate.value?.recipeIds ?: return
            estimate.value = null
            viewModelScope.launch { _events.send(SettingsEvent.EnrichQueued(enrichment.requestAll(ids))) }
        }

        fun dismissEnrich() {
            estimate.value = null
        }
    }
