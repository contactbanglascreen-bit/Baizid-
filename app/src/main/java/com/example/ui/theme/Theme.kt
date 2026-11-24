package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CinePulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CinePulseTheme(
    content: @Composable () -> Unit
) {
    val p = ThemeState.current
    val colorScheme = if (p.isDark) {
        darkColorScheme(
            primary = p.accent,
            onPrimary = OnAccent,
            primaryContainer = p.accentSoft,
            onPrimaryContainer = p.textPrimary,
            secondary = p.accentDark,
            onSecondary = OnAccent,
            tertiary = p.accent,
            onTertiary = OnAccent,
            background = p.background,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            surfaceVariant = p.surfaceVariant,
            onSurfaceVariant = p.textSecondary,
            surfaceContainerLowest = p.background,
            surfaceContainerLow = p.surface,
            surfaceContainer = p.surfaceVariant,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.border,
            outline = p.border,
            outlineVariant = p.border.copy(alpha = 0.7f),
            error = p.accent,
            onError = OnAccent,
            errorContainer = p.accentSoft,
            onErrorContainer = p.textPrimary
        )
    } else {
        lightColorScheme(
            primary = p.accent,
            onPrimary = OnAccent,
            primaryContainer = p.accentSoft,
            onPrimaryContainer = p.accentDark,
            secondary = p.accentDark,
            onSecondary = OnAccent,
            tertiary = p.accent,
            onTertiary = OnAccent,
            background = p.background,
            onBackground = p.textPrimary,
            surface = p.surface,
            onSurface = p.textPrimary,
            surfaceVariant = p.surfaceVariant,
            onSurfaceVariant = p.textSecondary,
            surfaceContainerLowest = p.background,
            surfaceContainerLow = p.surface,
            surfaceContainer = p.surfaceVariant,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.border,
            outline = p.border,
            outlineVariant = p.border.copy(alpha = 0.7f),
            error = p.accent,
            onError = OnAccent,
            errorContainer = p.accentSoft,
            onErrorContainer = p.textPrimary
        )
    }

    // Status / navigation bar icons follow the theme (dark icons on light themes).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !p.isDark
            controller.isAppearanceLightNavigationBars = !p.isDark
        }
    }

    // Keep layouts compact even when the phone uses a very large system font size.
    val systemDensity = LocalDensity.current
    val cappedDensity = Density(
        density = systemDensity.density,
        fontScale = systemDensity.fontScale.coerceAtMost(1.0f)
    )
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CinePulseTypography,
            shapes = CinePulseShapes,
            content = content
        )
    }
}
