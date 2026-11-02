package nl.potentplum.mise.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.data.backup.BackupException
import nl.potentplum.mise.data.backup.RestoreMode

/** The word to type before Vervangen wipes the current data (spec §13.2). */
private const val REPLACE_CONFIRMATION = "VERVANGEN"

/**
 * Back-up maken and Back-up terugzetten (spec §6.15, §13): the system file picker, the summary, Samenvoegen / Vervangen
 * (typed confirmation), and "Ingrediënten verrijken" offered afterwards through [onEnrich].
 */
@Composable
fun BackupSection(
    onRestored: () -> Unit,
    /** Null where enrichment can't be confirmed (first run): the dialog then points to Instellingen. */
    onEnrich: (() -> Unit)?,
    showExport: Boolean = true,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let(viewModel::export)
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::read) }

    val exportedMessage = (state as? BackupState.Exported)?.let { pluralStringResource(R.plurals.backup_exported, it.recipes, it.recipes) }
    LaunchedEffect(state) {
        if (exportedMessage != null) {
            snackbar.show(exportedMessage)
            viewModel.dismiss()
        }
        if (state is BackupState.Restored) onRestored()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.backup_sub), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val idle = state != BackupState.Working
            if (showExport) {
                OutlinedButton(onClick = { exportLauncher.launch(viewModel.fileName()) }, enabled = idle) {
                    Text(stringResource(R.string.backup_export))
                }
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = idle) {
                Text(stringResource(R.string.backup_restore))
            }
        }
        if (state == BackupState.Working) LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    when (val current = state) {
        is BackupState.Ready -> {
            RestoreDialog(current, onRestore = viewModel::restore, onDismiss = viewModel::dismiss)
        }

        is BackupState.Restored -> {
            val result = current.result
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text(stringResource(R.string.backup_restored_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary(result.recipes, result.quests, result.attempts))
                        if (result.needsEnrichment > 0) {
                            Text(pluralStringResource(R.plurals.backup_restored_enrich, result.needsEnrichment, result.needsEnrichment))
                            if (onEnrich == null) Text(stringResource(R.string.backup_restored_enrich_later))
                        }
                    }
                },
                confirmButton = {
                    if (result.needsEnrichment > 0 && onEnrich != null) {
                        TextButton(onClick = {
                            viewModel.dismiss()
                            onEnrich()
                        }) { Text(stringResource(R.string.settings_enrich_title)) }
                    } else {
                        TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.common_ok)) }
                    }
                },
                dismissButton =
                    if (result.needsEnrichment > 0 && onEnrich != null) {
                        { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.onboarding_later)) } }
                    } else {
                        null
                    },
            )
        }

        is BackupState.Failed -> {
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text(stringResource(R.string.common_error_title)) },
                text = {
                    Text(
                        stringResource(
                            when (current.reason) {
                                BackupException.Reason.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
                                BackupException.Reason.NEWER_VERSION -> R.string.backup_error_newer
                                BackupException.Reason.CORRUPT -> R.string.backup_error_corrupt
                                null -> R.string.backup_error_io
                            },
                        ),
                    )
                },
                confirmButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.common_ok)) } },
            )
        }

        else -> {
            Unit
        }
    }
}

@Composable
private fun summary(
    recipes: Int,
    quests: Int,
    attempts: Int,
): String =
    stringResource(
        R.string.backup_summary,
        pluralStringResource(R.plurals.backup_count_recipes, recipes, recipes),
        pluralStringResource(R.plurals.backup_count_quests, quests, quests),
        pluralStringResource(R.plurals.backup_count_attempts, attempts, attempts),
    )

@Composable
private fun RestoreDialog(
    ready: BackupState.Ready,
    onRestore: (RestoreMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmReplace by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    if (!confirmReplace) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(summary(ready.backup.recipeCount, ready.backup.questCount, ready.backup.attemptCount))
                    Text(stringResource(R.string.backup_restore_merge_sub), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.backup_restore_replace_sub), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { onRestore(RestoreMode.MERGE) }) { Text(stringResource(R.string.backup_restore_merge)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                    TextButton(onClick = { confirmReplace = true }) { Text(stringResource(R.string.backup_restore_replace)) }
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.backup_replace_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_replace_body, REPLACE_CONFIRMATION))
                    OutlinedTextField(value = typed, onValueChange = { typed = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = { onRestore(RestoreMode.REPLACE) }, enabled = typed.trim() == REPLACE_CONFIRMATION) {
                    Text(stringResource(R.string.backup_restore_replace))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
