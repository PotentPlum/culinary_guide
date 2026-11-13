package nl.potentplum.mise.feature.curriculum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CourseKind
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.HeroHeader
import nl.potentplum.mise.core.designsystem.component.MiseTopAppBar
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.core.designsystem.styledStringResource
import nl.potentplum.mise.domain.NextStep
import nl.potentplum.mise.domain.StepCount

/** A curriculum screen's frame: top bar and the loading / not-found states. */
@Composable
internal fun <T> CurriculumScaffold(
    title: String,
    state: Loadable<T>,
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    content: @Composable (T) -> Unit,
) {
    Scaffold(topBar = { MiseTopAppBar(title = title, onOverflowItem = onOverflowItem, onBack = onBack) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                Loadable.Loading -> Unit
                Loadable.NotFound -> EmptyState(title = stringResource(R.string.error_not_found))
                is Loadable.Loaded -> content(state.value)
            }
        }
    }
}

/** Opleiding (spec §6.17): the five stages with their courses, the overall progress and the next open step. */
@Composable
fun CurriculumScreen(
    onBack: () -> Unit,
    onCourse: (String) -> Unit,
    onLesson: (String) -> Unit,
    onExercise: (String) -> Unit,
    onCheck: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: CurriculumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CurriculumScaffold(stringResource(R.string.curriculum_title), state, onBack, onOverflowItem) { home ->
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroHeader(
                        title = styledStringResource(R.string.curriculum_hero),
                        subtitle = stringResource(R.string.curriculum_intro),
                    )
                    Progress(home.overall, stringResource(R.string.curriculum_overall, home.overall.done, home.overall.total))
                }
            }
            item {
                val next = home.next
                if (next == null) {
                    Text(stringResource(R.string.curriculum_all_done), style = MaterialTheme.typography.titleMedium)
                } else {
                    NextStepCard(next) {
                        when (next) {
                            is NextStep.Lesson -> onLesson(next.id)
                            is NextStep.Exercise -> onExercise(next.id)
                            is NextStep.Check -> onCheck(next.module.id)
                        }
                    }
                }
            }
            home.stages.forEach { section ->
                item(key = section.stage.id) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow(stringResource(R.string.curriculum_stage_eyebrow, section.number))
                        Text(
                            section.stage.titleNl,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.curriculum_steps, section.steps.done, section.steps.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiseTheme.colors.inkSoft,
                        )
                    }
                }
                items(section.courses, key = { it.course.id }) { row -> CourseCard(row) { onCourse(row.course.id) } }
            }
        }
    }
}

@Composable
internal fun Progress(
    steps: StepCount,
    label: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { steps.fraction },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MiseTheme.colors.line,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
    }
}

@Composable
private fun NextStepCard(
    next: NextStep,
    onOpen: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MiseTheme.colors.line),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(stringResource(R.string.curriculum_next_eyebrow))
            val kind =
                when (next) {
                    is NextStep.Lesson -> stringResource(R.string.curriculum_next_lesson, next.course.id)
                    is NextStep.Exercise -> stringResource(R.string.curriculum_next_exercise, next.course.id)
                    is NextStep.Check -> stringResource(R.string.curriculum_next_check, next.course.id)
                }
            Text(kind, style = MaterialTheme.typography.labelLarge, color = MiseTheme.colors.inkSoft)
            val title =
                when (next) {
                    is NextStep.Lesson -> next.title
                    is NextStep.Exercise -> next.title
                    is NextStep.Check -> next.module.title
                }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(next.course.title, style = MaterialTheme.typography.bodyMedium, color = MiseTheme.colors.inkSoft)
            Button(onClick = onOpen) { Text(stringResource(R.string.curriculum_next_open)) }
        }
    }
}

@Composable
private fun CourseCard(
    row: CourseRow,
    onClick: () -> Unit,
) {
    val course = row.course
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MiseTheme.colors.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val kind =
                    when (course.kind) {
                        CourseKind.COURSE -> course.id
                        CourseKind.MILESTONE -> stringResource(R.string.curriculum_milestone)
                        CourseKind.CAPSTONE -> stringResource(R.string.curriculum_capstone)
                    }
                Eyebrow(kind, Modifier.weight(1f))
                if (course.status.isDraft()) DraftLabel()
            }
            Text(course.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.curriculum_hours, hoursLabel(course.hours)),
                style = MaterialTheme.typography.bodySmall,
                color = MiseTheme.colors.inkSoft,
            )
            if (row.steps.total > 0) Progress(row.steps, stringResource(R.string.curriculum_steps, row.steps.done, row.steps.total))
        }
    }
}
