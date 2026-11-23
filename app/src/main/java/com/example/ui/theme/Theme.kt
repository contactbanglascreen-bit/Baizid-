package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorAlphaOutline = ObsidianBorder.copy(alpha = 0.7f)

private val CinePulseDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = TextPrimary,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = TextPrimary,
    secondary = CinedorViolet,
    onSecondary = TextPrimary,
    secondaryContainer = CinemaGoldContainer,
    onSecondaryContainer = CinemaGold,
    tertiary = LiveEmerald,
    onTertiary = ObsidianBackground,
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
    onError = ObsidianBackground,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimary
)

private val CinePulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun CinePulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinePulseDarkColorScheme,
        typography = CinePulseTypography,
        shapes = CinePulseShapes,
        content = content
    )
}
