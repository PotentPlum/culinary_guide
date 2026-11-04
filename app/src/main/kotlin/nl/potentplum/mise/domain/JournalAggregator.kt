package nl.potentplum.mise.domain

import nl.potentplum.mise.content.Content
import nl.potentplum.mise.content.model.Archetype
import nl.potentplum.mise.content.model.BadgeRule
import nl.potentplum.mise.content.model.DisciplineId
import nl.potentplum.mise.data.db.entity.AttemptEntity
import java.time.LocalDate

/** The four cooking seasons of spec §12; winter runs from December into the next year. */
enum class CookingSeason(
    val months: Set<Int>,
) {
    LENTE(setOf(3, 4, 5)),
    ZOMER(setOf(6, 7, 8)),
    HERFST(setOf(9, 10, 11)),
    WINTER(setOf(12, 1, 2)),
    ;

    companion object {
        fun of(month: Int): CookingSeason = entries.first { month in it.months }

        /** The first day of the season [date] is in: 1 December of the previous year for January and February. */
        fun startOf(date: LocalDate): LocalDate =
            when (date.monthValue) {
                1, 2 -> LocalDate.of(date.year - 1, 12, 1)
                12 -> LocalDate.of(date.year, 12, 1)
                else -> LocalDate.of(date.year, (date.monthValue - 3) / 3 * 3 + 3, 1)
            }
    }
}

data class DisciplineSummary(
    val discipline: DisciplineId,
    val count: Int,
    /** Mean of the non-null execution scores of matching attempts; null when there are none. */
    val avgScore: Double?,
)

data class CountedValue(
    val value: String,
    val count: Int,
)

/** "Dit seizoen" (spec §6.13, §12): shown without a denominator. */
data class SeasonSummary(
    val season: CookingSeason,
    /** Distinct vegetables cooked at peak this season, in Dutch order. */
    val cookedVegIds: List<String>,
    /** PEAK this month and not cooked this season, in §11.1 order (Laatste kans first). */
    val toTry: List<String>,
)

/** What is still missing for a badge (spec §12); [Earned] when nothing is. */
sealed interface BadgeProgress {
    data object Earned : BadgeProgress

    data class MissingAttempts(
        val missing: Int,
    ) : BadgeProgress

    /** Enough attempts, but their mean execution score is below [needed] ([average] null = no scores yet). */
    data class MissingScore(
        val average: Double?,
        val needed: Double,
    ) : BadgeProgress

    data class MissingPeakVegetables(
        val missing: Int,
    ) : BadgeProgress

    data class MissingPeakMonths(
        val missing: Int,
    ) : BadgeProgress
}

data class BadgeStatus(
    val archetype: Archetype,
    val progress: BadgeProgress,
) {
    val earned: Boolean get() = progress == BadgeProgress.Earned
}

data class JournalInsights(
    val attemptsThisMonth: Int,
    val totalAttempts: Int,
    val questsCompleted: Int,
    /** In `disciplines.json` order. */
    val disciplines: List<DisciplineSummary>,
    /** Top 10 technique ids. */
    val topTechniques: List<CountedValue>,
    val neglected: List<DisciplineId>,
    /** Top 5 cuisines. */
    val topCuisines: List<CountedValue>,
    val season: SeasonSummary,
    val badges: List<BadgeStatus>,
)

/**
 * Dagboek › Inzichten (spec §12). "Matches discipline D" means a technique in the attempt's snapshot has discipline D;
 * cookware guides have no discipline and never match.
 */
object JournalAggregator {
    private const val TOP_TECHNIQUES = 10
    private const val TOP_CUISINES = 5

    fun insights(
        attempts: List<AttemptEntity>,
        questsCompleted: Int,
        content: Content,
        today: LocalDate,
    ): JournalInsights {
        val cookedOn = attempts.associate { it.id to DisciplineStats.cookedOn(it) }
        val disciplineIds = content.disciplines.map { it.id }
        return JournalInsights(
            attemptsThisMonth = attempts.count { cookedOn.getValue(it.id).let { d -> d.year == today.year && d.month == today.month } },
            totalAttempts = attempts.size,
            questsCompleted = questsCompleted,
            disciplines = disciplines(attempts, content),
            topTechniques = counted(attempts.flatMap { it.techniques.distinct() }, TOP_TECHNIQUES),
            neglected =
                DisciplineStats
                    .activity(disciplineIds, content.techniquesById, attempts, today)
                    .filter { it.isNeglected(today) }
                    .map { it.discipline },
            topCuisines = counted(attempts.flatMap { it.cuisines.distinct() }, TOP_CUISINES),
            season = season(attempts, content, today),
            badges = BadgeEvaluator.evaluate(content.archetypes, attempts, content),
        )
    }

