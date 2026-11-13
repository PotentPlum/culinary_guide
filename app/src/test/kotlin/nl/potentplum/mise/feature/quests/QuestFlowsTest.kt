package nl.potentplum.mise.feature.quests

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.claude.ContentPart
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeClaudeClient
import nl.potentplum.mise.testing.FakeNetworkStatus
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.11 UI flows with a recorded reply: Quest van vandaag, cancel, Verfijn, history, Bewaar recept. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class QuestFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var secrets: SecretStore

    @Inject
    lateinit var quests: QuestRepository

    @Inject
    lateinit var recipes: RecipeRepository

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var attempts: AttemptDao

    @Before
    fun setUp() {
        hilt.inject()
        FakeClaudeClient.reset()
        FakeNetworkStatus.state.value = true
        FakeClaudeClient.reply =
            FakeClaudeClient.json(
                """
                {"questText":"Rooster pastinaak tot hij karamelliseert.","skill":"Groenten roosteren","difficulty":"BEGINNER",
                 "timeEstimateMinutes":45,"why":"Roosteren maakt zoet.","dishType":"Bijgerecht","chosenIngredient":"pastinaak",
                 "chosenVegId":null,"tags":{"cuisine":[],"mainIngredient":["Groenten"],"techniques":["groenten-roasten"]},
                 "recipe":{"title":"Geroosterde pastinaak","servings":2,
                   "ingredientGroups":[{"name":null,"items":[
                     {"text":"500 g pastinaak","quantity":500,"quantityMax":null,"unit":"g","name":"pastinaak","note":null}]}],
                   "steps":[{"text":"Rooster 30 minuten op 200 °C."}],"tips":["Snijd gelijke stukken."]}}
                """,
            )
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
    }

    private fun saveKey() = runBlocking { secrets.saveClaudeKey("sk-ant-test") }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        // The click action, not a touch: a lazy item can recompose or sit half under the top bar between finding and tapping.
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun waitForText(text: String) = compose.reveal(text)

    /**
     * Dagboek › Quests is a lazy list: scroll it to the quest, retrying until it is there. Right after a filter change the
     * list can still hold the old rows, so a single scroll attempt flakes.
     */
    private fun scrollHistoryTo(text: String) = compose.reveal(hasText(text, substring = true))

    private fun gone(text: String) =
        compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()

    @Test
    fun `without a key the card explains, and Verbergen hides it`() {
        waitForText("Voor een quest is een Claude-sleutel nodig.")
        click("Verbergen")
        compose.waitUntil(5_000) { gone("Voor een quest is een Claude-sleutel nodig.") }
        assertFalse(runBlocking { settings.settings.first().showQuestCard })
    }

    @Test
    fun `one tap generates today's quest, hidden from the library until saved`() {
        saveKey()
        click("Geef quest")
        waitForText("Rooster pastinaak tot hij karamelliseert.")
        waitForText("Start koken")
        val text =
            (
                FakeClaudeClient.requests
                    .single()
                    .content
                    .single() as ContentPart.Text
            ).text
        assertTrue(text, text.contains("(seizoensgroente, id:"))
        assertTrue(text, text.contains("Focus: oefen vooral"))
        assertEquals(emptyList<Any>(), runBlocking { recipes.observeLibrary().first() })

        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Bewaar recept in bibliotheek")
        compose.waitUntil(5_000) { runBlocking { recipes.observeLibrary().first() }.size == 1 }

        compose.onNodeWithContentDescription("Terug").performClick()
        waitForText("Bekijk quest")
        assertTrue(runBlocking { quests.observeDaily().first() }!!.isDaily)
    }

    @Test
    fun `cancelling while generating records nothing`() {
        saveKey()
        FakeClaudeClient.gate = CompletableDeferred()
        click("Geef quest")
        waitForText("Seizoensgroente:")
        waitForText("Basis")
        click("Annuleren")
        waitForText("Geef quest")
        FakeClaudeClient.gate!!.complete(Unit)
        compose.waitForIdle()
        assertEquals(emptyList<Any>(), runBlocking { quests.observeAll().first() })
    }

    @Test
    fun `offline the button is disabled with an explanation`() {
        saveKey()
        FakeNetworkStatus.state.value = false
        waitForText("Je bent offline.")
        compose.onAllNodes(hasText("Geef quest") and hasClickAction()).onFirst().assertIsNotEnabled()
    }

    @Test
    fun `Verfijn sends the chosen parameters and the quest shows up in Dagboek`() {
        saveKey()
        click("Verfijn")
        click("Saus")
        click("Gevorderd")
        click("Geef quest")
        waitForText("Start koken")
        val text =
            (
                FakeClaudeClient.requests
                    .single()
                    .content
                    .single() as ContentPart.Text
            ).text
        assertTrue(text, text.contains("Ingrediënt: verras me."))
        assertTrue(text, text.contains("Soort quest: saus (geen hele maaltijd)"))
        assertTrue(text, text.contains("Moeilijkheid: gevorderd (ADVANCED)"))

        compose.onAllNodesWithText("Dagboek", useUnmergedTree = true).onFirst().performClick()
        click("Quests")
        waitForText("Nieuwe quest")
        compose.waitUntil(5_000) { runBlocking { quests.observeAll().first() }.isNotEmpty() }
        click("Voltooid")
        compose.waitUntil(5_000) { gone("Geroosterde pastinaak") }
        click("Alle")
        scrollHistoryTo("Geroosterde pastinaak")
        // The click action, not a touch: after scrolling, the button can sit at the list edge under the top bar.
        compose.onNodeWithContentDescription("Opties voor Geroosterde pastinaak").performSemanticsAction(SemanticsActions.OnClick)
        click("Verwijderen")
        compose.waitUntil(5_000) { gone("Geroosterde pastinaak") }
        click("Ongedaan maken")
        compose.waitUntil(5_000) { runBlocking { quests.observeAll().first() }.isNotEmpty() }
        scrollHistoryTo("Geroosterde pastinaak")
        assertEquals(1, runBlocking { quests.observeAll().first() }.size)
    }

    private fun type(
        label: String,
        text: String,
    ) {
        val field = hasSetTextAction() and hasText(label)
        compose.reveal(field)
        compose.onNode(field).performTextInput(text)
    }

    private fun requestText() =
        (
            FakeClaudeClient.requests
                .last()
                .content
                .single() as ContentPart.Text
        ).text

    @Test
    fun `Zelf kiezen takes several ingredients, each with its own amount`() {
        saveKey()
        click("Verfijn")
        click("Zelf kiezen")
        type("Ingrediënt", "restje kool")
        click("Nog een ingrediënt")
        // With two rows they are numbered, and each can be removed.
        type("Ingrediënt 2", "zure room")
        // The first row's amount stays with the first ingredient.
        compose.onAllNodes(hasSetTextAction() and hasText("Hoeveelheid"))[0].performTextInput("0,5")
        click("Nog een ingrediënt")
        type("Ingrediënt 3", "dille")
        compose.onNodeWithContentDescription("Ingrediënt 3 verwijderen").performClick()
        compose.onAllNodes(hasSetTextAction() and hasText("Ingrediënt 3")).assertCountEquals(0)
        click("Geef quest")
        waitForText("Start koken")
        assertTrue(
            requestText(),
            requestText().startsWith("Ingrediënten (gebruik ze allemaal): restje kool (hoeveelheid: 0,5); zure room\n"),
        )
        // The quest names both.
        waitForText("restje kool en zure room")
    }

    @Test
    fun `Seizoensgroente takes several vegetables from the picker`() {
        saveKey()
        click("Verfijn")
        click("Seizoensgroente")
        compose.onNodeWithContentDescription("Groenten kiezen").performClick()
        click("Pompoen")
        click("Prei")
        click("Klaar")
        waitForText("Aanpassen")
        click("Geef quest")
        waitForText("Start koken")
        assertTrue(
            requestText(),
            requestText().startsWith("Ingrediënten (gebruik ze allemaal): pompoen (seizoensgroente, id: pumpkin); prei"),
        )
    }

    @Test
    fun `Quest voltooid opens the evaluation and logging completes the quest`() {
        saveKey()
        click("Geef quest")
        waitForText("Start koken")
        click("Quest voltooid")
        waitForText("Hoe ging het?")
        click("Top")
        click("Vastleggen")
        waitForText("Goed gedaan!")
        val quest = runBlocking { quests.observeAll().first() }.single()
        assertEquals(QuestStatus.COMPLETED, quest.status)
        val attempt = runBlocking { attempts.all() }.single()
        assertEquals(quest.id, attempt.questId)
        assertEquals(10, attempt.score)
    }
}
