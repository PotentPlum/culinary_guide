package nl.potentplum.mise.claude

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingTest {
    @Test
    fun `cost is tokens times the per-MTok price`() {
        // 1M input + 1M output on Sonnet 5.5 = $2 + $10.
        assertEquals(12_000_000, Pricing.costMicroUsd(ClaudeModels.SONNET, Usage(1_000_000, 1_000_000)))
        // Haiku 5.5: 10k in, 2k out = 0.001 + 0.001 USD.
        assertEquals(2_000, Pricing.costMicroUsd(ClaudeModels.HAIKU, Usage(10_000, 2_000)))
        // Opus 5.5 with cache reads (0.20) and writes (1.25 × 4.00).
        assertEquals(4_000_000 + 200_000 + 5_000_000, Pricing.costMicroUsd(ClaudeModels.OPUS, Usage(1_000_000, 0, 1_000_000, 1_000_000)))
    }

    @Test
    fun `unknown models are priced as opus`() {
        assertEquals(Pricing.price(ClaudeModels.OPUS), Pricing.price("claude-opus-4-8"))
    }

    @Test
    fun `text estimates grow with the text`() {
        assertTrue(
            Pricing.estimateMicroUsdForText(ClaudeModels.SONNET, 60_000) > Pricing.estimateMicroUsdForText(ClaudeModels.SONNET, 1_000),
        )
    }

    @Test
    fun `models follow kwaliteit and enrichment is always haiku`() {
        assertEquals(ClaudeModels.HAIKU, ClaudeModels.forQuality(nl.potentplum.mise.data.settings.Quality.FAST))
        assertEquals(ClaudeModels.SONNET, ClaudeModels.forQuality(nl.potentplum.mise.data.settings.Quality.BALANCED))
        assertEquals(ClaudeModels.OPUS, ClaudeModels.forQuality(nl.potentplum.mise.data.settings.Quality.BEST))
        assertEquals(ClaudeModels.HAIKU, ClaudeModels.ENRICHMENT)
    }
}
