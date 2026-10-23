package nl.potentplum.mise.data.repo

import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.ShoppingDao
import nl.potentplum.mise.data.db.entity.ShoppingItemEntity
import javax.inject.Inject
import javax.inject.Singleton

/** A line to put on the list, already scaled (spec §11.7). */
data class ShoppingLine(
    val text: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val name: String? = null,
)

@Singleton
class ShoppingRepository
    @Inject
    constructor(
        private val dao: ShoppingDao,
        private val clock: MiseClock,
    ) {
        fun observe(): Flow<List<ShoppingItemEntity>> = dao.observeAll()

        /** Adds lines from [recipeId] (or manual lines when null) at the end of the list. */
        suspend fun add(
            lines: List<ShoppingLine>,
            recipeId: String? = null,
        ): Int {
            if (lines.isEmpty()) return 0
            val start = dao.nextPosition()
            val now = clock.nowMillis()
            dao.upsert(
                lines.mapIndexed { i, line ->
                    ShoppingItemEntity(newId(), line.text, line.quantity, line.unit, line.name, recipeId, false, start + i, now)
                },
            )
            return lines.size
        }

        suspend fun setChecked(
            id: String,
            checked: Boolean,
        ) = dao.setChecked(id, checked)

        suspend fun delete(id: String) = dao.delete(id)

        suspend fun deleteChecked() = dao.deleteChecked()

        suspend fun deleteAll() = dao.deleteAll()

        suspend fun restore(items: List<ShoppingItemEntity>) = dao.upsert(items)
    }
