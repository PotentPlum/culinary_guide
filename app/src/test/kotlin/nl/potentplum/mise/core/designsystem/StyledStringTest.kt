package nl.potentplum.mise.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontStyle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.potentplum.mise.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StyledStringTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `italic markup becomes instrument serif italic on exactly the emphasis word`() {
        val text =
            ApplicationProvider
                .getApplicationContext<android.content.Context>()
                .resources
                .getText(R.string.today_hero_title)
                .toStyledAnnotatedString()

        assertEquals("Wat kook ik vandaag?", text.text)
        val italic = text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italic.size)
        assertEquals("vandaag", text.text.substring(italic.single().start, italic.single().end))
        assertEquals(InstrumentSerif, italic.single().item.fontFamily)
    }

    @Test
    fun `plain strings have no styles`() {
        val text =
            ApplicationProvider
                .getApplicationContext<android.content.Context>()
                .resources
                .getText(R.string.nav_today)
                .toStyledAnnotatedString()
        assertEquals("Vandaag", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }

    @Test
    fun `the headline is one node, so TalkBack reads one sentence`() {
        compose.setContent { MiseTheme { Text(styledStringResource(R.string.today_hero_title)) } }
        compose.onNodeWithText("Wat kook ik vandaag?").assertExists()
        compose.onAllNodesWithText("vandaag", substring = true).assertCountEquals(1)
    }
}
