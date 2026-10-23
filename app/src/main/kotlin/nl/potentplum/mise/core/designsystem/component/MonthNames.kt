package nl.potentplum.mise.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import java.util.Locale

private val Dutch: Locale = Locale.forLanguageTag("nl-NL")

/** Full month name, lowercase for use mid-sentence ("oktober"). [month] is 1–12. */
@Composable
@ReadOnlyComposable
fun monthName(month: Int): String = LocalResources.current.getStringArray(R.array.months_long)[month - 1].lowercase(Dutch)

/** "mei, juni", or "geen" when empty. Month names are spelled out for TalkBack (spec §15.3). */
@Composable
@ReadOnlyComposable
fun monthList(months: List<Int>): String {
    val names = LocalResources.current.getStringArray(R.array.months_long)
    return months.sorted().joinToString(", ") { names[it - 1].lowercase(Dutch) }.ifEmpty { stringResource(R.string.peakbar_none) }
}
