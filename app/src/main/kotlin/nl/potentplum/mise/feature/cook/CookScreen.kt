package nl.potentplum.mise.feature.cook

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.Technique
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.component.EmptyState
import nl.potentplum.mise.core.designsystem.component.Eyebrow
import nl.potentplum.mise.core.designsystem.component.LocalMiseSnackbar
import nl.potentplum.mise.core.designsystem.component.TimerChip
import nl.potentplum.mise.core.designsystem.component.TimerChipState
import nl.potentplum.mise.core.designsystem.component.durationLabel
import nl.potentplum.mise.feature.recipes.IngredientGroupLines

/**
 * Kookmodus (spec §6.10): a step pager with timers and technique help, the ingredients one tap away (portrait) or beside the
 * step (landscape), the screen kept on. "Klaar met koken" and the exit prompt open the evaluation sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookScreen(
    onClose: () -> Unit,
    onLogged: () -> Unit,
    viewModel: CookViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reachedEnd by viewModel.reachedEnd.collectAsStateWithLifecycle()
    val snackbar = LocalMiseSnackbar.current
    val resources = LocalResources.current
    var askCooked by rememberSaveable { mutableStateOf(false) }
    var evaluating by rememberSaveable { mutableStateOf(false) }
    var showIngredients by rememberSaveable { mutableStateOf(false) }
    var technique by rememberSaveable { mutableStateOf<String?>(null) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CookEvent.TimerStarted -> {
                    snackbar.show(resources.getString(R.string.cook_timer_started, durationLabel(resources, event.seconds)))
                }

                CookEvent.TimerFailed -> {
                    snackbar.show(resources.getString(R.string.cook_timer_unsupported))
                }
            }
        }
    }
    // Leaving early never asks: the session is kept for resuming.
    val exit = { if (reachedEnd) askCooked = true else onClose() }
    BackHandler(enabled = !evaluating, onBack = exit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = exit) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cook_close)) }
                },
                actions = {
                    if (!landscape && state.ingredients.isNotEmpty()) {
                        IconButton(onClick = { showIngredients = true }) {
                            Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = stringResource(R.string.cook_ingredients))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> {
                    Unit
                }

                state.notFound -> {
                    EmptyState(title = stringResource(R.string.error_not_found))
                }

                state.steps.isEmpty() -> {
                    EmptyState(title = stringResource(R.string.cook_no_steps))
                }

                landscape -> {
                    Row(Modifier.fillMaxSize()) {
                        StepPane(
                            state,
                            viewModel,
                            onTechnique = { technique = it.id },
                            onFinish = { evaluating = true },
                            Modifier.weight(1f),
                        )
                        VerticalDivider()
                        IngredientChecklist(
                            state.ingredients,
                            viewModel::toggleIngredient,
                            Modifier
                                .weight(0.8f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }

                else -> {
                    StepPane(
                        state,
                        viewModel,
                        onTechnique = { technique = it.id },
                        onFinish = { evaluating = true },
                        Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showIngredients) {
        ModalBottomSheet(onDismissRequest = { showIngredients = false }) {
            Text(
                stringResource(R.string.cook_ingredients),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp).semantics { heading() },
            )
            IngredientChecklist(
                state.ingredients,
                viewModel::toggleIngredient,
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            )
        }
    }
    state.steps
        .flatMap { it.techniques }
        .firstOrNull { it.id == technique }
        ?.let { TechniqueSheet(it, onDismiss = { technique = null }) }
    if (askCooked) {
        AlertDialog(
            onDismissRequest = { askCooked = false },
            title = { Text(stringResource(R.string.cook_exit_prompt)) },
            confirmButton = {
                TextButton(onClick = {
                    askCooked = false
                    evaluating = true
                }) { Text(stringResource(R.string.cook_exit_log)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askCooked = false
                    onClose()
                }) { Text(stringResource(R.string.cook_exit_not_now)) }
            },
        )
    }
    if (evaluating) {
        EvaluationSheet(recipeId = viewModel.recipeId, onDismiss = { evaluating = false }, onLogged = onLogged)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPane(
    state: CookUiState,
    viewModel: CookViewModel,
    onTechnique: (Technique) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState(initialPage = state.stepIndex) { state.steps.size }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect(viewModel::goTo) }
    // The session moved elsewhere (another device rotation, a restored session): follow it.
    LaunchedEffect(state.stepIndex) { if (pager.settledPage != state.stepIndex) pager.scrollToPage(state.stepIndex) }

    Column(modifier) {
        if (state.timersSupported && state.timers.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                items(state.timers, key = { "${it.timer.stepIndex}-${it.timer.startedAt}" }) { running ->
                    TimerChip(running.chip, onClick = viewModel::showTimers)
                }
            }
        }
        if (state.showTimerHint) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.cook_timer_unsupported),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::dismissTimerHint) { Text(stringResource(R.string.common_done)) }
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.cook_step_progress, pager.currentPage + 1, state.steps.size),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(progress = { (pager.currentPage + 1f) / state.steps.size }, modifier = Modifier.fillMaxWidth())
        }
        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { state.steps[it].index }) { page ->
            val step = state.steps[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                step.groupName?.let { Eyebrow(it) }
                Text(step.text, style = MaterialTheme.typography.headlineSmall)
                if (state.timersSupported && step.durations.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        step.durations.forEach { duration ->
                            val description = stringResource(R.string.cook_timer_start, durationLabel(duration.seconds))
                            TimerChip(
                                TimerChipState.Idle(duration.seconds),
                                onClick = {
                                    viewModel.startTimer(
                                        step.index,
                                        duration.seconds,
                                        resources.getString(R.string.cook_timer_message, step.index + 1),
                                    )
                                },
                                modifier = Modifier.semantics { this.contentDescription = description },
                            )
                        }
                    }
                }
                if (step.techniques.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        step.techniques.forEach { technique ->
                            AssistChip(
                                onClick = { onTechnique(technique) },
                                label = { Text(stringResource(R.string.cook_technique_info, technique.nl)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.cook_previous), Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (pager.currentPage == state.steps.lastIndex) {
                Button(onClick = onFinish, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.cook_finish_btn), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.cook_next), Modifier.padding(end = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

/** Scaled ingredients with ticks shared with recipe detail (spec §6.8). */
@Composable
internal fun IngredientChecklist(
    groups: List<IngredientGroupLines>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        groups.forEach { group ->
            group.name?.let { Eyebrow(it, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)) }
            group.lines.forEach { line ->
                Row(
                    Modifier
                        .toggleable(value = line.checked, role = Role.Checkbox, onValueChange = { onToggle(line.id) })
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = line.checked, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
                    Text(
                        line.display,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (line.checked) MiseTheme.colors.inkSoft else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (line.checked) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechniqueSheet(
    technique: Technique,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(technique.nl, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(technique.subtitle, style = MaterialTheme.typography.bodyLarge, color = MiseTheme.colors.inkSoft)
            technique.body.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            technique.tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
