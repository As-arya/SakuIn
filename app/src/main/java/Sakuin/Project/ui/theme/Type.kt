package Sakuin.Project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import Sakuin.Project.R

private fun plusJakartaSans(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

// Plus Jakarta Sans variable font (OFL, from google/fonts). The weight axis is applied on
// API 26+; older devices render the font's default instance.
internal val SakuinFontFamily = FontFamily(
    plusJakartaSans(FontWeight.Normal),
    plusJakartaSans(FontWeight.Medium),
    plusJakartaSans(FontWeight.SemiBold),
    plusJakartaSans(FontWeight.Bold),
    plusJakartaSans(FontWeight.ExtraBold),
)

/** Text styles, mirrored from the Figma text styles. */
@Immutable
data class SakuinTypography(
    val displayAmount: TextStyle,
    val headlineLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyMedium: TextStyle,
    val bodyStrong: TextStyle,
    val labelMedium: TextStyle,
    val caption: TextStyle,
)

private fun style(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = SakuinFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

internal val DefaultSakuinTypography = SakuinTypography(
    displayAmount = style(FontWeight.ExtraBold, 32, 40, -0.5f),
    headlineLarge = style(FontWeight.Bold, 24, 32, -0.25f),
    titleMedium = style(FontWeight.SemiBold, 18, 26),
    titleSmall = style(FontWeight.SemiBold, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodyStrong = style(FontWeight.SemiBold, 14, 20),
    labelMedium = style(FontWeight.Medium, 13, 18),
    caption = style(FontWeight.Medium, 12, 16, 0.2f),
)

/** Maps the Sakuin styles onto Material 3 so stock components look consistent. */
internal fun SakuinTypography.toMaterial() = Typography(
    displaySmall = displayAmount,
    headlineSmall = headlineLarge,
    titleLarge = titleMedium,
    titleMedium = titleSmall,
    titleSmall = bodyStrong,
    bodyLarge = bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = bodyMedium,
    bodySmall = caption,
    labelLarge = bodyStrong,
    labelMedium = labelMedium,
    labelSmall = caption,
)
