package nl.potentplum.mise.domain

import kotlin.math.roundToInt

/** A duration named in a step, e.g. "8–10 minuten" → 600 s. [range] is where it sits in the step text. */
data class DetectedDuration(
    val seconds: Int,
    val range: IntRange,
)

/**
 * Finds the durations in a step for its timer chips (spec §11.3): Dutch and English seconds, minutes and hours, the upper bound
 * of a range, and decimal commas ("1,5 uur"). Numbers followed by a temperature or a quantity unit are not durations.
 */
object DurationDetector {
    private const val NUMBER = """(\d+(?:[.,]\d+)?)"""
    private val PATTERN =
        Regex(
            "$NUMBER(?:\\s*(?:-|–|tot|to)\\s*$NUMBER)?\\s*" +
                "(seconden|seconde|seconds?|secs?|minuten|minuut|minutes?|mins?|uren|uur|hours?|hrs?|u)(?![\\p{L}\\d])",
            RegexOption.IGNORE_CASE,
        )

    /** A temperature or a quantity right after the number: "200 °C", "20 g". */
    private val NOT_A_DURATION = Regex("""^\s*(°|g\b|gr\b|gram\b|ml\b|cl\b|dl\b|l\b|kg\b)""", RegexOption.IGNORE_CASE)

    private const val MAX_SECONDS = 24 * 60 * 60

    fun detect(text: String): List<DetectedDuration> =
        PATTERN
            .findAll(text)
            .mapNotNull { match ->
                val (low, high, unit) = match.destructured
                val firstNumberEnd = match.groups[1]!!.range.last + 1
                if (NOT_A_DURATION.containsMatchIn(text.substring(firstNumberEnd))) return@mapNotNull null
                val amount = number(high.ifEmpty { low }) ?: return@mapNotNull null
                val seconds = (amount * unitSeconds(unit.lowercase())).roundToInt()
                seconds.takeIf { it in 1..MAX_SECONDS }?.let { DetectedDuration(it, match.range) }
            }.distinctBy { it.seconds }
            .toList()

    private fun number(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

    private fun unitSeconds(unit: String): Int =
        when {
            unit.startsWith("sec") -> 1
            unit.startsWith("min") -> 60
            else -> 3_600
        }
}
