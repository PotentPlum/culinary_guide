package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.Difficulty

@Composable
fun difficultyLabel(difficulty: Difficulty): String =
    stringResource(
        when (difficulty) {
            Difficulty.BEGINNER -> R.string.level_basis
            Difficulty.INTERMEDIATE -> R.string.level_gemiddeld
            Difficulty.ADVANCED -> R.string.level_gevorderd
        },
    )

/** Basis / Gemiddeld / Gevorderd with one to three filled pips (decorative; the label carries the meaning). */
@Composable
fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = ink,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { index ->
                    val filled = index <= difficulty.ordinal
                    Box(
                        Modifier
                            .size(6.dp)
                            .then(if (filled) Modifier.background(ink, CircleShape) else Modifier.border(1.dp, ink, CircleShape)),
                    )
                }
            }
            Text(difficultyLabel(difficulty), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6)
@Composable
private fun DifficultyBadgePreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { DifficultyBadge(it) }
        }
    }
}
