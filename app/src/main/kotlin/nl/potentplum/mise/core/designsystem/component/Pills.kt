package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.core.designsystem.MiseTextStyles
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.hexColor

data class FilterPillOption<T>(
    val value: T,
    val label: String,
    val count: Int? = null,
)

/** A single-choice row of pills with counts. The active pill is a terracotta fill with white text (4.9:1). */
@Composable
fun <T> FilterPills(
    options: List<FilterPillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = option.value == selected
            val content = if (active) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
            Surface(
                shape = CircleShape,
                color = if (active) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                contentColor = content,
                border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(option.value) }),
            ) {
                Row(
                    Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                    if (option.count != null) Text(option.count.toString(), style = MiseTextStyles.meta)
                }
            }
        }
    }
}

data class FacetChip(
    val label: String,
    /** Optional `#RRGGBB` from `facets.json`, shown as a small leading dot. */
    val color: String? = null,
)

/** Tag chips (cuisine, dish type, main ingredient). Interactive when [onClick] is set. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FacetChips(
    chips: List<FacetChip>,
    modifier: Modifier = Modifier,
    onClick: ((FacetChip) -> Unit)? = null,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEach { chip ->
            val shape = CircleShape
            val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            val body =
                @Composable {
                    Row(
                        Modifier.heightIn(min = 28.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (chip.color != null) {
                            Surface(Modifier.padding(vertical = 2.dp), shape = CircleShape, color = hexColor(chip.color)) {
                                Column(Modifier.padding(4.dp)) {}
                            }
                        }
                        Text(chip.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            if (onClick != null) {
                Surface(
                    onClick = { onClick(chip) },
                    shape = shape,
                    border = border,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    content = body,
                )
            } else {
                Surface(shape = shape, border = border, color = MaterialTheme.colorScheme.surfaceContainerLow, content = body)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun PillsPreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterPills(
                options =
                    listOf(
                        FilterPillOption(0, "Verkrijgbaar", 52),
                        FilterPillOption(1, "Op z'n best", 38),
                        FilterPillOption(2, "Komt eraan", 0),
                    ),
                selected = 0,
                onSelect = {},
            )
            FacetChips(listOf(FacetChip("Italiaans", "#C25B2A"), FacetChip("Hoofdgerecht"), FacetChip("Groente")))
            FacetChips(listOf(FacetChip("Frans", "#2A6BB9"), FacetChip("Saus")), onClick = {})
        }
    }
}
