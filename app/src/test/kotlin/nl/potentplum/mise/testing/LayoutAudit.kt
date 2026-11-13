package nl.potentplum.mise.testing

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.Hyphens
import nl.potentplum.mise.core.designsystem.component.TruncationAllowed
import java.io.File

/**
 * Checks every text on screen the way a reader would see it (spec §15.3): all of it is visible (no ellipsis, nothing hidden by
 * maxLines, nothing past its own bounds or the screen edge), no word is split over two lines, and no button label wraps.
 * Text in a node marked `allowTruncation()` and text inside a sideways-scrolling row are skipped.
 */
private const val FULL_WIDTH = 0.7f

/**
 * Set `LAYOUT_AUDIT_SHOTS` to a directory to also save the first page of every audited screen there as a PNG, one folder per
 * test class: a quick visual gallery of the app at that size (spec §15.3). Robolectric draws dialog and sheet windows
 * without their content, so for those the gallery shows the screen underneath; the audit itself does read their text.
 */
class LayoutAudit(
    private val compose: ComposeTestRule,
    private val profile: String = "",
) {
    private val shots = System.getenv("LAYOUT_AUDIT_SHOTS")?.let { File(it, profile).apply { mkdirs() } }

    private val issues = linkedSetOf<String>()

    /** Audits [screen], scrolling its main vertical list or column to the end. */
    fun screen(
        screen: String,
        maxPages: Int = 12,
    ) {
        compose.waitForIdle()
        val all = mutableSetOf<String>()
        var seen = emptySet<String>()
        shots?.let { dir ->
            // One image per window: a sheet or dialog is its own root next to the screen under it.
            val roots = compose.onAllNodes(isRoot())
            val name = screen.replace(Regex("[^A-Za-z0-9]+"), "-")
            for (index in roots.fetchSemanticsNodes().indices) {
                val bitmap = roots[index].captureToImage().asAndroidBitmap()
                File(dir, if (index == 0) "$name.png" else "$name-$index.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        for (page in 0 until maxPages) {
            val visible = check(screen)
            all += visible
            if (visible == seen || !scrollDown()) break
            seen = visible
        }
        // An empty screen means the audit looked at nothing (still loading, wrong window): that is a failure, not a pass.
        if (all.isEmpty()) issues += "$screen: no text on screen"
        println("LayoutAudit $screen: ${all.size} texts")
    }

    fun assertNoIssues() {
        if (issues.isNotEmpty()) throw AssertionError("${issues.size} layout issues:\n" + issues.joinToString("\n"))
    }

    private fun check(screen: String): Set<String> {
        val root = compose.onAllNodes(SemanticsMatcher("root") { it.parent == null }, useUnmergedTree = true).fetchSemanticsNodes()
        val screenWidth = root.maxOfOrNull { it.boundsInRoot.right } ?: Float.MAX_VALUE
        val nodes =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                .fetchSemanticsNodes()
        val visible = mutableSetOf<String>()
        for (node in nodes) {
            val text = node.config[SemanticsProperties.Text].joinToString("") { it.text }
            if (text.isBlank()) continue
            visible += text
            if (node.ancestors().any { it.config.getOrNull(TruncationAllowed) == true }) continue
            if (node.ancestors().any { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null }) continue
            val layout = node.layout() ?: continue
            problems(text, node, layout, screenWidth).forEach { issues += "$screen: \"${text.take(60)}\" $it" }
        }
        return visible
    }

    private fun problems(
        text: String,
        node: SemanticsNode,
        layout: TextLayoutResult,
        screenWidth: Float,
    ): List<String> {
        val found = mutableListOf<String>()
        val last = layout.lineCount - 1
        if (layout.isLineEllipsized(last)) {
            found += "ends in an ellipsis"
        } else if (layout.getLineEnd(last, visibleEnd = true) < text.trimEnd().length) {
            found += "is cut off after line ${layout.lineCount}"
        }
        // Centred text is laid out at the full width and offset, so a line's extent is right minus left.
        val widest = (0..last).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
        if (widest > node.size.width + 1f) found += "is clipped (needs ${widest.toInt()} px, has ${node.size.width} px)"
        if (node.boundsInRoot.right > screenWidth + 1f) found += "runs past the screen edge"
        // A word wider than a full-width line cannot be helped ("Boerenkoolstamppot" at 200 %): only narrower text must
        // keep its words whole, and only a narrower button must keep its label on one line.
        val fullWidth = node.size.width >= screenWidth * FULL_WIDTH
        // Hyphenated text (`dutchHyphenated()`) breaks a long word at a syllable, with a hyphen: that is not a mid-word split.
        val hyphenated = layout.layoutInput.style.hyphens == Hyphens.Auto
        for (line in 0 until last) {
            if (fullWidth || hyphenated) break
            val end = layout.getLineEnd(line)
            if (end in 1 until text.length && text[end - 1].isLetter() && text[end].isLetter()) {
                found += "breaks mid-word (\"${text.substring(maxOf(0, end - 6), minOf(text.length, end + 6))}\")"
            }
        }
        val button = node.ancestors().firstOrNull { it.config.getOrNull(SemanticsProperties.Role) == Role.Button }
        if (button != null && layout.lineCount > 1 && button.size.width < screenWidth * FULL_WIDTH) {
            found += "is a button label on ${layout.lineCount} lines"
        }
        return found
    }

    private fun SemanticsNode.ancestors(): Sequence<SemanticsNode> = generateSequence(this) { it.parent }

    private fun SemanticsNode.layout(): TextLayoutResult? {
        val action = config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: return null
        val results = mutableListOf<TextLayoutResult>()
        compose.runOnIdle { action(results) }
        return results.firstOrNull()
    }

    /** Scrolls the tallest vertical scroller by most of its height; false when there is none or it is at the end. */
    private fun scrollDown(): Boolean {
        val scrollers =
            compose
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange), useUnmergedTree = true)
                .fetchSemanticsNodes()
        val scroller = scrollers.maxByOrNull { it.boundsInRoot.height } ?: return false
        val range = scroller.config[SemanticsProperties.VerticalScrollAxisRange]
        if (range.value() >= range.maxValue()) return false
        val scrollBy = scroller.config.getOrNull(SemanticsActions.ScrollBy)?.action ?: return false
        compose.runOnIdle { scrollBy(0f, scroller.boundsInRoot.height * 0.7f) }
        compose.waitForIdle()
        return true
    }
}
