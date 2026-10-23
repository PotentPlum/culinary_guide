package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Technique

/**
 * Finds the techniques a cook-mode step names (spec §6.10), for its info buttons. Names are infinitives ("Blancheren") while
 * steps use the imperative ("Blancheer de bonen"), so each single-word name also matches its stem and the stem with a doubled
 * vowel (smoren → smor, smoor). Only practised skills count; cookware guides are not techniques of a step.
 */
class TechniqueMentions(
    techniques: List<Technique>,
) {
    private val patterns: List<Pair<Technique, Regex>> =
        techniques.filter { it.isSkill }.map { technique ->
            val forms = forms(TextMatch.fold(technique.nl))
            technique to Regex("(?<!\\p{L})(${forms.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})(?!\\p{L})")
        }

    /** In the order they first appear in [text], at most [limit]. */
    fun find(
        text: String,
        limit: Int = MAX_PER_STEP,
    ): List<Technique> {
        val folded = TextMatch.fold(text)
        return patterns
            .mapNotNull { (technique, pattern) -> pattern.find(folded)?.let { technique to it.range.first } }
            .sortedBy { it.second }
            .map { it.first }
            .take(limit)
    }

    companion object {
        const val MAX_PER_STEP = 3
        private const val VOWELS = "aeiou"

        fun forms(name: String): Set<String> {
            if (' ' in name || !name.endsWith("en") || name.length < 5) return setOf(name)
            val stem = name.dropLast(2)
            val forms = mutableSetOf(name, stem)
            // smor → smoor, kok → kook, blancher → blancheer: a single vowel between consonants before the last consonant.
            val last = stem.last()
            val vowel = stem[stem.length - 2]
            val before = stem.getOrNull(stem.length - 3)
            if (last !in VOWELS && vowel in VOWELS && (before == null || before !in VOWELS)) {
                forms += stem.dropLast(1) + vowel + last
            }
            return forms
        }
    }
}
