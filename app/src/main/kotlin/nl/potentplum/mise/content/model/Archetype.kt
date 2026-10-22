package nl.potentplum.mise.content.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/** Mirrors `data/schemas/archetypes.schema.json`: the Dagboek badges (spec §12). */
@Serializable
data class ArchetypesFile(
    val schemaVersion: Int,
    val archetypes: List<Archetype>,
)

@Serializable
data class Archetype(
    val id: String,
    val nl: String,
    val description: String,
    val rule: BadgeRule,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface BadgeRule {
    /**
     * At least [minAttempts] attempts with a technique of [discipline] or one of [techniqueIds] (exactly one of the two
     * is set), and, if [minAvgScore] is set, the mean of their non-null execution scores reaches it.
     */
    @Serializable
    @SerialName("techniques")
    data class Techniques(
        val discipline: DisciplineId? = null,
        val techniqueIds: List<String>? = null,
        val minAttempts: Int,
        val minAvgScore: Double? = null,
    ) : BadgeRule {
        init {
            require((discipline == null) != (techniqueIds == null)) { "A techniques rule needs either discipline or techniqueIds" }
        }
    }

    /** At least [min] distinct vegetables cooked while at PEAK. */
    @Serializable
    @SerialName("distinctPeakVegetables")
    data class DistinctPeakVegetables(
        val min: Int,
    ) : BadgeRule

    /** At least [min] distinct calendar months with a vegetable cooked while at PEAK. */
    @Serializable
    @SerialName("peakMonths")
    data class PeakMonths(
        val min: Int,
    ) : BadgeRule
}
