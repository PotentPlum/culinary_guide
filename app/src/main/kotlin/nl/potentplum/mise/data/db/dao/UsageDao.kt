package nl.potentplum.mise.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nl.potentplum.mise.data.db.entity.ClaudeUsageEntity

data class UsageTotals(
    val inputTokens: Long,
    val outputTokens: Long,
    val costMicroUsd: Long,
)

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(usage: ClaudeUsageEntity)

    /** Totals since [from] (epoch millis), e.g. the start of this month (spec §6.15). */
    @Query(
        "SELECT COALESCE(SUM(inputTokens), 0) AS inputTokens, COALESCE(SUM(outputTokens), 0) AS outputTokens, " +
            "COALESCE(SUM(costMicroUsd), 0) AS costMicroUsd FROM claude_usage WHERE at >= :from",
    )
    fun observeTotalsSince(from: Long): Flow<UsageTotals>
}
