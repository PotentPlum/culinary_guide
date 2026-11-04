package nl.potentplum.mise.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Dutch number formatting: decimal comma, no trailing zeros (spec §11.2, §15.4). */
object DutchNumbers {
    fun format(
        value: Double,
        maxDecimals: Int = 2,
    ): String =
        BigDecimal
            .valueOf(value)
            .setScale(maxDecimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')

    /** "1.234.567": whole numbers with Dutch grouping (token counts). */
    fun grouped(value: Long): String = NumberFormat.getIntegerInstance(DUTCH).format(value)

    /**
     * An estimated Claude cost in euros, from micro-dollars (spec §6.15). The rate is a fixed rough one: the figure is an
     * estimate either way, and Anthropic bills in dollars.
     */
    fun euros(microUsd: Long): String {
        val euros = microUsd / 1_000_000.0 * USD_TO_EUR
        val format =
            NumberFormat.getCurrencyInstance(DUTCH).apply {
                currency = Currency.getInstance("EUR")
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
        return if (euros > 0 && euros < 0.01) "< ${format.format(0.01)}" else format.format(euros)
    }

    const val USD_TO_EUR = 0.9

    private val DUTCH: Locale = Locale.forLanguageTag("nl-NL")
}
