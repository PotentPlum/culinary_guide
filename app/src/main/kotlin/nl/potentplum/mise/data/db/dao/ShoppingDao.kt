package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity

/** A list item with the title of the recipe it came from (the source chips, spec §6.14). */
data class ShoppingRow(
    @Embedded val item: ShoppingItemEntity,
    val recipeTitle: String?,
)

@Dao
interface ShoppingDao {
    @Upsert
    suspend fun upsert(items: List<ShoppingItemEntity>)

    @Query("SELECT * FROM shopping_item ORDER BY checked, position")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query(
        "SELECT s.*, r.title AS recipeTitle FROM shopping_item s LEFT JOIN recipe r ON r.id = s.recipeId ORDER BY s.checked, s.position",
    )
    fun observeRows(): Flow<List<ShoppingRow>>

    @Query("SELECT * FROM shopping_item WHERE id IN (:ids)")
    suspend fun get(ids: List<String>): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_item")
    suspend fun all(): List<ShoppingItemEntity>

    @Query("UPDATE shopping_item SET checked = :checked WHERE id IN (:ids)")
    suspend fun setChecked(
        ids: List<String>,
        checked: Boolean,
    )

    @Query("DELETE FROM shopping_item WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    /** The Boodschappen tab badge (spec §6.1). */
    @Query("SELECT COUNT(*) FROM shopping_item WHERE checked = 0")
    fun observeUncheckedCount(): Flow<Int>

    @Query("UPDATE shopping_item SET checked = :checked WHERE id = :id")
    suspend fun setChecked(
        id: String,
        checked: Boolean,
    )

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM shopping_item")
    suspend fun nextPosition(): Int

    @Query("DELETE FROM shopping_item WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_item")
    suspend fun deleteAll()

    @Query("DELETE FROM shopping_item WHERE id = :id")
    suspend fun delete(id: String)
}
