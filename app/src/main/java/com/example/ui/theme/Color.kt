package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ---------------------------------------------------------------------------
// Theme palettes. Every colour token below reads from the active palette, so
// switching ThemeState.current re-colours the whole app instantly.
// ---------------------------------------------------------------------------

data class CinedorPalette(
    val name: String,
    val description: String,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val border: Color,
    val accent: Color,
    val accentDark: Color,
    val accentSoft: Color,
    val blush: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val chipBg: Color,
    val chipBorder: Color
)

val CinedorPalettes: List<CinedorPalette> = listOf(
    CinedorPalette(
        name = "Cinema", description = "Deep black with classic cinema red", isDark = true,
        background = Color(0xFF0E0C0D), surface = Color(0xFF171415), surfaceVariant = Color(0xFF221D1E),
        card = Color(0xFF1C1819), border = Color(0xFF2F2829),
        accent = Color(0xFFE50914), accentDark = Color(0xFFB00610), accentSoft = Color(0xFF3A1416),
        blush = Color(0xFF1A1012),
        textPrimary = Color(0xFFF5F2F2), textSecondary = Color(0xFFB3AAAB), textMuted = Color(0xFF7D7476),
        chipBg = Color(0xFF221D1E), chipBorder = Color(0xFF3A3233)
    ),
    CinedorPalette(
        name = "Midnight", description = "Navy night with electric blue", isDark = true,
        background = Color(0xFF0B0F1A), surface = Color(0xFF121828), surfaceVariant = Color(0xFF1A2238),
        card = Color(0xFF161D2F), border = Color(0xFF26304A),
        accent = Color(0xFF3B82F6), accentDark = Color(0xFF1D4ED8), accentSoft = Color(0xFF14234A),
        blush = Color(0xFF0E1527),
        textPrimary = Color(0xFFEEF2FF), textSecondary = Color(0xFFA6B0C8), textMuted = Color(0xFF6F7A94),
        chipBg = Color(0xFF1A2238), chipBorder = Color(0xFF2A3552)
    ),
    CinedorPalette(
        name = "Emerald", description = "Dark forest with fresh green", isDark = true,
        background = Color(0xFF0A110E), surface = Color(0xFF101A15), surfaceVariant = Color(0xFF16241D),
        card = Color(0xFF14201A), border = Color(0xFF22342B),
        accent = Color(0xFF10B981), accentDark = Color(0xFF047857), accentSoft = Color(0xFF0F2A20),
        blush = Color(0xFF0C1712),
        textPrimary = Color(0xFFECFDF5), textSecondary = Color(0xFFA3B8AE), textMuted = Color(0xFF6B8076),
        chipBg = Color(0xFF16241D), chipBorder = Color(0xFF26392F)
    ),
    CinedorPalette(
        name = "Sunset", description = "Warm dusk with glowing orange", isDark = true,
        background = Color(0xFF120D0A), surface = Color(0xFF1B1410), surfaceVariant = Color(0xFF261C16),
        card = Color(0xFF211813), border = Color(0xFF3A2A20),
        accent = Color(0xFFF97316), accentDark = Color(0xFFC2410C), accentSoft = Color(0xFF3A1E0E),
        blush = Color(0xFF1A110B),
        textPrimary = Color(0xFFFFF4EC), textSecondary = Color(0xFFC2AFA3), textMuted = Color(0xFF8A766A),
        chipBg = Color(0xFF261C16), chipBorder = Color(0xFF3E2E24)
    ),
    CinedorPalette(
        name = "Royal", description = "Velvet dark with royal purple", isDark = true,
        background = Color(0xFF0F0B16), surface = Color(0xFF171122), surfaceVariant = Color(0xFF211932),
        card = Color(0xFF1C1529), border = Color(0xFF2F2542),
        accent = Color(0xFFA855F7), accentDark = Color(0xFF7E22CE), accentSoft = Color(0xFF2A1645),
        blush = Color(0xFF150F20),
        textPrimary = Color(0xFFF5EEFF), textSecondary = Color(0xFFB6A9C8), textMuted = Color(0xFF7D7092),
        chipBg = Color(0xFF211932), chipBorder = Color(0xFF352A4A)
    ),
    CinedorPalette(
        name = "Daylight", description = "Bright warm paper, ink text and red accents", isDark = false,
        background = Color(0xFFFBF7F6), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF1EBEA),
        card = Color(0xFFFFFFFF), border = Color(0xFFE4DCDB),
        accent = Color(0xFFD70F1B), accentDark = Color(0xFFA80B14), accentSoft = Color(0xFFF9D9DB),
        blush = Color(0xFFF6E6E6),
        textPrimary = Color(0xFF16171B), textSecondary = Color(0xFF5E636E), textMuted = Color(0xFF8D919B),
        chipBg = Color(0xFFEFE8E7), chipBorder = Color(0xFFD9D0CF)
    )
)

