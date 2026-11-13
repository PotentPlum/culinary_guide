package nl.potentplum.mise.feature.shopping

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingLine
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.14: adding a selection from a recipe, combined lines with source chips, ticking, manual items, clearing with undo. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class ShoppingFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var recipes: RecipeRepository

    @Inject lateinit var shopping: ShoppingRepository

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun seed() =
        runBlocking {
            recipes.save(Recipe(id = "soup", title = "Uiensoep", sourceType = RecipeSourceType.MANUAL))
            recipes.save(Recipe(id = "stew", title = "Stoofpot", sourceType = RecipeSourceType.MANUAL))
            shopping.add(listOf(ShoppingLine("2 uien", 2.0, null, "ui"), ShoppingLine("500 g wortel", 500.0, "g", "wortel")), "soup")
            shopping.add(listOf(ShoppingLine("1 ui", 1.0, null, "ui"), ShoppingLine("0,5 kg wortel", 0.5, "kg", "wortel")), "stew")
        }

    private fun launch() {
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
        click("Boodschappen")
    }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `lines from two recipes are combined, with both recipes as chips`() {
        seed()
        launch()
        compose.reveal("3 ui")
        compose.reveal("1 kg wortel")
        assertTrue(shown("Uiensoep"))
        assertTrue(shown("Stoofpot"))
        assertTrue(!shown("2 uien"))
    }

    @Test
    fun `with one recipe on the list it is named once, not as a chip on every line`() {
        runBlocking {
            recipes.save(Recipe(id = "soup", title = "Uiensoep", sourceType = RecipeSourceType.MANUAL))
            shopping.add(listOf(ShoppingLine("2 uien", 2.0, null, "ui"), ShoppingLine("500 g wortel", 500.0, "g", "wortel")), "soup")
        }
        launch()
        compose.reveal("Voor Uiensoep")
        compose.reveal("500 g wortel")
        assertTrue(!shown("Uiensoep"))
    }

    @Test
    fun `ticking a combined line ticks all its items`() {
        seed()
        launch()
        compose.reveal("3 ui")
        compose.onAllNodes(isToggleable() and hasText("3 ui", substring = true)).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.reveal(isToggleable() and hasText("3 ui", substring = true) and isOn())
        val items = runBlocking { shopping.observe().first() }
        assertEquals(listOf(true, true), items.filter { it.name == "ui" }.map { it.checked })
        assertEquals(listOf(false, false), items.filter { it.name == "wortel" }.map { it.checked })
    }

    @Test
    fun `manual items go under Los, and clearing the list can be undone`() {
        seed()
        launch()
        compose.reveal("3 ui")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Koffiefilters")
        compose.onAllNodes(hasSetTextAction()).onFirst().performImeAction()
        compose.reveal("Koffiefilters")
        // Eyebrows carry their label as a content description.
        compose.reveal(hasContentDescription("Los"))

        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Hele lijst wissen")
        compose.reveal("Je lijst is leeg")
        click("Ongedaan maken")
        compose.reveal("3 ui")
        compose.reveal("Koffiefilters")
        assertEquals(5, runBlocking { shopping.observe().first() }.size)
    }

    @Test
    fun `from recipe detail, only the ticked lines are added, scaled to the servings shown`() {
        runBlocking {
            recipes.save(
                Recipe(
                    id = "pasta",
                    title = "Pasta met prei",
                    sourceType = RecipeSourceType.MANUAL,
                    servings = 2,
                    ingredientGroups =
                        listOf(
                            IngredientGroup(
                                items =
                                    listOf(
                                        Ingredient(
                                            text = "250 g prei",
                                            quantity = 250.0,
                                            unit = "g",
                                            name = "prei",
                                            parseSource = ParseSource.MANUAL,
                                        ),
                                        Ingredient(text = "1 ui", quantity = 1.0, name = "ui", parseSource = ParseSource.MANUAL),
                                    ),
                            ),
                            IngredientGroup(name = "Voor erbij", items = listOf(Ingredient(text = "zout"))),
                        ),
                ),
            )
        }
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
        click("Recepten")
        click("Pasta met prei")
        // The action bar shows before the recipe has loaded; the stepper only with it.
        compose.reveal(hasContentDescription("Meer porties"))
        compose.onNodeWithContentDescription("Meer porties").performClick()
        compose.onNodeWithContentDescription("Meer porties").performClick()
        compose.reveal("500 g prei")
        click("Op boodschappenlijst")

        // In the sheet (its own window, next to the recipe's identical lines): untick the salt.
        val inSheet = hasAnyAncestor(hasAnyDescendant(hasText("Alles uitvinken")))
        compose.reveal(hasText("Hoeveelheden voor 4 porties"))
        compose.onAllNodes(isToggleable() and hasText("zout") and inSheet).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        click("2 ingrediënten toevoegen")
        compose.reveal("2 ingrediënten op je lijst gezet")
        val items = runBlocking { shopping.observe().first() }
        assertEquals(listOf("500 g prei", "2 ui"), items.map { it.text })
        assertEquals(listOf(500.0, 2.0), items.map { it.quantity })
        assertTrue(items.all { it.recipeId == "pasta" })
    }

    @Test
    fun `an empty list explains where items come from`() {
        launch()
        compose.reveal("Je lijst is leeg")
        compose.reveal("Voeg ingrediënten toe vanuit een recept")
    }
}
