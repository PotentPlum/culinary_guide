package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.Contrast
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.hexColor
import java.io.File

/**
 * A library card (spec §6.7): image or a cuisine-coloured placeholder, title, source, total time, cooked count and the
 * vegetables in it that are in season now.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(
    title: String,
    source: String,
    totalMinutes: Int?,
    cookedCount: Int,
    imagePath: String?,
    placeholderColor: String?,
    isFavorite: Boolean,
    inSeason: List<VegTokenData>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        RecipeImage(imagePath, placeholderColor, Modifier.fillMaxWidth().height(140.dp))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavorite) Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (source.isNotEmpty()) {
                    Text(source, style = MaterialTheme.typography.bodySmall, color = MiseTheme.colors.inkSoft, maxLines = 1)
                }
                TotalTime(totalMinutes)
                if (cookedCount > 0) {
                    Text(
                        pluralStringResource(R.plurals.recipes_cooked_count, cookedCount, cookedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (inSeason.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    inSeason.take(4).forEach { VegToken(it.name, it.color, size = 20.dp, nameStyle = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

/** The recipe's stored image, or a placeholder in the cuisine colour (or the warm surface) with a decorative icon. */
@Composable
fun RecipeImage(
    imagePath: String?,
    placeholderColor: String?,
    modifier: Modifier = Modifier,
) {
    if (imagePath != null) {
        AsyncImage(model = File(imagePath), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        val fill = placeholderColor?.let(::hexColor) ?: MaterialTheme.colorScheme.surfaceContainerHigh
        Box(modifier.background(fill), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Contrast.inkOrWhiteOn(fill).copy(alpha = 0.6f))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun RecipeCardPreview() {
    MiseTheme {
        RecipeCard(
            title = "Stamppot boerenkool met rookworst",
            source = "leukerecepten.nl",
            totalMinutes = 40,
            cookedCount = 3,
            imagePath = null,
            placeholderColor = "#2A6BB9",
            isFavorite = true,
            inSeason =
                listOf(
                    VegTokenData("kale", "Boerenkool", hexColor("#3E5A3A")),
                    VegTokenData("potato", "Aardappel", Color(0xFFD8C08A)),
                ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
