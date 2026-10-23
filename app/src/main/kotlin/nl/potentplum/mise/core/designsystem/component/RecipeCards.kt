package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.CuratedRecipe
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.domain.Difficulty

/** "25 min" with a clock icon; nothing when the time is unknown. */
@Composable
fun TotalTime(
    minutes: Int?,
    modifier: Modifier = Modifier,
) {
    if (minutes == null) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.minutes_short, minutes), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MiseCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

/** A Vandaag suggestion (spec §6.2): title, total time, the reason it is suggested, and two actions. */
@Composable
fun SuggestionCard(
    title: String,
    reason: String,
    totalMinutes: Int?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onCookTonight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiseCard(onClick = onOpen, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TotalTime(totalMinutes)
            if (reason.isNotBlank()) {
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.today_dismiss)) }
                OutlinedButton(onClick = onCookTonight) { Text(stringResource(R.string.today_tonight_action)) }
            }
        }
    }
}

/** The Vanavond pin on Vandaag (spec §6.2). */
@Composable
fun TonightCard(
    title: String,
    totalMinutes: Int?,
    onOpen: () -> Unit,
    onCook: () -> Unit,
    onAddToShopping: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MiseCard(onClick = onOpen, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(stringResource(R.string.today_tonight_title), color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            TotalTime(totalMinutes)
            Button(onClick = onCook, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.recipes_cooking_mode)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddToShopping, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipe_add_to_shopping), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = onUnpin) { Text(stringResource(R.string.today_tonight_unpin)) }
            }
        }
    }
}

/** A curated link from the content (spec §6.4); opens in the browser. */
@Composable
fun CuratedRecipeCard(
    recipe: CuratedRecipe,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (eyebrow != null) Eyebrow(eyebrow, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(R.string.detail_open_link),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                stringResource(R.string.detail_curated_meta, recipe.source, recipe.timeMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MiseTheme.colors.inkSoft,
            )
            Text(recipe.why, style = MaterialTheme.typography.bodySmall)
            DifficultyBadge(Difficulty.of(recipe.difficulty))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun RecipeCardsPreview() {
    MiseTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TonightCard(
                title = "Pompoenrisotto met salie",
                totalMinutes = 45,
                onOpen = {},
                onCook = {},
                onAddToShopping = {},
                onUnpin = {},
            )
            SuggestionCard(
                title = "Stamppot boerenkool met rookworst",
                reason = "Boerenkool en pastinaak zijn op hun best · 6 weken niet gekookt",
                totalMinutes = 40,
                onOpen = {},
                onDismiss = {},
                onCookTonight = {},
            )
        }
    }
}
