package Sakuin.Project.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale, mirrored from the Figma "Dimension" collection (4pt grid). */
@Immutable
data class SakuinSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

/** Corner radii, mirrored from the Figma "Dimension" collection. */
@Immutable
data class SakuinRadius(
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

internal fun SakuinRadius.toMaterialShapes() = Shapes(
    extraSmall = RoundedCornerShape(sm),
    small = RoundedCornerShape(md),
    medium = RoundedCornerShape(lg),
    large = RoundedCornerShape(xl),
    extraLarge = RoundedCornerShape(xl),
)
