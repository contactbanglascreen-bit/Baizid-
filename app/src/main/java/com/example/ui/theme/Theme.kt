package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val ColorAlphaOutline = ObsidianBorder.copy(alpha = 0.7f)

private val CinePulseLightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = OnAccent,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = CrimsonPrimaryDark,
    secondary = CinedorViolet,
    onSecondary = OnAccent,
    secondaryContainer = CinemaGoldContainer,
    onSecondaryContainer = CinemaGold,
    tertiary = LiveEmerald,
    onTertiary = OnAccent,
    tertiaryContainer = LiveEmeraldContainer,
    onTertiaryContainer = LiveEmerald,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianCard,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianBorder,
    outlineVariant = ColorAlphaOutline,
    error = ErrorRed,
    onError = OnAccent,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimary
)

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
    // Keep layouts compact even when the phone uses a very large system font size.
    val systemDensity = LocalDensity.current
    val cappedDensity = Density(
        density = systemDensity.density,
        fontScale = systemDensity.fontScale.coerceAtMost(1.0f)
    )
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        MaterialTheme(
            colorScheme = CinePulseLightColorScheme,
            typography = CinePulseTypography,
            shapes = CinePulseShapes,
            content = content
        )
    }
}
