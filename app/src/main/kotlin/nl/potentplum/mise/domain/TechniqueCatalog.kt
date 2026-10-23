package nl.potentplum.mise.domain

import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.content.model.TechniqueCategory
import nl.potentplum.mise.content.model.TechniqueLevel

data class TechniqueSubgroup(
    val subcategory: String?,
    val techniques: List<Technique>,
)

data class TechniqueCategoryGroup(
    val category: TechniqueCategory,
    val subgroups: List<TechniqueSubgroup>,
)

/** The Technieken list (spec §6.6): categories in display order, grouped by subcategory, filtered by level and search. */
object TechniqueCatalog {
    fun grouped(
        techniques: List<Technique>,
        level: TechniqueLevel? = null,
        query: String = "",
    ): List<TechniqueCategoryGroup> {
        val folded = TextMatch.fold(query)
        val matching =
            techniques.filter { technique ->
                (level == null || technique.level == level) &&
                    (folded.isEmpty() || listOf(technique.nl, technique.subtitle).any { TextMatch.fold(it).contains(folded) })
            }
        return TechniqueCategory.entries.mapNotNull { category ->
            val inCategory = matching.filter { it.category == category }
            if (inCategory.isEmpty()) {
                null
            } else {
                // Subcategories keep the order of first appearance in techniques.json.
                TechniqueCategoryGroup(category, inCategory.groupBy { it.subcategory }.map { (sub, list) -> TechniqueSubgroup(sub, list) })
            }
        }
    }

    /** Techniques whose `parentId` is [id]: "Vervolgtechnieken". */
    fun children(
        techniques: List<Technique>,
        id: String,
    ): List<Technique> = techniques.filter { it.parentId == id }
}
