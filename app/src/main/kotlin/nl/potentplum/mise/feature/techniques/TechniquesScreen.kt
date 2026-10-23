package nl.potentplum.mise.feature.techniques

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.content.model.TechniqueLevel
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.FilterPillOption
import nl.potentplum.mise.core.designsystem.component.FilterPills
import nl.potentplum.mise.core.designsystem.component.MiseSearchField
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.difficultyLabel
import nl.potentplum.mise.domain.Difficulty

/** Technieken (spec §6.6): categories → subcategories, level filter, search. Cookware guides are marked "Naslag". */
@Composable
fun TechniquesScreen(
    onBack: () -> Unit,
    onTechnique: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: TechniquesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = stringResource(R.string.techniques_page_title),
                onOverflowItem = onOverflowItem,
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(key = "controls") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.techniques_page_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiseTheme.colors.inkSoft,
                    )
                    MiseSearchField(
                        query = state.query,
                        onQuery = viewModel::setQuery,
                        placeholder = stringResource(R.string.techniques_search_placeholder),
                        description = stringResource(R.string.techniques_search_aria_label),
                    )
                    FilterPills(
                        options =
                            listOf(FilterPillOption<TechniqueLevel?>(null, stringResource(R.string.techniques_filter_all))) +
                                TechniqueLevel.entries.map { FilterPillOption(it, difficultyLabel(Difficulty.of(it))) },
                        selected = state.level,
                        onSelect = viewModel::setLevel,
                    )
                }
            }
            if (!state.loading && state.groups.isEmpty()) {
                item(key = "empty") { EmptyState(title = stringResource(R.string.techniques_empty_state)) }
            }
            state.groups.forEach { group ->
                item(key = "category-${group.category}") {
                    Text(
                        categoryLabel(group.category),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier =
                            Modifier
                                .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                                .semantics { heading() },
                    )
                }
                group.subgroups.forEach { subgroup ->
                    subgroup.subcategory?.let { name ->
                        item(key = "sub-${group.category}-$name") {
                            Eyebrow(name, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
                        }
                    }
                    items(subgroup.techniques, key = { it.id }) { TechniqueRow(it, onClick = { onTechnique(it.id) }) }
                }
            }
        }
    }
}

@Composable
private fun TechniqueRow(
    technique: Technique,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(technique.nl, style = MaterialTheme.typography.titleMedium)
                Text(technique.subtitle, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            }
            if (technique.isSkill) {
                DifficultyBadge(Difficulty.of(technique.level))
            } else {
                Text(
                    stringResource(R.string.techniques_reference),
                    style = MaterialTheme.typography.labelMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            }
        }
    }
}
