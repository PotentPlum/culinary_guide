package nl.potentplum.mise.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.domain.Difficulty

/**
 * A generated quest and the parameters it was generated with (spec §6.11). There are no checkpoints (spec §0 D5).
 * The recipe is a hidden `QUEST` recipe; deleting a quest deletes it (repository), so the key restricts deleting it directly.
 */
@Entity(
    tableName = "quest",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("status", "createdAt"), Index("recipeId")],
)
data class QuestEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val isDaily: Boolean,
    val status: QuestStatus = QuestStatus.OPEN,
    val completedAt: Long? = null,
    val ingredientMode: IngredientMode,
    val ingredient: String,
    val vegId: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val questType: QuestType,
    /** Null = Automatisch. */
    val requestedDifficulty: Difficulty? = null,
    val focusDiscipline: DisciplineId? = null,
    val requestedTechniqueId: String? = null,
    val requestedCuisine: String? = null,
    val timeLimit: TimeLimit? = null,
    val simpleAndSmall: Boolean = false,
    val text: String,
    val skill: String,
    val difficulty: Difficulty,
    val timeEstimateMinutes: Int,
    val why: String,
    val dishType: String,
    val tips: List<String> = emptyList(),
    val recipeId: String,
)

/**
 * A logged cook (spec §6.12) with snapshots, so the journal doesn't change when a recipe is edited. Deleting the recipe
 * or quest keeps the attempt.
 */
@Entity(
    tableName = "attempt",
    foreignKeys = [
        ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(QuestEntity::class, ["id"], ["questId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("cookedAt"), Index("recipeId"), Index("questId")],
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val sourceType: AttemptSourceType,
    val recipeId: String? = null,
    val questId: String? = null,
    val cookedAt: Long,
    /** Execution, 1–10; the UI writes 2/4/6/8/10. */
    val score: Int? = null,
    val tasteScore: Int? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    /** Technique ids at the time of cooking. */
    val techniques: List<String> = emptyList(),
    val cuisines: List<String> = emptyList(),
    /** Vegetables used (spec §11.4). */
    val vegIds: List<String> = emptyList(),
    /** The subset of [vegIds] at PEAK on [cookedAt]. */
    val peakVegIds: List<String> = emptyList(),
)

@Entity(
    tableName = "shopping_item",
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("recipeId")],
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val text: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val name: String? = null,
    /** Null for manual items. */
    val recipeId: String? = null,
    val checked: Boolean = false,
    val position: Int,
    val createdAt: Long,
)

@Entity(tableName = "claude_usage", indices = [Index("at")])
data class ClaudeUsageEntity(
    @PrimaryKey val id: String,
    val at: Long,
    val feature: UsageFeature,
    val model: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val cacheReadTokens: Long = 0,
    val costMicroUsd: Long,
)
