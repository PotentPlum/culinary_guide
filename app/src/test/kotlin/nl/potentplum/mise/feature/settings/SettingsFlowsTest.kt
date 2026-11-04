package nl.potentplum.mise.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.potentplum.mise.HiltTestActivity
import nl.potentplum.mise.MiseAppShell
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.data.settings.Quality
import nl.potentplum.mise.data.settings.SettingsRepository
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeClaudeClient
import nl.potentplum.mise.testing.reveal
import org.junit.Assert.assertEquals
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

/** Spec §6.15 and §9: key entry states, Kwaliteit, and the quest card coming back with a key. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SettingsFlowsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var secrets: SecretStore

    @Inject lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        hilt.inject()
        FakeClaudeClient.reset()
    }

    private fun launch() = compose.setContent { MiseTheme { MiseAppShell(today = LocalDate.of(2026, 10, 22), uncheckedShoppingCount = 0) } }

    private fun click(text: String) {
        val matcher = hasText(text) and hasClickAction()
        compose.reveal(matcher)
        compose.onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun openKeyScreen() {
        launch()
        compose.onAllNodesWithText("Wat kook ik", substring = true).onFirst()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Instellingen")
        click("Sleutel toevoegen")
        compose.reveal("API-sleutel")
    }

    private fun typeKey(key: String) {
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput(key)
    }

    private fun keyStatus() = runBlocking { secrets.keyState.first() }.status

    @Test
    fun `a refused key shows Ongeldige sleutel and is not stored`() {
        FakeClaudeClient.keyCheck = KeyCheck.INVALID
        openKeyScreen()
        typeKey("sk-ant-wrong")
        click("Controleren en opslaan")
        compose.reveal("Ongeldige sleutel")
        assertEquals(ClaudeKeyStatus.NONE, keyStatus())
        assertEquals(listOf("sk-ant-wrong"), FakeClaudeClient.checkedKeys)
    }

    @Test
    fun `a valid key is saved, masked, and brings the quest card back`() {
        runBlocking { settings.setShowQuestCard(false) }
        openKeyScreen()
        typeKey("  sk-ant-api03-good-abcd  ")
        click("Controleren en opslaan")
        compose.reveal("Sleutel opgeslagen")
        assertEquals(ClaudeKeyStatus.STORED, keyStatus())
        assertEquals("sk-ant-api03-good-abcd", runBlocking { secrets.readClaudeKey() })
        assertTrue(runBlocking { settings.current() }.showQuestCard)

        click("Klaar")
        compose.reveal("Sleutel opgeslagen: sk-ant-…abcd")
    }

    @Test
    fun `without an answer from Claude the key can still be saved`() {
        FakeClaudeClient.keyCheck = KeyCheck.UNREACHABLE
        openKeyScreen()
        typeKey("sk-ant-offline-1234")
        click("Controleren en opslaan")
        click("Toch opslaan")
        compose.reveal("Sleutel opgeslagen")
        assertEquals(ClaudeKeyStatus.STORED, keyStatus())
    }

    @Test
    fun `a key without the sk-ant prefix gets a soft warning only`() {
        openKeyScreen()
        typeKey("abc")
        compose.reveal("begint meestal met sk-ant-")
    }

    @Test
    fun `Kwaliteit and deleting the key`() {
        runBlocking { secrets.saveClaudeKey("sk-ant-delete-wxyz") }
        launch()
        compose.onNodeWithContentDescription("Meer opties").performClick()
        click("Instellingen")
        compose.reveal("Sleutel opgeslagen: sk-ant-…wxyz")
        click("Beste")
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Beste") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Quality.BEST, runBlocking { settings.current() }.quality)

        click("Sleutel verwijderen")
        // The dialog's confirm button has the same label.
        compose.reveal("Sleutel verwijderen?")
        // The dialog's confirm button has the same label as the one that opened it.
        compose
            .onAllNodes(hasText("Sleutel verwijderen") and hasClickAction() and hasAnyAncestor(isDialog()))
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.reveal("Geen sleutel ingesteld")
        assertEquals(ClaudeKeyStatus.NONE, keyStatus())
        assertNull(runBlocking { secrets.readClaudeKey() })
    }
}
