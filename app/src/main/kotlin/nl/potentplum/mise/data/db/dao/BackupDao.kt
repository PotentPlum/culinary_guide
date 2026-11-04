package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import nl.potentplum.mise.data.db.entity.AttemptEntity
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity

data class RecipeVersion(
    val id: String,
    val updatedAt: Long,
)

/** Whole-table reads and writes for export and restore (spec §13). Callers run restores in one transaction. */
@Dao
interface BackupDao {
    @Query("SELECT id FROM recipe ORDER BY createdAt")
    suspend fun recipeIds(): List<String>

    @Query("SELECT id, updatedAt FROM recipe")
    suspend fun recipeVersions(): List<RecipeVersion>

    @Query("SELECT * FROM quest ORDER BY createdAt")
    suspend fun quests(): List<QuestEntity>

    @Query("SELECT * FROM attempt ORDER BY cookedAt")
    suspend fun attempts(): List<AttemptEntity>

    @Query("SELECT * FROM shopping_item ORDER BY position")
    suspend fun shoppingItems(): List<ShoppingItemEntity>

    @Upsert
    suspend fun upsertQuests(quests: List<QuestEntity>)

    @Upsert
    suspend fun upsertAttempts(attempts: List<AttemptEntity>)

    @Upsert
    suspend fun upsertShoppingItems(items: List<ShoppingItemEntity>)

    @Query("DELETE FROM shopping_item")
    suspend fun deleteShoppingItems()

    @Query("DELETE FROM attempt")
    suspend fun deleteAttempts()

    @Query("DELETE FROM quest")
    suspend fun deleteQuests()

    /** Groups, items, steps, extra fields and tags go with them (cascade). */
    @Query("DELETE FROM recipe")
    suspend fun deleteRecipes()
}
