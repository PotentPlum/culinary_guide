package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.TextMatch

/**
 * One kind of tag: the chosen ones as removable chips, and **Kiezen** / **Aanpassen**, which opens a sheet with every option
 * (radio buttons when [single], check boxes otherwise; a search field when [searchable]). With [maxSelected], the other
 * options are disabled once that many are chosen, and the sheet says so.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    single: Boolean = false,
    searchable: Boolean = false,
    maxSelected: Int? = null,
    onToggle: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val labels = options.toMap()
    Text(title, style = MaterialTheme.typography.titleSmall)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        options.filter { it.first in selected }.forEach { (value, label) ->
            val remove = stringResource(R.string.edit_tag_remove, label)
            InputChip(
                selected = true,
                onClick = { onToggle(value) },
                label = { Text(label) },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
                modifier =
                    Modifier.semantics {
                        onClick(remove) {
                            onToggle(value)
                            true
                        }
                    },
            )
        }
        val pick = stringResource(R.string.edit_tag_pick_for, title)
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = pick }) {
            Text(stringResource(if (selected.isEmpty()) R.string.edit_tag_pick else R.string.edit_tag_change))
        }
    }
    if (!open) return
    var query by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { open = false }) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
            )
            val full = maxSelected != null && selected.size >= maxSelected
            if (maxSelected != null) {
                Text(
                    stringResource(R.string.picker_max, maxSelected),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (full) MaterialTheme.colorScheme.primary else MiseTheme.colors.inkSoft,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            if (searchable) {
                MiseSearchField(
                    query = query,
                    onQuery = { query = it },
                    placeholder = stringResource(R.string.techniques_search_placeholder),
                    description = stringResource(R.string.techniques_search_aria_label),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            val shown = options.filter { TextMatch.contains(it.second, query) }
            LazyColumn(Modifier.weight(1f, fill = false).selectableGroup()) {
                items(shown, key = { it.first }) { (value, label) ->
                    val checked = value in selected
                    val enabled = checked || !full
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .then(
                                if (single) {
                                    Modifier.selectable(checked, role = Role.RadioButton) {
                                        onToggle(value)
                                        open = false
                                    }
                                } else {
                                    Modifier.toggleable(checked, enabled = enabled, role = Role.Checkbox) { onToggle(value) }
                                },
                            ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (single) {
                            RadioButton(selected = checked, onClick = null)
                        } else {
                            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                        }
                        Text(
                            labels[value] ?: label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface else MiseTheme.colors.inkSoft,
                        )
                    }
                }
            }
            Button(onClick = { open = false }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.common_done))
            }
        }
    }
}
