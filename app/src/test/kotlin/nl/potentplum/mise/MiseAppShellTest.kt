package nl.potentplum.mise

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.testing.reveal
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
        // Each tab's label and the title of the screen it opens; "Lijstje" opens Boodschappen.
        val titles = mapOf("Seizoen" to "Seizoen", "Recepten" to "Recepten", "Lijstje" to "Boodschappen", "Dagboek" to "Dagboek")
        titles.forEach { (label, title) ->
            tab(label).performClick()
            if (label == title) {
                compose.onAllNodesWithText(label, useUnmergedTree = true).assertCountEquals(2)
            } else {
                compose.onAllNodesWithText(label, useUnmergedTree = true).assertCountEquals(1)
                compose.onAllNodesWithText(title, useUnmergedTree = true).assertCountEquals(1)
            }
        }
        tab("Vandaag").performClick()
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
    }

    @Test
    fun `the overflow menu opens instellingen and back returns`() {
        launch()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Instellingen").performClick()
        compose.onNodeWithContentDescription("Terug").assertExists()
        // A detail screen without actions of its own has no ⋮: the app-wide items live on the tab screens (spec §6.1).
        compose.onNodeWithContentDescription("Meer opties").assertDoesNotExist()
        compose.onNodeWithContentDescription("Terug").performClick()
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
    }

    @Test
    fun `each tab keeps its own back stack`() {
        launch()
        // A vegetable opened from Seizoen stacks on that tab.
        tab("Seizoen").performClick()
        compose.reveal("Pompoen")
        compose.onAllNodesWithText("Pompoen").onFirst().performClick()
        compose.onNodeWithContentDescription("Terug").assertExists()

        tab("Recepten").performClick()
        compose.onNodeWithContentDescription("Terug").assertDoesNotExist()

        tab("Seizoen").performClick()
        // Pompoen was opened from Seizoen, so it is restored there.
        compose.onNodeWithContentDescription("Terug").assertExists()
    }

    @Test
    fun `app-wide pages from the menu have no bottom bar`() {
        launch()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Over Mise").performClick()
        compose.onNodeWithContentDescription("Terug").assertExists()
        // They belong to no tab, so no tab can be highlighted; back is the way out (spec §6.1).
        compose.onAllNodesWithText("Lijstje", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Terug").performClick()
        tab("Lijstje").assertExists()
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