const val DEFAULT_THEME_INDEX = 5

object ThemeState {
    var current by mutableStateOf(CinedorPalettes[DEFAULT_THEME_INDEX])
}

private val P: CinedorPalette get() = ThemeState.current

// Core accent
val CinetorRed: Color get() = P.accent
val CinetorRedDark: Color get() = P.accentDark
val CinetorRedSoft: Color get() = P.accentSoft
val CinetorBlush: Color get() = P.blush
val AccentBright: Color get() = lerp(P.accent, Color.White, 0.10f)

// Chips / sheets / misc
val ChipBg: Color get() = P.chipBg
val ChipBorder: Color get() = P.chipBorder
val SheetBg: Color get() = if (P.isDark) P.surface else Color(0xFFF6F1EF)
val ShimmerBase: Color get() = if (P.isDark) P.surfaceVariant else Color(0xFFEDE6E5)
val RankOutline: Color get() = if (P.isDark) P.textMuted else Color(0xFF8E6B6E)
val NavGlassTop: Color get() = if (P.isDark) P.surface.copy(alpha = 0.97f) else Color.White.copy(alpha = 0.96f)
val NavGlassBottom: Color get() = if (P.isDark) P.background.copy(alpha = 0.95f) else Color(0xFFF7F2F2).copy(alpha = 0.94f)
val IsDarkTheme: Boolean get() = P.isDark

// Content drawn on top of the accent or on top of artwork/video
val OnAccent = Color.White
val OnMediaText = Color.White
val OnMediaTextSecondary = Color(0xFFE2E4EA)

// Dark "media" cards (live TV / sports tiles)
val MediaCardDark = Color(0xFF15171D)
val MediaCardDarkEnd = Color(0xFF0D0E12)

// Surface hierarchy
val ObsidianBackground: Color get() = P.background
val ObsidianSurface: Color get() = P.surface
val ObsidianSurfaceVariant: Color get() = P.surfaceVariant
val ObsidianCard: Color get() = P.card
val ObsidianBorder: Color get() = P.border

// Former logo gradient tokens -> accent family
val CinedorViolet: Color get() = P.accentDark
val CinedorMagenta: Color get() = P.accent
val CinedorPink: Color get() = lerp(P.accent, Color.White, 0.22f)
val CinedorGradientColors: List<Color> get() = listOf(AccentBright, P.accentDark)
val CinedorNavSurface: Color get() = P.surface
val CinedorTintedSurface: Color get() = P.accentSoft

// Semantic accents
val CrimsonPrimary: Color get() = P.accent
val CrimsonPrimaryDark: Color get() = P.accentDark
val CrimsonContainer: Color get() = P.accentSoft

val CinemaGold = Color(0xFFE0A100)
val CinemaGoldContainer: Color get() = if (P.isDark) Color(0xFF2D2206) else Color(0xFFFFF3D6)

val LiveEmerald: Color get() = P.accent
val LiveEmeraldContainer: Color get() = P.accentSoft

val ElectricCyan = Color(0xFF0E8BD1)

// Text
val TextPrimary: Color get() = P.textPrimary
val TextSecondary: Color get() = P.textSecondary
val TextMuted: Color get() = P.textMuted

val ErrorRed: Color get() = P.accent
val ErrorContainer: Color get() = P.accentSoft

// ---------------------------------------------------------------------------
// Fixed dark palette for full-screen video contexts (player, shorts feed)
// ---------------------------------------------------------------------------
val MediaBackground = Color(0xFF0B0C0F)
val MediaSurface = Color(0xFF13151A)
val MediaSurfaceVariant = Color(0xFF1A1D24)
val MediaCard = Color(0xFF1B1E23)
val MediaBorder = Color(0xFF2A2E37)
val MediaTextPrimary = Color(0xFFF6F7FA)
val MediaTextSecondary = Color(0xFFA5AAB5)
val MediaTextMuted = Color(0xFF727785)
