package nl.potentplum.mise.feature.today

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeClaudeClient
import nl.potentplum.mise.testing.FakeNetworkStatus
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.2: suggestions, Niet vandaag, the ≤ 30 min chip, Vanavond, the empty library and "Alle quests". */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TodayFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var recipes: RecipeRepository

    @Inject
    lateinit var shopping: ShoppingRepository

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var secrets: SecretStore

    @Before
    fun setUp() {
        hilt.inject()
        FakeClaudeClient.reset()
        FakeNetworkStatus.state.value = true
    }

    private fun recipe(
        id: String,
        title: String,
        vegetable: String,
        minutes: Int,
        favorite: Boolean = false,
    ) = Recipe(
        id = id,
        title = title,
        sourceType = RecipeSourceType.MANUAL,
        servings = 2,
        totalMinutes = minutes,
        isFavorite = favorite,
        ingredientGroups =
            listOf(
                IngredientGroup(
                    items =
                        listOf(
                            Ingredient(
                                text = "500 g $vegetable",
                                quantity = 500.0,
                                unit = "g",
                                name = vegetable,
                                parseSource = ParseSource.MANUAL,
                            ),
                            Ingredient(text = "1 ui", quantity = 1.0, name = "ui", parseSource = ParseSource.MANUAL),
                        ),
                ),
            ),
    )

    /** October: all four peak; never cooked. Ranked: the favourite, then by title. */
    private fun seed() =
        runBlocking {
            recipes.save(recipe("kale", "Boerenkoolstamppot", "boerenkool", 40, favorite = true))
            recipes.save(recipe("leek", "Pasta met prei", "prei", 25))
            recipes.save(recipe("pumpkin", "Pompoensoep", "pompoen", 45))
            recipes.save(recipe("beet", "Snelle bietensalade", "rode biet", 20))
        }

    private fun launch() = compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }

    private fun shown(text: String) =
        compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(text: String) = compose.reveal(text)

    private fun waitGone(text: String) = compose.waitUntil(5_000) { !shown(text) }

    /** Vandaag is a lazy list: bring [matcher] into view first. */
    private fun scrollTo(matcher: SemanticsMatcher) = compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(matcher)

    /** Not anywhere in the list, not merely below the fold. */
    private fun absent(text: String) = !shown(text) && runCatching { scrollTo(hasText(text, substring = true)) }.isFailure

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        // The click action, not a touch: on Robolectric's small screen a half-scrolled node's centre can sit under the top
        // bar or the snackbar.
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    /** The button [label] on the card titled [title]. */
    private fun clickOnCard(
        title: String,
        label: String,
    ) {
        val button = hasText(label) and hasClickAction() and hasAnyAncestor(hasText(title, substring = true))
        compose.reveal(button)
        compose
            .onAllNodes(button)
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun `three ranked suggestions with reasons, and Niet vandaag refills the slot`() {
        seed()
        launch()
        waitFor("Boerenkoolstamppot")
        waitFor("Boerenkool is op z'n best · Nog nooit gekookt · Favoriet")
        waitFor("Pasta met prei")
        waitFor("Pompoensoep")
        assertTrue(absent("Snelle bietensalade"))

        clickOnCard("Pompoensoep", "Niet vandaag")
        waitFor("Snelle bietensalade")
        waitGone("Pompoensoep")
        assertEquals(setOf("pumpkin"), runBlocking { settings.todayDismissed.first() })
    }

    @Test
    fun `the 30 minute chip keeps the quick recipes`() {
        seed()
        launch()
        waitFor("Boerenkoolstamppot")
        click("≤ 30 min")
        waitGone("Boerenkoolstamppot")
        waitFor("Pasta met prei")
        waitFor("Snelle bietensalade")
        assertTrue(absent("Pompoensoep"))
    }

    @Test
    fun `Dit kook ik vanavond pins the recipe, and Toch niet unpins it`() {
        seed()
        launch()
        clickOnCard("Pasta met prei", "Dit kook ik vanavond")
        waitFor("Toch niet")
        // The other suggestions fold away under "Andere ideeën".
        compose.waitUntil(5_000) { absent("Boerenkoolstamppot") }
        click("Andere ideeën")
        waitFor("Boerenkoolstamppot")
        assertEquals("leek", runBlocking { settings.tonight.first() }?.recipeId)

        click("Op boodschappenlijst")
        // The snackbar follows the insert.
        waitFor("2 ingrediënten op je lijst gezet")
        assertEquals(listOf("500 g prei", "1 ui"), runBlocking { shopping.observe().first() }.map { it.text })

        click("Toch niet")
        compose.waitUntil(5_000) { absent("Toch niet") }
        assertNull(runBlocking { settings.tonight.first() })
    }

    @Test
    fun `an empty library leads with import and curated links, then the peak shortlist`() {
        launch()
        waitFor("Begin met je eerste recept")
        waitFor("Link plakken")
        waitFor("Seizoensrecepten om te proberen")
        waitFor("Nu op z'n best")
    }

    @Test
    fun `Alle quests opens Dagboek on Quests`() {
        runBlocking { secrets.saveClaudeKey("sk-ant-test") }
        launch()
        click("Alle quests")
        waitFor("Nog geen quests")
    }
}
