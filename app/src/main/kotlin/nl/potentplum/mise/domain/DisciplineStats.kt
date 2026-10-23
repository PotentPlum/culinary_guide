package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.entity.AttemptEntity
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Per-discipline attempt facts (spec §12): an attempt matches when a technique in its snapshot has that discipline. */
data class DisciplineActivity(
    val discipline: DisciplineId,
    val count: Int,
    val lastCooked: LocalDate?,
    val countLast30Days: Int,
) {
    /** No matching attempt, or the most recent one is more than 30 days ago. */
    fun isNeglected(today: LocalDate): Boolean = lastCooked == null || ChronoUnit.DAYS.between(lastCooked, today) > NEGLECT_DAYS

    companion object {
        const val NEGLECT_DAYS = 30L
    }
}

object DisciplineStats {
    fun activity(
        disciplines: List<DisciplineId>,
        techniques: Map<String, Technique>,
        attempts: List<AttemptEntity>,
        today: LocalDate,
    ): List<DisciplineActivity> =
        disciplines.map { discipline ->
            val matching = attempts.filter { attempt -> attempt.techniques.any { techniques[it]?.discipline == discipline } }
            val dates = matching.map { cookedOn(it) }
            DisciplineActivity(
                discipline = discipline,
                count = matching.size,
                lastCooked = dates.maxOrNull(),
                countLast30Days = dates.count { ChronoUnit.DAYS.between(it, today) in 0..DisciplineActivity.NEGLECT_DAYS },
            )
        }

    fun cookedOn(attempt: AttemptEntity): LocalDate = Instant.ofEpochMilli(attempt.cookedAt).atZone(MiseClock.ZONE).toLocalDate()
}
