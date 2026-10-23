package nl.potentplum.mise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import nl.potentplum.mise.core.designsystem.MiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MiseAppShellTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    private fun launch(unchecked: Int = 0) {
        hilt.inject()
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
