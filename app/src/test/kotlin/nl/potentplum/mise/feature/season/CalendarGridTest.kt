package nl.potentplum.mise.feature.season

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.potentplum.mise.content.model.Vegetable
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.CalendarGrouping
import nl.potentplum.mise.domain.CalendarLayout
import nl.potentplum.mise.testing.TestContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Spec §6.4 acceptance: the whole year fits a 360 dp wide screen at 200 % font scale, without horizontal scrolling. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w360dp-h640dp")
class CalendarGridTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        fontScale: Float,
        vegetables: List<Vegetable> = TestContent.content.vegetables,
    ) {
        val sections = CalendarLayout.sections(vegetables, CalendarGrouping.FAMILY)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MiseTheme { CalendarGrid(sections, currentMonth = 10, onVegetable = {}) }
            }
        }
    }

    @Test
    fun `december fits at 200 percent font scale`() {
        show(fontScale = 2f)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        // Allium sorts first, so its rows are on screen; long names ellipsize instead of pushing the months out.
        val rows = TestContent.content.vegetables.filter { it.family.nl == "Allium" }
        rows.forEach { vegetable ->
            val december = compose.onNodeWithTag("calendar-cell-${vegetable.id}-12", useUnmergedTree = true).fetchSemanticsNode()
            assertTrue("${vegetable.id}: ${december.boundsInRoot}", december.boundsInRoot.right <= root.right)
            assertTrue(december.boundsInRoot.width > 0f)
        }
    }

    @Test
    fun `nothing scrolls horizontally`() {
        show(fontScale = 2f)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)).assertCountEquals(0)
    }

    @Test
    fun `rows read as one sentence with month names in full`() {
        show(fontScale = 1f, vegetables = listOf(TestContent.content.vegetable("asparagus-white")!!))
        compose.onNodeWithContentDescription("Witte asperges: op z'n best mei, juni; verkrijgbaar april").assertExists()
    }
}
