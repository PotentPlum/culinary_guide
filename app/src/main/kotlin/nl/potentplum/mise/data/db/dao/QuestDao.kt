package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.QuestEntity

@Dao
interface QuestDao {
    @Upsert
    suspend fun upsert(quest: QuestEntity)

    @Query("SELECT * FROM quest WHERE id = :id")
    suspend fun get(id: String): QuestEntity?

    @Query("SELECT * FROM quest ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<QuestEntity>

    @Query("SELECT * FROM quest WHERE recipeId = :recipeId LIMIT 1")
    suspend fun forRecipe(recipeId: String): QuestEntity?

    @Query("DELETE FROM quest WHERE id = :id")
    suspend fun deleteQuest(id: String)

    @Query("DELETE FROM recipe WHERE id = :recipeId")
    suspend fun deleteRecipe(recipeId: String)

    /** Deleting a quest deletes its hidden recipe too (spec §7.2); attempts keep their snapshots. */
    @Transaction
    suspend fun deleteWithRecipe(quest: QuestEntity) {
        deleteQuest(quest.id)
        deleteRecipe(quest.recipeId)
    }
}
