package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.ShopDepartment
import nl.potentplum.mise.content.model.Vegetable
import java.text.Normalizer

/**
 * Puts a shopping line in a shop department (spec §11.7), so the list follows a walk through the shop.
 *
 * Every word of the line is compared with every keyword. The best match wins: a whole word, then the end of a compound
 * (in Dutch the head comes last: "paprikapoeder" is a poeder, "kippenbouillon" a bouillon), then the start ("uien",
 * "kippendijen"); within a kind, the longer keyword wins, then the earlier department. A word may carry a plural or
 * diminutive ending ("cherrytomaatjes"). The vegetables are groente by their own names, so the content needs no copy of
 * them. Without a match the line goes to the last department, Overig.
 */
class DepartmentClassifier(
    departments: List<ShopDepartment>,
    vegetables: List<Vegetable>,
) {
    /** In display order. */
    val departmentIds: List<String> = departments.map { it.id }

    private val fallback = departmentIds.lastOrNull() ?: ""
    private val keywords: List<Pair<String, Int>> =
        buildList {
            departments.forEachIndexed { index, department -> department.keywords.forEach { add(words(it).joinToString("") to index) } }
            val groente = departmentIds.indexOf(GROENTE)
            if (groente >= 0) {
                vegetables
                    .flatMap { it.nl.split(" / ") }
                    .mapNotNull { words(it).lastOrNull() }
                    .distinct()
                    .forEach { add(it to groente) }
            }
        }

    fun departmentOf(text: String): String {
        var best: Match? = null
        for (word in words(text)) {
            val forms = forms(word)
            for ((keyword, department) in keywords) {
                val kind =
                    when {
                        keyword in forms -> EXACT
                        keyword.length >= MIN_SUFFIX && forms.any { it.endsWith(keyword) } -> SUFFIX
                        word.startsWith(keyword) -> PREFIX
                        else -> continue
                    }
                val match = Match(kind, keyword.length, department)
                if (best == null || match > best) best = match
            }
        }
        return best?.let { departmentIds[it.department] } ?: fallback
    }

    private data class Match(
        val kind: Int,
        val length: Int,
        val department: Int,
    ) : Comparable<Match> {
        override fun compareTo(other: Match): Int = compareValuesBy(this, other, { it.kind }, { it.length }, { -it.department })
    }

    companion object {
        const val GROENTE = "groente"
        private const val PREFIX = 0
        private const val SUFFIX = 1
        private const val EXACT = 2

        /** Shorter endings ("ui", "ei", "ham") would match inside unrelated words ("prei", "boterham"). */
        private const val MIN_SUFFIX = 4
        private val ENDINGS = listOf("tjes", "jes", "en", "es", "s")
        private val NON_LETTERS = Regex("[^a-z]+")
        private val MARKS = Regex("\\p{M}+")

        /** Lowercase words without diacritics: "Crème fraîche" → creme, fraiche. */
        fun words(text: String): List<String> =
            MARKS
                .replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
                .split(NON_LETTERS)
                .filter { it.length >= 2 }

        /** The word and the word without a plural or diminutive ending. */
        private fun forms(word: String): List<String> =
            listOf(word) + ENDINGS.filter { word.length > it.length + 1 && word.endsWith(it) }.map { word.removeSuffix(it) }
    }
}
