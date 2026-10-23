package nl.potentplum.mise.content.model

import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/units.schema.json`. */
@Serializable
data class UnitsFile(
    val schemaVersion: Int,
    val units: List<MeasureUnit>,
)

/** A unit for structured ingredients (spec §11.2). Named so it doesn't clash with [kotlin.Unit]. */
@Serializable
data class MeasureUnit(
    val id: String,
    val singular: String,
    val plural: String,
    /** Lowercase spellings the local parser recognises after a quantity. */
    val aliases: List<String>,
    val dimension: Dimension,
    /** To the base unit of [dimension] (g, ml, stuks); null for [Dimension.OTHER]. */
    val factor: Double?,
) {
    fun label(quantity: Double): String = if (quantity == 1.0) singular else plural
}

@Serializable
enum class Dimension {
    MASS,
    VOLUME,
    COUNT,

    /** Snuf, teen, bos, blik, …: never converted or summed across units. */
    OTHER,
}
