package nl.potentplum.mise

import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.navigation.RecipeRoute
import nl.potentplum.mise.core.navigation.VegetableRoute
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

/**
 * Layout bugs found by using the app on an ordinary phone (393 × 852 dp). Native graphics, so text is measured with the
 * bundled fonts and a label that does not fit really wraps or clips.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w393dp-h852dp-xhdpi")
class PhoneLayoutTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var recipes: RecipeRepository

    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun launch() {
        compose.setContent {
            nav = rememberNavController()
            MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0, navController = remember { nav }) }
        }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun textLayout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onAllNodesWithText(text, useUnmergedTree = true)
            .onFirst()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.first()
    }

    /** One line, and the whole line inside the space the text was given on screen (not wrapped, not clipped). */
    private fun assertOneFullLine(text: String) {
        val layout = textLayout(text)
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst().fetchSemanticsNode()
        assertEquals("$text: lines", 1, layout.lineCount)
        // maxLines = 1 hides a wrapped rest ("Boodschap|pen"): the one line must hold every character.
        assertEquals("$text: characters on the line", text.length, layout.getLineEnd(0, visibleEnd = true))
        assertTrue(
            "$text is clipped: needs ${layout.getLineRight(0)} px, has ${node.size.width} px",
            layout.getLineRight(0) <= node.size.width + 1f,
        )
    }

    private fun textCentreY(text: String): Dp {
        val bounds = compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst().getBoundsInRoot()
        return (bounds.top + bounds.bottom) / 2
    }

    @Test
    fun `every bottom bar label fits on one line`() {
        launch()
        waitFor("Vandaag")
        listOf("Vandaag", "Seizoen", "Recepten", "Lijstje", "Dagboek").forEach(::assertOneFullLine)
    }

    @Test
    fun `the import buttons of an empty library keep their labels on one line`() {
        launch()
        waitFor("Begin met je eerste recept")
        compose.reveal("Foto's maken")
        assertOneFullLine("Link plakken")
        assertOneFullLine("Foto's maken")
    }

    @Test
    fun `the three journal stat tiles are equally tall`() {
        launch()
        waitFor("Dagboek")
        compose.onAllNodesWithText("Dagboek").onFirst().performClick()
        waitFor("Inzichten")
        compose.onAllNodesWithText("Inzichten").onFirst().performClick()
        waitFor("quests voltooid")
        val heights =
            listOf("deze maand gekookt", "keer gekookt in totaal", "quests voltooid").map { label ->
                compose
                    .onAllNodes(hasText(label))
                    .onFirst()
                    .getBoundsInRoot()
                    .let { it.bottom - it.top }
            }
        assertEquals(1, heights.distinct().size)
    }

    @Test
    fun `substitution chips on one line share a centre line`() {
        launch()
        compose.runOnUiThread { nav.navigate(VegetableRoute("pumpkin")) }
        waitFor("Als je het niet hebt")
        compose.onAllNodesWithText("Butternut squash").onFirst().performScrollTo()
        // Butternut squash is plain text; Zoete aardappel opens that vegetable, so it has a 48 dp touch target.
        assertTrue(abs((textCentreY("Butternut squash") - textCentreY("Zoete aardappel")).value) < 1f)
    }

    @Test
    fun `the snackbar after adding to the shopping list does not cover Kookmodus`() {
        runBlocking {
            recipes.save(
                Recipe(
                    id = "kale",
                    title = "Boerenkoolstamppot",
                    sourceType = RecipeSourceType.MANUAL,
                    servings = 2,
                    ingredientGroups =
                        listOf(
                            IngredientGroup(
                                items =
                                    listOf(
                                        Ingredient(
                                            text = "1 kg boerenkool",
                                            quantity = 1.0,
                                            unit = "kg",
                                            name = "boerenkool",
                                            parseSource = ParseSource.MANUAL,
                                        ),
                                        Ingredient(
                                            text = "1 rookworst",
                                            quantity = 1.0,
                                            name = "rookworst",
                                            parseSource = ParseSource.MANUAL,
                                        ),
                                    ),
                            ),
                        ),
                ),
            )
        }
        launch()
        compose.runOnUiThread { nav.navigate(RecipeRoute("kale")) }
        waitFor("Op boodschappenlijst")
        compose.onAllNodesWithText("Op boodschappenlijst").onFirst().performClick()
        waitFor("ingrediënten toevoegen")
        compose.onAllNodesWithText("ingrediënten toevoegen", substring = true).onFirst().performClick()
        waitFor("op je lijst gezet")
        compose.waitForIdle()

        val snackbar = compose.onAllNodesWithText("op je lijst gezet", substring = true).onFirst().getBoundsInRoot()
        val cook = compose.onAllNodes(hasText("Kookmodus")).onFirst().getBoundsInRoot()
        assertTrue("snackbar bottom ${snackbar.bottom} overlaps Kookmodus top ${cook.top}", snackbar.bottom <= cook.top)
    }
}
