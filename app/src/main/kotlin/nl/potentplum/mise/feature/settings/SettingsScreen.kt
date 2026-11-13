package nl.potentplum.mise.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.util.DutchNumbers
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.security.ClaudeKeyStatus

/** Instellingen (spec §6.15): Claude, Gegevens and Over. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onKey: () -> Unit,
    onAbout: () -> Unit,
    onRestored: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message =
                when (event) {
                    is SettingsEvent.KeyTested -> {
                        resources.getString(
                            when (event.result) {
                                KeyCheck.VALID -> R.string.settings_key_test_ok
                                KeyCheck.INVALID -> R.string.key_invalid
                                KeyCheck.UNREACHABLE -> R.string.key_unreachable
                                null -> R.string.settings_key_none
                            },
                        )
                    }

                    SettingsEvent.KeyDeleted -> {
                        resources.getString(R.string.settings_key_deleted)
                    }

                    SettingsEvent.NothingToEnrich -> {
                        resources.getString(R.string.settings_enrich_none)
                    }

                    is SettingsEvent.EnrichQueued -> {
                        resources.getQuantityString(R.plurals.settings_enrich_queued, event.count, event.count)
                    }
                }
            snackbar.show(message)
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        MiseTopAppBar(title = stringResource(R.string.menu_settings), onOverflowItem = onOverflowItem, onBack = onBack)
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(stringResource(R.string.settings_section_claude)) {
                KeyStatus(state)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onKey) {
                        Text(
                            stringResource(
                                if (state.key.status == ClaudeKeyStatus.NONE ||
                                    state.key.status == ClaudeKeyStatus.NEEDS_REENTRY
                                ) {
                                    R.string.settings_key_add
                                } else {
                                    R.string.settings_key_replace
                                },
                            ),
                        )
                    }
                    if (state.key.masked != null) {
                        OutlinedButton(onClick = viewModel::testKey, enabled = !state.testingKey) {
                            Text(stringResource(R.string.settings_key_test))
                        }
                        if (state.testingKey) CircularProgressIndicator(Modifier.padding(4.dp))
                    }
                }
                if (state.key.masked != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.settings_key_delete)) }
                }

                // Model choice and spend mean nothing before there is a key to spend with.
                if (state.key.masked != null) {
                    Text(stringResource(R.string.settings_quality_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_quality_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                    Column(Modifier.selectableGroup()) {
                        Quality.entries.forEach { quality ->
                            QualityRow(quality, selected = state.settings.quality == quality) { viewModel.setQuality(quality) }
                        }
                    }

                    Text(stringResource(R.string.settings_usage_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.settings_usage_value,
                            DutchNumbers.grouped(state.usage.inputTokens + state.usage.outputTokens),
                            DutchNumbers.euros(state.usage.costMicroUsd),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.settings_usage_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiseTheme.colors.inkSoft,
                    )
                }

                SwitchRow(
                    label = stringResource(R.string.settings_show_quest_card),
                    checked = state.settings.showQuestCard,
                    onChange = viewModel::setShowQuestCard,
                )
            }

            HorizontalDivider()
            Section(stringResource(R.string.settings_section_data)) {
                BackupSection(onRestored = onRestored, onEnrich = viewModel::prepareEnrich)
                Text(stringResource(R.string.settings_enrich_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_enrich_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
                OutlinedButton(onClick = viewModel::prepareEnrich, enabled = state.key.status == ClaudeKeyStatus.STORED) {
                    Text(stringResource(R.string.settings_enrich_title))
                }
            }

            HorizontalDivider()
            Section(stringResource(R.string.menu_about)) {
                Text(
                    stringResource(R.string.settings_about_open),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
                OutlinedButton(onClick = onAbout) { Text(stringResource(R.string.settings_about_button)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_key_delete_title)) },
            text = { Text(stringResource(R.string.settings_key_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteKey()
                }) { Text(stringResource(R.string.settings_key_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    state.enrichEstimate?.let { estimate ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEnrich,
            title = { Text(stringResource(R.string.settings_enrich_confirm_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.settings_enrich_confirm_body,
                        estimate.recipeIds.size,
                        estimate.recipeIds.size,
                        DutchNumbers.euros(estimate.costMicroUsd),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmEnrich) { Text(stringResource(R.string.settings_enrich_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissEnrich) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(title)
        content()
    }
}

@Composable
private fun KeyStatus(state: SettingsUiState) {
    val masked = state.key.masked.orEmpty()
    val (text, error) =
        when (state.key.status) {
            ClaudeKeyStatus.NONE -> stringResource(R.string.settings_key_none) to false
            ClaudeKeyStatus.STORED -> stringResource(R.string.settings_key_stored, masked) to false
            ClaudeKeyStatus.INVALID -> stringResource(R.string.settings_key_invalid, masked) to true
            ClaudeKeyStatus.NEEDS_REENTRY -> stringResource(R.string.settings_key_reentry_body) to true
        }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun QualityRow(
    quality: Quality,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val (label, sub) =
        when (quality) {
            Quality.FAST -> R.string.settings_quality_fast to R.string.settings_quality_fast_sub
            Quality.BALANCED -> R.string.settings_quality_balanced to R.string.settings_quality_balanced_sub
            Quality.BEST -> R.string.settings_quality_best to R.string.settings_quality_best_sub
        }
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(sub), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        }
    }
}

@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
