package nl.potentplum.mise.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.potentplum.mise.R
import nl.potentplum.mise.core.designsystem.MiseTheme
import nl.potentplum.mise.core.designsystem.hexColor

data class VegTokenData(
    val id: String,
    val name: String,
    val color: Color,
)

/**
 * Dagboek › Inzichten "Dit seizoen" (spec §6.13): the count of vegetables cooked at peak, without a denominator, the
 * cooked vegetables, and up to three to try next.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeasonTally(
    count: Int,
    cooked: List<VegTokenData>,
    toTry: List<VegTokenData>,
    onVegetable: (String) -> Unit,
    onAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(pluralStringResource(R.plurals.journal_season_count, count, count), style = MaterialTheme.typography.headlineSmall)
        if (cooked.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cooked.forEach { VegToken(it.name, it.color, size = 28.dp, nameStyle = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (toTry.isNotEmpty()) {
            Eyebrow(stringResource(R.string.journal_season_to_try))
            toTry.take(3).forEach { vegetable ->
                Surface(onClick = { onVegetable(vegetable.id) }, color = Color.Transparent) {
                    VegToken(vegetable.name, vegetable.color, Modifier.padding(vertical = 4.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAll) { Text(stringResource(R.string.journal_season_all)) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4EFE6, widthDp = 360)
@Composable
private fun SeasonTallyPreview() {
    MiseTheme {
        SeasonTally(
            count = 5,
            cooked =
                listOf(
                    VegTokenData("pumpkin", "Pompoen", hexColor("#D9822B")),
                    VegTokenData("kale", "Boerenkool", hexColor("#3E5A3A")),
                    VegTokenData("leek", "Prei", hexColor("#B8C99A")),
                ),
            toTry =
                listOf(
                    VegTokenData("parsnip", "Pastinaak", hexColor("#E8DCC0")),
                    VegTokenData("celeriac", "Knolselderij", hexColor("#DCD3BC")),
                ),
            onVegetable = {},
            onAll = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
