package com.example.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)

/**
 * Checks this app's GitHub Releases for a newer build (tag "build-N", N = versionCode).
 */
object AppUpdater {
    private const val LATEST_URL =
        "https://api.github.com/repos/contactbanglascreen-bit/Baizid-/releases/latest"

    /** Set when the user taps "Later", so we don't ask again until the next app start. */
    var dismissedThisSession by mutableStateOf(false)

    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    suspend fun fetchLatest(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Cinedor-App")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.optString("tag_name")
                val code = Regex("(\\d+)").findAll(tag).lastOrNull()?.value?.toIntOrNull() ?: return@runCatching null
                val assets = json.optJSONArray("assets") ?: return@runCatching null
                var apk: String? = null
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apk = a.optString("browser_download_url")
                        break
                    }
                }
                if (apk.isNullOrBlank()) return@runCatching null
                AppUpdateInfo(
                    versionCode = code,
                    versionName = json.optString("name").ifBlank { "Build $code" },
                    apkUrl = apk,
                    notes = json.optString("body")
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun startDownload(context: Context, info: AppUpdateInfo): Long? = runCatching {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "Cinedor-${info.versionCode}.apk")
        val req = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("Cinedor update ${info.versionName}")
            .setDescription("Downloading update")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(req)
    }.getOrNull()

    /** Returns progress 0..1, 1.0 when done, or -1 when failed. */
    fun progress(context: Context, id: Long): Float {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return -1f
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) return 1f
            if (status == DownloadManager.STATUS_FAILED) return -1f
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return if (total > 0) (done.toFloat() / total).coerceIn(0f, 0.99f) else 0f
        }
        return -1f
    }

    /** Opens the Android installer. Returns false if the user first has to allow installs from this app. */
    fun install(context: Context, id: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return false
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(id) ?: return false
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}

/** Bytes downloaded / total for an update download. */
internal fun updateBytes(context: Context, id: Long): Pair<Long, Long> {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
        if (c.moveToFirst()) {
            return c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) to
                c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        }
    }
    return 0L to 0L
}

/**
 * Modern "Update available" screen: version change, notes, live progress ring with size and
 * speed, then opens the installer. Updates required by the Console (minVersionCode) can't be skipped.
 */
@Composable
fun AppUpdatePrompt(manualCheck: Boolean = false, onFinished: () -> Unit = {}) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    var bytes by remember { mutableStateOf(0L to 0L) }
    var speed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val latest = AppUpdater.fetchLatest()
        if (latest != null && latest.versionCode > AppUpdater.currentVersionCode) {
            info = latest
        } else {
            if (manualCheck) {
                Toast.makeText(
                    context,
                    if (latest == null) "Couldn't check for updates" else "You have the latest version (${AppUpdater.currentVersionName})",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onFinished()
        }
    }

    LaunchedEffect(downloadId) {
        if (downloadId < 0) return@LaunchedEffect
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()
        while (true) {
            val p = AppUpdater.progress(context, downloadId)
            if (p < 0f) {
                failed = true
                break
            }
            progress = p
            val b = updateBytes(context, downloadId)
            bytes = b
            val now = System.currentTimeMillis()
            if (now - lastTime >= 400L) {
                speed = ((b.first - lastBytes).coerceAtLeast(0L) * 1000L) / (now - lastTime)
                lastBytes = b.first
                lastTime = now
            }
            if (p >= 1f) {
                AppUpdater.install(context, downloadId)
                break
            }
            delay(500L)
        }
    }

    val update = info ?: return
    val forced = (RemoteConsole.config?.minVersionCode ?: 0) > AppUpdater.currentVersionCode
    if (!manualCheck && !forced && AppUpdater.dismissedThisSession) return

    fun later() {
        AppUpdater.dismissedThisSession = true
        info = null
        onFinished()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!forced && downloadId < 0) later() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !forced,
            dismissOnClickOutside = !forced
        )
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(ObsidianCard)
        ) {
            // Gradient header with rocket
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(com.example.ui.theme.AccentBright, com.example.ui.theme.CinetorRedDark)
                        )
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                if (downloadId >= 0 && !failed) {
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progress.coerceIn(0.01f, 1f) },
                            color = androidx.compose.ui.graphics.Color.White,
                            trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
                            strokeWidth = 7.dp,
                            modifier = Modifier.size(96.dp)
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    if (forced) "Update required" else "New update available",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    VersionChip(AppUpdater.currentVersionName, highlighted = false)
                    Text("  →  ", color = TextSecondary, fontWeight = FontWeight.Bold)
                    VersionChip(update.versionName.substringAfterLast(' '), highlighted = true)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    update.notes.ifBlank { "Bug fixes and improvements." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your downloads, My List and settings are kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (downloadId >= 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    if (failed) {
                        Text("Download failed. Check your internet and try again.", color = CinetorRed, fontWeight = FontWeight.SemiBold)
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = CinetorRed,
                            trackColor = CinetorRedSoft,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val sizeLine = buildString {
                            if (bytes.second > 0) {
                                append(android.text.format.Formatter.formatShortFileSize(context, bytes.first))
                                append(" / ")
                                append(android.text.format.Formatter.formatShortFileSize(context, bytes.second))
                            }
                            if (speed > 0) append("  ·  ${android.text.format.Formatter.formatShortFileSize(context, speed)}/s")
                        }
                        Text(
                            if (progress >= 1f) "Opening installer…" else sizeLine.ifBlank { "Starting…" },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(com.example.ui.theme.AccentBright, com.example.ui.theme.CinetorRedDark)
                            )
                        )
                        .clickable(enabled = downloadId < 0 || failed || progress >= 1f) {
                            when {
                                downloadId < 0 || failed -> {
                                    failed = false
                                    progress = 0f
                                    downloadId = AppUpdater.startDownload(context, update) ?: -1L
                                    if (downloadId < 0) failed = true
                                }
                                progress >= 1f -> AppUpdater.install(context, downloadId)
                            }
                        },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        when {
                            downloadId < 0 -> "Update now"
                            failed -> "Retry"
                            progress >= 1f -> "Install"
                            else -> "Downloading…"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = OnAccent
                    )
                }
                if (!forced && downloadId < 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { later() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Later", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionChip(text: String, highlighted: Boolean) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (highlighted) CinetorRed else CinetorRedSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (highlighted) OnAccent else CinetorRed
        )
    }
}
