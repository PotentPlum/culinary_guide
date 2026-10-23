package nl.potentplum.mise.feature.techniques

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.VegToken
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.domain.Difficulty

/** Technique detail (spec §6.6). "Zo vaak geoefend" arrives with the journal (P2). */
@Composable
fun TechniqueScreen(
    onBack: () -> Unit,
    onTechnique: (String) -> Unit,
    onVegetable: (String) -> Unit,
    onQuest: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: TechniqueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = (state as? TechniqueUiState.Loaded)?.technique?.nl.orEmpty()
    Scaffold(topBar = { MiseTopAppBar(title = title, onOverflowItem = onOverflowItem, onBack = onBack) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val current = state) {
                TechniqueUiState.Loading -> Unit
                TechniqueUiState.NotFound -> EmptyState(title = stringResource(R.string.error_not_found))
                is TechniqueUiState.Loaded -> TechniqueDetail(current, onTechnique, onVegetable, onQuest)
            }
        }
    }
}

@Composable
private fun TechniqueDetail(
    state: TechniqueUiState.Loaded,
    onTechnique: (String) -> Unit,
    onVegetable: (String) -> Unit,
    onQuest: (String) -> Unit,
) {
    val technique = state.technique
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(listOfNotNull(categoryLabel(technique.category), technique.subcategory).joinToString(" · "))
            Text(technique.nl, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(technique.subtitle, style = MaterialTheme.typography.bodyLarge, color = MiseTheme.colors.inkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (technique.isSkill) {
                    DifficultyBadge(Difficulty.of(technique.level))
                } else {
                    Text(stringResource(R.string.techniques_reference), style = MaterialTheme.typography.labelMedium)
                }
                technique.heatLevel?.let {
                    Text(
                        heatLabel(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MiseTheme.colors.inkSoft,
                    )
                }
            }
        }
        technique.body.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        if (technique.tips.isNotEmpty()) Section(stringResource(R.string.techniques_section_tips)) { Bullets(technique.tips) }
        if (technique.cookware.isNotEmpty()) Section(stringResource(R.string.techniques_section_cookware)) { Bullets(technique.cookware) }
        if (state.vegetables.isNotEmpty()) {
            Section(stringResource(R.string.techniques_section_related_veg)) {
                state.vegetables.forEach { vegetable ->
                    Surface(onClick = { onVegetable(vegetable.id) }, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                        VegToken(vegetable.nl, hexColor(vegetable.color), Modifier.padding(vertical = 4.dp), size = 32.dp)
                    }
                }
            }
        }
        state.parent?.let { parent -> Section(stringResource(R.string.techniques_parent)) { Links(listOf(parent), onTechnique) } }
        if (state.children.isNotEmpty()) Section(stringResource(R.string.techniques_children)) { Links(state.children, onTechnique) }
        if (state.related.isNotEmpty()) {
            Section(stringResource(R.string.techniques_section_related_techniques)) { Links(state.related, onTechnique) }
        }
        // Cookware guides are reference pages, never practised in a quest (spec §6.6).
        if (technique.isSkill) {
            Button(onClick = { onQuest(technique.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.techniques_practice))
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Bullets(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Links(
    techniques: List<Technique>,
    onTechnique: (String) -> Unit,
) {
    techniques.forEach { technique ->
        Surface(onClick = { onTechnique(technique.id) }, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(technique.nl, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Text(technique.subtitle, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            }
        }
    }
}
