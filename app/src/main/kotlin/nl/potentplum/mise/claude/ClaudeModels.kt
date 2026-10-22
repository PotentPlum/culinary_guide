package nl.potentplum.mise.claude

import nl.potentplum.mise.data.settings.Quality

/** Model ids (spec §0 A8, §8.2). */
object ClaudeModels {
    const val HAIKU = "claude-haiku-5-5"
    const val SONNET = "claude-sonnet-5-5"
    const val OPUS = "claude-opus-5-5"

    /** Quests and photo/page import follow Kwaliteit. */
    fun forQuality(quality: Quality): String =
        when (quality) {
            Quality.FAST -> HAIKU
            Quality.BALANCED -> SONNET
            Quality.BEST -> OPUS
        }

    /** Background enrichment always uses Haiku. */
    const val ENRICHMENT = HAIKU

    /** Haiku has no server-side refusal fallback (spec §8.1). */
    fun supportsRefusalFallback(model: String): Boolean = model == SONNET || model == OPUS
}
