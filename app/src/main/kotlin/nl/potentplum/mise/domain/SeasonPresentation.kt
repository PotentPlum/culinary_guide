package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Vegetable
import java.text.Collator
import java.util.Locale

/** The filters of the Seizoen "Nu" view (spec §6.3). ENDING vegetables are not available, so they have no filter. */
enum class SeasonFilter {
    /** PEAK or SHOULDER; the default. */
    AVAILABLE,
    PEAK,
    SOON,
    ;

    fun matches(status: SeasonStatus): Boolean =
        when (this) {
            AVAILABLE -> status.isAvailable
            PEAK -> status == SeasonStatus.PEAK
            SOON -> status == SeasonStatus.SOON
        }
}

/** A vegetable as presented in a given month: its status and badges. */
data class VegetableInSeason(
    val vegetable: Vegetable,
    val status: SeasonStatus,
    /** Badge "Laatste kans": PEAK now and neither PEAK nor SHOULDER next month. */
    val lastChance: Boolean,
    /** Badge "Kort seizoen": at most 3 months of PEAK or SHOULDER. */
    val shortSeason: Boolean,
    /** Number of months with PEAK or SHOULDER. */
    val availableMonths: Int,
)

/** Presentation rules of spec §11.1: badges, filters and the ranked order. */
object SeasonPresentation {
    private const val SHORT_SEASON_MAX_MONTHS = 3

    fun present(
        vegetable: Vegetable,
        month: Int,
    ): VegetableInSeason {
        val status = Seasonality.statusFor(vegetable, month)
        val next = Seasonality.nextMonth(month)
        val available = (vegetable.peak + vegetable.shoulder).toSet().size
        return VegetableInSeason(
            vegetable = vegetable,
            status = status,
            lastChance = status == SeasonStatus.PEAK && next !in vegetable.peak && next !in vegetable.shoulder,
            shortSeason = available <= SHORT_SEASON_MAX_MONTHS,
            availableMonths = available,
        )
    }

    /** All vegetables in [month], in the ranked order. */
    fun ranked(
        vegetables: List<Vegetable>,
        month: Int,
    ): List<VegetableInSeason> = vegetables.map { present(it, month) }.sortedWith(order)

    /** The vegetables matching [filter] in [month], in the ranked order. */
    fun filtered(
        vegetables: List<Vegetable>,
        month: Int,
        filter: SeasonFilter,
    ): List<VegetableInSeason> = ranked(vegetables, month).filter { filter.matches(it.status) }

    fun counts(
        vegetables: List<Vegetable>,
        month: Int,
    ): Map<SeasonFilter, Int> {
        val statuses = vegetables.map { Seasonality.statusFor(it, month) }
        return SeasonFilter.entries.associateWith { filter -> statuses.count(filter::matches) }
    }

    /**
     * Status (PEAK, SHOULDER, SOON, then the rest) → Laatste kans first → Kort seizoen first → fewer available months
     * first → Dutch name. Keeps long lists meaningful in September, when 43 vegetables peak at once.
     */
    val order: Comparator<VegetableInSeason> =
        compareBy<VegetableInSeason> { statusRank(it.status) }
            .thenBy { !it.lastChance }
            .thenBy { !it.shortSeason }
            .thenBy { it.availableMonths }
            .thenBy(dutchCollator()) { it.vegetable.nl }

    private fun statusRank(status: SeasonStatus): Int =
        when (status) {
            SeasonStatus.PEAK -> 0
            SeasonStatus.SHOULDER -> 1
            SeasonStatus.SOON -> 2
            SeasonStatus.ENDING, SeasonStatus.OFF -> 3
        }

    /** Dutch collation: case- and accent-insensitive at the primary level. */
    fun dutchCollator(): Collator = Collator.getInstance(Locale.forLanguageTag("nl-NL")).apply { strength = Collator.SECONDARY }
}
