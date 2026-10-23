package nl.potentplum.mise.feature.techniques

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.potentplum.mise.R
import nl.potentplum.mise.content.model.HeatLevel
import nl.potentplum.mise.content.model.TechniqueCategory

@Composable
fun categoryLabel(category: TechniqueCategory): String =
    stringResource(
        when (category) {
            TechniqueCategory.SNIJDEN -> R.string.techniques_category_snijden
            TechniqueCategory.GARING -> R.string.techniques_category_garing
            TechniqueCategory.SMAAK -> R.string.techniques_category_smaak
            TechniqueCategory.EIWITTEN -> R.string.techniques_category_eiwitten
            TechniqueCategory.SAUZEN -> R.string.techniques_category_sauzen
            TechniqueCategory.GROENTEN -> R.string.techniques_category_groenten
            TechniqueCategory.GEREEDSCHAP -> R.string.techniques_category_gereedschap
            TechniqueCategory.BEWAREN -> R.string.techniques_category_bewaren
        },
    )

@Composable
fun heatLabel(heat: HeatLevel): String =
    stringResource(
        when (heat) {
            HeatLevel.HOOG -> R.string.techniques_heat_hoog
            HeatLevel.MIDDEL -> R.string.techniques_heat_middel
            HeatLevel.LAAG -> R.string.techniques_heat_laag
            HeatLevel.GEEN -> R.string.techniques_heat_geen
        },
    )
