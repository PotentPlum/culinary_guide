package nl.potentplum.mise.feature.recipes

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.domain.LibraryItem
import nl.potentplum.mise.testing.FakePageFetcher
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
        click("Recept toevoegen")
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
        click("Recept toevoegen")
        click("Zelf invoeren")
        click("Opslaan")
        compose.onNodeWithText("Geef het recept een naam.").assertExists()

        compose.onNodeWithText("Naam").performTextInput("Pesto")
        compose.onNodeWithText("Ingrediënten (één per regel)").performTextInput("1 bos basilicum\n50 g pijnboompitten")
        click("Opslaan")
        waitForText("Kookmodus")
        compose.onNodeWithText("50 g pijnboompitten").assertExists()

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
        compose.onNodeWithText("750 g pompoen").assertExists()
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
}
