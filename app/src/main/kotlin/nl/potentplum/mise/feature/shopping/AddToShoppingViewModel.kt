package nl.potentplum.mise.feature.shopping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.domain.IngredientScaler
import javax.inject.Inject

data class PickLine(
    /** The ingredient id. */
    val id: String,
    /** The scaled line, exactly as it lands on the list. */
    val text: String,
)

data class PickGroup(
    val name: String?,
    val lines: List<PickLine>,
)

data class AddToShoppingUiState(
    val loading: Boolean = true,
    val groups: List<PickGroup> = emptyList(),
    /** Unticked lines; everything is ticked at first. */
    val excluded: Set<String> = emptySet(),
    val adding: Boolean = false,
) {
    val lineCount: Int get() = groups.sumOf { it.lines.size }
    val selectedCount: Int get() = groups.sumOf { group -> group.lines.count { it.id !in excluded } }
    val allSelected: Boolean get() = selectedCount == lineCount
}

sealed interface AddToShoppingEvent {
    data class Added(
        val count: Int,
    ) : AddToShoppingEvent
}

/**
 * "Op boodschappenlijst" (spec §6.8, §6.14): a recipe's ingredients, scaled, all ticked; untick what you already have. Used by
 * recipe detail and the Vanavond card. The unticked ids survive rotation; the recipe is read once per sheet.
 */
@HiltViewModel
class AddToShoppingViewModel
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val shopping: ShoppingRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private class Loaded(
            /** Null when the recipe was deleted meanwhile: the sheet then shows no lines. */
            val recipe: Recipe?,
            val scaler: IngredientScaler,
            val servings: Int?,
            val groups: List<PickGroup>,
        )

        private var opened = false
        private val loaded = MutableStateFlow<Loaded?>(null)
        private val adding = MutableStateFlow(false)
        private val _events = Channel<AddToShoppingEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        val state: StateFlow<AddToShoppingUiState> =
            combine(loaded, savedState.getStateFlow(KEY_EXCLUDED, emptyList<String>()), adding) { loaded, excluded, adding ->
                AddToShoppingUiState(
                    loading = loaded == null,
                    groups = loaded?.groups.orEmpty(),
                    excluded = excluded.toSet(),
                    adding = adding,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, AddToShoppingUiState())

        /** Reads [recipeId] scaled to [servings] (null = its own servings). Later calls are ignored. */
        fun open(
            recipeId: String,
            servings: Int?,
        ) {
            if (opened) return
            opened = true
            viewModelScope.launch {
                val recipe = recipes.get(recipeId)
                val scaler = recipes.scaler()
                val lines = recipe?.let { shopping.recipeLines(it, scaler, servings).toMap() }.orEmpty()
                val groups =
                    recipe
                        ?.ingredientGroups
                        ?.map { group -> PickGroup(group.name, group.items.map { PickLine(it.id, lines.getValue(it.id).text) }) }
                        ?.filter { it.lines.isNotEmpty() }
                        .orEmpty()
                loaded.value = Loaded(recipe, scaler, servings, groups)
            }
        }

        fun toggle(id: String) {
            val excluded = state.value.excluded
            savedState[KEY_EXCLUDED] = ArrayList(if (id in excluded) excluded - id else excluded + id)
        }

        /** Ticks everything, or unticks everything when all is ticked. */
        fun toggleAll() {
            val current = state.value
            savedState[KEY_EXCLUDED] =
                ArrayList(if (current.allSelected) current.groups.flatMap { g -> g.lines.map { it.id } } else emptyList())
        }

        fun add() {
            val current = loaded.value ?: return
            val recipe = current.recipe ?: return
            if (adding.value) return
            val selected =
                state.value.groups
                    .flatMap { it.lines }
                    .map { it.id }
                    .filterNot { it in state.value.excluded }
                    .toSet()
            if (selected.isEmpty()) return
            adding.value = true
            viewModelScope.launch {
                val count = shopping.addRecipe(recipe, current.scaler, current.servings, only = selected)
                _events.send(AddToShoppingEvent.Added(count))
            }
        }

        private companion object {
            const val KEY_EXCLUDED = "excluded"
        }
    }
