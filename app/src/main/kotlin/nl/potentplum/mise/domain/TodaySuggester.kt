package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.CuratedRecipe
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.util.MiseClock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Why a recipe is suggested today; the UI turns it into the reason line (spec §11.5). */
data class SuggestionReason(
    /** Up to two PEAK vegetables, the strongest first. */
    val peakVegIds: List<String>,
    /** Set when the recipe was last cooked more than 30 days ago. */
    val weeksNotCooked: Int?,
    val neverCooked: Boolean,
    val favourite: Boolean,
)

data class Suggestion(
    val item: LibraryItem,
    val score: Double,
    val reason: SuggestionReason,
)

/** The "Wat kook ik vandaag?" ranking of spec §11.5. Pure: the same library and day always give the same order. */
object TodaySuggester {
    const val SLOTS = 3
    const val QUICK_MAX_MINUTES = 30

    private const val PEAK = 1.0
    private const val PEAK_BADGE_BONUS = 0.5
    private const val SHOULDER = 0.5
    private const val FAVOURITE = 0.4
    private const val NEVER_COOKED = 0.3
    private const val EXCLUDED_DAYS = 7
    private const val RECENT_DAYS = 30
    private const val RECENT_FACTOR = 0.5
    private const val RECENCY_CAP_DAYS = 90L
    private const val RECENCY_MAX = 0.6
    private const val MAX_REASON_VEGETABLES = 2

    /**
     * Every library recipe that may be suggested on [today], best first. Recipes cooked in the last 7 days are left out;
     * those cooked 8–30 days ago keep half their score. Ties go to the fewest times cooked, then the title.
     */
    fun rank(
        library: List<LibraryItem>,
        vegetables: Map<String, Vegetable>,
        today: LocalDate,
    ): List<Suggestion> {
        val month = today.monthValue
        val seasons = mutableMapOf<String, VegetableInSeason?>()
        val collator = SeasonPresentation.dutchCollator()
        return library
            .mapNotNull { item ->
                val daysSince = item.lastCookedAt?.let { daysBetween(it, today) }
                if (daysSince != null && daysSince <= EXCLUDED_DAYS) return@mapNotNull null
                val inSeason =
                    item.vegIds.mapNotNull { id -> seasons.getOrPut(id) { vegetables[id]?.let { SeasonPresentation.present(it, month) } } }
                val season = inSeason.sumOf(::seasonScore)
                val favourite = if (item.isFavorite) FAVOURITE else 0.0
                val score =
                    when {
                        daysSince == null -> season + favourite + NEVER_COOKED
                        daysSince <= RECENT_DAYS -> (season + favourite) * RECENT_FACTOR
                        else -> season + favourite + RECENCY_MAX * minOf(daysSince, RECENCY_CAP_DAYS) / RECENCY_CAP_DAYS.toDouble()
                    }
                val reason =
                    SuggestionReason(
                        peakVegIds =
                            inSeason
                                .filter { it.status == SeasonStatus.PEAK }
                                .sortedWith(SeasonPresentation.order)
                                .take(MAX_REASON_VEGETABLES)
                                .map { it.vegetable.id },
                        weeksNotCooked = daysSince?.takeIf { it > RECENT_DAYS }?.let { (it / 7).toInt() },
                        neverCooked = daysSince == null,
                        favourite = item.isFavorite,
                    )
                Suggestion(item, score, reason)
            }.sortedWith(
                compareByDescending<Suggestion> { it.score }
                    .thenBy { it.item.cookedCount }
                    .thenBy(collator) { it.item.title }
                    .thenBy { it.item.id },
            )
    }

    /**
     * The cards Vandaag shows: the first [count] of [ranked] that were not dismissed today and are not the Vanavond pin,
     * restricted to recipes of at most 30 minutes when [quickOnly] (recipes without a time are hidden then).
     */
    fun pick(
        ranked: List<Suggestion>,
        dismissed: Set<String>,
        pinnedId: String?,
        quickOnly: Boolean,
        count: Int = SLOTS,
    ): List<Suggestion> =
        ranked
            .asSequence()
            .filter { it.item.id !in dismissed && it.item.id != pinnedId }
            .filter { !quickOnly || (it.item.totalMinutes != null && it.item.totalMinutes <= QUICK_MAX_MINUTES) }
            .take(count)
            .toList()

    private fun seasonScore(veg: VegetableInSeason): Double =
        when (veg.status) {
            SeasonStatus.PEAK -> PEAK + if (veg.lastChance || veg.shortSeason) PEAK_BADGE_BONUS else 0.0
            SeasonStatus.SHOULDER -> SHOULDER
            else -> 0.0
        }

    private fun daysBetween(
        millis: Long,
        today: LocalDate,
    ): Long = ChronoUnit.DAYS.between(Instant.ofEpochMilli(millis).atZone(MiseClock.ZONE).toLocalDate(), today)
}

/** A curated link shown in an empty suggestion slot (spec §6.2). */
data class CuratedSuggestion(
    val vegetable: Vegetable,
    val recipe: CuratedRecipe,
)

/** Curated links of the PEAK vegetables, one per vegetable, for the slots the library cannot fill. */
object CuratedFallback {
    /**
     * In the ranked vegetable order, starting at a point that moves daily so the links vary. With [quickOnly], only links of
     * at most 30 minutes count, like the suggestions.
     */
    fun pick(
        vegetables: List<Vegetable>,
        today: LocalDate,
        count: Int,
        quickOnly: Boolean = false,
    ): List<CuratedSuggestion> {
        if (count <= 0) return emptyList()
        val day = today.toEpochDay()
        val candidates =
            SeasonPresentation
                .ranked(vegetables, today.monthValue)
                .filter { it.status == SeasonStatus.PEAK }
                .mapNotNull { row ->
                    val links = row.vegetable.curatedRecipes.filter { !quickOnly || it.timeMinutes <= TodaySuggester.QUICK_MAX_MINUTES }
                    links.takeIf { it.isNotEmpty() }?.let {
                        CuratedSuggestion(
                            row.vegetable,
                            it[Math.floorMod(day, it.size.toLong()).toInt()],
                        )
                    }
                }
        if (candidates.isEmpty()) return emptyList()
        val start = Math.floorMod(day, candidates.size.toLong()).toInt()
        return (candidates.drop(start) + candidates.take(start)).take(count)
    }
}
