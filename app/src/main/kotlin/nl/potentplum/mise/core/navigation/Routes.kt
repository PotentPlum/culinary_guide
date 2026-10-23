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
