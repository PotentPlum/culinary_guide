package nl.potentplum.mise.feature.season

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Seizoen → vegetable detail → technique detail through the real shell, content and Hilt graph. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SeasonScreensTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        hilt.inject()
        compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }
        compose.onAllNodesWithText("Seizoen", useUnmergedTree = true).onFirst().performClick()
    }

    @Test
    fun `search finds a vegetable and opens its detail`() {
        compose.onNodeWithContentDescription("Groenten zoeken").performTextInput("boerenkool")
        compose.waitUntil { compose.onAllNodesWithText("Boerenkool").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Boerenkool").onFirst().performClick()
        compose.onNodeWithText("Bewaren").performScrollTo()
        compose.onNodeWithText("Quest met Boerenkool").performScrollTo()
    }

    @Test
    fun `the kalender toggle shows the year grid`() {
        compose.onNodeWithText("Kalender").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Per familie").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Deze maand: oktober").assertExists()
    }

    @Test
    fun `technieken opens from the menu and links to a technique`() {
        compose.onNodeWithContentDescription("Meer opties").performClick()
        compose.onNodeWithText("Technieken").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Snijdtechnieken").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Technieken zoeken").performTextInput("julienne")
        compose.waitUntil { compose.onAllNodesWithText("Julienne").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Julienne").onFirst().performClick()
        compose.onNodeWithText("Oefen in een quest").performScrollTo()
    }
}
