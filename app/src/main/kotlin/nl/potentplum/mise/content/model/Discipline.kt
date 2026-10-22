package nl.potentplum.mise.content.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/disciplines.schema.json`; the list is in display order. */
@Serializable
data class DisciplinesFile(
    val schemaVersion: Int,
    val disciplines: List<Discipline>,
)

@Serializable
data class Discipline(
    val id: DisciplineId,
    val nl: String,
)

@Serializable
enum class DisciplineId {
    @SerialName("mechanical")
    MECHANICAL,

    @SerialName("thermal")
    THERMAL,

    @SerialName("flavor")
    FLAVOR,
}
