package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.IngredientEntity
import nl.potentplum.mise.data.db.entity.IngredientGroupEntity
import nl.potentplum.mise.data.db.entity.RecipeEntity
import nl.potentplum.mise.data.db.entity.RecipeExtraFieldEntity
import nl.potentplum.mise.data.db.entity.RecipeTagEntity
import nl.potentplum.mise.data.db.entity.StepEntity
import nl.potentplum.mise.data.db.entity.StepGroupEntity

data class IngredientGroupWithItems(
    @Embedded val group: IngredientGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val items: List<IngredientEntity>,
)

data class StepGroupWithSteps(
    @Embedded val group: StepGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val steps: List<StepEntity>,
)

/** A recipe with everything that belongs to it. Children come unsorted; order by `position`. */
data class RecipeGraph(
    @Embedded val recipe: RecipeEntity,
    @Relation(entity = IngredientGroupEntity::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredientGroups: List<IngredientGroupWithItems>,
    @Relation(entity = StepGroupEntity::class, parentColumn = "id", entityColumn = "recipeId")
    val stepGroups: List<StepGroupWithSteps>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val extraFields: List<RecipeExtraFieldEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val tags: List<RecipeTagEntity>,
)

/** One ingredient line with its recipe, for search and seasonal matching across the library. */
data class IngredientRow(
    val recipeId: String,
    val name: String?,
    val text: String,
)

@Dao
interface RecipeDao {
    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Upsert
    suspend fun upsertIngredientGroups(groups: List<IngredientGroupEntity>)

    @Upsert
    suspend fun upsertIngredients(ingredients: List<IngredientEntity>)

    @Upsert
    suspend fun upsertStepGroups(groups: List<StepGroupEntity>)

    @Upsert
    suspend fun upsertSteps(steps: List<StepEntity>)

    @Upsert
    suspend fun upsertExtraFields(fields: List<RecipeExtraFieldEntity>)

    @Upsert
    suspend fun upsertTags(tags: List<RecipeTagEntity>)

    @Query("DELETE FROM ingredient_group WHERE recipeId = :recipeId")
    suspend fun deleteIngredientGroups(recipeId: String)

    @Query("DELETE FROM step_group WHERE recipeId = :recipeId")
    suspend fun deleteStepGroups(recipeId: String)

    @Query("DELETE FROM recipe_extra_field WHERE recipeId = :recipeId")
    suspend fun deleteExtraFields(recipeId: String)

    @Query("DELETE FROM recipe_tag WHERE recipeId = :recipeId")
    suspend fun deleteTags(recipeId: String)

    /** Writes a whole recipe in one transaction, replacing any previous children (spec §15.5). */
    @Transaction
    suspend fun replaceGraph(
        recipe: RecipeEntity,
        ingredientGroups: List<IngredientGroupEntity>,
        ingredients: List<IngredientEntity>,
        stepGroups: List<StepGroupEntity>,
        steps: List<StepEntity>,
        extraFields: List<RecipeExtraFieldEntity>,
        tags: List<RecipeTagEntity>,
    ) {
        upsertRecipe(recipe)
        deleteIngredientGroups(recipe.id)
        deleteStepGroups(recipe.id)
        deleteExtraFields(recipe.id)
        deleteTags(recipe.id)
        upsertIngredientGroups(ingredientGroups)
        upsertIngredients(ingredients)
        upsertStepGroups(stepGroups)
        upsertSteps(steps)
        upsertExtraFields(extraFields)
        upsertTags(tags)
    }

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun getGraph(id: String): RecipeGraph?

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :id")
    fun observeGraph(id: String): Flow<RecipeGraph?>

    @Query("SELECT * FROM recipe WHERE inLibrary = 1 ORDER BY createdAt DESC")
    fun observeLibrary(): Flow<List<RecipeEntity>>

    @Query(
        "SELECT g.recipeId AS recipeId, i.name AS name, i.text AS text FROM ingredient i " +
            "JOIN ingredient_group g ON i.groupId = g.id JOIN recipe r ON g.recipeId = r.id WHERE r.inLibrary = 1",
    )
    fun observeLibraryIngredients(): Flow<List<IngredientRow>>

    @Query("SELECT t.* FROM recipe_tag t JOIN recipe r ON t.recipeId = r.id WHERE r.inLibrary = 1")
    fun observeLibraryTags(): Flow<List<RecipeTagEntity>>

    @Query("UPDATE recipe SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(
        id: String,
        favorite: Boolean,
        now: Long,
    )

    @Query("SELECT EXISTS(SELECT 1 FROM recipe WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT * FROM recipe WHERE sourceUrl = :url AND inLibrary = 1 LIMIT 1")
    suspend fun findInLibraryBySourceUrl(url: String): RecipeEntity?

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun delete(id: String)
}
