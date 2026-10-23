package nl.potentplum.mise.feature.vegetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CuratedRecipe
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.designsystem.Contrast
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.DifficultyBadge
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.FacetChip
import nl.potentplum.mise.core.designsystem.component.FacetChips
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.PeakBar
import nl.potentplum.mise.core.designsystem.component.SeasonBadge
import nl.potentplum.mise.core.designsystem.component.SeasonBadgeKind
import nl.potentplum.mise.core.designsystem.component.StatusDot
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.domain.Difficulty
import java.net.URI

/** Groente-detail (spec §6.5). "In jouw recepten" arrives with the recipe library; "Jouw dagboek" is P2. */
@Composable
fun VegetableScreen(
    onBack: () -> Unit,
    onVegetable: (String) -> Unit,
    onTechnique: (String) -> Unit,
    onQuest: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: VegetableViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title =
        (state as? VegetableUiState.Loaded)
            ?.presentation
            ?.vegetable
            ?.nl
            .orEmpty()
    Scaffold(topBar = { MiseTopAppBar(title = title, onOverflowItem = onOverflowItem, onBack = onBack) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val current = state) {
                VegetableUiState.Loading -> Unit
                VegetableUiState.NotFound -> EmptyState(title = stringResource(R.string.error_not_found))
                is VegetableUiState.Loaded -> VegetableDetail(current, onVegetable, onTechnique, onQuest)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VegetableDetail(
    state: VegetableUiState.Loaded,
    onVegetable: (String) -> Unit,
    onTechnique: (String) -> Unit,
    onQuest: (String) -> Unit,
) {
    val vegetable = state.presentation.vegetable
    val uriHandler = LocalUriHandler.current
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header(state)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(vegetable.blurb, style = MaterialTheme.typography.bodyLarge)
            Section(stringResource(R.string.detail_section_storage)) { Body(vegetable.storage) }
            Section(stringResource(R.string.detail_section_prep)) { Body(vegetable.prep) }
            Section(stringResource(R.string.detail_section_technique)) { Body(vegetable.technique) }
            Section(stringResource(R.string.detail_section_pairings)) { FacetChips(vegetable.pairings.map { FacetChip(it) }) }
            Section(stringResource(R.string.detail_section_subs)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.substitutions.forEach { substitution ->
                        val target = substitution.vegetableId
                        FacetChips(
                            listOf(FacetChip(substitution.text)),
                            onClick = if (target != null) ({ onVegetable(target) }) else null,
                        )
                    }
                }
            }
            if (state.techniques.isNotEmpty()) {
                Section(stringResource(R.string.detail_section_techniques)) {
                    state.techniques.forEach { TechniqueLink(it, onClick = { onTechnique(it.id) }) }
                }
            }
            if (vegetable.curatedRecipes.isNotEmpty()) {
                Section(stringResource(R.string.detail_section_ideas)) {
                    vegetable.curatedRecipes.forEach { CuratedRecipeCard(it, onOpen = { uriHandler.openUri(it.url) }) }
                }
            }
            Button(onClick = { onQuest(vegetable.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.detail_quest_with, vegetable.nl))
            }
            vegetable.sourceUrl?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(R.string.detail_source, URI(url).host.removePrefix("www.")))
                }
            }
        }
    }
}

/** The vegetable's colour as a band with ink or white text, then status, badges and the year in a card. */
@Composable
private fun Header(state: VegetableUiState.Loaded) {
    val vegetable = state.presentation.vegetable
    val fill = hexColor(vegetable.color)
    val onFill = Contrast.inkOrWhiteOn(fill)
    Column(
        Modifier
            .fillMaxWidth()
            .background(fill)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(
                vegetable.nl,
                style = MaterialTheme.typography.headlineLarge,
                color = onFill,
                modifier = Modifier.semantics { heading() },
            )
            Text(vegetable.family.nl, style = MaterialTheme.typography.bodyMedium, color = onFill)
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(state.presentation.status)
                    if (state.presentation.lastChance) SeasonBadge(SeasonBadgeKind.LastChance)
                    if (state.presentation.shortSeason) SeasonBadge(SeasonBadgeKind.ShortSeason)
                }
                PeakBar(vegetable.peak, vegetable.shoulder, currentMonth = state.month)
                Legend()
            }
        }
    }
}

@Composable
private fun Legend() {
    val colors = MiseTheme.colors
    val shape = RoundedCornerShape(2.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(12.dp).background(colors.peak, shape))
            Text(stringResource(R.string.detail_legend_peak), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(12.dp).background(colors.shoulderFill, shape).border(1.dp, colors.shoulderOutline, shape))
            Text(stringResource(R.string.detail_legend_shoulder), style = MaterialTheme.typography.labelMedium)
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
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun TechniqueLink(
    technique: Technique,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(technique.nl, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(technique.subtitle, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        }
    }
}

@Composable
private fun CuratedRecipeCard(
    recipe: CuratedRecipe,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.detail_open_link),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                stringResource(R.string.detail_curated_meta, recipe.source, recipe.timeMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MiseTheme.colors.inkSoft,
            )
            Text(recipe.why, style = MaterialTheme.typography.bodySmall)
            DifficultyBadge(Difficulty.of(recipe.difficulty))
        }
    }
}
