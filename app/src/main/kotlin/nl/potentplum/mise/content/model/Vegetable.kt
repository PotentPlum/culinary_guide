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

/** The family as written in the content (Dutch); content, so it is displayed as is. */
@Serializable
enum class Family(
    val nl: String,
) {
    @SerialName("Wortel")
    WORTEL("Wortel"),

    @SerialName("Blad")
    BLAD("Blad"),

    @SerialName("Brassica")
    BRASSICA("Brassica"),

    @SerialName("Allium")
    ALLIUM("Allium"),

    @SerialName("Peulvrucht")
    PEULVRUCHT("Peulvrucht"),

    @SerialName("Stengel")
    STENGEL("Stengel"),

    @SerialName("Pompoen")
    POMPOEN("Pompoen"),

    @SerialName("Knolgewas")
    KNOLGEWAS("Knolgewas"),

    @SerialName("Vruchtgroente")
    VRUCHTGROENTE("Vruchtgroente"),

    @SerialName("Paddenstoel")
    PADDENSTOEL("Paddenstoel"),

    @SerialName("Kruid")
    KRUID("Kruid"),
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
