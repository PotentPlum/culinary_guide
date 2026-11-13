package nl.potentplum.mise.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics

/**
 * Marks text that may be cut short on purpose: a top-bar title that repeats the screen's heading, a preview of a longer
 * text, a name in a fixed grid column. Everything else must show in full at every font size (spec §15.3); the layout audit
 * (`LayoutAuditTest`) checks that, and skips text inside a node marked with this.
 */
val TruncationAllowed = SemanticsPropertyKey<Boolean>("TruncationAllowed")

fun Modifier.allowTruncation(): Modifier = semantics { this[TruncationAllowed] = true }
