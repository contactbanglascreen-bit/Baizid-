package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.StreamOption
import com.example.ui.theme.ChipBg
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// ---------------------------------------------------------------------------
// Storage of the app's own download records (SharedPreferences + DownloadManager status)
// ---------------------------------------------------------------------------

data class DownloadRecord(
    val id: Long,
    val title: String,
    val quality: String,
    val posterUrl: String,
    val filePath: String,
    val createdAt: Long
)

enum class DownloadState { QUEUED, RUNNING, PAUSED, DONE, FAILED }

data class DownloadEntry(
    val record: DownloadRecord,
    val state: DownloadState,
    val downloadedBytes: Long,
    val totalBytes: Long
) {
    val progress: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

object DownloadStore {
    private const val PREFS = "cinedor_downloads"
    private const val KEY = "records"

    fun downloadDir(context: Context): File? {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return null
        val dir = File(base, "Cinedor")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun readAll(context: Context): MutableList<DownloadRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                DownloadRecord(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    quality = o.optString("quality"),
                    posterUrl = o.optString("posterUrl"),
                    filePath = o.optString("filePath"),
                    createdAt = o.optLong("createdAt")
                )
            }
        }.getOrDefault(mutableListOf())
    }

    private fun writeAll(context: Context, list: List<DownloadRecord>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("quality", r.quality)
                    .put("posterUrl", r.posterUrl)
                    .put("filePath", r.filePath)
                    .put("createdAt", r.createdAt)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, record: DownloadRecord) {
        val list = readAll(context)
        list.removeAll { it.id == record.id }
        list.add(0, record)
        writeAll(context, list)
    }

    fun remove(context: Context, record: DownloadRecord) {
        runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.remove(record.id)
        }
        runCatching { File(record.filePath).takeIf { it.exists() }?.delete() }
        writeAll(context, readAll(context).filterNot { it.id == record.id })
    }

    @SuppressLint("Range")
    fun entries(context: Context): List<DownloadEntry> {
        val records = readAll(context)
        if (records.isEmpty()) return emptyList()
        val statusById = HashMap<Long, Triple<Int, Long, Long>>()
        runCatching {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(*records.map { it.id }.toLongArray())
            dm.query(query)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID))
                    val status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val done = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    statusById[id] = Triple(status, done, total)
                }
            }
        }
        return records.map { r ->
            val file = File(r.filePath)
            val s = statusById[r.id]
            val state = when (s?.first) {
                DownloadManager.STATUS_PENDING -> DownloadState.QUEUED
                DownloadManager.STATUS_RUNNING -> DownloadState.RUNNING
                DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadState.DONE
                DownloadManager.STATUS_FAILED -> DownloadState.FAILED
                else -> if (file.exists() && file.length() > 0L) DownloadState.DONE else DownloadState.FAILED
            }
            val total = s?.third?.takeIf { it > 0L } ?: file.length()
            val done = if (state == DownloadState.DONE) total else (s?.second ?: 0L)
            DownloadEntry(r, state, done, total)
        }
    }
}

// ---------------------------------------------------------------------------
// Downloads list (shown as a tab in Me / My List)
// ---------------------------------------------------------------------------

@Composable
fun InAppDownloadsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf(DownloadStore.entries(context)) }
    var playing by remember { mutableStateOf<DownloadRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<DownloadRecord?>(null) }

    // Refresh every second while something is still downloading
    LaunchedEffect(refreshTick) {
        while (true) {
            entries = DownloadStore.entries(context)
            DownloadSpeedTracker.update(entries)
            val active = entries.any { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED || it.state == DownloadState.PAUSED }
            delay(if (active) 1000L else 4000L)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (entries.isEmpty()) {
            EmptyLibraryCard(
                icon = Icons.Rounded.Download,
                title = "No Downloads Yet",
                subtitle = "Open any movie or episode, tap Download and pick a quality. It will appear here to watch offline."
            )
        } else {
            entries.forEach { entry ->
                DownloadRow(
                    entry = entry,
                    onPlay = { OfflinePlayback.record = entry.record },
                    onDelete = { pendingDelete = entry.record }
                )
            }
        }
    }

    pendingDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("\"${rec.title}\" (${rec.quality}) will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    DownloadStore.remove(context, rec)
                    pendingDelete = null
                    refreshTick++
                }) { Text("Delete", color = CinetorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = ObsidianCard
        )
    }

    playing?.let { rec ->
        OfflinePlayback.record = rec
    }
}

