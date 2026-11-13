package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.core.designsystem.MiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a season badge never breaks its label over two lines`() {
        // Narrower than "Kort seizoen": the label must stay one line (the row around it wraps the badge instead).
        compose.setContent { MiseTheme { SeasonBadge(SeasonBadgeKind.ShortSeason, Modifier.width(40.dp)) } }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Kort seizoen").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(1, results.first().lineCount)
    }

    @Test
    fun `the loading spinner waits before it shows`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MiseTheme { LoadingState() } }
        compose.mainClock.advanceTimeBy(100)
        compose.onAllNodesWithContentDescription("Laden…").assertCountEquals(0)
        compose.mainClock.advanceTimeBy(400)
        compose.onAllNodesWithContentDescription("Laden…").assertCountEquals(1)
    }
}
