package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Vegetable

/** What the matcher needs from an ingredient line: its structured name, else its text. */
data class IngredientRef(
    val name: String?,
    val text: String,
)

/**
 * Which vegetables a recipe uses (spec §11.4): any of a vegetable's " / " names, folded, matches an ingredient's
 * structured name, or for unparsed lines appears in its text, as a whole word, singular or with a Dutch plural.
 */
class SeasonalMatcher(
    vegetables: List<Vegetable>,
) {
    private val patterns: List<Pair<String, Regex>> =
        vegetables.map { vegetable ->
            val forms =
                vegetable.nl
                    .split(" / ")
                    .flatMap { forms(TextMatch.fold(it)) }
                    .toSet()
            vegetable.id to Regex("(?<!\\p{L})(${forms.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }})(?!\\p{L})")
        }

    fun vegetableIds(ingredients: List<IngredientRef>): Set<String> {
        val texts = ingredients.map { TextMatch.fold(it.name ?: it.text) }
        return patterns.filter { (_, pattern) -> texts.any { pattern.containsMatchIn(it) } }.map { it.first }.toSet()
    }

    companion object {
        private val DOUBLE_VOWEL = Regex("(aa|ee|oo|uu)([^aeiou])$")

        /** "tomaat" → tomaat, tomaten; "wortelen" → wortelen, wortel; "radijs" → radijzen; "sjalot" → sjalotten. */
        fun forms(name: String): Set<String> {
            val forms = mutableSetOf(name, name + "s", name + "en", name + "'s")
            DOUBLE_VOWEL.find(name)?.let { forms += name.substring(0, it.range.first) + it.groupValues[1][0] + it.groupValues[2] + "en" }
            name.lastOrNull()?.let { last ->
                if (last !in "aeiou") forms += name + last + "en"
                if (last == 's') forms += name.dropLast(1) + "zen"
                if (last == 'f') forms += name.dropLast(1) + "ven"
            }
            listOf("en", "s", "'s").forEach { suffix ->
                if (name.endsWith(suffix) && name.length - suffix.length >= 3) forms += name.removeSuffix(suffix)
            }
            return forms
        }
    }
}

/** The derived prep-time facet (spec §8.4): `<15`, `<30`, `<60`, `>60` from `totalMinutes`; never stored or asked of Claude. */
enum class PrepTimeBucket(
    val facetValue: String,
) {
    UNDER_15("<15"),
    UNDER_30("<30"),
    UNDER_60("<60"),
    OVER_60(">60"),
    ;

    companion object {
        fun of(totalMinutes: Int?): PrepTimeBucket? =
            when {
                totalMinutes == null || totalMinutes <= 0 -> null
                totalMinutes < 15 -> UNDER_15
                totalMinutes < 30 -> UNDER_30
                totalMinutes < 60 -> UNDER_60
                else -> OVER_60
            }
    }
}
