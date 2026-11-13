package nl.potentplum.mise.feature.recipes

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.repo.AttemptRepository
import nl.potentplum.mise.data.repo.Evaluation
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingLine
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.testing.FakePageFetcher
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §16 UI tests: import from URL (fake fetcher; a clean import saves in one tap), edit in place, detail. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class RecipeFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var recipes: RecipeRepository

    @Inject
    lateinit var quests: QuestRepository

    @Inject
    lateinit var attempts: AttemptRepository

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var shopping: ShoppingRepository

    private val url = "https://www.leukerecepten.nl/pompoensoep"

    @Before
    fun setUp() {
        hilt.inject()
        FakePageFetcher.pages.clear()
        FakePageFetcher.pages[url] =
            FakePageFetcher.recipePage(
                "Pompoensoep",
                listOf("500 g pompoen", "1 ui", "iets geheims 2x"),
                listOf("Snijd de pompoen.", "Kook 20 minuten."),
            )
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
        compose.onAllNodesWithText("Recepten", useUnmergedTree = true).onFirst().performClick()
    }

    /** Clicks the element showing [text]; the unmerged tree also finds labels inside buttons. */
    private fun click(text: String) {
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun importLink(link: String) {
        // An empty library offers only its own first-recipe button, not the add button too.
        if (library().isEmpty()) {
            compose.onAllNodesWithText("Recept toevoegen", useUnmergedTree = true).assertCountEquals(0)
            click("Eerste recept toevoegen")
        } else {
            click("Recept toevoegen")
        }
        click("Link plakken")
        compose.onNodeWithText("Receptlink").performTextInput(link)
        click("Laden")
    }

    private fun library(): List<LibraryItem> = runBlocking { recipes.observeLibrary().first() }

    @Test
    fun `a clean import previews in read view and saves in one tap`() {
        importLink(url)
        waitForText("Bewaren")
        compose.onNodeWithText("500 g pompoen").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("iets geheims 2x"))
        compose.onNodeWithText("Niet herkend", substring = true, useUnmergedTree = true).assertExists()
        click("Bewaren")
        waitForText("Kookmodus")
        assertEquals(listOf("Pompoensoep"), library().map { it.title })
    }

    @Test
    fun `links inside imported ingredients show as plain text in the preview`() {
        FakePageFetcher.pages[url] =
            FakePageFetcher.recipePage(
                "Groene curry",
                listOf("2 el &lt;a href=&quot;https://example.com/paste&quot;&gt;groene currypasta&lt;/a&gt;", "1 ui"),
                listOf("Bak de <a href='https://example.com'>currypasta</a> aan."),
            )
        importLink(url)
        waitForText("Bewaren")
        compose.onNodeWithText("2 el groene currypasta").assertExists()
        compose.onAllNodesWithText("href", substring = true, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("<", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `the same link again is detected as a duplicate`() {
        importLink(url)
        waitForText("Bewaren")
        click("Bewaren")
        waitForText("Kookmodus")
        compose.onNodeWithContentDescription("Terug").performClick()
        importLink(url)
        waitForText("Dit recept heb je al")
        click("Openen")
        waitForText("Kookmodus")
        assertEquals(1, library().size)
    }

    @Test
    fun `an import without steps opens edit mode with the gap marked`() {
        FakePageFetcher.pages[url] = FakePageFetcher.recipePage("Pompoensoep", listOf("500 g pompoen"), emptyList())
        importLink(url)
        waitForText("Vul de ontbrekende delen aan.")
        click("Opslaan")
        waitForText("Kookmodus")
        assertEquals(listOf("Pompoensoep"), library().map { it.title })
    }

    @Test
    fun `an unreachable link explains itself and offers a retry`() {
        importLink("https://nergens.nl/recept")
        waitForText("Kon het internet niet bereiken")
        compose.onNodeWithText("Opnieuw proberen").assertExists()
    }

    @Test
    fun `manual entry, edit in place and the unsaved-changes guard`() {
        click("Eerste recept toevoegen")
        click("Zelf invoeren")
        click("Opslaan")
        compose.onNodeWithText("Geef het recept een naam.").assertExists()

        compose.onNodeWithText("Naam").performTextInput("Pesto")
        compose.onNodeWithText("Ingrediënten (één per regel)").performTextInput("1 bos basilicum\n50 g pijnboompitten")
        click("Opslaan")
        waitForText("Kookmodus")
        compose.reveal("50 g pijnboompitten")

        // Edit: change the title, then leave without saving → asked first; keep editing, then save.
        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Bewerken")
        compose.onNodeWithText("Pesto").performTextReplacement("Pesto genovese")
        compose.onNodeWithContentDescription("Annuleren").performClick()
        compose.onNodeWithText("Wijzigingen weggooien?").assertExists()
        click("Verder bewerken")
        click("Opslaan")
        waitForText("Kookmodus")
        compose.waitUntil(5_000) { library().map { it.title } == listOf("Pesto genovese") }
    }

    @Test
    fun `servings scale the ingredients and ticks stay ticked`() {
        importLink(url)
        waitForText("Bewaren")
        click("Bewaren")
        waitForText("Kookmodus")
        compose.onNodeWithContentDescription("Meer porties").performClick()
        compose.onNodeWithContentDescription("Meer porties").performClick()
        compose.reveal("750 g pompoen")
        click("750 g pompoen")
        compose.waitUntil(5_000) {
            compose.onNodeWithText("750 g pompoen").fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On
        }
    }

    @Test
    fun `delete offers undo`() {
        importLink(url)
        waitForText("Bewaren")
        click("Bewaren")
        waitForText("Kookmodus")
        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Verwijderen")
        waitForText("Recept verwijderd")
        assertEquals(0, library().size)
        click("Ongedaan maken")
        compose.waitUntil(5_000) { library().size == 1 }
    }

    /** Opens a library recipe from the Recepten tab and deletes it from its ⋮ menu. */
    private fun deleteFromLibrary(title: String) {
        waitForText(title)
        click(title)
        waitForText("Kookmodus")
        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Verwijderen")
        waitForText("Recept verwijderd")
    }

    @Test
    fun `a cooked, pinned recipe on the shopping list can be deleted and restored`() {
        runBlocking {
            recipes.save(Recipe(id = "soup", title = "Pompoensoep", sourceType = RecipeSourceType.MANUAL))
            attempts.log("soup", Evaluation(cookedOn = LocalDate.of(2026, 10, 20)))
            settings.pinTonight("soup")
            shopping.add(listOf(ShoppingLine("1 pompoen", 1.0, null, "pompoen")), "soup")
        }
        deleteFromLibrary("Pompoensoep")
        assertEquals(0, library().size)
        compose.onAllNodesWithText("Vandaag", useUnmergedTree = true).onLast().performClick()
        waitForText("Wat kook ik vandaag?")
        // The journal keeps the cook, under "Verwijderd recept" (spec §7.2).
        compose.onAllNodesWithText("Dagboek", useUnmergedTree = true).onLast().performClick()
        waitForText("Verwijderd recept")
        compose.onAllNodesWithText("Lijstje", useUnmergedTree = true).onLast().performClick()
        waitForText("1 pompoen")
    }

    @Test
    fun `a quest's recipe saved to the library can be deleted from Recepten, and the quest keeps it`() {
        runBlocking {
            quests.insert(
                QuestEntity(
                    id = "q1",
                    createdAt = 0,
                    isDaily = true,
                    ingredientMode = IngredientMode.SEASONAL,
                    ingredient = "pompoen",
                    questType = QuestType.BIJGERECHT,
                    text = "Rooster pompoen.",
                    skill = "Roosteren",
                    difficulty = Difficulty.BEGINNER,
                    timeEstimateMinutes = 40,
                    why = "Zoet.",
                    dishType = "Bijgerecht",
                    recipeId = "qr",
                ),
                Recipe(id = "qr", title = "Geroosterde pompoen", sourceType = RecipeSourceType.QUEST, inLibrary = true),
            )
        }
        deleteFromLibrary("Geroosterde pompoen")
        assertEquals(0, library().size)
        assertEquals("qr", runBlocking { quests.get("q1") }?.recipeId)
        assertEquals("Geroosterde pompoen", runBlocking { recipes.get("qr") }?.title)
        click("Ongedaan maken")
        compose.waitUntil(5_000) { library().size == 1 }
    }
}
