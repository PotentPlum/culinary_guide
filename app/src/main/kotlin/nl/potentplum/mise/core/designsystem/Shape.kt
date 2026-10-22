package nl.potentplum.mise.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radius 8 / 12 / 14 / 20 dp (spec §5); pills use [CircleShape][androidx.compose.foundation.shape.CircleShape]. */
val MiseShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
