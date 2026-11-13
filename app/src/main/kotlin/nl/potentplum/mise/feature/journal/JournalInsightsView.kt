package nl.potentplum.mise.feature.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.content.Content
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.EvenRow
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.SeasonTally
import nl.potentplum.mise.core.designsystem.component.VegTokenData
import nl.potentplum.mise.core.designsystem.hexColor
import nl.potentplum.mise.core.util.DutchNumbers
import nl.potentplum.mise.domain.BadgeProgress
import nl.potentplum.mise.domain.BadgeStatus
import nl.potentplum.mise.domain.CookingSeason
import nl.potentplum.mise.domain.DisciplineSummary
import nl.potentplum.mise.domain.JournalInsights
import nl.potentplum.mise.domain.ScoreSteps
import kotlin.math.roundToInt

/** Dagboek › Inzichten (spec §6.13): header stats, Dit seizoen, Technieken, Keukens and Insignes. */
@Composable
fun JournalInsightsView(
    state: JournalUiState,
    onVegetable: (String) -> Unit,
    onAllVegetables: () -> Unit,
    onTechnique: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insights = state.insights ?: return
    val content = state.content ?: return
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HeaderStats(insights)
        if (insights.totalAttempts == 0) EmptyState(title = stringResource(R.string.journal_empty))

        Section(stringResource(R.string.journal_season_title, seasonName(insights.season.season))) {
            fun token(id: String) = content.vegetable(id)?.let { VegTokenData(it.id, it.nl, hexColor(it.color)) }
            SeasonTally(
                count = insights.season.cookedVegIds.size,
                cooked = insights.season.cookedVegIds.mapNotNull(::token),
                toTry =
                    insights.season.toTry
                        .take(3)
                        .mapNotNull(::token),
                onVegetable = onVegetable,
                onAll = onAllVegetables,
            )
        }

        Section(stringResource(R.string.journal_techniques_title)) {
            DisciplineBars(insights.disciplines, content)
            Text(stringResource(R.string.journal_top_techniques), style = MaterialTheme.typography.titleSmall)
            if (insights.topTechniques.isEmpty()) {
                Text(
                    stringResource(R.string.journal_none_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            }
            insights.topTechniques.forEach { counted ->
                val name = content.technique(counted.value)?.nl ?: counted.value
                CountRow(name, counted.count, onClick = content.technique(counted.value)?.let { { onTechnique(it.id) } })
            }
            Text(stringResource(R.string.journal_neglected), style = MaterialTheme.typography.titleSmall)
            if (insights.neglected.isEmpty()) {
                Text(
                    stringResource(R.string.journal_neglected_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            } else {
                Text(
                    insights.neglected.joinToString(", ") { id -> content.disciplines.first { it.id == id }.nl },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Section(stringResource(R.string.journal_top_cuisines)) {
            if (insights.topCuisines.isEmpty()) {
                Text(
                    stringResource(R.string.journal_none_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            }
            insights.topCuisines.forEach { CountRow(it.value, it.count) }
        }

        Section(stringResource(R.string.journal_badges_title)) {
            insights.badges.forEach { BadgeRow(it) }
        }
    }
}

@Composable
private fun HeaderStats(insights: JournalInsights) {
    // Equal tiles, as tall as the tallest; stacked when a label's longest word no longer fits a third of the width.
    EvenRow(Modifier.fillMaxWidth()) {
        Stat(insights.attemptsThisMonth, stringResource(R.string.journal_stat_month), Modifier)
        Stat(insights.totalAttempts, stringResource(R.string.journal_stat_total), Modifier)
        Stat(insights.questsCompleted, stringResource(R.string.journal_stat_quests), Modifier)
    }
}

@Composable
private fun Stat(
    value: Int,
    label: String,
    modifier: Modifier,
) {
    Card(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MiseTheme.colors.inkSoft, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(title)
        content()
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

/** Count and average execution score per discipline (spec §12). */
@Composable
private fun DisciplineBars(
    disciplines: List<DisciplineSummary>,
    content: Content,
) {
    val max = disciplines.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val steps = stringArrayResource(R.array.score_steps)
    disciplines.forEach { summary ->
        val name = content.disciplines.first { it.id == summary.discipline }.nl
        val average = summary.avgScore?.let { steps[ScoreSteps.stepOf(it.roundToInt())] }
        val detail =
            average?.let { pluralStringResource(R.plurals.journal_discipline_detail_scored, summary.count, summary.count, it) }
                ?: pluralStringResource(R.plurals.journal_discipline_detail, summary.count, summary.count)
        Column(
            Modifier.clearAndSetSemantics { contentDescription = "$name: $detail" },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row {
                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            }
            LinearProgressIndicator(
                progress = { summary.count.toFloat() / max },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun CountRow(
    label: String,
    count: Int,
    onClick: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onClick != null) {
            TextButton(onClick = onClick, modifier = Modifier.weight(1f)) { Text(label, modifier = Modifier.fillMaxWidth()) }
        } else {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Text(stringResource(R.string.journal_count, count), style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
    }
}

@Composable
private fun BadgeRow(badge: BadgeStatus) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            if (badge.earned) Icons.Outlined.WorkspacePremium else Icons.Outlined.Lock,
            contentDescription = stringResource(if (badge.earned) R.string.journal_badge_earned else R.string.journal_badge_locked),
            tint = if (badge.earned) MaterialTheme.colorScheme.primary else MiseTheme.colors.inkSoft,
            modifier = Modifier.size(32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(badge.archetype.nl, style = MaterialTheme.typography.titleMedium)
            Text(badge.archetype.description, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            progressText(badge.progress)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun progressText(progress: BadgeProgress): String? =
    when (progress) {
        BadgeProgress.Earned -> {
            null
        }

        is BadgeProgress.MissingAttempts -> {
            pluralStringResource(R.plurals.badge_missing_attempts, progress.missing, progress.missing)
        }

        is BadgeProgress.MissingPeakVegetables -> {
            pluralStringResource(R.plurals.badge_missing_peak_vegetables, progress.missing, progress.missing)
        }

        is BadgeProgress.MissingPeakMonths -> {
            pluralStringResource(R.plurals.badge_missing_peak_months, progress.missing, progress.missing)
        }

        is BadgeProgress.MissingScore -> {
            val steps = stringArrayResource(R.array.score_steps)
            val needed = steps[ScoreSteps.stepOf(progress.needed.roundToInt())]
            progress.average?.let {
                stringResource(R.string.badge_missing_score, DutchNumbers.format(it, 1), DutchNumbers.format(progress.needed, 1), needed)
            }
                ?: stringResource(R.string.badge_missing_scores, needed)
        }
    }

@Composable
private fun seasonName(season: CookingSeason): String =
    stringResource(
        when (season) {
            CookingSeason.LENTE -> R.string.season_spring
            CookingSeason.ZOMER -> R.string.season_summer
            CookingSeason.HERFST -> R.string.season_autumn
            CookingSeason.WINTER -> R.string.season_winter
        },
    )
