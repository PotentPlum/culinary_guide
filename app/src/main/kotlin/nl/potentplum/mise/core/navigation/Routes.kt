package nl.potentplum.mise.core.navigation

import kotlinx.serialization.Serializable

// Type-safe routes (spec §4.1). Features navigate to each other only through these (spec §4.2).
// Each bottom-bar tab is a nested graph, so every tab keeps its own back stack.

@Serializable
data object TodayGraph

@Serializable
data object TodayRoute

@Serializable
data object SeasonGraph

@Serializable
data object SeasonRoute

@Serializable
data object RecipesGraph

@Serializable
data object RecipesRoute

@Serializable
data object ShoppingGraph

@Serializable
data object ShoppingRoute

@Serializable
data object JournalGraph

@Serializable
data object JournalRoute

// Overflow menu destinations (spec §6.1).

@Serializable
data object TechniquesRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

// Detail destinations, reachable from several tabs; they stack on the current tab.

@Serializable
data class VegetableRoute(
    val id: String,
)

@Serializable
data class TechniqueRoute(
    val id: String,
)

/** The Verfijn quest form, optionally pre-filled (spec §6.5 "Quest met …", §6.6 "Oefen in een quest"). */
@Serializable
data class QuestRefineRoute(
    val vegId: String? = null,
    val techniqueId: String? = null,
)

/** A generated quest and its recipe (spec §6.11 Result). */
@Serializable
data class QuestRoute(
    val id: String,
)

@Serializable
data class RecipeRoute(
    val id: String,
)

/**
 * Edit mode of recipe detail; a null [id] starts an empty recipe ("Zelf invoeren"). With [draft], [id] names an unsaved
 * import in [nl.potentplum.mise.data.repo.RecipeDrafts]; [highlightMissing] marks a missing title, ingredients or steps.
 */
@Serializable
data class RecipeEditRoute(
    val id: String? = null,
    val draft: Boolean = false,
    val highlightMissing: Boolean = false,
)

/** Import by link (spec §6.9); [url] is prefilled from a share or the clipboard. */
@Serializable
data class ImportLinkRoute(
    val url: String? = null,
)

/** Import from cookbook photos (spec §6.9). */
@Serializable
data class PhotoImportRoute(
    /** Open the camera right away. */
    val camera: Boolean = false,
    /** Content URIs shared into Mise; copied into the import folder at once, while the share grant holds. */
    val shared: List<String> = emptyList(),
)

/** Kookmodus for a recipe (spec §6.10). */
@Serializable
data class CookRoute(
    val recipeId: String,
    /** The servings chosen on recipe detail; an open session's servings win. */
    val servings: Int? = null,
)
