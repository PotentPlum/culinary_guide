package nl.potentplum.mise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.claude.ClaudeService
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.ClaudeKeyState
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import javax.inject.Inject

/** Where the key entry stands (spec §9 Entry UX). */
enum class KeyEntryStep {
    IDLE,
    CHECKING,

    /** 401/403 from `GET /v1/models`: "Ongeldige sleutel". */
    INVALID,

    /** No answer: offer "Toch opslaan". */
    UNREACHABLE,
    SAVED,
}

/**
 * The key entry screen. The typed key is never kept here: the screen holds it in plain composition state (not saved
 * state, which would put it in the saved-instance bundle) and hands it to [check] or [saveAnyway].
 */
@HiltViewModel
class KeyViewModel
    @Inject
    constructor(
        private val claude: ClaudeService,
        private val secrets: SecretStore,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val keyState: StateFlow<ClaudeKeyState> =
            secrets.keyState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClaudeKeyState(ClaudeKeyStatus.NONE))

        private val _step = MutableStateFlow(KeyEntryStep.IDLE)
        val step: StateFlow<KeyEntryStep> = _step.asStateFlow()

        private var job: Job? = null

        /** The live check; a valid key is saved at once. */
        fun check(key: String) {
            val trimmed = key.trim()
            if (trimmed.isEmpty() || _step.value == KeyEntryStep.CHECKING) return
            job =
                viewModelScope.launch {
                    _step.value = KeyEntryStep.CHECKING
                    _step.value =
                        when (claude.checkKey(trimmed)) {
                            KeyCheck.VALID -> save(trimmed)
                            KeyCheck.INVALID -> KeyEntryStep.INVALID
                            KeyCheck.UNREACHABLE -> KeyEntryStep.UNREACHABLE
                        }
                }
        }

        /** "Toch opslaan" after a network error. */
        fun saveAnyway(key: String) {
            val trimmed = key.trim()
            if (trimmed.isEmpty()) return
            job = viewModelScope.launch { _step.value = save(trimmed) }
        }

        fun cancel() {
            job?.cancel()
            _step.value = KeyEntryStep.IDLE
        }

        /** Typing again clears a failed check. */
        fun edited() {
            if (_step.value == KeyEntryStep.INVALID || _step.value == KeyEntryStep.UNREACHABLE || _step.value == KeyEntryStep.SAVED) {
                _step.value = KeyEntryStep.IDLE
            }
        }

        private suspend fun save(key: String): KeyEntryStep {
            secrets.saveClaudeKey(key)
            // A hidden Quest van vandaag card comes back once there is a key (spec §6.2).
            settings.setShowQuestCard(true)
            return KeyEntryStep.SAVED
        }

        companion object {
            /** Soft format check only (spec §9): a key without it can still be checked and saved. */
            fun looksLikeKey(key: String): Boolean = key.trim().startsWith("sk-ant-")
        }
    }
