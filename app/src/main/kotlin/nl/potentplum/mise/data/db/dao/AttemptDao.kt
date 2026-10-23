package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.AttemptEntity

/** Cooked count and last cooked date are derived from attempts, never stored (spec §7.2). */
data class CookedStats(
    val recipeId: String,
    val count: Int,
    val lastCookedAt: Long,
)

@Dao
interface AttemptDao {
    @Upsert
    suspend fun upsert(attempt: AttemptEntity)

    @Query("SELECT * FROM attempt WHERE id = :id")
    suspend fun get(id: String): AttemptEntity?

    @Query("SELECT * FROM attempt ORDER BY cookedAt DESC")
    fun observeAll(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempt ORDER BY cookedAt DESC")
    suspend fun all(): List<AttemptEntity>

    @Query("SELECT * FROM attempt WHERE recipeId = :recipeId ORDER BY cookedAt DESC")
    fun observeForRecipe(recipeId: String): Flow<List<AttemptEntity>>

    @Query(
        "SELECT recipeId, COUNT(*) AS count, MAX(cookedAt) AS lastCookedAt FROM attempt " +
            "WHERE recipeId IS NOT NULL GROUP BY recipeId",
    )
    fun observeCookedStats(): Flow<List<CookedStats>>

    @Query("SELECT id FROM attempt WHERE recipeId = :recipeId")
    suspend fun idsForRecipe(recipeId: String): List<String>

    @Query("UPDATE attempt SET recipeId = :recipeId WHERE id IN (:ids)")
    suspend fun relink(
        ids: List<String>,
        recipeId: String,
    )

    @Query("DELETE FROM attempt WHERE id = :id")
    suspend fun delete(id: String)
}
