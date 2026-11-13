package nl.potentplum.mise.feature.curriculum

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.navigation.CheckRoute
import nl.potentplum.mise.data.db.dao.CurriculumDao
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Opleiding through the real shell, curriculum assets and Hilt graph (spec §6.17). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class CurriculumScreensTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var dao: CurriculumDao

    private lateinit var nav: NavHostController

    @Before
    fun setUp() {
        hilt.inject()
        compose.setContent {
            nav = rememberNavController()
            MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0, navController = remember { nav }) }
        }
    }

    private fun waitForText(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    /** Eyebrows show uppercase text and expose the sentence-case label as their content description. */
    private fun waitForLabel(label: String) =
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun `opleiding opens from the menu, and the next step is the first lesson`() {
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Opleiding").performClick()
        waitForLabel("Volgende stap")
        compose.onNodeWithText("Learning to cook, not recipes").assertExists()

        compose.onNodeWithText("Openen").performScrollTo().performClick()
        waitForText("Markeer als gedaan")
        compose.onNodeWithText("Lesinhoud in het Engels").assertExists()
        compose.onNodeWithText("Markeer als gedaan").performScrollTo().performClick()
        waitForText("Markeer als niet gedaan")
        assertEquals(listOf("ORI-1.M1.L1"), runBlocking { dao.lessons() }.map { it.lessonId })
    }

    @Test
    fun `a knowledge check marks multiple choice and shows a model answer for the other types`() {
        compose.runOnUiThread { nav.navigate(CheckRoute("ORI-1.M1")) }
        waitForLabel("Vraag 1 van 8")
        // Item 1's answer is B.
        compose.onAllNodesWithText("B)", substring = true).onFirst().performClick()
        waitForText("Goed")
        compose.onNodeWithText("Volgende vraag").performScrollTo().performClick()
        waitForLabel("Vraag 2 van 8")
        // Item 2's answer is C; A is wrong.
        compose.onAllNodesWithText("A)", substring = true).onFirst().performClick()
        waitForText("Fout. Het juiste antwoord is C.")
        compose.onNodeWithText("Volgende vraag").performScrollTo().performClick()
        // Item 3 is a scenario: model answer and self-marking.
        waitForText("Toon modelantwoord")
        compose.onNodeWithText("Toon modelantwoord").performClick()
        waitForLabel("Modelantwoord")
        // Nothing is saved until the learner finishes.
        assertEquals(0, runBlocking { dao.checks() }.size)
    }
}
