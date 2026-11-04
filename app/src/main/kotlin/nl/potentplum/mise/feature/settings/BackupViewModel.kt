package nl.potentplum.mise.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.potentplum.mise.data.backup.BackupException
import nl.potentplum.mise.data.backup.BackupManager
import nl.potentplum.mise.data.backup.ReadBackup
import nl.potentplum.mise.data.backup.RestoreMode
import nl.potentplum.mise.data.backup.RestoreResult
import timber.log.Timber
import javax.inject.Inject

sealed interface BackupState {
    data object Idle : BackupState

    data object Working : BackupState

    data class Exported(
        val recipes: Int,
    ) : BackupState

    /** Read and valid: show the summary and Samenvoegen / Vervangen. */
    data class Ready(
        val backup: ReadBackup,
    ) : BackupState

    data class Restored(
        val result: RestoreResult,
    ) : BackupState

    data class Failed(
        /** Null for a file that couldn't be read or written at all. */
        val reason: BackupException.Reason?,
    ) : BackupState
}

/** Back-up maken / Back-up terugzetten (spec §6.15, §13). */
@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val backups: BackupManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
        val state: StateFlow<BackupState> = _state.asStateFlow()

        fun fileName(): String = backups.fileName()

        fun export(uri: Uri) =
            launchOperation {
                val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("no output stream")
                val backup = stream.use { backups.export(it) }
                BackupState.Exported(backup.recipes.count { it.inLibrary })
            }

        fun read(uri: Uri) =
            launchOperation {
                val stream = context.contentResolver.openInputStream(uri) ?: error("no input stream")
                BackupState.Ready(stream.use { backups.read(it) })
            }

        fun restore(mode: RestoreMode) {
            val ready = _state.value as? BackupState.Ready ?: return
            launchOperation { BackupState.Restored(backups.restore(ready.backup, mode)) }
        }

        fun dismiss() {
            (_state.value as? BackupState.Ready)?.let { backups.discard(it.backup) }
            _state.value = BackupState.Idle
        }

        override fun onCleared() {
            (_state.value as? BackupState.Ready)?.let { backups.discard(it.backup) }
        }

        private fun launchOperation(block: suspend () -> BackupState) {
            if (_state.value == BackupState.Working) return
            viewModelScope.launch {
                _state.value = BackupState.Working
                _state.value =
                    try {
                        block()
                    } catch (e: BackupException) {
                        BackupState.Failed(e.reason)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Timber.w(e, "Backup failed")
                        BackupState.Failed(null)
                    }
            }
        }
    }
