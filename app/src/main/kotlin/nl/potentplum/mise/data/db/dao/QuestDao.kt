package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.domain.PastQuest

/** A quest with its recipe's title, for Dagboek › Quests. */
data class QuestWithTitle(
    @Embedded val quest: QuestEntity,
    val title: String,
)

@Dao
interface QuestDao {
    @Upsert
    suspend fun upsert(quest: QuestEntity)

    @Query("SELECT * FROM quest WHERE id = :id")
    suspend fun get(id: String): QuestEntity?

    @Query("SELECT * FROM quest ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE id = :id")
    fun observe(id: String): Flow<QuestEntity?>

    @Query("SELECT q.*, r.title AS title FROM quest q JOIN recipe r ON r.id = q.recipeId ORDER BY q.createdAt DESC")
    fun observeHistory(): Flow<List<QuestWithTitle>>

    @Query("SELECT * FROM quest WHERE isDaily = 1 AND createdAt >= :since ORDER BY createdAt DESC LIMIT 1")
    fun observeDailySince(since: Long): Flow<QuestEntity?>

    @Query(
        "SELECT q.createdAt, q.completedAt, q.difficulty, q.skill, r.title, " +
            "(SELECT a.score FROM attempt a WHERE a.questId = q.id AND a.score IS NOT NULL ORDER BY a.cookedAt DESC LIMIT 1) " +
            "AS score FROM quest q JOIN recipe r ON r.id = q.recipeId ORDER BY q.createdAt DESC",
    )
    suspend fun pastQuests(): List<PastQuest>

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
