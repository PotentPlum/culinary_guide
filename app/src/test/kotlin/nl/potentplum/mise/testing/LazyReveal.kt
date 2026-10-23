package nl.potentplum.mise.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode

/**
 * Generous because the first screen of a test class is composed on a cold JVM: on GitHub's runners that alone took over
 * 5 s (spec §16). The wait ends as soon as the node shows up, so passing tests are not slower.
 */
const val REVEAL_TIMEOUT_MILLIS = 15_000L

/**
 * Waits until [text] is on screen, scrolling the screen's first lazy list towards it on each try: lazy items below the
 * fold are not composed, so they cannot be found without scrolling.
 */
fun ComposeTestRule.reveal(
    text: String,
    substring: Boolean = true,
    timeoutMillis: Long = REVEAL_TIMEOUT_MILLIS,
) = reveal(hasText(text, substring = substring), timeoutMillis) {
    onAllNodesWithText(text, substring = substring, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}

fun ComposeTestRule.reveal(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = REVEAL_TIMEOUT_MILLIS,
    found: () -> Boolean = { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() },
) = waitUntil(timeoutMillis) {
    if (!found()) {
        val lists = onAllNodes(hasScrollToNodeAction())
        if (lists.fetchSemanticsNodes().isNotEmpty()) runCatching { lists.onFirst().performScrollToNode(matcher) }
    }
    found()
}
