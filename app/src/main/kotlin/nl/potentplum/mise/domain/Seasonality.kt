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

    fun previousMonth(month: Int): Int = (month + 10) % 12 + 1
}
