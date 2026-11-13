package nl.potentplum.mise

import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.navigation.AboutRoute
import nl.potentplum.mise.core.navigation.CookRoute
import nl.potentplum.mise.core.navigation.CourseRoute
import nl.potentplum.mise.core.navigation.CurriculumRoute
import nl.potentplum.mise.core.navigation.JournalGraph
import nl.potentplum.mise.core.navigation.LessonRoute
import nl.potentplum.mise.core.navigation.RecipeRoute
import nl.potentplum.mise.core.navigation.RecipesGraph
import nl.potentplum.mise.core.navigation.SeasonGraph
import nl.potentplum.mise.core.navigation.SettingsRoute
import nl.potentplum.mise.core.navigation.ShoppingGraph
import nl.potentplum.mise.core.navigation.TechniqueRoute
import nl.potentplum.mise.core.navigation.TechniquesRoute
import nl.potentplum.mise.core.navigation.TodayGraph
import nl.potentplum.mise.core.navigation.VegetableRoute
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.repo.AttemptRepository
import nl.potentplum.mise.data.repo.Evaluation
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.IngredientParser
import nl.potentplum.mise.testing.LayoutAudit
import nl.potentplum.mise.testing.REVEAL_TIMEOUT_MILLIS
import nl.potentplum.mise.testing.TestContent
import nl.potentplum.mise.testing.reveal
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import javax.inject.Inject

/**
 * Opens every screen and checks every text on it with [LayoutAudit] (spec §15.3): nothing cut off, clipped or split
 * mid-word, no button label on two lines. Native graphics, so text is measured with the bundled fonts.
 * The device profiles are the subclasses below.
 */
