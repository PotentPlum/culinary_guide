package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Dimension
import nl.potentplum.mise.content.model.MeasureUnit
import nl.potentplum.mise.core.util.DutchNumbers
import kotlin.math.roundToLong

/** A quantity ready to show: amounts already rounded, [unit] possibly switched (g → kg). */
data class DisplayQuantity(
    val quantity: Double,
    val quantityMax: Double?,
    val unit: MeasureUnit?,
)

/**
 * Scaling and Dutch formatting (spec §11.2). Parsed items scale their quantity and round sensibly per dimension;
 * unparsed items fall back to the web app's text scaling (every number multiplied). A pack size lives in the note,
 * so it never scales.
 */
class IngredientScaler(
    units: List<MeasureUnit>,
) {
    private val unitsById = units.associateBy { it.id }
    private val gram = unitsById.getValue("g")
    private val kilogram = unitsById.getValue("kg")
    private val milliliter = unitsById.getValue("ml")
    private val liter = unitsById.getValue("l")

    /** `targetServings / baseServings`; null when the base is unknown (the stepper is hidden then). */
    fun ratio(
        baseServings: Int?,
        targetServings: Int,
    ): Double? = baseServings?.takeIf { it > 0 }?.let { targetServings.toDouble() / it }

    /** The line as shown: "150 g boter, in blokjes", "2–3 tenen knoflook", or the (scaled) text. */
    fun format(
        ingredient: ParsedIngredient,
        ratio: Double = 1.0,
    ): String {
        if (!ingredient.parsed) return scaleText(ingredient.text, ratio)
        val parts = mutableListOf<String>()
        val unit = ingredient.unit?.let(unitsById::get)
        ingredient.quantity?.let { quantity ->
            val display = scale(quantity, ingredient.quantityMax, unit, ratio)
            parts += formatAmount(display)
            display.unit?.let { parts += it.label(display.quantityMax ?: display.quantity) }
        } ?: unit?.takeIf { it.id != TO_TASTE }?.let { parts += it.singular }
        ingredient.name?.let { parts += it }
        // "peper naar smaak", not "naar smaak peper".
        if (ingredient.quantity == null && unit?.id == TO_TASTE) parts += unit.singular
        val line = parts.joinToString(" ")
        return ingredient.note?.let { if (line.isEmpty()) it else "$line, $it" } ?: line
    }

    fun scale(
        quantity: Double,
        quantityMax: Double?,
        unit: MeasureUnit?,
        ratio: Double,
    ): DisplayQuantity {
        if (ratio == 1.0) return DisplayQuantity(quantity, quantityMax, unit)
        val min = quantity * ratio
        val max = quantityMax?.times(ratio)
        return when {
            unit == null || unit.dimension == Dimension.COUNT || unit.dimension == Dimension.OTHER -> {
                DisplayQuantity(nearestHalf(min), max?.let(::nearestHalf), unit)
            }

            unit.id == "el" || unit.id == "tl" -> {
                DisplayQuantity(nearestHalf(min), max?.let(::nearestHalf), unit)
            }

            else -> {
                metric(min, max, unit)
            }
        }
    }

    /** g/ml: < 50 whole numbers, ≥ 50 nearest 5; from 1000 on in kg/l with one decimal. */
    private fun metric(
        min: Double,
        max: Double?,
        unit: MeasureUnit,
    ): DisplayQuantity {
        val factor = unit.factor ?: 1.0
        val base = if (unit.dimension == Dimension.MASS) gram else milliliter
        val big = if (unit.dimension == Dimension.MASS) kilogram else liter
        val minBase = roundBase(min * factor)
        val maxBase = max?.let { roundBase(it * factor) }
        return if ((maxBase ?: minBase) >= 1000) {
            DisplayQuantity(oneDecimal(minBase / 1000), maxBase?.let { oneDecimal(it / 1000) }, big)
        } else {
            DisplayQuantity(minBase, maxBase, base)
        }
    }

    private fun roundBase(value: Double): Double =
        when {
            value < 50 -> maxOf(1.0, value.roundToLong().toDouble())
            else -> (value / 5).roundToLong() * 5.0
        }

    private fun oneDecimal(value: Double): Double = (value * 10).roundToLong() / 10.0

    private fun nearestHalf(value: Double): Double = maxOf(0.5, (value * 2).roundToLong() / 2.0)

    private fun formatAmount(display: DisplayQuantity): String {
        val min = DutchNumbers.format(display.quantity)
        return display.quantityMax?.let { "$min–${DutchNumbers.format(it)}" } ?: min
    }

    companion object {
        private const val TO_TASTE = "naar-smaak"
        private val FRACTIONS =
            mapOf(
                '½' to "0.5",
                '¼' to "0.25",
                '¾' to "0.75",
                '⅓' to "0.333",
                '⅔' to "0.667",
                '⅛' to "0.125",
                '⅜' to "0.375",
                '⅝' to "0.625",
                '⅞' to "0.875",
            )
        private val NUMBER = Regex("\\d+(?:[.,]\\d+)?")

        /** The web app's text scaling for unparsed lines, with Dutch decimal commas. */
        fun scaleText(
            text: String,
            ratio: Double,
        ): String {
            if (ratio == 1.0) return text
            var result = text
            FRACTIONS.forEach { (char, decimal) -> result = result.replace(char.toString(), decimal) }
            return NUMBER.replace(result) { match ->
                val scaled = match.value.replace(',', '.').toDouble() * ratio
                DutchNumbers.format(scaled, maxDecimals = 1)
            }
        }
    }
}
