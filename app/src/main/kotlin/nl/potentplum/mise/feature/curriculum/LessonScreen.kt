package nl.potentplum.mise.feature.curriculum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CompetencyLevel
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.OverflowItem

/** A lesson (spec §6.17): English text, Dutch frame; "Markeer als gedaan" counts it as a step. */
@Composable
fun LessonScreen(
    onBack: () -> Unit,
    onLesson: (String) -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title =
        (state as? Loadable.Loaded)
            ?.value
            ?.lesson
            ?.id
            .orEmpty()
    CurriculumScaffold(title, state, onBack, onOverflowItem) { detail ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(listOf(detail.course.id, detail.module.title).joinToString(" · "))
                Text(detail.lesson.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                Text(
                    stringResource(R.string.curriculum_language_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MiseTheme.colors.inkSoft,
                )
            }
            Blocks(detail.lesson.blocks)
            Competencies(detail.lesson.competencies, detail.competencyTitles)
            if (detail.done) {
                OutlinedButton(onClick = { viewModel.setDone(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.curriculum_mark_undone))
                }
            } else {
                Button(onClick = { viewModel.setDone(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.curriculum_mark_done))
                }
            }
            detail.nextLessonId?.let { next ->
                OutlinedButton(onClick = { onLesson(next) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.curriculum_next_lesson_btn))
                }
            }
        }
    }
}

@Composable
internal fun Competencies(
    levels: List<CompetencyLevel>,
    titles: Map<String, String>,
) {
    if (levels.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Eyebrow(stringResource(R.string.curriculum_competencies))
        levels.forEach { level ->
            Text(
                stringResource(R.string.curriculum_competency_level, titles[level.id] ?: level.id, level.level),
                style = MaterialTheme.typography.bodySmall,
                color = MiseTheme.colors.inkSoft,
            )
        }
    }
}
