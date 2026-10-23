package nl.potentplum.mise.content.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors `data/schemas/vegetables.schema.json`. */
@Serializable
data class VegetablesFile(
    val schemaVersion: Int,
    val vegetables: List<Vegetable>,
)

@Serializable
data class Vegetable(
    val id: String,
    /** Dutch display name; may hold aliases separated by " / " ("Wortelen / peen"). */
    val nl: String,
    /** Internal English alias for search. Never displayed. */
    val nameEn: String,
    val family: Family,
    /** `#RRGGBB`; the `VegToken` fill and detail header background. */
    val color: String,
    /** `#RRGGBB`; decorative only (spec §5), never used for status. */
    val accent: String,
    val peak: List<Int>,
    val shoulder: List<Int>,
    val blurb: String,
    val storage: String,
    val prep: String,
    val technique: String,
    val pairings: List<String>,
    val substitutions: List<String>,
    /** English; never displayed until translated (todo: Content). */
    val nutrientsEn: String? = null,
    val sourceUrl: String? = null,
    val curatedRecipes: List<CuratedRecipe>,
)

@Serializable
enum class Family {
    @SerialName("Wortel")
    WORTEL,

    @SerialName("Blad")
    BLAD,

    @SerialName("Brassica")
    BRASSICA,

    @SerialName("Allium")
    ALLIUM,

    @SerialName("Peulvrucht")
    PEULVRUCHT,

    @SerialName("Stengel")
    STENGEL,

    @SerialName("Pompoen")
    POMPOEN,

    @SerialName("Knolgewas")
    KNOLGEWAS,

    @SerialName("Vruchtgroente")
    VRUCHTGROENTE,

    @SerialName("Paddenstoel")
    PADDENSTOEL,

    @SerialName("Kruid")
    KRUID,
}

@Serializable
data class CuratedRecipe(
    val title: String,
    val url: String,
    val source: String,
    val why: String,
    val difficulty: CuratedDifficulty,
    val timeMinutes: Int,
)

@Serializable
enum class CuratedDifficulty {
    @SerialName("easy")
    EASY,

    @SerialName("medium")
    MEDIUM,

    @SerialName("hard")
    HARD,
}
