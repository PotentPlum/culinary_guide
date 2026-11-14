package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Vegetable

enum class SeasonStatus {
    PEAK,
    SHOULDER,
    SOON,
    ENDING,
    OFF,
    ;

    /** Op z'n best or Verkrijgbaar: the default "Verkrijgbaar" filter (spec §11.1). */
    val isAvailable: Boolean get() = this == PEAK || this == SHOULDER
}

/** The seasonal rule of spec §11.1, unchanged from the web app. Months are 1–12. */
object Seasonality {
    fun statusFor(
        vegetable: Vegetable,
        month: Int,
    ): SeasonStatus = statusFor(vegetable.peak, vegetable.shoulder, month)

    fun statusFor(
        peak: Collection<Int>,
        shoulder: Collection<Int>,
        month: Int,
    ): SeasonStatus {
        require(month in 1..12) { "month must be 1..12, was $month" }
        return when {
            month in peak -> SeasonStatus.PEAK
            month in shoulder -> SeasonStatus.SHOULDER
            peak.any { Math.floorMod(it - month, 12) in 1..2 } -> SeasonStatus.SOON
            previousMonth(month) in peak -> SeasonStatus.ENDING
            else -> SeasonStatus.OFF
        }
    }

    fun nextMonth(month: Int): Int = month % 12 + 1

    /**
     * The last month of the unbroken run of [months] that holds [month], wrapping round the year (peak 11, 12, 1 and month 11
     * gives 1). Null when [month] is not in [months] or the run is the whole year.
     */
    fun runEnd(
        months: Collection<Int>,
        month: Int,
    ): Int? {
        if (month !in months || months.toSet().size == 12) return null
        var end = month
        while (nextMonth(end) in months) end = nextMonth(end)
        return end
    }

    /** The first month of [months] after [month] (wrapping round the year), or null when [months] is empty. */
    fun nextStart(
        months: Collection<Int>,
        month: Int,
    ): Int? = (1..12).map { Math.floorMod(month + it - 1, 12) + 1 }.firstOrNull { it in months && previousMonth(it) !in months }

    fun previousMonth(month: Int): Int = (month + 10) % 12 + 1
}

/** What a season row says in words (spec §6.3): "Op z'n best t/m november", "Op z'n best vanaf mei", … */
sealed interface SeasonLine {
    data class PeakUntil(
        val month: Int,
    ) : SeasonLine

    data object PeakAllYear : SeasonLine

    data class AvailableUntil(
        val month: Int,
    ) : SeasonLine

    data class PeakFrom(
        val month: Int,
    ) : SeasonLine

    data object None : SeasonLine

    companion object {
        fun of(
            peak: Collection<Int>,
            shoulder: Collection<Int>,
            month: Int,
        ): SeasonLine {
            val peakFrom = Seasonality.nextStart(peak, month)?.let(::PeakFrom) ?: None
            return when (Seasonality.statusFor(peak, shoulder, month)) {
                SeasonStatus.PEAK -> {
                    Seasonality.runEnd(peak, month)?.let(::PeakUntil) ?: PeakAllYear
                }

                SeasonStatus.SHOULDER -> {
                    // Before the peak it is coming; after it, say how long it stays available.
                    val available = peak.toSet() + shoulder
                    var m = month
                    var peakAhead: Int? = null
                    repeat(12) {
                        if (peakAhead == null && m in available) {
                            if (m in peak) peakAhead = m
                            m = Seasonality.nextMonth(m)
                        }
                    }
                    peakAhead?.let(::PeakFrom) ?: Seasonality.runEnd(available, month)?.let(::AvailableUntil) ?: peakFrom
                }

                SeasonStatus.SOON, SeasonStatus.ENDING, SeasonStatus.OFF -> {
                    peakFrom
                }
            }
        }
    }
}
