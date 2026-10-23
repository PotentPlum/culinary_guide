package nl.potentplum.mise.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.CuratedRecipeCard
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.SeasonBadge
import nl.potentplum.mise.core.designsystem.component.SeasonBadgeKind
import nl.potentplum.mise.core.designsystem.component.SuggestionCard
import nl.potentplum.mise.core.designsystem.component.TonightCard
import nl.potentplum.mise.core.designsystem.component.VegToken
import nl.potentplum.mise.core.designsystem.component.VegTokenLayout
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.domain.CuratedSuggestion
import nl.potentplum.mise.domain.VegetableInSeason
import nl.potentplum.mise.feature.quests.TodayQuestCard
import java.time.LocalDate
import java.util.Locale

/** Where Vandaag leads. */
data class TodayActions(
    val onRecipe: (String) -> Unit,
    val onCook: (String) -> Unit,
    val onImportLink: () -> Unit,
    val onPhotos: () -> Unit,
    val onVegetable: (String) -> Unit,
    val onAllVegetables: () -> Unit,
    val onOpenQuest: (String) -> Unit,
    val onRefineQuest: () -> Unit,
    val onAllQuests: () -> Unit,
    val onAddKey: () -> Unit,
)

/** Vandaag (spec §6.2): hero, Vanavond, suggestions, Quest van vandaag and "Nu op z'n best". Works offline. */
@Composable
fun TodayScreen(
    today: LocalDate,
    actions: TodayActions,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalResources.current
    LaunchedEffect(today) { viewModel.onDay(today) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.AddedToShopping -> {
                    snackbar.show(resources.getQuantityString(R.plurals.shopping_added, event.count, event.count))
                }
            }
        }
    }
    val month = resources.getStringArray(R.array.months_long)[today.monthValue - 1]
    var othersOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { MiseTopAppBar(title = stringResource(R.string.nav_today), onOverflowItem = onOverflowItem) }) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                HeroHeader(
                    title = styledStringResource(R.string.today_hero_title),
                    eyebrow = stringResource(R.string.today_eyebrow, month, today.year),
                )
            }
            if (!state.loading) {
                val tonight = state.tonight
                if (tonight != null) {
                    item(key = "tonight") {
                        TonightCard(
                            title = tonight.title,
                            totalMinutes = tonight.totalMinutes,
                            onOpen = { actions.onRecipe(tonight.id) },
                            onCook = { actions.onCook(tonight.id) },
                            onAddToShopping = viewModel::addTonightToShopping,
                            onUnpin = viewModel::unpin,
                        )
                    }
                    item(key = "others") {
                        TextButton(onClick = { othersOpen = !othersOpen }) {
                            Text(stringResource(R.string.today_other_ideas))
                            Icon(if (othersOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                        }
                    }
                }
                if (tonight == null || othersOpen) suggestions(state, viewModel, actions)
            }
            item(key = "quest") {
                TodayQuestCard(
                    onOpenQuest = actions.onOpenQuest,
                    onRefine = actions.onRefineQuest,
                    onAddKey = actions.onAddKey,
                    onAllQuests = actions.onAllQuests,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            if (state.peak.isNotEmpty()) {
                item(key = "peak") { PeakShortlist(state.peak, state.availableCount, actions) }
            }
        }
    }
}

private fun LazyListScope.suggestions(
    state: TodayUiState,
    viewModel: TodayViewModel,
    actions: TodayActions,
) {
    if (state.libraryEmpty) {
        item(key = "empty") {
            EmptyState(
                title = stringResource(R.string.today_empty_library_title),
                body = stringResource(R.string.today_empty_library_sub),
                icon = Icons.Outlined.MenuBook,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = actions.onImportLink) {
                        Icon(Icons.Outlined.Link, contentDescription = null)
                        Text(stringResource(R.string.recipes_add_link), Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = actions.onPhotos) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                        Text(stringResource(R.string.recipes_add_camera), Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
        if (state.curated.isNotEmpty()) {
            item(key = "curated-title") { SectionTitle(stringResource(R.string.today_curated_title)) }
            curated(state.curated)
        }
        return
    }
    item(key = "suggestions-title") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.today_suggestions_title), Modifier.weight(1f))
            FilterChip(
                selected = state.quickOnly,
                onClick = { viewModel.setQuickOnly(!state.quickOnly) },
                label = { Text(stringResource(R.string.today_quick_filter)) },
            )
        }
    }
    items(state.suggestions, key = { "s-" + it.item.id }) { suggestion ->
        SuggestionCard(
            title = suggestion.item.title,
            reason = reasonLine(suggestion),
            totalMinutes = suggestion.item.totalMinutes,
            onOpen = { actions.onRecipe(suggestion.item.id) },
            onDismiss = { viewModel.dismiss(suggestion.item.id) },
            onCookTonight = { viewModel.pin(suggestion.item.id) },
            modifier = Modifier.animateItem(),
        )
    }
    if (state.quickOnly && state.suggestions.isEmpty() && state.curated.isEmpty()) {
        item(key = "quick-empty") {
            Text(
                stringResource(R.string.today_quick_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MiseTheme.colors.inkSoft,
            )
        }
    }
    curated(state.curated)
}

private fun LazyListScope.curated(links: List<CuratedSuggestion>) {
    items(links, key = { "c-" + it.recipe.url }) { link ->
        val uriHandler = LocalUriHandler.current
        CuratedRecipeCard(
            recipe = link.recipe,
            onOpen = { uriHandler.openUri(link.recipe.url) },
            eyebrow = stringResource(R.string.today_curated_eyebrow, link.vegetable.nl.substringBefore(" / ")),
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.semantics { heading() })
}

/** "Pompoen en boerenkool zijn op hun best · 6 weken niet gekookt" (spec §11.5). */
@Composable
private fun reasonLine(suggestion: SuggestionUi): String {
    val resources = LocalResources.current
    val reason = suggestion.suggestion.reason
    val names = suggestion.peakNames
    val parts =
        buildList {
            when (names.size) {
                0 -> {
                    Unit
                }

                1 -> {
                    add(resources.getString(R.string.today_reason_peak_one, names[0]))
                }

                else -> {
                    val second = names[1].replaceFirstChar { it.lowercase(Locale.forLanguageTag("nl")) }
                    add(resources.getString(R.string.today_reason_peak_two, names[0], second))
                }
            }
            reason.weeksNotCooked?.let { add(resources.getQuantityString(R.plurals.today_reason_weeks, it, it)) }
            if (reason.neverCooked) add(resources.getString(R.string.today_reason_never))
            if (reason.favourite) add(resources.getString(R.string.today_reason_favourite))
        }
    return parts.reduceOrNull { line, part -> resources.getString(R.string.today_reason_join, line, part) }.orEmpty()
}

@Composable
private fun PeakShortlist(
    peak: List<VegetableInSeason>,
    availableCount: Int,
    actions: TodayActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.home_spotlight_title), Modifier.weight(1f))
            TextButton(onClick = actions.onAllVegetables) { Text(stringResource(R.string.home_spotlight_more, availableCount)) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(peak, key = { it.vegetable.id }) { row ->
                Surface(onClick = { actions.onVegetable(row.vegetable.id) }, color = Color.Transparent) {
                    Column(
                        Modifier
                            .width(96.dp)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        VegToken(
                            row.vegetable.nl,
                            hexColor(row.vegetable.color),
                            layout = VegTokenLayout.Vertical,
                            size = 48.dp,
                            nameStyle = MaterialTheme.typography.bodyMedium,
                        )
                        if (row.lastChance) SeasonBadge(SeasonBadgeKind.LastChance)
                        if (row.shortSeason) SeasonBadge(SeasonBadgeKind.ShortSeason)
                    }
                }
            }
        }
    }
}
