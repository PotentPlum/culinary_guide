package nl.potentplum.mise.feature.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.designsystem.component.FacetChip
import nl.potentplum.mise.core.navigation.RecipeRoute
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.DeletedRecipe
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.settings.CookSession
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.RecipeText
import javax.inject.Inject

data class IngredientLine(
    val id: String,
    val display: String,
    val checked: Boolean,
    val parsed: Boolean,
)

data class IngredientGroupLines(
    val name: String?,
    val lines: List<IngredientLine>,
)

sealed interface RecipeDetailEvent {
    data class Deleted(
        val recipe: DeletedRecipe,
    ) : RecipeDetailEvent

    data class AddedToShopping(
        val count: Int,
    ) : RecipeDetailEvent
}

data class RecipeDetailUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    /** Target servings; null when the recipe has no base (the stepper is hidden). */
    val servings: Int? = null,
    val ingredients: List<IngredientGroupLines> = emptyList(),
    val techniques: List<Technique> = emptyList(),
    val tags: List<FacetChip> = emptyList(),
    val attempts: List<AttemptEntity> = emptyList(),
    /** 1-based step to resume at, when a cook session for this recipe is under way. */
    val resumeStep: Int? = null,
) {
    val notFound: Boolean get() = !loading && recipe == null
}

@HiltViewModel
class RecipeDetailViewModel
    @Inject
    constructor(
        private val recipes: RecipeRepository,
        private val shopping: ShoppingRepository,
        private val settings: SettingsRepository,
        attemptDao: AttemptDao,
        content: ContentRepository,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        val id = savedState.toRoute<RecipeRoute>().id
        private val servingsOverride = savedState.getStateFlow<Int?>(KEY_SERVINGS, null)
        private val tools = flow { emit(content.content() to recipes.scaler()) }

        val state: StateFlow<RecipeDetailUiState> =
            combine(
                tools,
                recipes.observe(id),
                settings.cookSession,
                attemptDao.observeForRecipe(id),
                servingsOverride,
            ) { (content, scaler), recipe, session, attempts, override ->
                if (recipe == null) return@combine RecipeDetailUiState(loading = false)
                val ownSession = session?.takeIf { it.recipeId == id }
                val servings = recipe.servings?.let { ownSession?.servings ?: override ?: it }
                val ratio = scaler.ratio(recipe.servings, servings ?: 0) ?: 1.0
                val checked = ownSession?.checkedIngredientIds.orEmpty().toSet()
                RecipeDetailUiState(
                    loading = false,
                    recipe = recipe,
                    servings = servings,
                    ingredients =
                        recipe.ingredientGroups.map { group ->
                            IngredientGroupLines(
                                group.name,
                                group.items.map {
                                    IngredientLine(
                                        it.id,
                                        scaler.format(it.parsed, ratio),
                                        it.id in checked,
                                        it.parsed.parsed,
                                    )
                                },
                            )
                        },
                    techniques = recipe.tagValues(TagKey.TECHNIQUE).mapNotNull(content::technique),
                    tags =
                        listOf(
                            TagKey.CUISINE to FacetKey.CUISINE,
                            TagKey.DISH_TYPE to FacetKey.DISH_TYPE,
                            TagKey.MAIN_INGREDIENT to FacetKey.MAIN_INGREDIENT,
                        ).flatMap { (tag, facet) ->
                            recipe.tagValues(tag).map { value ->
                                val option = content.facet(facet).options.firstOrNull { it.value == value }
                                FacetChip(option?.nl ?: value, option?.color)
                            }
                        },
                    attempts = attempts,
                    resumeStep = ownSession?.takeIf { it.stepIndex > 0 || it.timers.isNotEmpty() }?.let { it.stepIndex + 1 },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

        fun setServings(value: Int) {
            val clamped = value.coerceIn(1, 99)
            savedState[KEY_SERVINGS] = clamped
            viewModelScope.launch {
                settings.cookSession
                    .first()
                    ?.takeIf { it.recipeId == id }
                    ?.let { settings.saveCookSession(it.copy(servings = clamped)) }
            }
        }

        /** Ticks are the cook session's ticks (spec §6.8), so they follow into cook mode and back. */
        fun toggleIngredient(ingredientId: String) {
            viewModelScope.launch {
                val current = settings.cookSession.first()?.takeIf { it.recipeId == id }
                val session = current ?: CookSession(recipeId = id, stepIndex = 0, servings = state.value.servings, updatedAt = 0)
                val ticks = session.checkedIngredientIds.toMutableSet()
                if (!ticks.add(ingredientId)) ticks.remove(ingredientId)
                settings.saveCookSession(session.copy(checkedIngredientIds = ticks.toList()))
            }
        }

        fun toggleFavorite() {
            val recipe = state.value.recipe ?: return
            viewModelScope.launch { recipes.setFavorite(id, !recipe.isFavorite) }
        }

        fun pinTonight() {
            viewModelScope.launch { settings.pinTonight(id) }
        }

        private val _events = Channel<RecipeDetailEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        fun delete() {
            viewModelScope.launch { recipes.deleteForUndo(id)?.let { _events.send(RecipeDetailEvent.Deleted(it)) } }
        }

        suspend fun restore(deleted: DeletedRecipe) = recipes.restore(deleted)

        /** Every ingredient, scaled to the current servings (spec §6.14). */
        fun addToShopping() {
            viewModelScope.launch { _events.send(RecipeDetailEvent.AddedToShopping(addLines())) }
        }

        private suspend fun addLines(): Int {
            val recipe = state.value.recipe ?: return 0
            return shopping.addRecipe(recipe, recipes.scaler(), state.value.servings)
        }

        fun shareText(
            ingredientsHeading: String,
            stepsHeading: String,
        ): String? {
            val current = state.value
            val recipe = current.recipe ?: return null
            return RecipeText.format(
                title = recipe.title,
                sourceUrl = recipe.sourceUrl,
                ingredientGroups = current.ingredients.map { group -> RecipeText.Group(group.name, group.lines.map { it.display }) },
                stepGroups = recipe.stepGroups.map { group -> RecipeText.Group(group.name, group.steps.map { it.text }) },
                ingredientsHeading = ingredientsHeading,
                stepsHeading = stepsHeading,
            )
        }

        private companion object {
            const val KEY_SERVINGS = "servings"
        }
    }
