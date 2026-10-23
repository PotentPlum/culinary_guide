package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Vegetable
import java.text.Normalizer

/** Case- and diacritic-insensitive text matching (spec §6.3): "mais" finds "Suikermaïs". */
object TextMatch {
    private val MARKS = Regex("\\p{M}+")

    fun fold(text: String): String = MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase().trim()

    fun contains(
        text: String,
        query: String,
    ): Boolean = fold(text).contains(fold(query))
}

object VegetableSearch {
    /** Matches `nl`, the hidden alias `nameEn` and the family; an empty query matches everything. */
    fun matches(
        vegetable: Vegetable,
        query: String,
    ): Boolean {
        val folded = TextMatch.fold(query)
        if (folded.isEmpty()) return true
        return listOf(vegetable.nl, vegetable.nameEn, vegetable.family.nl).any { TextMatch.fold(it).contains(folded) }
    }
}

/** Kalender grouping (spec §6.4). */
enum class CalendarGrouping { FAMILY, ALPHABETICAL }

data class CalendarSection(
    /** Null for the single A–Z section. */
    val family: String?,
    val vegetables: List<Vegetable>,
)

object CalendarLayout {
    fun sections(
        vegetables: List<Vegetable>,
        grouping: CalendarGrouping,
    ): List<CalendarSection> {
        val collator = SeasonPresentation.dutchCollator()
        val byName = vegetables.sortedWith(compareBy(collator) { it.nl })
        return when (grouping) {
            CalendarGrouping.ALPHABETICAL -> {
                listOf(CalendarSection(null, byName))
            }

            CalendarGrouping.FAMILY -> {
                byName
                    .groupBy { it.family.nl }
                    .toSortedMap(collator)
                    .map { (family, members) -> CalendarSection(family, members) }
            }
        }
    }
}