@Composable
private fun DownloadRow(
    entry: DownloadEntry,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val rec = entry.record
    val done = entry.state == DownloadState.DONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = done, onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(82.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (rec.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = rec.posterUrl,
                    contentDescription = rec.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.Movie, contentDescription = null, tint = TextMuted)
            }
            if (done) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CinetorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = OnAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rec.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CinetorRedSoft)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = rec.quality,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = CinetorRed
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                val sizeText = when {
                    entry.totalBytes > 0L && !done ->
                        "${Formatter.formatShortFileSize(context, entry.downloadedBytes)} / ${Formatter.formatShortFileSize(context, entry.totalBytes)}"
                    entry.totalBytes > 0L -> Formatter.formatShortFileSize(context, entry.totalBytes)
                    else -> ""
                }
                val stateText = when (entry.state) {
                    DownloadState.QUEUED -> "Waiting…"
                    DownloadState.RUNNING -> "Downloading ${(entry.progress * 100).toInt()}%"
                    DownloadState.PAUSED -> "Paused (waiting for network)"
                    DownloadState.DONE -> "Downloaded"
                    DownloadState.FAILED -> "Failed"
                }
                Text(
                    text = listOf(stateText, sizeText).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (entry.state == DownloadState.FAILED) CinetorRed else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.state == DownloadState.RUNNING) {
                val bps = DownloadSpeedTracker.speeds[rec.id] ?: 0L
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = CinetorRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    val left = entry.totalBytes - entry.downloadedBytes
                    val eta = if (bps > 0L && left > 0L) formatEta(left / bps) else ""
                    Text(
                        text = buildString {
                            append(if (bps > 0L) "${Formatter.formatShortFileSize(context, bps)}/s" else "Starting…")
                            if (eta.isNotBlank()) append("  ·  $eta left")
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = CinetorRed,
                        maxLines = 1
                    )
                }
            }
            if (!done && entry.state != DownloadState.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                if (entry.totalBytes > 0L) {
                    LinearProgressIndicator(
                        progress = { entry.progress },
                        color = CinetorRed,
                        trackColor = CinetorRedSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                } else {
                    LinearProgressIndicator(
                        color = CinetorRed,
                        trackColor = CinetorRedSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = if (entry.state == DownloadState.FAILED) Icons.Rounded.ErrorOutline else Icons.Rounded.DeleteOutline,
                contentDescription = "Delete download",
                tint = if (entry.state == DownloadState.FAILED) CinetorRed else TextMuted
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Offline player (full-screen dialog, same controls as the streaming player)
// ---------------------------------------------------------------------------

private fun Context.findHostActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(UnstableApi::class)
@Composable
fun OfflinePlayerScreen(record: DownloadRecord, onClose: () -> Unit) {
    val context = LocalContext.current
    // Offline playback opens straight into landscape full screen.
    var isFullscreen by remember { mutableStateOf(true) }
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var resize by remember { mutableStateOf(VideoResizeOption.FIT) }
    val speeds = remember { listOf(1f, 1.25f, 1.5f, 2f, 0.75f) }

    val player = remember(record.filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(record.filePath))))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                PipState.videoPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (player.duration > 0L) durationMs = player.duration
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            PipState.videoPlaying = false
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            bufferedMs = player.bufferedPosition
            if (player.duration > 0L) durationMs = player.duration
            delay(400L)
        }
    }

    DisposableEffect(isFullscreen) {
        val activity = context.findHostActivity()
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        activity?.window?.let { w ->
            val controller = WindowCompat.getInsetsController(w, w.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
            else controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val fakeStream = remember(record) {
        StreamOption(id = record.id.toString(), label = record.quality, quality = record.quality, url = record.filePath, format = "Offline")
    }

    BackHandler {
        if (isFullscreen) isFullscreen = false else onClose()
    }
    run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clipToBounds()
        ) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx)
                        .inflate(com.example.R.layout.cinedor_player_view, null, false) as PlayerView).apply {
                        useController = false
                        keepScreenOn = true
                        this.player = player
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.resizeMode = when (resize) {
                        VideoResizeOption.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoResizeOption.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoResizeOption.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoResizeOption.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    val sc = pinchScale * (if (resize == VideoResizeOption.CROP) 1.3f else 1f)
                    view.scaleX = sc
                    view.scaleY = sc
                },
                onRelease = { view -> view.player = null },
                modifier = Modifier.fillMaxSize()
            )
            CinedorPlayerControlsOverlay(
                title = "${record.title} • ${record.quality}",
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = positionMs,
                bufferedPositionMs = bufferedMs,
                durationMs = durationMs,
                isFullscreen = isFullscreen,
                playbackSpeed = speed,
                resizeOption = resize,
                streams = listOf(fakeStream),
                selectedStream = fakeStream,
                captions = emptyList(),
                selectedCaption = null,
                onBack = { if (isFullscreen) isFullscreen = false else onClose() },
                onTogglePlayPause = {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0L)
                        player.play()
                    } else if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                },
                onSeekTo = { target ->
                    val clamped = if (durationMs > 0L) target.coerceIn(0L, durationMs) else target.coerceAtLeast(0L)
                    player.seekTo(clamped)
                    positionMs = clamped
                },
                onSeekBy = { delta ->
                    val target = (player.currentPosition + delta).coerceAtLeast(0L)
                    val clamped = if (durationMs > 0L) target.coerceAtMost(durationMs) else target
                    player.seekTo(clamped)
                    positionMs = clamped
                },
                onCycleSpeed = {
                    speed = speeds[(speeds.indexOf(speed) + 1) % speeds.size]
                    player.playbackParameters = PlaybackParameters(speed)
                },
                onCycleResize = {
                    resize = VideoResizeOption.entries[(resize.ordinal + 1) % VideoResizeOption.entries.size]
                    pinchScale = 1f
                },
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                onSelectStream = {},
                onSelectCaption = {},
                modifier = if (isFullscreen) Modifier.fillMaxSize() else Modifier.fillMaxSize().systemBarsPadding(),
                onPinchZoom = { z -> pinchScale = (pinchScale * z).coerceIn(1f, 3f) },
                onHoldSpeed = { holding ->
                    player.playbackParameters = PlaybackParameters(if (holding) 2f else speed)
                },
                onSetSpeed = { sp ->
                    speed = sp
                    player.playbackParameters = PlaybackParameters(sp)
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Full Downloads page: storage usage bar + Active / Completed / Failed filters
// ---------------------------------------------------------------------------

private fun folderSize(dir: File?): Long =
    dir?.listFiles()?.sumOf { if (it.isFile) it.length() else folderSize(it) } ?: 0L

@Composable
fun DownloadsPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf(DownloadStore.entries(context)) }
    var filter by remember { mutableIntStateOf(0) } // 0 active, 1 completed, 2 failed
    var playing by remember { mutableStateOf<DownloadRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<DownloadRecord?>(null) }
    var usedBytes by remember { mutableLongStateOf(0L) }
    var freeBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(1L) }

    LaunchedEffect(refreshTick) {
        while (true) {
            entries = DownloadStore.entries(context)
            DownloadSpeedTracker.update(entries)
            val dir = DownloadStore.downloadDir(context)
            usedBytes = folderSize(dir)
            runCatching {
                val stat = android.os.StatFs((dir ?: context.filesDir).absolutePath)
                freeBytes = stat.availableBytes
                totalBytes = stat.totalBytes.coerceAtLeast(1L)
            }
            val active = entries.any { it.state != DownloadState.DONE && it.state != DownloadState.FAILED }
            delay(if (active) 1000L else 4000L)
        }
    }

    // Open on Completed when nothing is running
    LaunchedEffect(Unit) {
        val e = DownloadStore.entries(context)
        if (e.none { it.state != DownloadState.DONE && it.state != DownloadState.FAILED } &&
            e.any { it.state == DownloadState.DONE }
        ) filter = 1
    }

    val activeList = entries.filter { it.state != DownloadState.DONE && it.state != DownloadState.FAILED }
    val doneList = entries.filter { it.state == DownloadState.DONE }
    val failedList = entries.filter { it.state == DownloadState.FAILED }
    val shown = when (filter) {
        0 -> activeList
        1 -> doneList
        else -> failedList
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Storage bar
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            val usedOther = (totalBytes - freeBytes - usedBytes).coerceAtLeast(0L)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val appFrac = (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    val otherFrac = (usedOther.toFloat() / totalBytes).coerceIn(0f, 1f - appFrac)
                    if (appFrac > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(appFrac.coerceAtLeast(0.01f))
                                .height(8.dp)
                                .background(CinetorRed)
                        )
                    }
                    if (otherFrac > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(otherFrac / (1f - appFrac).coerceAtLeast(0.01f))
                                .height(8.dp)
                                .background(Color(0xFF6E6F78))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CinetorRed)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Cinedor ${Formatter.formatShortFileSize(context, usedBytes)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Free ${Formatter.formatShortFileSize(context, freeBytes)} of ${Formatter.formatShortFileSize(context, totalBytes)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Active" to activeList.size, "Completed" to doneList.size, "Failed" to failedList.size)
                .forEachIndexed { idx, (label, count) ->
                    val sel = filter == idx
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .background(if (sel) CinetorRed else ChipBg)
                            .clickable { filter = idx }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$label $count",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (sel) OnAccent else TextPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (shown.isEmpty()) {
            Text(
                text = when (filter) {
                    0 -> "Nothing downloading. Tap Download on any title."
                    1 -> "No completed downloads yet."
                    else -> "No failed downloads."
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
            )
        } else {
            shown.forEach { entry ->
                DownloadRow(
                    entry = entry,
                    onPlay = { OfflinePlayback.record = entry.record },
                    onDelete = { pendingDelete = entry.record }
                )
            }
        }
    }

    pendingDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("\"${rec.title}\" (${rec.quality}) will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    DownloadStore.remove(context, rec)
                    pendingDelete = null
                    refreshTick++
                }) { Text("Delete", color = CinetorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = ObsidianCard
        )
    }

    playing?.let { rec ->
        OfflinePlayback.record = rec
    }
}

/** Total size of finished downloads, for the Settings page. */
fun downloadedBytes(context: Context): Long = folderSize(DownloadStore.downloadDir(context))


/** The downloaded video being played offline; MainActivity shows the full-screen player while set. */
object OfflinePlayback {
    var record by mutableStateOf<DownloadRecord?>(null)
}


/** Measures live download speed (bytes per second) between list refreshes. */
object DownloadSpeedTracker {
    private val last = HashMap<Long, Pair<Long, Long>>() // id -> (bytes, timeMs)
    val speeds = androidx.compose.runtime.mutableStateMapOf<Long, Long>()

    fun update(entries: List<DownloadEntry>) {
        val now = System.currentTimeMillis()
        val running = entries.filter { it.state == DownloadState.RUNNING }
        running.forEach { e ->
            val id = e.record.id
            val prev = last[id]
            if (prev != null) {
                val dt = now - prev.second
                val db = e.downloadedBytes - prev.first
                if (dt >= 300L && db >= 0L) {
                    val instant = db * 1000L / dt
                    val old = speeds[id]
                    // Smooth it so the number doesn't jump around.
                    speeds[id] = if (old == null || old == 0L) instant else (old * 6 + instant * 4) / 10
                }
            }
            last[id] = e.downloadedBytes to now
        }
        val runningIds = running.map { it.record.id }.toSet()
        last.keys.filter { it !in runningIds }.forEach { last.remove(it) }
        speeds.keys.filter { it !in runningIds }.forEach { speeds.remove(it) }
    }
}

internal fun formatEta(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60} min"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}
