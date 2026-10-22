package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity

@Dao
interface ShoppingDao {
    @Upsert
    suspend fun upsert(items: List<ShoppingItemEntity>)

    @Query("SELECT * FROM shopping_item ORDER BY checked, position")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

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
