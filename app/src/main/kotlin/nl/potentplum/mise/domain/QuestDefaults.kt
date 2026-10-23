package nl.potentplum.mise.domain

import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.data.db.entity.AttemptEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A past quest as the defaults need it; [score] is the execution score of its latest scored attempt. */
data class PastQuest(
    val createdAt: Long,
    val completedAt: Long?,
    val difficulty: Difficulty,
    val skill: String,
    val title: String,
    val score: Int?,
)

/** One entry of the "niet herhalen" list (spec §8.3). */
data class QuestHistoryEntry(
    val skill: String,
    val title: String,
)

data class QuestDefaultChoice(
    /** "Verras me" candidates in the §11.1 order. */
    val vegCandidates: List<String>,
    /** The vegetable today's one-tap quest uses (shown in "what was chosen"). */
    val vegId: String?,
    val focusDiscipline: DisciplineId?,
    val difficulty: Difficulty,
    val history: List<QuestHistoryEntry>,
)

/** The Quest van vandaag defaults (spec §11.6). Pure: everything comes in as arguments. */
object QuestDefaults {
    const val RECENT_VEG_DAYS = 60L
    const val MIN_FRESH_CANDIDATES = 3
    const val DIFFICULTY_WINDOW = 5
    const val HISTORY_SIZE = 10
    private const val LEVEL_UP_AVG = 8.0
    private const val LEVEL_DOWN_AVG = 5.0

    /**
     * [pick] chooses among the candidates: today's quest uses the day's index, so it is stable all day and changes from
     * day to day; "Andere quest" passes the next one.
     */
    fun choose(
        today: LocalDate,
        content: Content,
        attempts: List<AttemptEntity>,
        quests: List<PastQuest>,
        pick: Int = today.toEpochDay().toInt(),
    ): QuestDefaultChoice {
        val candidates = vegetableCandidates(today, content, attempts)
        return QuestDefaultChoice(
            vegCandidates = candidates,
            vegId = candidates.takeIf { it.isNotEmpty() }?.let { it[Math.floorMod(pick, it.size)] },
            focusDiscipline = focusDiscipline(today, content, attempts),
            difficulty = difficulty(quests),
            history = history(quests),
        )
    }

    /** PEAK vegetables not cooked in 60 days; all PEAK if that leaves fewer than 3; all in season if nothing peaks. */
    fun vegetableCandidates(
        today: LocalDate,
        content: Content,
        attempts: List<AttemptEntity>,
    ): List<String> {
        val ranked = SeasonPresentation.ranked(content.vegetables, today.monthValue)
        val peak = ranked.filter { it.status == SeasonStatus.PEAK }.map { it.vegetable.id }
        if (peak.isEmpty()) return ranked.filter { it.status.isAvailable }.map { it.vegetable.id }
        val recent =
            attempts
                .filter { ChronoUnit.DAYS.between(DisciplineStats.cookedOn(it), today) in 0..RECENT_VEG_DAYS }
                .flatMap { it.vegIds }
                .toSet()
        val fresh = peak.filter { it !in recent }
        return if (fresh.size < MIN_FRESH_CANDIDATES) peak else fresh
    }

    /** The most neglected discipline (never cooked first, then the longest ago); else the fewest attempts in 30 days. */
    fun focusDiscipline(
        today: LocalDate,
        content: Content,
        attempts: List<AttemptEntity>,
    ): DisciplineId? {
        val activity = DisciplineStats.activity(content.disciplines.map { it.id }, content.techniquesById, attempts, today)
        val neglected = activity.filter { it.isNeglected(today) }
        if (neglected.isNotEmpty()) return neglected.minBy { it.lastCooked?.toEpochDay() ?: Long.MIN_VALUE }.discipline
        return activity.minByOrNull { it.countLast30Days }?.discipline
    }

    /** Adaptive (spec §11.6): the last 5 completed, scored quests; ≥ 8 → one up, ≤ 5 → one down. No history → Basis. */
    fun difficulty(quests: List<PastQuest>): Difficulty {
        val scored =
            quests
                .filter { it.completedAt != null && it.score != null }
                .sortedByDescending { it.completedAt }
                .take(DIFFICULTY_WINDOW)
        val latest = scored.firstOrNull() ?: return Difficulty.BEGINNER
        val average = scored.map { it.score!! }.average()
        val levels = Difficulty.entries
        return when {
            average >= LEVEL_UP_AVG -> levels[minOf(latest.difficulty.ordinal + 1, levels.lastIndex)]
            average <= LEVEL_DOWN_AVG -> levels[maxOf(latest.difficulty.ordinal - 1, 0)]
            else -> latest.difficulty
        }
    }

    fun history(quests: List<PastQuest>): List<QuestHistoryEntry> =
        quests.sortedByDescending { it.createdAt }.take(HISTORY_SIZE).map { QuestHistoryEntry(it.skill, it.title) }
}
