package nl.potentplum.mise.feature.curriculum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CourseKind
import nl.potentplum.mise.content.model.CourseModule
import nl.potentplum.mise.content.model.ModuleSummary
import nl.potentplum.mise.content.model.RubricRow
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.domain.CurriculumRules

/** A course (spec §6.17): its modules with lessons, exercises and the knowledge check; a milestone shows its rubric. */
@Composable
fun CourseScreen(
    onBack: () -> Unit,
    onCourse: (String) -> Unit,
    onLesson: (String) -> Unit,
    onExercise: (String) -> Unit,
    onCheck: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: CourseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title =
        (state as? Loadable.Loaded)
            ?.value
            ?.summary
            ?.title
            .orEmpty()
    CurriculumScaffold(title, state, onBack, onOverflowItem) { detail ->
        val summary = detail.summary
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow(
                        listOf(
                            stringResource(R.string.curriculum_stage_eyebrow, detail.stageNumber),
                            detail.stageTitle,
                            stringResource(R.string.curriculum_hours, hoursLabel(summary.hours)),
                        ).joinToString(" · "),
                    )
                    Text(summary.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    Text(summary.purpose, style = MaterialTheme.typography.bodyLarge, color = MiseTheme.colors.inkSoft)
                    Text(
                        stringResource(R.string.curriculum_language_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MiseTheme.colors.inkSoft,
                    )
                    if (summary.status.isDraft()) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DraftLabel()
                            Text(
                                stringResource(R.string.curriculum_draft_explained, summary.safetyItemsRead, summary.safetyItems),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (detail.prerequisites.isNotEmpty()) {
                        Text(
                            stringResource(R.string.curriculum_prerequisites),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiseTheme.colors.inkSoft,
                        )
                        detail.prerequisites.forEach { prerequisite ->
                            LinkRow(prerequisite.title, stringResource(R.string.curriculum_hours, hoursLabel(prerequisite.hours))) {
                                onCourse(prerequisite.id)
                            }
                        }
                    }
                    if (detail.steps.total > 0) {
                        Progress(detail.steps, stringResource(R.string.curriculum_steps, detail.steps.done, detail.steps.total))
                    }
                }
            }
            val file = detail.file
            if (file == null || summary.kind != CourseKind.COURSE) {
                item { Rubric(summary.rubric) }
            } else {
                if (file.introduction.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(stringResource(R.string.curriculum_introduction))
                            Blocks(file.introduction)
                        }
                    }
                }
                itemsIndexed(file.modules, key = { _, module -> module.id }) { i, module ->
                    val moduleSummary = summary.modules.first { it.id == module.id }
                    ModuleCard(i + 1, module, moduleSummary, detail, onLesson, onExercise, onCheck)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun ModuleCard(
    number: Int,
    module: CourseModule,
    summary: ModuleSummary,
    detail: CourseDetail,
    onLesson: (String) -> Unit,
    onExercise: (String) -> Unit,
    onCheck: (String) -> Unit,
) {
    val progress = detail.progress
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MiseTheme.colors.line),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val steps = CurriculumRules.moduleSteps(summary, progress)
            Eyebrow(
                listOf(
                    stringResource(R.string.curriculum_module_eyebrow, number),
                    stringResource(R.string.curriculum_steps, steps.done, steps.total),
                ).joinToString(" · "),
            )
            Text(module.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (module.lessons.isNotEmpty()) {
                Eyebrow(stringResource(R.string.curriculum_lessons))
                summary.lessons.forEach { lesson ->
                    StepRow(
                        lesson.title,
                        stringResource(R.string.curriculum_hours, hoursLabel(lesson.hours)),
                        progress.lessonDone(lesson.id),
                    ) {
                        onLesson(lesson.id)
                    }
                }
            }
            if (module.exercises.isNotEmpty()) {
                Eyebrow(stringResource(R.string.curriculum_exercises))
                summary.exercises.forEach { exercise ->
                    val band = progress.bestBands[exercise.id]
                    val meta = listOfNotNull(stringResource(exercise.support.labelRes), band?.let { stringResource(it.labelRes) })
                    StepRow(exercise.title, meta.joinToString(" · "), progress.exerciseDone(exercise.id)) { onExercise(exercise.id) }
                }
            }
            Eyebrow(stringResource(R.string.curriculum_check))
            if (module.items.isEmpty()) {
                Text(
                    stringResource(R.string.curriculum_check_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiseTheme.colors.inkSoft,
                )
                Blocks(module.note)
            } else {
                val best = detail.bestChecks[module.id]
                val meta =
                    listOfNotNull(
                        stringResource(R.string.curriculum_check_summary, summary.checkItems, summary.safetyCheckItems),
                        best?.let { stringResource(R.string.curriculum_check_best, it) },
                    ).joinToString(" · ")
                StepRow(stringResource(R.string.curriculum_check_start), meta, progress.checkPassed(module.id)) { onCheck(module.id) }
            }
        }
    }
}

@Composable
private fun StepRow(
    title: String,
    meta: String,
    done: Boolean,
    onClick: () -> Unit,
) {
    val stateLabel = stringResource(if (done) R.string.curriculum_done else R.string.curriculum_not_done)
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = stateLabel },
    ) {
        Row(
            Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.primary else MiseTheme.colors.inkSoft,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
            }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    meta: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
        }
    }
}

/** A milestone's or the capstone's rubric (assessment §3): criteria, weights and the evidence they look at. */
@Composable
private fun Rubric(rows: List<RubricRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.curriculum_rubric_title))
        Text(stringResource(R.string.curriculum_rubric_intro), style = MaterialTheme.typography.bodyMedium)
        rows.forEach { row ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MiseTheme.colors.line),
            ) {
                Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val weight = row.weight
                    Eyebrow(
                        if (weight == null) {
                            stringResource(R.string.curriculum_rubric_gate)
                        } else {
                            stringResource(R.string.curriculum_rubric_weight, weight)
                        },
                    )
                    Text(inlineMarks(row.criterion), style = MaterialTheme.typography.bodyLarge)
                    if (row.evidence.isNotEmpty()) {
                        Text(
                            stringResource(R.string.curriculum_rubric_evidence, row.evidence.joinToString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiseTheme.colors.inkSoft,
                        )
                    }
                }
            }
        }
    }
}
