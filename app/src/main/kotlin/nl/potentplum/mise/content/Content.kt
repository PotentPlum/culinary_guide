package nl.potentplum.mise.content

import nl.potentplum.mise.content.model.Archetype
import nl.potentplum.mise.content.model.Discipline
import nl.potentplum.mise.content.model.Facet
import nl.potentplum.mise.content.model.FacetKey
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.content.model.Vegetable

/** The read-only curated content from `data/assets` (spec §7.1), with O(1) lookups by id. */
class Content(
    val vegetables: List<Vegetable>,
    val techniques: List<Technique>,
    val facets: List<Facet>,
    /** In display order: mechanical, thermal, flavor. */
    val disciplines: List<Discipline>,
    val archetypes: List<Archetype>,
) {
    val vegetablesById: Map<String, Vegetable> = vegetables.associateBy { it.id }
    val techniquesById: Map<String, Technique> = techniques.associateBy { it.id }
    val facetsByKey: Map<FacetKey, Facet> = facets.associateBy { it.key }

    /** Techniques that can be practised, tagged on recipes and sent to Claude (cookware guides excluded). */
    val skills: List<Technique> get() = techniques.filter { it.isSkill }

    fun vegetable(id: String): Vegetable? = vegetablesById[id]

    fun technique(id: String): Technique? = techniquesById[id]

    fun facet(key: FacetKey): Facet = facetsByKey.getValue(key)
}
