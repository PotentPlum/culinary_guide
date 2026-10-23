package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme

sealed interface TimerChipState {
    /** A duration detected in a step; tapping starts a Clock-app timer. */
    data class Idle(
        val minutes: Int,
    ) : TimerChipState

    /** Started from step [step]; [remainingMinutes] is a local estimate, the Clock app rings. */
    data class Running(
        val step: Int,
        val remainingMinutes: Int,
    ) : TimerChipState

    data class Done(
        val step: Int,
    ) : TimerChipState
}

/** A timer chip in cook mode (spec §6.10): idle "12 min", running "Stap 4 · nog ~9 min", done "Stap 4 · klaar". */
@Composable
fun TimerChip(
    state: TimerChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, icon) =
        when (state) {
            is TimerChipState.Idle -> {
                stringResource(R.string.minutes_short, state.minutes) to Icons.Outlined.Timer
            }

            is TimerChipState.Running -> {
                stringResource(R.string.cook_timer_running, state.step, state.remainingMinutes) to Icons.Outlined.AlarmOn
            }

            is TimerChipState.Done -> {
                stringResource(R.string.cook_timer_done, state.step) to Icons.Outlined.CheckCircle
            }
        }
    val active = state !is TimerChipState.Idle
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = modifier,
        colors =
            if (active) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary,
                )
            } else {
                AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary)
            },
    )
}

/**
 * The sticky bottom bar of recipe detail (spec §6.8): Kookmodus, or "Verder bij stap n" when a cook session for this
 * recipe is open ([resumeStep] is 1-based), and Gekookt.
 */
@Composable
fun RecipeActionBar(
    resumeStep: Int?,
    onCook: () -> Unit,
    onCooked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onCook, modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            if (resumeStep != null) {
                                stringResource(R.string.cook_resume, resumeStep)
                            } else {
                                stringResource(R.string.recipes_cooking_mode)
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(onClick = onCooked) { Text(stringResource(R.string.recipe_action_cooked)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun CookControlsPreview() {
    MiseTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerChip(TimerChipState.Idle(12), onClick = {})
                TimerChip(TimerChipState.Running(step = 4, remainingMinutes = 9), onClick = {})
                TimerChip(TimerChipState.Done(step = 2), onClick = {})
            }
            RecipeActionBar(resumeStep = null, onCook = {}, onCooked = {})
            RecipeActionBar(resumeStep = 3, onCook = {}, onCooked = {})
        }
    }
}
