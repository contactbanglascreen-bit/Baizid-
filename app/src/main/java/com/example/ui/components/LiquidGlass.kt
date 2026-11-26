package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid Glass look for the whole app.
 *
 * - [glassBackground]: the soft colour lights behind every screen. They are plain radial gradients
 *   drawn once, so they cost nothing while scrolling, and they are what makes the see-through
 *   cards look like glass.
 * - [liquidGlass]: real background blur (Android 12+) for the few floating bars, with a frosted
 *   tint instead on older phones so every phone stays smooth.
 * - [glassRim]: the thin bright edge light that real glass has.
 */
object LiquidGlass {
    /** The screen content the floating bars blur. Set once in MainActivity. */
    val LocalState = staticCompositionLocalOf<HazeState?> { null }

    /** Real blur is cheap from Android 12 (RenderEffect); older phones get the frosted tint. */
    val realBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/** Soft accent-coloured lights over the theme background (static, drawn once). */
fun Modifier.glassBackground(): Modifier = drawWithCache {
    val p = ThemeState.current
    val base = p.background
    val w = size.width
    val h = size.height
    val r = maxOf(w, h)
    val lightA = if (p.isDark) 0.30f else 0.22f
    val lightB = if (p.isDark) 0.22f else 0.16f
    val third = lerp(p.accent, Color(0xFF6A4BFF), 0.55f)
    val glowA = Brush.radialGradient(
        listOf(p.accent.copy(alpha = lightA), Color.Transparent),
        center = Offset(w * 0.05f, h * 0.02f), radius = r * 0.75f
    )
    val glowB = Brush.radialGradient(
        listOf(p.accentDark.copy(alpha = lightB), Color.Transparent),
        center = Offset(w * 1.0f, h * 0.45f), radius = r * 0.70f
    )
    val glowC = Brush.radialGradient(
        listOf(third.copy(alpha = lightB * 0.8f), Color.Transparent),
        center = Offset(w * 0.15f, h * 0.95f), radius = r * 0.65f
    )
    onDrawBehind {
        drawRect(base)
        drawRect(glowA)
        drawRect(glowB)
        drawRect(glowC)
    }
}

/** Thin bright rim: strong light on the top-left edge, a softer one bottom-right. */
fun Modifier.glassRim(shape: Shape, strength: Float = 1f): Modifier {
    val dark = ThemeState.current.isDark
    val hi = (if (dark) 0.42f else 0.95f) * strength
    val lo = (if (dark) 0.10f else 0.45f) * strength
    return border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = hi), Color.White.copy(alpha = lo * 0.4f), Color.White.copy(alpha = lo))
        ),
        shape = shape
    )
}

/** Frosted glass fill without blur (cheap, for cards, chips and dialogs). */
fun Modifier.glassFill(shape: Shape, opacity: Float = 0f): Modifier {
    val p = ThemeState.current
    val top: Color
    val bottom: Color
    if (p.isDark) {
        top = lerp(p.surface, Color.White, 0.10f).copy(alpha = 0.62f + opacity)
        bottom = lerp(p.surface, Color.White, 0.03f).copy(alpha = 0.52f + opacity)
    } else {
        top = Color.White.copy(alpha = 0.78f + opacity)
        bottom = Color.White.copy(alpha = 0.58f + opacity)
    }
    return clip(shape).background(Brush.verticalGradient(listOf(top, bottom)))
}

/**
 * Real Liquid Glass for floating bars: blurs whatever scrolls behind it (Android 12+), with a
 * light tint and bright rim. Without blur support it falls back to [glassFill].
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    state: HazeState? = LiquidGlass.LocalState.current,
    rim: Boolean = true
): Modifier {
    val p = ThemeState.current
    val glass = if (state != null && LiquidGlass.realBlur) {
        val tint = if (p.isDark) lerp(p.surface, Color.White, 0.06f).copy(alpha = 0.46f) else Color.White.copy(alpha = 0.55f)
        clip(shape).hazeEffect(
            state = state,
            style = HazeStyle(
                backgroundColor = p.background,
                tints = listOf(HazeTint(tint)),
                blurRadius = 22.dp,
                noiseFactor = 0.04f
            )
        )
    } else {
        glassFill(shape, opacity = 0.22f)
    }
    return if (rim) glass.glassRim(shape) else glass
}

/**
 * Call first inside a Dialog: on Android 12+ the phone itself blurs the app behind the popup
 * (done by the system compositor, so it costs the app nothing). Older phones keep the normal dim.
 */
@Composable
fun DialogBackdropBlur(radius: Int = 48) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window ?: return@SideEffect
        runCatching {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
    }
}
