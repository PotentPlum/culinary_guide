package nl.potentplum.mise

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.navigation.AboutRoute
import nl.potentplum.mise.core.navigation.JournalGraph
import nl.potentplum.mise.core.navigation.JournalRoute
import nl.potentplum.mise.core.navigation.QuestRefineRoute
import nl.potentplum.mise.core.navigation.RecipesGraph
import nl.potentplum.mise.core.navigation.RecipesRoute
import nl.potentplum.mise.core.navigation.SeasonGraph
import nl.potentplum.mise.core.navigation.SeasonRoute
import nl.potentplum.mise.core.navigation.SettingsRoute
import nl.potentplum.mise.core.navigation.ShoppingGraph
import nl.potentplum.mise.core.navigation.ShoppingRoute
import nl.potentplum.mise.core.navigation.TechniqueRoute
import nl.potentplum.mise.core.navigation.TechniquesRoute
import nl.potentplum.mise.core.navigation.TodayGraph
import nl.potentplum.mise.core.navigation.TodayRoute
import nl.potentplum.mise.core.navigation.VegetableRoute
import nl.potentplum.mise.feature.journal.JournalScreen
import nl.potentplum.mise.feature.quests.QuestRefineScreen
import nl.potentplum.mise.feature.recipes.RecipesScreen
import nl.potentplum.mise.feature.season.SeasonScreen
import nl.potentplum.mise.feature.settings.AboutScreen
import nl.potentplum.mise.feature.settings.SettingsScreen
import nl.potentplum.mise.feature.shopping.ShoppingScreen
import nl.potentplum.mise.feature.techniques.TechniqueScreen
import nl.potentplum.mise.feature.techniques.TechniquesScreen
import nl.potentplum.mise.feature.today.TodayScreen
import nl.potentplum.mise.feature.vegetable.VegetableScreen
import java.time.LocalDate

/** The five bottom-bar tabs (spec §6.1). Quests have no tab; their history lives in Dagboek. */
enum class TopLevelDestination(
    val graph: Any,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    Today(TodayGraph, R.string.nav_today, Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    Season(SeasonGraph, R.string.nav_seasonal, Icons.Filled.Eco, Icons.Outlined.Eco),
    Recipes(RecipesGraph, R.string.nav_recipes, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    Shopping(ShoppingGraph, R.string.nav_shopping, Icons.Filled.ShoppingBasket, Icons.Outlined.ShoppingBasket),
    Journal(JournalGraph, R.string.nav_journal, Icons.Filled.Book, Icons.Outlined.Book),
}

/** Single-activity shell: a [NavHost] with one nested graph per tab and the bottom [NavigationBar]. */
@Composable
fun MiseAppShell(
    today: LocalDate,
    uncheckedShoppingCount: Int,
    navController: NavHostController = rememberNavController(),
) {
    val onOverflowItem: (OverflowItem) -> Unit = { item ->
        navController.navigate(
            when (item) {
                OverflowItem.Techniques -> TechniquesRoute
                OverflowItem.Settings -> SettingsRoute
                OverflowItem.About -> AboutRoute
            },
        ) { launchSingleTop = true }
    }
    val onBack: () -> Unit = { navController.navigateUp() }
    val onVegetable: (String) -> Unit = { navController.navigate(VegetableRoute(it)) }
    val onTechnique: (String) -> Unit = { navController.navigate(TechniqueRoute(it)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { MiseNavigationBar(navController, uncheckedShoppingCount) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TodayGraph,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            navigation<TodayGraph>(startDestination = TodayRoute) {
                composable<TodayRoute> { TodayScreen(today = today, onOverflowItem = onOverflowItem) }
            }
            navigation<SeasonGraph>(startDestination = SeasonRoute) {
                composable<SeasonRoute> { SeasonScreen(onVegetable = onVegetable, onOverflowItem = onOverflowItem) }
            }
            navigation<RecipesGraph>(startDestination = RecipesRoute) {
                composable<RecipesRoute> { RecipesScreen(onOverflowItem = onOverflowItem) }
            }
            navigation<ShoppingGraph>(startDestination = ShoppingRoute) {
                composable<ShoppingRoute> { ShoppingScreen(onOverflowItem = onOverflowItem) }
            }
            navigation<JournalGraph>(startDestination = JournalRoute) {
                composable<JournalRoute> { JournalScreen(onOverflowItem = onOverflowItem) }
            }
            composable<TechniquesRoute> { TechniquesScreen(onBack = onBack, onTechnique = onTechnique, onOverflowItem = onOverflowItem) }
            composable<VegetableRoute> {
                VegetableScreen(
                    onBack = onBack,
                    onVegetable = onVegetable,
                    onTechnique = onTechnique,
                    onQuest = { navController.navigate(QuestRefineRoute(vegId = it)) },
                    onOverflowItem = onOverflowItem,
                )
            }
            composable<TechniqueRoute> {
                TechniqueScreen(
                    onBack = onBack,
                    onTechnique = onTechnique,
                    onVegetable = onVegetable,
                    onQuest = { navController.navigate(QuestRefineRoute(techniqueId = it)) },
                    onOverflowItem = onOverflowItem,
                )
            }
            composable<QuestRefineRoute> { QuestRefineScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
            composable<SettingsRoute> { SettingsScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
            composable<AboutRoute> { AboutScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
        }
    }
}

@Composable
private fun MiseNavigationBar(
    navController: NavHostController,
    uncheckedShoppingCount: Int,
) {
    // Detail and overflow screens live at the root and stack on the tab they were opened from, so they keep that tab
    // selected: the selection follows clicks and any destination inside a tab graph, and stays put otherwise.
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelDestination.Today) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val inTab =
        backStackEntry?.destination?.hierarchy?.firstNotNullOfOrNull { destination ->
            TopLevelDestination.entries.firstOrNull { destination.hasRoute(it.graph::class) }
        }
    if (inTab != null && inTab != selectedTab) selectedTab = inTab
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = {
                    selectedTab = destination
                    navController.navigate(destination.graph) {
                        // Switching tabs saves the old tab's stack and restores the new one's (spec §6.1).
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { TabIcon(destination, selected, uncheckedShoppingCount) },
                label = { Text(stringResource(destination.labelRes), maxLines = 1) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
            )
        }
    }
}

@Composable
private fun TabIcon(
    destination: TopLevelDestination,
    selected: Boolean,
    uncheckedShoppingCount: Int,
) {
    val icon = @Composable { Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null) }
    if (destination == TopLevelDestination.Shopping && uncheckedShoppingCount > 0) {
        val description = pluralStringResource(R.plurals.shopping_badge_description, uncheckedShoppingCount, uncheckedShoppingCount)
        BadgedBox(
            badge = {
                Badge(Modifier.semantics { contentDescription = description }) { Text(uncheckedShoppingCount.toString()) }
            },
        ) { icon() }
    } else {
        icon()
    }
}