abstract class LayoutAuditTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var recipes: RecipeRepository

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var shopping: ShoppingRepository

    @Inject lateinit var attempts: AttemptRepository

    private lateinit var nav: NavHostController
    private val audit by lazy { LayoutAudit(compose, javaClass.simpleName) }
    private val parser = IngredientParser(TestContent.content.units)

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun launch(firstRun: Boolean = false) {
        compose.setContent {
            nav = rememberNavController()
            MiseTheme {
                MiseAppShell(
                    today = LocalDate.of(2026, 10, 22),
                    uncheckedShoppingCount = 5,
                    showOnboarding = firstRun,
                    navController = remember { nav },
                )
            }
        }
        settle()
    }

    private fun settle() {
        compose.waitForIdle()
        runCatching {
            compose.waitUntil(REVEAL_TIMEOUT_MILLIS) {
                compose.onAllNodesWithContentDescription("Laden…").fetchSemanticsNodes().isEmpty()
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    private fun go(route: Any) {
        compose.runOnUiThread { nav.navigate(route) }
        settle()
    }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        settle()
    }

    private fun back() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        settle()
    }

    private fun waitFor(text: String) =
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun recipe(
        id: String,
        title: String,
        minutes: Int,
        source: String,
        lines: List<String>,
    ) = Recipe(
        id = id,
        title = title,
        sourceType = RecipeSourceType.MANUAL,
        sourceLabel = source,
        servings = 4,
        totalMinutes = minutes,
        stepGroups =
            listOf(
                StepGroup(
                    steps =
                        listOf(
                            "Schil de groenten en snijd ze in gelijke stukken.",
                            "Fruit de ui in 5 minuten glazig in een scheut olie.",
                            "Voeg de rest toe en laat 20 minuten zachtjes koken.",
                            "Breng op smaak met peper en zout.",
                        ).map { Step(text = it) },
                ),
            ),
        ingredientGroups = listOf(IngredientGroup(items = lines.map { Ingredient.fromLocalParse(parser.parse(it)) })),
    )

    private fun seed() =
        runBlocking {
            settings.setOnboardingDone(true)
            val kale =
                recipe(
                    "kale",
                    "Boerenkoolstamppot met rookworst",
                    40,
                    "Oma's schrift",
                    listOf("1 kg boerenkool", "1 kg kruimige aardappelen", "1 rookworst", "200 ml warme melk", "30 g boter"),
                )
            recipes.save(kale.copy(isFavorite = true))
            recipes.save(recipe("leek", "Pasta met prei en room", 25, "leukerecepten.nl", listOf("2 preien", "250 g pasta")))
            recipes.save(recipe("pumpkin", "Geroosterde pompoensoep", 45, "Ottolenghi", listOf("1 pompoen", "1 l bouillon")))
            recipes.save(recipe("beet", "Snelle bietensalade", 20, "ah.nl", listOf("3 gekookte rode bieten", "100 g geitenkaas")))
            recipes.save(recipe("celeriac", "Knolselderijschnitzel", 35, "Tobias", listOf("1 knolselderij", "2 eieren")))
            shopping.addRecipe(kale, recipes.scaler())
            shopping.addManual("Koffie")
            attempts.log("kale", Evaluation(cookedOn = LocalDate.of(2026, 10, 20), score = 8, tasteScore = 10, notes = "Meer mosterd."))
        }

    @Test
    fun `every screen of a used app shows its text in full`() {
        seed()
        launch()
        waitFor("Suggesties")
        audit.screen("Vandaag")
        runBlocking { settings.pinTonight("leek") }
        settle()
        audit.screen("Vandaag met Vanavond")

        go(SeasonGraph)
        audit.screen("Seizoen")
        click("Kalender")
        audit.screen("Kalender")
        go(VegetableRoute("pumpkin"))
        audit.screen("Pompoen")

        go(RecipesGraph)
        audit.screen("Recepten")
        go(RecipeRoute("kale"))
        audit.screen("Recept")
        click("Gekookt")
        waitFor("Hoe lukte het?")
        audit.screen("Hoe ging het?")
        back()
        go(CookRoute("kale"))
        waitFor("Stap 1 van 4")
        audit.screen("Kookmodus stap 1")
        repeat(3) { click("Volgende stap") }
        waitFor("Klaar met koken")
        audit.screen("Kookmodus laatste stap")
        back()

        go(ShoppingGraph)
        audit.screen("Boodschappen")
        go(JournalGraph)
        audit.screen("Dagboek")
        click("Inzichten")
        audit.screen("Inzichten")
        click("Quests")
        audit.screen("Quests")

        go(TechniquesRoute)
        audit.screen("Technieken")
        go(TechniqueRoute("julienne"))
        audit.screen("Julienne")
        go(CurriculumRoute)
        waitFor("Openen")
        audit.screen("Opleiding")
        go(CourseRoute("ORI-1"))
        audit.screen("Cursus")
        go(LessonRoute("ORI-1.M1.L1"))
        audit.screen("Les")
        go(SettingsRoute)
        audit.screen("Instellingen")
        go(AboutRoute)
        audit.screen("Over Mise")

        audit.assertNoIssues()
    }

    @Test
    fun `a fresh install shows its text in full`() {
        launch(firstRun = true)
        waitFor("Beginnen")
        audit.screen("Welkom")
        click("Beginnen")
        audit.screen("Sleutel")
        click("Later")
        audit.screen("Back-up")
        click("Overslaan")
        audit.screen("Eerste recept")
        click("Later")
        go(TodayGraph)
        audit.screen("Vandaag leeg")
        go(RecipesGraph)
        audit.screen("Recepten leeg")
        go(ShoppingGraph)
        audit.screen("Boodschappen leeg")
        go(JournalGraph)
        audit.screen("Dagboek leeg")
        audit.assertNoIssues()
    }
}

/** An ordinary phone at the default text size. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w393dp-h852dp-xhdpi")
class LayoutAuditPhoneTest : LayoutAuditTest()

/** A small phone with the system text size raised to 130 %. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h640dp-xhdpi", fontScale = 1.3f)
class LayoutAuditLargeTextTest : LayoutAuditTest()

/** The largest system text size (200 %) on a small phone. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h640dp-xhdpi", fontScale = 2.0f)
class LayoutAuditHugeTextTest : LayoutAuditTest()
