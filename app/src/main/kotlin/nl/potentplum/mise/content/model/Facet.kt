package nl.potentplum.mise.content.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/facets.schema.json`. */
@Serializable
data class FacetsFile(
    val schemaVersion: Int,
    val facets: List<Facet>,
)

@Serializable
data class Facet(
    val key: FacetKey,
    val nl: String,
    val options: List<FacetOption>,
)

@Serializable
data class FacetOption(
    val value: String,
    val nl: String,
    val color: String? = null,
)

@Serializable
enum class FacetKey {
    @SerialName("cuisine")
    CUISINE,

    @SerialName("dishType")
    DISH_TYPE,

    @SerialName("mainIngredient")
    MAIN_INGREDIENT,

    /** Derived from `totalMinutes`, never stored as a tag (spec §8.4). */
    @SerialName("prepTime")
    PREP_TIME,
}
