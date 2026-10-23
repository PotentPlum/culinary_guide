package nl.potentplum.mise.feature.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.FacetOption
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.designsystem.component.VegTokenData
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.settings.LibrarySort
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.LibraryFilter
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.domain.LibraryQuery
import nl.potentplum.mise.domain.LibrarySortOrder
import nl.potentplum.mise.domain.SeasonPresentation
import javax.inject.Inject

data class LibraryCard(
    val item: LibraryItem,
    /** Vegetables in the recipe that are at their best or available now. */
    val inSeason: List<VegTokenData>,
    /** Placeholder colour from the first cuisine tag, when there is no image. */
    val placeholderColor: String?,
)

/** Facet titles as written in `facets.json` (content, Dutch). */
data class FacetTitles(
    val cuisine: String = "",
    val mainIngredient: String = "",
    val prepTime: String = "",
)

data class LibraryOptions(
    val titles: FacetTitles = FacetTitles(),
    val dishTypes: List<FacetOption> = emptyList(),
    val cuisines: List<FacetOption> = emptyList(),
    val mainIngredients: List<FacetOption> = emptyList(),
    val prepTimes: List<FacetOption> = emptyList(),
    val techniques: List<Technique> = emptyList(),
)

data class LibraryUiState(
    val loading: Boolean = true,
    val libraryEmpty: Boolean = false,
    val cards: List<LibraryCard> = emptyList(),
    val filter: LibraryFilter = LibraryFilter(),
    val sort: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val options: LibraryOptions = LibraryOptions(),
)

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        repository: RecipeRepository,
        content: ContentRepository,
        clock: MiseClock,
        private val settings: SettingsRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val month = clock.currentMonth()
        private val filter =
            savedState.getStateFlow(KEY_FILTER, Json.encodeToString(LibraryFilter.serializer(), LibraryFilter())).map {
                Json.decodeFromString(LibraryFilter.serializer(), it)
            }

        val state: StateFlow<LibraryUiState> =
            combine(
                flow { emit(content.content()) },
                repository.observeLibrary(),
                filter,
                settings.settings.map { it.librarySort.toOrder() },
            ) { content, items, filter, sort ->
                val available =
                    content.vegetables.filter { SeasonPresentation.present(it, month).status.isAvailable }.associateBy { it.id }
                LibraryUiState(
                    loading = false,
                    libraryEmpty = items.isEmpty(),
                    cards = LibraryQuery.apply(items, filter, sort, available.keys).map { card(it, available, content) },
                    filter = filter,
                    sort = sort,
                    options = options(content),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

        fun setFilter(value: LibraryFilter) {
            savedState[KEY_FILTER] = Json.encodeToString(LibraryFilter.serializer(), value)
        }

        fun setSort(value: LibrarySortOrder) {
            viewModelScope.launch { settings.setLibrarySort(value.toSetting()) }
        }

        private fun card(
            item: LibraryItem,
            available: Map<String, nl.potentplum.mise.content.model.Vegetable>,
            content: Content,
        ) = LibraryCard(
            item = item,
            inSeason = item.vegIds.mapNotNull(available::get).map { VegTokenData(it.id, it.nl, hexColor(it.color)) },
            placeholderColor =
                item.cuisines.firstNotNullOfOrNull { cuisine ->
                    content
                        .facet(FacetKey.CUISINE)
                        .options
                        .firstOrNull { it.value == cuisine }
                        ?.color
                },
        )

        private fun options(content: Content) =
            LibraryOptions(
                titles =
                    FacetTitles(
                        cuisine = content.facet(FacetKey.CUISINE).nl,
                        mainIngredient = content.facet(FacetKey.MAIN_INGREDIENT).nl,
                        prepTime = content.facet(FacetKey.PREP_TIME).nl,
                    ),
                dishTypes = content.facet(FacetKey.DISH_TYPE).options,
                cuisines = content.facet(FacetKey.CUISINE).options,
                mainIngredients = content.facet(FacetKey.MAIN_INGREDIENT).options,
                prepTimes = content.facet(FacetKey.PREP_TIME).options,
                techniques = content.skills.sortedBy { it.nl },
            )

        private companion object {
            const val KEY_FILTER = "filter"

            fun LibrarySort.toOrder() =
                when (this) {
                    LibrarySort.RECENTLY_ADDED -> LibrarySortOrder.RECENTLY_ADDED
                    LibrarySort.ALPHABETICAL -> LibrarySortOrder.ALPHABETICAL
                    LibrarySort.RECENTLY_COOKED -> LibrarySortOrder.RECENTLY_COOKED
                }

            fun LibrarySortOrder.toSetting() =
                when (this) {
                    LibrarySortOrder.RECENTLY_ADDED -> LibrarySort.RECENTLY_ADDED
                    LibrarySortOrder.ALPHABETICAL -> LibrarySort.ALPHABETICAL
                    LibrarySortOrder.RECENTLY_COOKED -> LibrarySort.RECENTLY_COOKED
                }
        }
    }
