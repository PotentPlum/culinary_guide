package nl.potentplum.mise

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.first
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.LocalSnackbarObstacles
import nl.potentplum.mise.core.designsystem.component.MiseSnackbar
import nl.potentplum.mise.core.designsystem.component.MiseSnackbarHost
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.component.SnackbarObstacles
import nl.potentplum.mise.core.navigation.AboutRoute
import nl.potentplum.mise.core.navigation.CheckRoute
import nl.potentplum.mise.core.navigation.CookRoute
import nl.potentplum.mise.core.navigation.CourseRoute
import nl.potentplum.mise.core.navigation.CurriculumRoute
import nl.potentplum.mise.core.navigation.ExerciseRoute
import nl.potentplum.mise.core.navigation.ImportLinkRoute
import nl.potentplum.mise.core.navigation.JournalGraph
import nl.potentplum.mise.core.navigation.JournalRoute
import nl.potentplum.mise.core.navigation.KeyRoute
import nl.potentplum.mise.core.navigation.LessonRoute
import nl.potentplum.mise.core.navigation.OnboardingRoute
import nl.potentplum.mise.core.navigation.PhotoImportRoute
import nl.potentplum.mise.core.navigation.QuestRefineRoute
import nl.potentplum.mise.core.navigation.QuestRoute
import nl.potentplum.mise.core.navigation.RecipeEditRoute
import nl.potentplum.mise.core.navigation.RecipeRoute
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
import nl.potentplum.mise.feature.PlaceholderScreen
import nl.potentplum.mise.feature.cook.CookScreen
import nl.potentplum.mise.feature.curriculum.CheckScreen
import nl.potentplum.mise.feature.curriculum.CourseScreen
import nl.potentplum.mise.feature.curriculum.CurriculumScreen
import nl.potentplum.mise.feature.curriculum.ExerciseScreen
import nl.potentplum.mise.feature.curriculum.LessonScreen
import nl.potentplum.mise.feature.journal.JournalScreen
import nl.potentplum.mise.feature.journal.JournalSegment
import nl.potentplum.mise.feature.onboarding.FirstRecipeChoice
import nl.potentplum.mise.feature.onboarding.OnboardingScreen
import nl.potentplum.mise.feature.quests.QuestRefineScreen
import nl.potentplum.mise.feature.quests.QuestScreen
import nl.potentplum.mise.feature.recipes.AddRecipeOption
import nl.potentplum.mise.feature.recipes.ImportLinkScreen
import nl.potentplum.mise.feature.recipes.LibraryScreen
import nl.potentplum.mise.feature.recipes.PhotoImportScreen
import nl.potentplum.mise.feature.recipes.RecipeDetailScreen
import nl.potentplum.mise.feature.recipes.RecipeEditScreen
import nl.potentplum.mise.feature.season.SeasonScreen
import nl.potentplum.mise.feature.settings.AboutScreen
import nl.potentplum.mise.feature.settings.KeyScreen
import nl.potentplum.mise.feature.settings.SettingsScreen
import nl.potentplum.mise.feature.shopping.ShoppingScreen
import nl.potentplum.mise.feature.techniques.TechniqueScreen
import nl.potentplum.mise.feature.techniques.TechniquesScreen
import nl.potentplum.mise.feature.today.TodayActions
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

