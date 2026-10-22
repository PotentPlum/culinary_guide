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
