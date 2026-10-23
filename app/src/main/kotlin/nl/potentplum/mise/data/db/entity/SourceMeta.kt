package nl.potentplum.mise.data.db.entity

import kotlinx.serialization.Serializable

/** Website metadata kept from an import but never displayed (spec §6.8). */
@Serializable
data class SourceMeta(
    val ratingValue: Double? = null,
    val ratingCount: Int? = null,
    val datePublished: String? = null,
    val keywords: List<String> = emptyList(),
    val diet: List<String> = emptyList(),
    val nutrition: Nutrition? = null,
    val video: String? = null,
)

@Serializable
data class Nutrition(
    val calories: String? = null,
    val protein: String? = null,
    val carbs: String? = null,
    val fat: String? = null,
)
