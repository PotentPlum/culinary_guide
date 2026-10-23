package nl.potentplum.mise.core.util

import java.math.BigDecimal
import java.math.RoundingMode

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
}
