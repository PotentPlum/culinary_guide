package nl.potentplum.mise.core.util

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates in Dutch, in Amsterdam time (spec §15.4). */
object DutchDates {
    private val DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("nl-NL"))

    /** "22 oktober 2026". */
    fun day(epochMillis: Long): String = DAY.format(Instant.ofEpochMilli(epochMillis).atZone(MiseClock.ZONE))
}
