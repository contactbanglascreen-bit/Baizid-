package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
    val realBlur: Boolean get() = !forceFrosted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Tests set this to draw the frosted (no-blur) look that older phones get. */
    var forceFrosted = false
}

/** Soft colour lights over the theme background (static, drawn once). Glass needs colour behind it. */
fun Modifier.glassBackground(): Modifier = drawWithCache {
    val p = ThemeState.current
    val base = p.background
    val w = size.width
    val h = size.height
    val r = maxOf(w, h)
    val dark = p.isDark
    val violet = Color(0xFF7B5CFF)
    val sky = Color(0xFF2EA8FF)
    val peach = Color(0xFFFF9A62)
    fun glow(c: Color, a: Float, x: Float, y: Float, rad: Float) = Brush.radialGradient(
        listOf(c.copy(alpha = a), c.copy(alpha = a * 0.35f), Color.Transparent),
        center = Offset(w * x, h * y), radius = r * rad
    )
    val k = if (dark) 1f else 0.85f
    val lights = listOf(
        glow(p.accent, 0.42f * k, 0.02f, 0.04f, 0.62f),
        glow(violet, 0.30f * k, 1.02f, 0.30f, 0.58f),
        glow(peach, 0.26f * k, 0.10f, 0.62f, 0.52f),
        glow(sky, 0.24f * k, 0.95f, 0.88f, 0.55f),
        glow(p.accentDark, 0.28f * k, 0.45f, 1.05f, 0.50f)
    )
    onDrawBehind {
        drawRect(base)
        lights.forEach { drawRect(it) }
    }
}

/** Thin bright rim: strong light on the top-left edge, a softer one bottom-right. */
fun Modifier.glassRim(shape: Shape, strength: Float = 1f): Modifier {
    val dark = ThemeState.current.isDark
    val hi = (if (dark) 0.42f else 0.95f) * strength
    val lo = (if (dark) 0.10f else 0.45f) * strength
    // On light themes a white rim alone disappears against white; a hairline shadow edge keeps
    // the glass shape visible.
    val base = if (dark) this else border(0.6.dp, Color.Black.copy(alpha = 0.07f * strength), shape)
    return base.border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = hi), Color.White.copy(alpha = lo * 0.4f), Color.White.copy(alpha = lo))
        ),
        shape = shape
    )
}

/** Glossy top sheen: the light reflection across the upper part of real glass. */
fun Modifier.glassSheen(strength: Float = 1f): Modifier = drawBehind {
    val dark = ThemeState.current.isDark
    val a = (if (dark) 0.16f else 0.55f) * strength
    drawRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = a), Color.White.copy(alpha = a * 0.25f), Color.Transparent),
            startY = 0f, endY = size.height * 0.62f
        )
    )
}

/** A small round glass button or chip: frosted fill, glossy sheen and bright rim. */
fun Modifier.glassBubble(shape: Shape): Modifier {
    val dark = ThemeState.current.isDark
    val fill = if (dark) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.55f)))
    return clip(shape).background(fill).glassSheen(0.8f).glassRim(shape)
}

/** Coloured (selected) glass: the accent colour behind glass, with the same shine and rim. */
fun Modifier.glassAccent(shape: Shape): Modifier {
    val p = ThemeState.current
    val fill = Brush.verticalGradient(listOf(lerp(p.accent, Color.White, 0.18f), p.accent, p.accentDark))
    return clip(shape).background(fill).drawBehind {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f), Color.Transparent),
                endY = size.height * 0.55f
            )
        )
    }.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.1f))), shape)
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
    return clip(shape).background(Brush.verticalGradient(listOf(top, bottom))).glassSheen(0.6f)
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
    val shiny = glass.glassSheen(0.7f)
    return if (rim) shiny.glassRim(shape) else shiny
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
