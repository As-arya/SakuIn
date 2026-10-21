package Sakuin.Project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSakuinColors = staticCompositionLocalOf { LightSakuinColors }
private val LocalSakuinTypography = staticCompositionLocalOf { DefaultSakuinTypography }
private val LocalSakuinSpacing = staticCompositionLocalOf { SakuinSpacing() }
private val LocalSakuinRadius = staticCompositionLocalOf { SakuinRadius() }

/** Access point for design tokens; names match the Android code syntax set on the Figma variables. */
object SakuinTheme {
    val colors: SakuinColors
        @Composable @ReadOnlyComposable get() = LocalSakuinColors.current
    val typography: SakuinTypography
        @Composable @ReadOnlyComposable get() = LocalSakuinTypography.current
    val spacing: SakuinSpacing
        @Composable @ReadOnlyComposable get() = LocalSakuinSpacing.current
    val radius: SakuinRadius
        @Composable @ReadOnlyComposable get() = LocalSakuinRadius.current
}

@Composable
fun SakuinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkSakuinColors else LightSakuinColors
    val typography = DefaultSakuinTypography
    val radius = SakuinRadius()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.bgApp,
            onBackground = colors.textPrimary,
            surface = colors.bgSurface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.bgSurfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.border,
            error = colors.expense,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.bgApp,
            onBackground = colors.textPrimary,
            surface = colors.bgSurface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.bgSurfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.border,
            error = colors.expense,
        )
    }

    CompositionLocalProvider(
        LocalSakuinColors provides colors,
        LocalSakuinTypography provides typography,
        LocalSakuinSpacing provides SakuinSpacing(),
        LocalSakuinRadius provides radius,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography.toMaterial(),
            shapes = radius.toMaterialShapes(),
            content = content,
        )
    }
}
