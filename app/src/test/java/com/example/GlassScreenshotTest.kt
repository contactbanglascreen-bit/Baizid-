package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import com.example.data.model.HomeFeed
import com.example.data.model.HomeSection
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import com.example.ui.components.CinedorNavDestination
import com.example.ui.components.CinedorPremium3DNavBar
import com.example.ui.components.LiquidGlass
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.CinePulseTheme
import com.example.ui.theme.CinedorPalettes
import com.example.ui.theme.ThemeState
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.UiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the real Home screen with sample posters and saves a picture, so the Liquid Glass look
 * can be checked without a phone. Pictures go to the folder in the SCREENSHOT_DIR env variable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val colors = listOf(
        0xFFB23A48.toInt() to 0xFF2B0B10.toInt(), 0xFF2E86AB.toInt() to 0xFF0B1F33.toInt(),
        0xFFF18F01.toInt() to 0xFF3A1A00.toInt(), 0xFF6A4C93.toInt() to 0xFF1B1030.toInt(),
        0xFF3BB273.toInt() to 0xFF0C2A1A.toInt(), 0xFFE84855.toInt() to 0xFF3A0A10.toInt(),
        0xFF1982C4.toInt() to 0xFF06223A.toInt(), 0xFFFFCA3A.toInt() to 0xFF3A2A00.toInt()
    )

    private fun poster(dir: File, i: Int, w: Int = 300, h: Int = 450): String {
        val f = File(dir, "p$i-$w.png")
        if (f.exists()) return f.absolutePath
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val (a, z) = colors[i % colors.size]
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), a, z, Shader.TileMode.CLAMP) })
        c.drawCircle(w * 0.5f, h * 0.38f, w * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF })
        c.drawText("MOVIE ${i + 1}", w * 0.1f, h * 0.85f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1; textSize = w * 0.12f; isFakeBoldText = true })
        f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f.absolutePath
    }

    private fun items(dir: File, from: Int, n: Int) = (from until from + n).map { i ->
        MediaItem(
            id = "m$i", title = "Movie Title ${i + 1}", posterUrl = poster(dir, i), backdropUrl = poster(dir, i, 900, 500),
            mediaType = if (i % 3 == 0) MediaCategoryType.SERIES else MediaCategoryType.MOVIE,
            year = "2026", rating = "8.${i % 10}", qualityBadge = "HD", languageBadge = if (i % 2 == 0) "Hindi" else "English"
        )
    }

    private fun shot(paletteIndex: Int, name: String) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(ctx.cacheDir, "posters").apply { mkdirs() }
        ThemeState.current = CinedorPalettes[paletteIndex]
        val feed = HomeFeed(
            banners = items(dir, 0, 5),
            trendingItems = items(dir, 5, 10),
            sections = listOf(
                HomeSection("s1", "Popular Movies", items(dir, 10, 8)),
                HomeSection("s2", "Bangla Movies", items(dir, 18, 8)),
                HomeSection("s3", "K-Drama", items(dir, 26, 8))
            ),
            upcomingItems = items(dir, 34, 6)
        )
        val state = HomeUiState(feedState = UiState.Success(feed), ranking = items(dir, 40, 10), updates24h = items(dir, 50, 8))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val glass = remember { HazeState() }
            CinePulseTheme {
                CompositionLocalProvider(LiquidGlass.LocalState provides glass) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().hazeSource(glass)) {
                            HomeScreen(
                                homeState = state, preferences = UserPreferences(), watchlist = emptyList(),
                                history = emptyList(), reminders = emptyList(), onSelectTab = {}, onCycleSource = {},
                                onOpenSearch = {}, onOpenMedia = {}, onResumeHistory = {}, onToggleWatchlist = {},
                                onToggleReminder = {}, onSportsClick = {}, onRetry = {}
                            )
                        }
                        CinedorPremium3DNavBar(
                            currentDestination = CinedorNavDestination.HOME, shortsThumbUrl = null,
                            onSelectDestination = {}, modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
        // Endless animations (banner carousel, shimmer) never let Compose go idle; drive the clock by hand.
        rule.mainClock.autoAdvance = false
        repeat(30) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.advanceTimeBy(3_000)
        Thread.sleep(1500)
        repeat(10) { rule.mainClock.advanceTimeByFrame() }
        val out = File(System.getenv("SCREENSHOT_DIR") ?: ctx.cacheDir.absolutePath).apply { mkdirs() }
        // Draw the window directly (captureToImage waits for an idle state that endless
        // animations never reach).
        var bmp: Bitmap? = null
        rule.activityRule.scenario.onActivity { act ->
            val v = act.window.decorView
            val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b)); bmp = b
        }
        val bmp2 = bmp!!
        File(out, "$name.png").outputStream().use { bmp2.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SAVED ${File(out, "$name.png")}")
    }

    @Test fun homeDaylight() = shot(5, "home-daylight")
    @Test fun homeDaylightFrosted() { LiquidGlass.forceFrosted = true; try { shot(5, "home-daylight-frosted") } finally { LiquidGlass.forceFrosted = false } }
    @Test fun homeCinema() = shot(0, "home-cinema")
}
