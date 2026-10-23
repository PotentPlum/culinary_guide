package nl.potentplum.mise.claude

import kotlin.math.roundToLong

/** USD per million tokens (spec §8.1). Prices as of [PRICES_AS_OF]; update with the Anthropic price page. */
data class ModelPrice(
    val input: Double,
    val output: Double,
    val cacheRead: Double,
) {
    /** Cache writes cost 1.25× input. */
    val cacheWrite: Double get() = input * 1.25
}

object Pricing {
    const val PRICES_AS_OF = "2026-10-22"

    private val PRICES =
        mapOf(
            ClaudeModels.HAIKU to ModelPrice(input = 0.10, output = 0.50, cacheRead = 0.01),
            ClaudeModels.SONNET to ModelPrice(input = 2.00, output = 10.00, cacheRead = 0.20),
            ClaudeModels.OPUS to ModelPrice(input = 4.00, output = 20.00, cacheRead = 0.20),
        )

    /** Unknown models (a fallback, a future id) are priced as Opus 5.5, the most expensive we use: estimates err high. */
    fun price(model: String): ModelPrice = PRICES[model] ?: PRICES.getValue(ClaudeModels.OPUS)

    /** The estimated cost of [usage] in micro-dollars (1e-6 USD). */
    fun costMicroUsd(
        model: String,
        usage: Usage,
    ): Long {
        val p = price(model)
        // Per-MTok price × tokens = micro-dollars.
        val micro =
            usage.inputTokens * p.input +
                usage.outputTokens * p.output +
                usage.cacheReadTokens * p.cacheRead +
                usage.cacheWriteTokens * p.cacheWrite
        return micro.roundToLong()
    }

    /** A rough cost estimate before sending text (spec §8.5, §6.15): about 4 characters per token, 1500 output tokens. */
    fun estimateMicroUsdForText(
        model: String,
        characters: Int,
        outputTokens: Long = 1_500,
    ): Long = costMicroUsd(model, Usage(inputTokens = characters / 4L + 2_000, outputTokens = outputTokens))
}