    fun disciplines(
        attempts: List<AttemptEntity>,
        content: Content,
    ): List<DisciplineSummary> =
        content.disciplines.map { discipline ->
            val matching = attempts.filter { matches(it, discipline.id, content) }
            DisciplineSummary(discipline.id, matching.size, matching.mapNotNull { it.score }.averageOrNull())
        }

    fun season(
        attempts: List<AttemptEntity>,
        content: Content,
        today: LocalDate,
    ): SeasonSummary {
        val start = CookingSeason.startOf(today)
        val cooked =
            attempts
                .filter { DisciplineStats.cookedOn(it).let { d -> !d.isBefore(start) && !d.isAfter(today) } }
                .flatMap { it.peakVegIds }
                .toSet()
        val collator = SeasonPresentation.dutchCollator()
        val toTry =
            SeasonPresentation
                .ranked(content.vegetables, today.monthValue)
                .filter { it.status == SeasonStatus.PEAK && it.vegetable.id !in cooked }
                .map { it.vegetable.id }
        return SeasonSummary(
            season = CookingSeason.of(today.monthValue),
            cookedVegIds = cooked.sortedWith(compareBy(collator) { content.vegetable(it)?.nl ?: it }),
            toTry = toTry,
        )
    }

    fun matches(
        attempt: AttemptEntity,
        discipline: DisciplineId,
        content: Content,
    ): Boolean = attempt.techniques.any { content.technique(it)?.discipline == discipline }

    private fun counted(
        values: List<String>,
        limit: Int,
    ): List<CountedValue> =
        values
            .groupingBy { it }
            .eachCount()
            .map { (value, count) -> CountedValue(value, count) }
            .sortedWith(compareByDescending<CountedValue> { it.count }.thenBy { it.value })
            .take(limit)

    internal fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()
}

/** The three badge rule types of `archetypes.json` (spec §12). */
object BadgeEvaluator {
    fun evaluate(
        archetypes: List<Archetype>,
        attempts: List<AttemptEntity>,
        content: Content,
    ): List<BadgeStatus> = archetypes.map { BadgeStatus(it, progress(it.rule, attempts, content)) }

    fun progress(
        rule: BadgeRule,
        attempts: List<AttemptEntity>,
        content: Content,
    ): BadgeProgress =
        when (rule) {
            is BadgeRule.Techniques -> {
                val matching =
                    attempts.filter { attempt ->
                        rule.discipline?.let { JournalAggregator.matches(attempt, it, content) }
                            ?: attempt.techniques.any { it in rule.techniqueIds.orEmpty() }
                    }
                val scores = matching.mapNotNull { it.score }
                val average = if (scores.isEmpty()) null else scores.average()
                when {
                    matching.size < rule.minAttempts -> {
                        BadgeProgress.MissingAttempts(rule.minAttempts - matching.size)
                    }

                    rule.minAvgScore != null && (average == null || average < rule.minAvgScore) -> {
                        BadgeProgress.MissingScore(
                            average,
                            rule.minAvgScore,
                        )
                    }

                    else -> {
                        BadgeProgress.Earned
                    }
                }
            }

            is BadgeRule.DistinctPeakVegetables -> {
                val distinct = attempts.flatMap { it.peakVegIds }.toSet().size
                if (distinct >= rule.min) BadgeProgress.Earned else BadgeProgress.MissingPeakVegetables(rule.min - distinct)
            }

            is BadgeRule.PeakMonths -> {
                val months =
                    attempts
                        .filter { it.peakVegIds.isNotEmpty() }
                        .map { DisciplineStats.cookedOn(it).monthValue }
                        .toSet()
                        .size
                if (months >= rule.min) BadgeProgress.Earned else BadgeProgress.MissingPeakMonths(rule.min - months)
            }
        }
}
