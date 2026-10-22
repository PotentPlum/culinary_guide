package nl.potentplum.mise.content.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/techniques.schema.json`. */
@Serializable
data class TechniquesFile(
    val schemaVersion: Int,
    val techniques: List<Technique>,
)

@Serializable
data class Technique(
    val id: String,
    val nl: String,
    val subtitle: String,
    val category: TechniqueCategory,
    val subcategory: String?,
    val parentId: String?,
    val level: TechniqueLevel,
    val heatLevel: HeatLevel?,
    /** Null for the cookware guides: reference pages, never a practised skill (spec §6.6). */
    val discipline: DisciplineId?,
    val body: List<String>,
    val tips: List<String>,
    val cookware: List<String>,
    val relatedTechniqueIds: List<String>,
    val relatedVegIds: List<String>,
) {
    val isSkill: Boolean get() = discipline != null
}

/** Declared in display order (spec §6.6). */
@Serializable
enum class TechniqueCategory {
    @SerialName("snijden")
    SNIJDEN,

    @SerialName("garing")
    GARING,

    @SerialName("smaak")
    SMAAK,

    @SerialName("eiwitten")
    EIWITTEN,

    @SerialName("sauzen")
    SAUZEN,

    @SerialName("groenten")
    GROENTEN,

    @SerialName("gereedschap")
    GEREEDSCHAP,

    @SerialName("bewaren")
    BEWAREN,
}

@Serializable
enum class TechniqueLevel {
    @SerialName("basis")
    BASIS,

    @SerialName("gemiddeld")
    GEMIDDELD,

    @SerialName("gevorderd")
    GEVORDERD,
}

@Serializable
enum class HeatLevel {
    @SerialName("hoog")
    HOOG,

    @SerialName("middel")
    MIDDEL,

    @SerialName("laag")
    LAAG,

    @SerialName("geen")
    GEEN,
}
