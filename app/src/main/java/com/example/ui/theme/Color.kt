package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Cinetor-style light theme: warm off-white surfaces + bold cinema red accent.
// Token names are kept from the previous dark theme so every screen picks the
// new look up automatically.
// ---------------------------------------------------------------------------

// Core red accent
val CinetorRed = Color(0xFFD70F1B)
val CinetorRedDark = Color(0xFFA80B14)
val CinetorRedSoft = Color(0xFFF9D9DB)
val CinetorBlush = Color(0xFFF6E6E6)

// Content drawn on top of the red accent or on top of artwork/video
val OnAccent = Color.White
val OnMediaText = Color.White
val OnMediaTextSecondary = Color(0xFFE2E4EA)

// Dark "media" cards (live TV / sports tiles in the reference design)
val MediaCardDark = Color(0xFF15171D)
val MediaCardDarkEnd = Color(0xFF0D0E12)

// Surface hierarchy (light)
val ObsidianBackground = Color(0xFFFBF7F6)
val ObsidianSurface = Color(0xFFFFFFFF)
val ObsidianSurfaceVariant = Color(0xFFF1EBEA)
val ObsidianCard = Color(0xFFFFFFFF)
val ObsidianBorder = Color(0xFFE4DCDB)

// Former logo gradient tokens -> red family
val CinedorViolet = CinetorRedDark
val CinedorMagenta = CinetorRed
val CinedorPink = Color(0xFFEF3A45)
val CinedorGradientColors = listOf(Color(0xFFE5121F), CinetorRedDark)
val CinedorNavSurface = Color(0xFFFFFFFF)
val CinedorTintedSurface = CinetorRedSoft

// Semantic accents
val CrimsonPrimary = CinetorRed
val CrimsonPrimaryDark = CinetorRedDark
val CrimsonContainer = CinetorRedSoft

val CinemaGold = Color(0xFFE0A100)
val CinemaGoldContainer = Color(0xFFFFF3D6)

val LiveEmerald = CinetorRed
val LiveEmeraldContainer = CinetorRedSoft

val ElectricCyan = Color(0xFF0E8BD1)

// Text (dark on light)
val TextPrimary = Color(0xFF16171B)
val TextSecondary = Color(0xFF5E636E)
val TextMuted = Color(0xFF8D919B)

val ErrorRed = Color(0xFFD70F1B)
val ErrorContainer = Color(0xFFFDE3E5)

// ---------------------------------------------------------------------------
// Dark palette kept for full-screen video contexts (player, shorts feed),
// where content sits on black video.
// ---------------------------------------------------------------------------
val MediaBackground = Color(0xFF0B0C0F)
val MediaSurface = Color(0xFF13151A)
val MediaSurfaceVariant = Color(0xFF1A1D24)
val MediaCard = Color(0xFF1B1E23)
val MediaBorder = Color(0xFF2A2E37)
val MediaTextPrimary = Color(0xFFF6F7FA)
val MediaTextSecondary = Color(0xFFA5AAB5)
val MediaTextMuted = Color(0xFF727785)
