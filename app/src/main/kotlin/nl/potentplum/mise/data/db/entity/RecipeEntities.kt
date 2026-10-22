package nl.potentplum.mise.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipe", indices = [Index("inLibrary", "updatedAt")])
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val sourceType: RecipeSourceType,
    val sourceUrl: String? = null,
    val sourceLabel: String = "",
    val author: String? = null,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val totalMinutes: Int? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    /** False for a quest's hidden recipe until it is saved to the library. */
    val inLibrary: Boolean = true,
    val enrichState: EnrichState = EnrichState.NONE,
    val sourceMeta: SourceMeta? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "ingredient_group",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class IngredientGroupEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    /** Null = the untitled default group. */
    val name: String? = null,
)

@Entity(
    tableName = "ingredient",
    foreignKeys = [ForeignKey(IngredientGroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId"), Index("name")],
)
data class IngredientEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val position: Int,
    /** The line as written. */
    val text: String,
    val quantity: Double? = null,
    /** Upper bound of a range ("2–3"). */
    val quantityMax: Double? = null,
    /** An id from `units.json`. */
    val unit: String? = null,
    /** Dutch, lowercase, singular. */
    val name: String? = null,
    /** "in blokjes", "à 400 g". */
    val note: String? = null,
    val parseSource: ParseSource = ParseSource.NONE,
)

@Entity(
    tableName = "step_group",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class StepGroupEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    val name: String? = null,
)

@Entity(
    tableName = "step",
    foreignKeys = [ForeignKey(StepGroupEntity::class, ["id"], ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId")],
)
data class StepEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val position: Int,
    val text: String,
)

@Entity(
    tableName = "recipe_extra_field",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class RecipeExtraFieldEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val position: Int,
    val label: String,
    val value: String,
)

@Entity(
    tableName = "recipe_tag",
    primaryKeys = ["recipeId", "key", "value"],
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("key", "value")],
)
data class RecipeTagEntity(
    val recipeId: String,
    val key: TagKey,
    /** A facet value, or a technique id for [TagKey.TECHNIQUE]. */
    val value: String,
)
