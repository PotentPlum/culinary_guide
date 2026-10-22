package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.ScoreSteps

/**
 * Five labelled steps, one tap each (spec §6.12): Mislukt · Matig · Oké · Goed · Top, reported as 2/4/6/8/10.
 * Tapping the selected step clears it. A legacy [value] (e.g. 7) highlights its nearest step and is kept until a tap.
 */
@Composable
fun ScoreScale(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = stringArrayResource(R.array.score_steps)
    val selectedStep = value?.let(ScoreSteps::stepOf)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            steps.forEachIndexed { step, stepLabel ->
                val selected = step == selectedStep
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier =
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onValueChange(if (selected) null else ScoreSteps.valueOf(step)) },
                            ),
                ) {
                    Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                        Text(stepLabel, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun ScoreScalePreview() {
    MiseTheme {
        var execution by remember { mutableStateOf<Int?>(8) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScoreScale(label = "Uitvoering", value = execution, onValueChange = { execution = it })
            ScoreScale(label = "Smaak", value = 7, onValueChange = {})
        }
    }
}
