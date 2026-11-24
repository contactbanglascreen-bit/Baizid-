package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    fun load(context: Context) {
        if (loaded) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        doubleTapSkipSec = p.getInt("skip", 10)
        holdFor2x = p.getBoolean("hold2x", true)
        wifiOnlyDownloads = p.getBoolean("wifiOnly", false)
        profileName = p.getString("name", "Main") ?: "Main"
        avatarStyle = p.getInt("avatar", 4)
        loaded = true
    }

    private fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("skip", doubleTapSkipSec)
            .putBoolean("hold2x", holdFor2x)
            .putBoolean("wifiOnly", wifiOnlyDownloads)
            .putString("name", profileName)
            .putInt("avatar", avatarStyle)
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

    fun reset(context: Context) {
        doubleTapSkipSec = 10
        holdFor2x = true
        wifiOnlyDownloads = false
        save(context)
    }
}