/** Single-activity shell: a [NavHost] with one nested graph per tab, the bottom [NavigationBar] and one snackbar host. */
@Composable
fun MiseAppShell(
    today: LocalDate,
    uncheckedShoppingCount: Int,
    navController: NavHostController = rememberNavController(),
    pendingShare: SharedContent? = null,
    onShareHandled: () -> Unit = {},
    /** First run (spec §6.16) is not finished yet: it opens on top of Vandaag. */
    showOnboarding: Boolean = false,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember(snackbarHost, scope) { MiseSnackbar(snackbarHost, scope) }
    val snackbarObstacles = remember { SnackbarObstacles() }

    val onOverflowItem: (OverflowItem) -> Unit = { item ->
        navController.navigate(
            when (item) {
                OverflowItem.Techniques -> TechniquesRoute
                OverflowItem.Curriculum -> CurriculumRoute
                OverflowItem.Settings -> SettingsRoute
                OverflowItem.About -> AboutRoute
            },
        ) { launchSingleTop = true }
    }
    val onBack: () -> Unit = { navController.navigateUp() }
    val onVegetable: (String) -> Unit = { navController.navigate(VegetableRoute(it)) }
    val onTechnique: (String) -> Unit = { navController.navigate(TechniqueRoute(it)) }
    val onCourse: (String) -> Unit = { navController.navigate(CourseRoute(it)) }
    val onLesson: (String) -> Unit = { navController.navigate(LessonRoute(it)) }
    val onExercise: (String) -> Unit = { navController.navigate(ExerciseRoute(it)) }
    val onCheck: (String) -> Unit = { navController.navigate(CheckRoute(it)) }
    val onRecipe: (String) -> Unit = { navController.navigate(RecipeRoute(it)) }
    val onAddKey: () -> Unit = { navController.navigate(KeyRoute) { launchSingleTop = true } }

    /** Opens [route] in place of the import screen that led to it. */
    val replaceImport: (Any) -> Unit = { route ->
        navController.navigate(route) { popUpTo<ImportLinkRoute> { inclusive = true } }
    }

    // Once per launch: the restored back stack already holds it after a configuration change.
    var onboardingOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showOnboarding) {
        if (!showOnboarding || onboardingOpened) return@LaunchedEffect
        navController.currentBackStackEntryFlow.first()
        onboardingOpened = true
        // A share that started the app goes first; first run comes back on the next launch.
        if (pendingShare == null) navController.navigate(OnboardingRoute) { launchSingleTop = true }
    }

    // Shares open the import flow on top of the current stack (spec §6.1).
    LaunchedEffect(pendingShare) {
        if (pendingShare == null) return@LaunchedEffect
        // A share can arrive with the cold start, before the NavHost has set its graph.
        navController.currentBackStackEntryFlow.first()
        when (pendingShare) {
            is SharedContent.Link -> navController.navigate(ImportLinkRoute(pendingShare.url))
            is SharedContent.Images -> navController.navigate(PhotoImportRoute(shared = pendingShare.uris.map { it.toString() }))
            is SharedContent.OpenRecipe -> navController.navigate(RecipeRoute(pendingShare.id))
            null -> return@LaunchedEffect
        }
        onShareHandled()
    }

    CompositionLocalProvider(LocalMiseSnackbar provides snackbar, LocalSnackbarObstacles provides snackbarObstacles) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                val entry by navController.currentBackStackEntryAsState()
                if (entry?.destination?.hidesBottomBar() != true) MiseNavigationBar(navController, uncheckedShoppingCount)
            },
            snackbarHost = { MiseSnackbarHost(snackbarHost, snackbarObstacles) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = TodayGraph,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            ) {
                navigation<TodayGraph>(startDestination = TodayRoute) {
                    composable<TodayRoute> {
                        TodayScreen(
                            today = today,
                            actions =
                                TodayActions(
                                    onRecipe = onRecipe,
                                    onCook = { navController.navigate(CookRoute(it)) },
                                    onImportLink = { navController.navigate(ImportLinkRoute()) },
                                    onPhotos = { navController.navigate(PhotoImportRoute(camera = true)) },
                                    onVegetable = onVegetable,
                                    onAllVegetables = { navController.navigateToTab(TopLevelDestination.Season) },
                                    onOpenQuest = { navController.navigate(QuestRoute(it)) },
                                    onRefineQuest = { navController.navigate(QuestRefineRoute()) },
                                    onAllQuests = {
                                        navController.navigateToTab(TopLevelDestination.Journal)
                                        runCatching { navController.getBackStackEntry<JournalRoute>() }
                                            .getOrNull()
                                            ?.savedStateHandle
                                            ?.set(KEY_JOURNAL_SEGMENT, JournalSegment.QUESTS)
                                    },
                                    onAddKey = onAddKey,
                                    onLesson = onLesson,
                                    onExercise = onExercise,
                                    onCheck = onCheck,
                                ),
                            onOverflowItem = onOverflowItem,
                        )
                    }
                }
                navigation<SeasonGraph>(startDestination = SeasonRoute) {
                    composable<SeasonRoute> { SeasonScreen(onVegetable = onVegetable, onOverflowItem = onOverflowItem) }
                }
                navigation<RecipesGraph>(startDestination = RecipesRoute) {
                    composable<RecipesRoute> {
                        LibraryScreen(
                            onRecipe = onRecipe,
                            onAdd = { option ->
                                navController.navigate(
                                    when (option) {
                                        AddRecipeOption.Link -> ImportLinkRoute()
                                        AddRecipeOption.Camera -> PhotoImportRoute(camera = true)
                                        AddRecipeOption.Gallery -> PhotoImportRoute(camera = false)
                                        AddRecipeOption.Manual -> RecipeEditRoute()
                                    },
                                )
                            },
                            onOverflowItem = onOverflowItem,
                        )
                    }
                }
                navigation<ShoppingGraph>(startDestination = ShoppingRoute) {
                    composable<ShoppingRoute> { ShoppingScreen(onOverflowItem = onOverflowItem) }
                }
                navigation<JournalGraph>(startDestination = JournalRoute) {
                    composable<JournalRoute> { entry ->
                        val requested by entry.savedStateHandle
                            .getStateFlow<JournalSegment?>(KEY_JOURNAL_SEGMENT, null)
                            .collectAsStateWithLifecycle()
                        JournalScreen(
                            onOverflowItem = onOverflowItem,
                            onOpenQuest = { navController.navigate(QuestRoute(it)) },
                            onNewQuest = { navController.navigate(QuestRefineRoute()) },
                            onRecipe = onRecipe,
                            onVegetable = onVegetable,
                            onAllVegetables = { navController.navigateToTab(TopLevelDestination.Season) },
                            onTechnique = onTechnique,
                            onToday = { navController.navigateToTab(TopLevelDestination.Today) },
                            requestedSegment = requested,
                            onSegmentRequestHandled = { entry.savedStateHandle[KEY_JOURNAL_SEGMENT] = null },
                        )
                    }
                }
                composable<TechniquesRoute> {
                    TechniquesScreen(
                        onBack = onBack,
                        onTechnique = onTechnique,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<CurriculumRoute> {
                    CurriculumScreen(
                        onBack = onBack,
                        onCourse = onCourse,
                        onLesson = onLesson,
                        onExercise = onExercise,
                        onCheck = onCheck,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<CourseRoute> {
                    CourseScreen(
                        onBack = onBack,
                        onCourse = onCourse,
                        onLesson = onLesson,
                        onExercise = onExercise,
                        onCheck = onCheck,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<LessonRoute> {
                    LessonScreen(
                        onBack = onBack,
                        // The next lesson replaces this one, so back returns to the course.
                        onLesson = { next -> navController.navigate(LessonRoute(next)) { popUpTo<LessonRoute> { inclusive = true } } },
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<ExerciseRoute> { ExerciseScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
                composable<CheckRoute> { CheckScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onBack = onBack,
                        onKey = onAddKey,
                        onAbout = { navController.navigate(AboutRoute) { launchSingleTop = true } },
                        onRestored = {},
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<KeyRoute> { KeyScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
                composable<OnboardingRoute> {
                    OnboardingScreen(
                        onAddKey = onAddKey,
                        onDone = { choice ->
                            when (choice) {
                                FirstRecipeChoice.LATER -> {
                                    navController.popBackStack()
                                }

                                FirstRecipeChoice.LINK, FirstRecipeChoice.PHOTOS -> {
                                    val route = if (choice == FirstRecipeChoice.LINK) ImportLinkRoute() else PhotoImportRoute(camera = true)
                                    navController.navigate(route) { popUpTo<OnboardingRoute> { inclusive = true } }
                                }
                            }
                        },
                    )
                }
                composable<AboutRoute> { AboutScreen(onBack = onBack, onOverflowItem = onOverflowItem) }
                composable<VegetableRoute> {
                    VegetableScreen(
                        onBack = onBack,
                        onVegetable = onVegetable,
                        onTechnique = onTechnique,
                        onQuest = { navController.navigate(QuestRefineRoute(vegId = it)) },
                        onRecipe = onRecipe,
                        onImportLink = { navController.navigate(ImportLinkRoute(it)) },
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
                composable<QuestRefineRoute> {
                    QuestRefineScreen(
                        onBack = onBack,
                        onOpenQuest = { navController.navigate(QuestRoute(it)) },
                        onAddKey = onAddKey,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<QuestRoute> {
                    QuestScreen(
                        onBack = onBack,
                        onCook = { navController.navigate(CookRoute(it)) },
                        onReplaced = { id -> navController.navigate(QuestRoute(id)) { popUpTo<QuestRoute> { inclusive = true } } },
                        onTechnique = onTechnique,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<RecipeRoute> {
                    RecipeDetailScreen(
                        onBack = onBack,
                        onEdit = { navController.navigate(RecipeEditRoute(it)) },
                        onCook = { id, servings -> navController.navigate(CookRoute(id, servings)) },
                        onTechnique = onTechnique,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<RecipeEditRoute> { entry ->
                    val route = entry.toRoute<RecipeEditRoute>()
                    RecipeEditScreen(
                        onClose = onBack,
                        onSaved = { id ->
                            if (route.id != null && !route.draft) {
                                navController.popBackStack()
                            } else {
                                // A new or imported recipe: show it in place of the editor (and the import screen).
                                navController.navigate(RecipeRoute(id)) {
                                    popUpTo<RecipeEditRoute> { inclusive = true }
                                    popUpTo<ImportLinkRoute> { inclusive = true }
                                    popUpTo<PhotoImportRoute> { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable<ImportLinkRoute> {
                    ImportLinkScreen(
                        onBack = onBack,
                        onSaved = { replaceImport(RecipeRoute(it)) },
                        onEdit = {
                            id,
                            highlight,
                            ->
                            navController.navigate(RecipeEditRoute(id, draft = true, highlightMissing = highlight))
                        },
                        onOpenExisting = { replaceImport(RecipeRoute(it)) },
                        onManual = { replaceImport(RecipeEditRoute()) },
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<PhotoImportRoute> {
                    PhotoImportScreen(
                        onBack = onBack,
                        onSaved = { id ->
                            navController.navigate(RecipeRoute(id)) { popUpTo<PhotoImportRoute> { inclusive = true } }
                        },
                        onEdit = { id, highlight ->
                            navController.navigate(RecipeEditRoute(id, draft = true, highlightMissing = highlight))
                        },
                        onAddKey = onAddKey,
                        onOverflowItem = onOverflowItem,
                    )
                }
                composable<CookRoute> {
                    CookScreen(onClose = onBack, onLogged = onBack)
                }
            }
        }
    }
}

private const val KEY_JOURNAL_SEGMENT = "journalSegment"

/** Switching tabs saves the old tab's stack and restores the new one's (spec §6.1). */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) =
    navigate(destination.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

/**
 * Screens without the bottom bar (spec §6.1): full-screen ones (Kookmodus, first run); the app-wide pages from ⋮, which belong
 * to no tab, so any highlighted tab would be wrong; and task screens that end in save or cancel.
 */
private val withoutBottomBar =
    listOf(
        CookRoute::class,
        OnboardingRoute::class,
        TechniquesRoute::class,
        TechniqueRoute::class,
        CurriculumRoute::class,
        CourseRoute::class,
        LessonRoute::class,
        ExerciseRoute::class,
        CheckRoute::class,
        SettingsRoute::class,
        KeyRoute::class,
        AboutRoute::class,
        RecipeEditRoute::class,
        ImportLinkRoute::class,
        PhotoImportRoute::class,
        QuestRefineRoute::class,
    )

private fun NavDestination.hidesBottomBar(): Boolean = withoutBottomBar.any { hasRoute(it) }

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
    BoxWithConstraints {
        val labelStyle = rememberTabLabelStyle(maxWidth)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            TabItems(navController, selectedTab, { selectedTab = it }, uncheckedShoppingCount, labelStyle)
        }
    }
}

/**
 * One size for all five labels: the largest (up to labelMedium) at which the widest still fits a fifth of the bar, so a
 * large font setting shrinks them together instead of clipping one or giving each its own size.
 */
@Composable
private fun rememberTabLabelStyle(barWidth: Dp): TextStyle {
    val base = MaterialTheme.typography.labelMedium
    val labels = TopLevelDestination.entries.map { stringResource(it.labelRes) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, barWidth, base, density) {
        // NavigationBar spaces its items 8dp apart; a label may use its whole item.
        val itemWidth = with(density) { ((barWidth - 8.dp * (labels.size - 1)) / labels.size - 4.dp).toPx() }
        var size = base.fontSize
        // At least 10dp on screen whatever the system text size: sp would scale this floor up with the text it must shrink.
        val min = (10f / density.fontScale).sp
        while (size.value > min.value) {
            val style = base.copy(fontSize = size)
            if (labels.all { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width <= itemWidth }) break
            size = (size.value - 0.5f).sp
        }
        base.copy(fontSize = size)
    }
}

@Composable
private fun RowScope.TabItems(
    navController: NavHostController,
    selectedTab: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    uncheckedShoppingCount: Int,
    labelStyle: TextStyle,
) {
    run {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = {
                    onSelect(destination)
                    navController.navigateToTab(destination)
                },
                icon = { TabIcon(destination, selected, uncheckedShoppingCount) },
                label = { Text(stringResource(destination.labelRes), maxLines = 1, softWrap = false, style = labelStyle) },
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
