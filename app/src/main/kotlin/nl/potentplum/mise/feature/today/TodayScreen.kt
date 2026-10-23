package nl.potentplum.mise.feature.today

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.feature.PlaceholderScreen
import nl.potentplum.mise.feature.quests.TodayQuestCard
import java.time.LocalDate

/** Vandaag (spec §6.2). The Quest van vandaag card is live; Vanavond, suggestions and the peak shortlist follow in P1. */
@Composable
fun TodayScreen(
    today: LocalDate,
    onOverflowItem: (OverflowItem) -> Unit,
    onOpenQuest: (String) -> Unit,
    onRefineQuest: () -> Unit,
    onAddKey: () -> Unit,
) {
    val month = LocalResources.current.getStringArray(R.array.months_long)[today.monthValue - 1]
    PlaceholderScreen(title = stringResource(R.string.nav_today), onOverflowItem = onOverflowItem) {
        HeroHeader(
            title = styledStringResource(R.string.today_hero_title),
            eyebrow = stringResource(R.string.today_eyebrow, month, today.year),
        )
        TodayQuestCard(
            onOpenQuest = onOpenQuest,
            onRefine = onRefineQuest,
            onAddKey = onAddKey,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
