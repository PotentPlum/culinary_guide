package nl.potentplum.mise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.potentplum.mise.core.designsystem.MiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MiseAppShellTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launch(unchecked: Int = 0) {
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = unchecked) } }
    }

    /** The bottom-bar item: the last match, after the top bar title. */
    private fun tab(label: String) = compose.onAllNodesWithText(label, useUnmergedTree = true).onLast()

    @Test
    fun `starts on vandaag with the dated hero`() {
        launch()
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
        // The eyebrow shows uppercase; TalkBack gets the sentence-case resource.
        compose.onNodeWithContentDescription("Nederland · Oktober 2026").assertExists()
    }

    @Test
    fun `the bottom bar switches between the five tabs`() {
        launch()
        listOf("Seizoen", "Recepten", "Boodschappen", "Dagboek", "Vandaag").forEach { label ->
            tab(label).performClick()
            // The tab label and the screen's top bar title.
            compose.onAllNodesWithText(label, useUnmergedTree = true).assertCountEquals(2)
        }
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
    }

    @Test
    fun `the overflow menu opens instellingen and back returns`() {
        launch()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Instellingen").performClick()
        compose.onNodeWithContentDescription("Terug").assertExists()
        compose.onNodeWithContentDescription("Terug").performClick()
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
    }

    @Test
    fun `each tab keeps its own back stack`() {
        launch()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Over Mise").performClick()
        compose.onNodeWithContentDescription("Terug").assertExists()

        tab("Seizoen").performClick()
        compose.onNodeWithContentDescription("Terug").assertDoesNotExist()

        tab("Vandaag").performClick()
        // Over Mise was opened from Vandaag, so it is restored there.
        compose.onNodeWithContentDescription("Terug").assertExists()
    }

    @Test
    fun `boodschappen shows the unticked count`() {
        launch(unchecked = 3)
        compose.onNodeWithContentDescription("3 items op je lijst", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `no badge when everything is ticked`() {
        launch(unchecked = 0)
        compose.onNodeWithContentDescription("items op je lijst", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }
}
