package nl.potentplum.mise.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.feature.PlaceholderScreen
import java.time.LocalDate

/** Vandaag (spec §6.2). P1 fills in the Vanavond card, suggestions, Quest van vandaag and the peak shortlist. */
@Composable
fun TodayScreen(
    today: LocalDate,
    onOverflowItem: (OverflowItem) -> Unit,
) {
    val month = LocalResources.current.getStringArray(R.array.months_long)[today.monthValue - 1]
    PlaceholderScreen(title = stringResource(R.string.nav_today), onOverflowItem = onOverflowItem) {
        HeroHeader(
            title = styledStringResource(R.string.today_hero_title),
            eyebrow = stringResource(R.string.today_eyebrow, month, today.year),
        )
    }
}
