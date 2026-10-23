package nl.potentplum.mise.feature.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.MiseSearchField
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.RecipeCard
import nl.potentplum.mise.domain.LibraryFilter
import nl.potentplum.mise.domain.LibrarySortOrder
import nl.potentplum.mise.domain.PrepTimeBucket

/** Ways to add a recipe (spec §6.7 FAB sheet). */
enum class AddRecipeOption { Link, Camera, Gallery, Manual }

/** Recepten — bibliotheek (spec §6.7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onRecipe: (String) -> Unit,
    onAdd: (AddRecipeOption) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addSheet by rememberSaveable { mutableStateOf(false) }
    var filterSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MiseTopAppBar(
                title = stringResource(R.string.nav_recipes),
                onOverflowItem = onOverflowItem,
                actions = { SortMenu(state.sort, viewModel::setSort) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addSheet = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.recipes_add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.loading && state.libraryEmpty) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.recipes_empty_title),
                        body = stringResource(R.string.recipes_empty_sub),
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                    ) { Button(onClick = { addSheet = true }) { Text(stringResource(R.string.recipes_empty_cta)) } }
                }
                return@LazyColumn
            }
            item(key = "search") {
                MiseSearchField(
                    query = state.filter.query,
                    onQuery = { viewModel.setFilter(state.filter.copy(query = it)) },
                    placeholder = stringResource(R.string.controls_search_placeholder),
                    description = stringResource(R.string.recipes_search_aria_label),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item(key = "chips") { QuickFilters(state, viewModel::setFilter, onMore = { filterSheet = true }) }
            if (!state.loading && state.cards.isEmpty()) {
                item(key = "no-results") {
                    EmptyState(title = stringResource(R.string.recipes_no_results_filters)) {
                        OutlinedButton(onClick = { viewModel.setFilter(LibraryFilter()) }) {
                            Text(stringResource(R.string.recipes_clear_all))
                        }
                    }
                }
            }
            items(state.cards, key = { it.item.id }) { card ->
                RecipeCard(
                    title = card.item.title,
                    source = card.item.sourceLabel,
                    totalMinutes = card.item.totalMinutes,
                    cookedCount = card.item.cookedCount,
                    imagePath = card.item.imagePath,
                    placeholderColor = card.placeholderColor,
                    isFavorite = card.item.isFavorite,
                    inSeason = card.inSeason,
                    onClick = { onRecipe(card.item.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text(
                    stringResource(R.string.recipes_add_panel_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
                )
                listOf(
                    Triple(AddRecipeOption.Link, Icons.Outlined.Link, R.string.recipes_add_link),
                    Triple(AddRecipeOption.Camera, Icons.Outlined.CameraAlt, R.string.recipes_add_camera),
                    Triple(AddRecipeOption.Gallery, Icons.Outlined.PhotoLibrary, R.string.recipes_add_gallery),
                    Triple(AddRecipeOption.Manual, Icons.Outlined.Edit, R.string.recipes_add_manual),
                ).forEach { (option, icon, label) ->
                    ListItem(
                        headlineContent = { Text(stringResource(label)) },
                        leadingContent = { Icon(icon, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier =
                            Modifier.clickable {
                                addSheet = false
                                onAdd(option)
                            },
                    )
                }
            }
        }
    }

    if (filterSheet) {
        ModalBottomSheet(onDismissRequest = { filterSheet = false }) {
            MoreFilters(state, viewModel::setFilter, onDone = { filterSheet = false })
        }
    }
}

@Composable
private fun SortMenu(
    sort: LibrarySortOrder,
    onSort: (LibrarySortOrder) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.recipes_sort_label))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        LibrarySortOrder.entries.forEach { entry ->
            DropdownMenuItem(
                text = { Text(stringResource(entry.labelRes)) },
                leadingIcon = { RadioButton(selected = entry == sort, onClick = null) },
                onClick = {
                    open = false
                    onSort(entry)
                },
            )
        }
    }
}

private val LibrarySortOrder.labelRes: Int
    get() =
        when (this) {
            LibrarySortOrder.RECENTLY_ADDED -> R.string.recipes_sort_recent
            LibrarySortOrder.ALPHABETICAL -> R.string.recipes_sort_alpha
            LibrarySortOrder.RECENTLY_COOKED -> R.string.recipes_sort_cooked
        }

@Composable
private fun QuickFilters(
    state: LibraryUiState,
    onFilter: (LibraryFilter) -> Unit,
    onMore: () -> Unit,
) {
    val filter = state.filter
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = filter.seasonNow,
                onClick = { onFilter(filter.copy(seasonNow = !filter.seasonNow)) },
                label = { Text(stringResource(R.string.recipes_filter_season_now)) },
            )
        }
        item {
            FilterChip(
                selected = filter.favorites,
                onClick = { onFilter(filter.copy(favorites = !filter.favorites)) },
                label = { Text(stringResource(R.string.recipes_filter_favorites)) },
            )
        }
        item {
            FilterChip(
                selected = filter.neverCooked,
                onClick = { onFilter(filter.copy(neverCooked = !filter.neverCooked)) },
                label = { Text(stringResource(R.string.recipes_filter_never_cooked)) },
            )
        }
        items(state.options.dishTypes, key = { it.value }) { option ->
            val selected = filter.dishType == option.value
            FilterChip(
                selected = selected,
                onClick = { onFilter(filter.copy(dishType = if (selected) null else option.value)) },
                label = { Text(option.nl) },
            )
        }
        item {
            FilterChip(
                selected = filter.moreCount > 0,
                onClick = onMore,
                leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                label = {
                    Text(
                        if (filter.moreCount > 0) {
                            stringResource(R.string.recipes_more_filters_count, filter.moreCount)
                        } else {
                            stringResource(R.string.recipes_more_filters)
                        },
                    )
                },
            )
        }
    }
}

/** "Meer filters" (spec §6.7): cuisine, main ingredient, technique and time; one choice each, tap again to clear. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreFilters(
    state: LibraryUiState,
    onFilter: (LibraryFilter) -> Unit,
    onDone: () -> Unit,
) {
    val filter = state.filter
    val options = state.options
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.recipes_more_filters),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        FilterGroup(options.titles.cuisine, options.cuisines.map { it.value to it.nl }, filter.cuisine) {
            onFilter(filter.copy(cuisine = it))
        }
        FilterGroup(options.titles.mainIngredient, options.mainIngredients.map { it.value to it.nl }, filter.mainIngredient) {
            onFilter(filter.copy(mainIngredient = it))
        }
        FilterGroup(
            options.titles.prepTime,
            options.prepTimes.map { it.value to it.nl },
            filter.prepTime?.facetValue,
        ) { value -> onFilter(filter.copy(prepTime = PrepTimeBucket.entries.firstOrNull { it.facetValue == value })) }
        FilterGroup(stringResource(R.string.recipes_filter_technique), options.techniques.map { it.id to it.nl }, filter.technique) {
            onFilter(filter.copy(technique = it))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = { onFilter(filter.copy(cuisine = null, mainIngredient = null, technique = null, prepTime = null)) }) {
                Text(stringResource(R.string.recipes_facet_filter_reset))
            }
            Button(onClick = onDone) { Text(stringResource(R.string.common_done)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(if (value == selected) null else value) },
                    label = { Text(label) },
                )
            }
        }
    }
}
