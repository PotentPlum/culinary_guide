package nl.potentplum.mise.feature.cook

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.SharedContent
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.db.dao.AttemptDao
import nl.potentplum.mise.data.db.entity.AttemptSourceType
import nl.potentplum.mise.data.db.entity.IngredientMode
import nl.potentplum.mise.data.db.entity.ParseSource
import nl.potentplum.mise.data.db.entity.QuestEntity
import nl.potentplum.mise.data.db.entity.QuestStatus
import nl.potentplum.mise.data.db.entity.QuestType
import nl.potentplum.mise.data.db.entity.RecipeSourceType
import nl.potentplum.mise.data.db.entity.TagKey
import nl.potentplum.mise.data.repo.Ingredient
import nl.potentplum.mise.data.repo.IngredientGroup
import nl.potentplum.mise.data.repo.QuestRepository
import nl.potentplum.mise.data.repo.Recipe
import nl.potentplum.mise.data.repo.RecipeRepository
import nl.potentplum.mise.data.repo.ShoppingRepository
import nl.potentplum.mise.data.repo.Step
import nl.potentplum.mise.data.repo.StepGroup
import nl.potentplum.mise.data.repo.Tag
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.domain.Difficulty
import nl.potentplum.mise.testing.FakeClaudeClient
import nl.potentplum.mise.testing.FakeClockTimers
import nl.potentplum.mise.testing.REVEAL_TIMEOUT_MILLIS
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/** Spec §6.10 and §6.12: resuming a cook session, timers, technique help, the exit prompt and the evaluation sheet. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class CookFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var recipes: RecipeRepository

    @Inject
    lateinit var settings: SettingsRepository

    @Inject
    lateinit var attempts: AttemptDao

    @Inject
    lateinit var quests: QuestRepository

    @Inject
    lateinit var shopping: ShoppingRepository

    private val recipe =
        Recipe(
            id = "r1",
            title = "Pompoen met boerenkool",
            sourceType = RecipeSourceType.MANUAL,
            servings = 2,
            ingredientGroups =
                listOf(
                    IngredientGroup(
                        items =
                            listOf(
                                Ingredient(
                                    text = "1 kg pompoen",
                                    quantity = 1.0,
                                    unit = "kg",
                                    name = "pompoen",
                                    parseSource = ParseSource.MANUAL,
                                ),
                                Ingredient(
                                    text = "200 g boerenkool",
                                    quantity = 200.0,
                                    unit = "g",
                                    name = "boerenkool",
                                    parseSource = ParseSource.MANUAL,
                                ),
                            ),
                    ),
                ),
            stepGroups =
                listOf(
                    StepGroup(
                        steps =
                            listOf(
                                Step(text = "Snijd de pompoen in blokjes."),
                                Step(text = "Blancheer de boerenkool 3 minuten."),
                                Step(text = "Rooster de pompoen 25 minuten op 200 °C."),
                                Step(text = "Meng alles en breng op smaak."),
                            ),
                    ),
                ),
            tags = setOf(Tag(TagKey.TECHNIQUE, "blancheren"), Tag(TagKey.CUISINE, "nederlands")),
        )

    @Before
    fun setUp() {
        hilt.inject()
        FakeClaudeClient.reset()
        FakeClockTimers.reset()
        runBlocking { recipes.save(recipe) }
    }

    private fun launch() =
        compose.setContent {
            MiseTheme {
                MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0, pendingShare = SharedContent.OpenRecipe("r1"))
            }
        }

    private fun shown(text: String) =
        compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(text: String) = compose.reveal(text)

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun clickDescribed(description: String) {
        val matcher = hasContentDescription(description) and hasClickAction()
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun session() = runBlocking { settings.cookSession.first() }

    private fun startCooking() {
        launch()
        waitFor("Pompoen met boerenkool")
        click("Stap voor stap")
        waitFor("Stap 1 van 4")
    }

    /** Whether any view in the window asks to keep the screen on (that sets `FLAG_KEEP_SCREEN_ON`). */
    private fun screenOn(): Boolean {
        fun on(view: View): Boolean = view.keepScreenOn || (view is ViewGroup && (0 until view.childCount).any { on(view.getChildAt(it)) })
        return on(compose.activity.window.decorView)
    }

    @Test
    fun `Kookmodus keeps the screen on from the recipe page, also through Stap voor stap`() {
        launch()
        waitFor("Pompoen met boerenkool")
        assertFalse(screenOn())
        compose.onNode(hasText("Kookmodus") and isToggleable()).performClick()
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { screenOn() }

        // Stap voor stap keeps it on too; coming back, the recipe page still has Kookmodus on.
        click("Stap voor stap")
        waitFor("Stap 1 van 4")
        assertTrue(screenOn())
        clickDescribed("Stap voor stap sluiten")
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { compose.onAllNodes(hasText("Kookmodus") and isOn()).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        assertTrue(screenOn())

        compose.onNode(hasText("Kookmodus") and isToggleable()).performClick()
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { !screenOn() }
    }

    @Test
    fun `Stap voor stap alone keeps the screen on only while it is open`() {
        startCooking()
        assertTrue(screenOn())
        clickDescribed("Stap voor stap sluiten")
        waitFor("Kookmodus")
        compose.waitForIdle()
        assertFalse(screenOn())
    }

    @Test
    fun `leaving early keeps the step, and the recipe offers to resume there`() {
        startCooking()
        click("Volgende stap")
        waitFor("Stap 2 van 4")
        click("Volgende stap")
        waitFor("Stap 3 van 4")
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { session()?.stepIndex == 2 }

        clickDescribed("Stap voor stap sluiten")
        // Not at the last step yet: no question, straight back.
        waitFor("Verder bij stap 3")
        assertTrue(!shown("Heb je dit gekookt?"))

        click("Verder bij stap 3")
        waitFor("Stap 3 van 4")
        waitFor("Rooster de pompoen 25 minuten")
    }

    @Test
    fun `each step shows the next one, and the labelled Ingredienten button opens the list`() {
        startCooking()
        // Step 1 previews step 2 under "Hierna" (the eyebrow's content description is its sentence-case label).
        compose.onAllNodesWithContentDescription("Hierna").assertCountEquals(1)
        assertTrue(shown("Blancheer de boerenkool 3 minuten."))

        repeat(3) { click("Volgende stap") }
        waitFor("Stap 4 van 4")
        // Nothing after the last step.
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { compose.onAllNodesWithContentDescription("Hierna").fetchSemanticsNodes().isEmpty() }

        click("Ingrediënten")
        waitFor("1 kg pompoen")
    }

    @Test
    fun `a timer chip starts a Clock timer and shows in the strip, and a technique opens as a sheet`() {
        startCooking()
        click("Volgende stap")
        waitFor("Blancheer de boerenkool")
        clickDescribed("Timer van 3 min starten")
        waitFor("Timer van 3 min gestart")
        assertEquals(listOf(180 to "Mise · Stap 2"), FakeClockTimers.started)
        waitFor("Stap 2 · nog ~3 min")
        assertEquals(1, session()?.timers?.size)

        click("Stap 2 · nog ~3 min")
        assertEquals(1, FakeClockTimers.shown)

        click("Over Blancheren")
        waitFor("Blancheren")
    }

    @Test
    fun `without a Clock app the chips are hidden and a hint explains once`() {
        FakeClockTimers.supported = false
        startCooking()
        waitFor("Je klok-app kan geen timers starten")
        click("Volgende stap")
        waitFor("Blancheer de boerenkool")
        assertTrue(compose.onAllNodes(hasContentDescription("Timer van 3 min starten")).fetchSemanticsNodes().isEmpty())
        click("Klaar")
        compose.waitUntil(5_000) { !shown("Je klok-app kan geen timers starten") }
        assertTrue(runBlocking { settings.settings.first() }.clockTimerHintShown)
    }

    @Test
    fun `after the last step, leaving asks, and the evaluation logs a snapshot and clears the session and pin`() {
        runBlocking { settings.pinTonight("r1") }
        startCooking()
        repeat(3) { click("Volgende stap") }
        waitFor("Stap 4 van 4")
        waitFor("Klaar met koken")

        clickDescribed("Stap voor stap sluiten")
        waitFor("Heb je dit gekookt?")
        click("Vastleggen")
        waitFor("Hoe ging het?")
        click("Goed")
        click("Vastleggen")
        waitFor("Vastgelegd in je dagboek")
        waitFor("Gekookt")

        val attempt = runBlocking { attempts.all() }.single()
        assertEquals(8, attempt.score)
        assertNull(attempt.tasteScore)
        assertEquals("r1", attempt.recipeId)
        assertEquals(AttemptSourceType.RECIPE, attempt.sourceType)
        assertEquals(listOf("blancheren"), attempt.techniques)
        assertEquals(listOf("nederlands"), attempt.cuisines)
        assertEquals(listOf("kale", "pumpkin"), attempt.vegIds)
        assertEquals(listOf("kale", "pumpkin"), attempt.peakVegIds)
        assertNull(session())
        assertNull(runBlocking { settings.tonight.first() })
    }

    @Test
    fun `Gekookt on the recipe logs without scores, and dismissing logs nothing`() {
        launch()
        click("Gekookt")
        waitFor("Hoe ging het?")
        click("Goed")
        click("Zonder cijfers vastleggen")
        waitFor("Vastgelegd in je dagboek")
        val attempt = runBlocking { attempts.all() }.single()
        assertNull(attempt.score)
        assertNull(attempt.tasteScore)
    }

    @Test
    fun `after logging, the recipe's groceries can be taken off the list, with undo`() {
        runBlocking {
            shopping.addRecipe(recipe, recipes.scaler())
            shopping.addManual("Koffie")
        }
        launch()
        click("Gekookt")
        waitFor("Hoe ging het?")
        click("Zonder cijfers vastleggen")
        // Offered, never done silently: both lines are still there until Weghalen.
        waitFor("Vastgelegd. Nog 2 boodschappen van dit recept op je lijst.")
        assertEquals(3, runBlocking { shopping.observe().first() }.size)

        click("Weghalen")
        waitFor("Verwijderd van je lijst")
        assertEquals(listOf("Koffie"), runBlocking { shopping.observe().first() }.map { it.text })

        click("Ongedaan maken")
        compose.waitUntil(REVEAL_TIMEOUT_MILLIS) { runBlocking { shopping.observe().first() }.size == 3 }
    }

    @Test
    fun `without groceries on the list, logging just says so`() {
        launch()
        click("Gekookt")
        waitFor("Hoe ging het?")
        click("Zonder cijfers vastleggen")
        waitFor("Vastgelegd in je dagboek")
        compose.onAllNodesWithText("Weghalen").assertCountEquals(0)
    }

    @Test
    fun `Klaar met koken on a quest's recipe completes the quest`() {
        runBlocking {
            recipes.delete("r1")
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
                    recipeId = "r1",
                ),
                recipe.copy(sourceType = RecipeSourceType.QUEST, inLibrary = false),
            )
        }
        startCooking()
        repeat(3) { click("Volgende stap") }
        click("Klaar met koken")
        waitFor("Hoe ging het?")
        click("Zonder cijfers vastleggen")
        waitFor("Goed gedaan!")
        assertEquals(QuestStatus.COMPLETED, runBlocking { quests.get("q1") }?.status)
        val attempt = runBlocking { attempts.all() }.single()
        assertEquals("q1", attempt.questId)
        assertEquals(AttemptSourceType.QUEST, attempt.sourceType)
    }
}
