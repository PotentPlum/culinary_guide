package nl.potentplum.mise.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import nl.potentplum.mise.content.ContentRepository
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.dao.RecipeDao
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.domain.IngredientParser
import nl.potentplum.mise.domain.IngredientRef
import nl.potentplum.mise.domain.IngredientScaler
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.domain.SeasonalMatcher
import javax.inject.Inject
import javax.inject.Singleton

/** Recipes: the whole graph read and written in one transaction, and the library as [LibraryItem]s (spec §6.7, §7.2). */
@Singleton
class RecipeRepository
    @Inject
    constructor(
        private val recipeDao: RecipeDao,
        private val attemptDao: AttemptDao,
        private val content: ContentRepository,
        private val clock: MiseClock,
    ) {
        fun observe(id: String): Flow<Recipe?> = recipeDao.observeGraph(id).map { it?.toRecipe() }

        suspend fun get(id: String): Recipe? = recipeDao.getGraph(id)?.toRecipe()

        /** Writes the recipe and all its children; sets `updatedAt` (and `createdAt` for a new recipe). */
        suspend fun save(recipe: Recipe): Recipe {
            val now = clock.nowMillis()
            val saved = recipe.copy(createdAt = recipe.createdAt.takeIf { it > 0 } ?: now, updatedAt = now)
            val rows = RecipeRows(saved)
            recipeDao.replaceGraph(
                rows.recipe,
                rows.ingredientGroups,
                rows.ingredients,
                rows.stepGroups,
                rows.steps,
                rows.extraFields,
                rows.tags,
            )
            return saved
        }

        suspend fun setFavorite(
            id: String,
            favorite: Boolean,
        ) = recipeDao.setFavorite(id, favorite, clock.nowMillis())

        /** Attempts keep their snapshots; their `recipeId` becomes null (spec §7.2). */
        suspend fun delete(id: String) = recipeDao.delete(id)

        /** Duplicate detection: the same canonical URL already in the library (spec §6.9). */
        suspend fun findInLibraryByUrl(url: String): String? = recipeDao.findInLibraryBySourceUrl(url)?.id

        suspend fun parser(): IngredientParser = IngredientParser(content.content().units)

        suspend fun scaler(): IngredientScaler = IngredientScaler(content.content().units)

        suspend fun matcher(): SeasonalMatcher = SeasonalMatcher(content.content().vegetables)

        /** Every library recipe with its cooked count, last cooked date, tags and the vegetables it uses. */
        fun observeLibrary(): Flow<List<LibraryItem>> =
            flow { emit(matcher()) }.let { matcherFlow ->
                combine(
                    matcherFlow,
                    recipeDao.observeLibrary(),
                    recipeDao.observeLibraryIngredients(),
                    recipeDao.observeLibraryTags(),
                    attemptDao.observeCookedStats(),
                ) { matcher, recipes, ingredients, tags, stats ->
                    val ingredientsByRecipe = ingredients.groupBy { it.recipeId }
                    val tagsByRecipe = tags.groupBy { it.recipeId }
                    val statsByRecipe = stats.associateBy { it.recipeId }
                    recipes.map { recipe ->
                        val lines = ingredientsByRecipe[recipe.id].orEmpty()
                        val recipeTags = tagsByRecipe[recipe.id].orEmpty()

                        fun tagged(key: TagKey) = recipeTags.filter { it.key == key }.map { it.value }.toSet()
                        LibraryItem(
                            id = recipe.id,
                            title = recipe.title,
                            sourceLabel = recipe.sourceLabel,
                            imagePath = recipe.imagePath,
                            totalMinutes = recipe.totalMinutes,
                            isFavorite = recipe.isFavorite,
                            createdAt = recipe.createdAt,
                            cookedCount = statsByRecipe[recipe.id]?.count ?: 0,
                            lastCookedAt = statsByRecipe[recipe.id]?.lastCookedAt,
                            ingredientTexts = lines.map { it.name ?: it.text },
                            vegIds = matcher.vegetableIds(lines.map { IngredientRef(it.name, it.text) }),
                            cuisines = tagged(TagKey.CUISINE),
                            dishTypes = tagged(TagKey.DISH_TYPE),
                            mainIngredients = tagged(TagKey.MAIN_INGREDIENT),
                            techniques = tagged(TagKey.TECHNIQUE),
                        )
                    }
                }
            }
    }
