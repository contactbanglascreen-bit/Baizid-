package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.CinedorPalettes
import com.example.ui.theme.DEFAULT_THEME_INDEX
import com.example.ui.theme.ThemeState

/**
 * Small on-device settings store for UI / player / download preferences.
 * Values are Compose state, so screens update instantly when they change.
 */
object AppSettings {
    private const val PREFS = "cinedor_ui_settings"
    private var loaded = false

    var doubleTapSkipSec by mutableIntStateOf(10)
        private set
    var holdFor2x by mutableStateOf(true)
        private set
    var wifiOnlyDownloads by mutableStateOf(false)
        private set
    var profileName by mutableStateOf("Main")
        private set
    var avatarStyle by mutableIntStateOf(4)
        private set
    var themeIndex by mutableIntStateOf(DEFAULT_THEME_INDEX)
        private set
    var pipEnabled by mutableStateOf(true)
        private set
    /** 0 = small, 1 = normal, 2 = large, 3 = extra large */
    var subtitleSize by mutableIntStateOf(1)
        private set
    var subtitleBackground by mutableStateOf(true)
        private set

    fun load(context: Context) {
        if (loaded) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        doubleTapSkipSec = p.getInt("skip", 10)
        holdFor2x = p.getBoolean("hold2x", true)
        wifiOnlyDownloads = p.getBoolean("wifiOnly", false)
        profileName = p.getString("name", "Main") ?: "Main"
        avatarStyle = p.getInt("avatar", 4)
        themeIndex = p.getInt("theme", DEFAULT_THEME_INDEX).coerceIn(0, CinedorPalettes.lastIndex)
        pipEnabled = p.getBoolean("pip", true)
        subtitleSize = p.getInt("subSize", 1).coerceIn(0, 3)
        subtitleBackground = p.getBoolean("subBg", true)
        ThemeState.current = CinedorPalettes[themeIndex]
        loaded = true
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("skip", doubleTapSkipSec)
            .putBoolean("hold2x", holdFor2x)
            .putBoolean("wifiOnly", wifiOnlyDownloads)
            .putString("name", profileName)
            .putInt("avatar", avatarStyle)
            .putInt("theme", themeIndex)
            .putBoolean("pip", pipEnabled)
            .putInt("subSize", subtitleSize)
            .putBoolean("subBg", subtitleBackground)
            .apply()
    }

    fun setSkip(context: Context, sec: Int) { doubleTapSkipSec = sec; save(context) }
    fun setHold2x(context: Context, on: Boolean) { holdFor2x = on; save(context) }
    fun setWifiOnly(context: Context, on: Boolean) { wifiOnlyDownloads = on; save(context) }
    fun setProfile(context: Context, name: String, avatar: Int) {
        profileName = name.trim().ifBlank { "Main" }.take(24)
        avatarStyle = avatar
        save(context)
    }

    fun setTheme(context: Context, index: Int) {
        themeIndex = index.coerceIn(0, CinedorPalettes.lastIndex)
        ThemeState.current = CinedorPalettes[themeIndex]
        save(context)
    }
    fun setPip(context: Context, on: Boolean) { pipEnabled = on; save(context) }
    fun setSubtitleSize(context: Context, size: Int) { subtitleSize = size.coerceIn(0, 3); save(context) }
    fun setSubtitleBackground(context: Context, on: Boolean) { subtitleBackground = on; save(context) }

    fun reset(context: Context) {
        doubleTapSkipSec = 10
        holdFor2x = true
        wifiOnlyDownloads = false
        pipEnabled = true
        subtitleSize = 1
        subtitleBackground = true
        themeIndex = DEFAULT_THEME_INDEX
        ThemeState.current = CinedorPalettes[themeIndex]
        save(context)
    }
}

/** Shared state for picture-in-picture. */
object PipState {
    /** True while a video is actively playing (enter PiP when the user leaves the app). */
    var videoPlaying by mutableStateOf(false)
    /** True while the activity is shown in picture-in-picture. */
    var inPip by mutableStateOf(false)
    /** True while the player is a small floating window inside the app. */
    var inAppMini by mutableStateOf(false)
    /** Set by the player so the PiP button can request PiP. */
    var requestPip: (() -> Unit)? = null
}
