package nl.potentplum.mise.feature.curriculum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CheckItem
import nl.potentplum.mise.content.model.CheckItemType
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.OverflowItem
import nl.potentplum.mise.domain.CheckResult

/**
 * A module's knowledge check (spec §6.17, assessment §4.1), one item at a time with feedback after each answer.
 * Multiple choice is marked by the app; the other types show a model answer and the learner marks their own.
 */
@Composable
fun CheckScreen(
    onBack: () -> Unit,
    onOverflowItem: (OverflowItem) -> Unit,
    viewModel: CheckViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title =
        (state as? Loadable.Loaded)
            ?.value
            ?.module
            ?.title
            .orEmpty()
    CurriculumScaffold(title, state, onBack, onOverflowItem) { session ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val result = session.result
            val item = session.current
            if (result != null) {
                ResultView(result, session.items, onRetry = viewModel::restart, onDone = onBack)
            } else if (item != null) {
                ItemView(session, item, viewModel)
            }
        }
    }
}

@Composable
private fun ItemView(
    session: CheckSession,
    item: CheckItem,
    viewModel: CheckViewModel,
) {
    val answer = session.answers[item.id]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(
            listOfNotNull(
                stringResource(R.string.curriculum_check_progress, session.position + 1, session.items.size),
                stringResource(R.string.curriculum_check_safety).takeIf { item.safety },
            ).joinToString(" · "),
        )
        Text(inlineMarks(item.question), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
    if (item.type == CheckItemType.MULTIPLE_CHOICE) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item.options.forEach { option ->
                val chosen = (answer as? ItemAnswer.Choice)?.key == option.key
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = chosen, enabled = answer == null, role = Role.RadioButton) { viewModel.choose(option.key) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = chosen, onClick = null, enabled = answer == null)
                    Text(
                        inlineMarks("${option.key}) ${option.text}"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        if (answer != null) {
            Feedback(
                headline =
                    if (answer.right) {
                        stringResource(R.string.curriculum_check_correct)
                    } else {
                        stringResource(R.string.curriculum_check_wrong, item.answer)
                    },
                item = item,
            )
        }
    } else {
        if (item.id !in session.revealed) {
            OutlinedButton(onClick = viewModel::reveal, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.curriculum_check_reveal))
            }
        } else {
            Panel {
                Eyebrow(stringResource(R.string.curriculum_check_model_answer))
                Text(inlineMarks(item.answer), style = MaterialTheme.typography.bodyLarge)
                if (item.why.isNotBlank()) {
                    Eyebrow(stringResource(R.string.curriculum_check_why))
                    Text(inlineMarks(item.why), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (answer == null) {
                Text(stringResource(R.string.curriculum_check_self_mark), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.selfMark(true) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.curriculum_check_self_right))
                    }
                    OutlinedButton(onClick = { viewModel.selfMark(false) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.curriculum_check_self_wrong))
                    }
                }
            }
        }
    }
    if (answer != null) {
        val last = session.position == session.items.lastIndex
        Button(onClick = if (last) viewModel::finish else viewModel::next, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (last) R.string.curriculum_check_finish else R.string.curriculum_check_next))
        }
    }
}

@Composable
private fun Feedback(
    headline: String,
    item: CheckItem,
) {
    Panel {
        Text(headline, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (item.why.isNotBlank()) {
            Eyebrow(stringResource(R.string.curriculum_check_why))
            Text(inlineMarks(item.why), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MiseTheme.colors.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun ResultView(
    result: CheckResult,
    items: List<CheckItem>,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (result.passed) R.string.curriculum_check_passed else R.string.curriculum_check_failed),
            style = MaterialTheme.typography.headlineLarge,
            color = if (result.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.curriculum_check_score, result.correct, result.total, result.percent),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (result.safetyTotal > 0) {
            Text(
                stringResource(R.string.curriculum_check_safety_score, result.safetyCorrect, result.safetyTotal),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(stringResource(R.string.curriculum_check_rule), style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft)
    }
    val wrong = items.filter { it.id in result.wrongItemIds }
    if (wrong.isNotEmpty()) {
        Text(
            stringResource(R.string.curriculum_check_wrong_items),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        wrong.forEach { item ->
            Surface(shape = RoundedCornerShape(12.dp), color = Color.Transparent, border = BorderStroke(1.dp, MiseTheme.colors.line)) {
                Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow(
                        listOfNotNull(
                            item.lessonId,
                            stringResource(R.string.curriculum_check_safety).takeIf { item.safety },
                        ).joinToString(" · "),
                    )
                    Text(inlineMarks(item.question), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        inlineMarks(item.options.firstOrNull { it.key == item.answer }?.let { "${it.key}) ${it.text}" } ?: item.answer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.curriculum_check_retry)) }
        Button(onClick = onDone, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.curriculum_check_done_btn)) }
    }
}
