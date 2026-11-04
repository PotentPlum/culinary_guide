package nl.potentplum.mise.feature.onboarding

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/** Spec §6.16: welcome → optional key → optional restore → Je eerste recept → done. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class OnboardingFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        hilt.inject()
    }

    private fun launch() =
        compose.setContent {
            MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0, showOnboarding = true) }
        }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun `skipping every step lands on Vandaag and marks first run done`() {
        launch()
        compose.reveal("Welkom bij")
        click("Beginnen")
        compose.reveal("Claude-sleutel (optioneel)")
        click("Later")
        compose.reveal("Back-up terugzetten (optioneel)")
        click("Overslaan")
        compose.reveal("Je eerste recept")
        click("Later")
        compose.reveal("Begin met je eerste recept")
        assertTrue(runBlocking { settings.current() }.onboardingDone)
    }

    @Test
    fun `Link plakken ends first run in the link import`() {
        launch()
        click("Beginnen")
        click("Later")
        click("Overslaan")
        click("Link plakken")
        compose.reveal("Receptlink")
        assertTrue(runBlocking { settings.current() }.onboardingDone)
    }

    @Test
    fun `Sleutel toevoegen opens the key screen`() {
        launch()
        click("Beginnen")
        click("Sleutel toevoegen")
        compose.reveal("API-sleutel")
    }
}
