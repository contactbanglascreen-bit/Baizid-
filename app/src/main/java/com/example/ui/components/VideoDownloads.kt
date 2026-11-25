package com.example.ui.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Stream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreamOption
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** HLS / DASH playlists are segment manifests, not single files, so they can't be saved directly. */
internal fun StreamOption.isDownloadable(): Boolean {
    val u = url.substringBefore('?').lowercase()
    val f = format.lowercase()
    if (url.isBlank()) return false
    if (u.endsWith(".m3u8") || u.endsWith(".mpd")) return false
    if ("hls" in f || "m3u8" in f || "dash" in f) return false
    return u.startsWith("http") || url.startsWith("http")
}

private fun safeFileName(raw: String): String =
    raw.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(110)
        .ifBlank { "Cinedor video" }

private fun guessExtension(option: StreamOption): String {
    val path = option.url.substringBefore('?').lowercase()
    return when {
        path.endsWith(".mkv") -> "mkv"
        path.endsWith(".webm") -> "webm"
        path.endsWith(".mov") -> "mov"
        option.format.lowercase().contains("mkv") -> "mkv"
        else -> "mp4"
    }
}

/**
 * Queues [option] with the system DownloadManager (progress in the notification shade,
 * survives app close). The file lives inside the app and is listed in Me / My List > Downloads.
 */
internal fun enqueueVideoDownload(
    context: Context,
    title: String,
    option: StreamOption,
    posterUrl: String = ""
): Boolean {
    return runCatching {
        val quality = option.quality.ifBlank { option.label }.ifBlank { "HD" }
        val fileName = "${safeFileName("$title [$quality]")}_${System.currentTimeMillis() % 100000}.${guessExtension(option)}"
        val dir = DownloadStore.downloadDir(context) ?: return false
        val file = java.io.File(dir, fileName)
        val headers = LinkedHashMap<String, String>()
        option.headers.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) headers[k] = v }
        // Cinedor's own downloader: 4 parallel parts, pause / resume, single notification.
        val id = CinedorDownloader.create(context, option.url, headers, file.absolutePath)
        DownloadStore.add(
            context,
            DownloadRecord(
                id = id,
                title = title,
                quality = quality,
                posterUrl = posterUrl,
                filePath = java.io.File(dir, fileName).absolutePath,
                createdAt = System.currentTimeMillis()
            )
        )
        true
    }.getOrDefault(false)
}

/**
 * Bottom sheet listing every available quality. Tapping a downloadable one starts the download.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualitySheet(
    title: String,
    streams: List<StreamOption>,
    onDismiss: () -> Unit,
    posterUrl: String = ""
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
    val started = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CinetorRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextPrimary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (streams.isEmpty()) {
                Text(
                    text = "No video sources yet. Wait for the stream to load, then try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Text(
                    text = "Saved inside the app • find it in Me › Downloads",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = CinetorRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a quality",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Every quality is listed; if one isn't available the closest one is downloaded.
                    val dlStreams = streams.filter { it.isDownloadable() }
                    val rows: List<Pair<Int?, StreamOption>> = if (dlStreams.isNotEmpty()) {
                        QualityLadder.steps.mapNotNull { st -> QualityLadder.pick(dlStreams, st.value)?.let { st.value to it } }
                    } else {
                        streams.map { null to it }
                    }
                    items(rows, key = { "${it.first ?: 0}_${it.second.url}" }) { (stepValue, opt) ->
                        val step = stepValue?.let { v -> QualityLadder.steps.firstOrNull { it.value == v } }
                        val actualLabel = QualityLadder.stepFor(QualityLadder.valueOf(opt))?.label
                            ?: opt.quality.ifBlank { opt.label }
                        val exact = step == null || QualityLadder.stepFor(QualityLadder.valueOf(opt))?.value == step.value
                        val downloadable = opt.isDownloadable()
                        val isStarted = opt.url in started
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isStarted) CinetorRedSoft else ObsidianSurfaceVariant.copy(alpha = 0.6f))
                                .border(1.dp, if (isStarted) CinetorRed.copy(alpha = 0.4f) else ObsidianBorder, RoundedCornerShape(14.dp))
                                .clickable(enabled = downloadable && !isStarted) {
                                    if (enqueueVideoDownload(context, title, opt, posterUrl)) {
                                        started.add(opt.url)
                                        DownloadNotifier.ensureRunning(context)
                                        Toast.makeText(
                                            context,
                                            "Downloading ${opt.quality.ifBlank { opt.label }} • open Me › Downloads",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Couldn't start download", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (downloadable) Icons.Rounded.HighQuality else Icons.Rounded.Stream,
                                contentDescription = null,
                                tint = if (downloadable) CinetorRed else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = step?.label ?: opt.quality.ifBlank { opt.label }.ifBlank { "Auto" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                        color = if (downloadable && exact) TextPrimary else TextMuted
                                    )
                                    if (step != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (exact) CinetorRed else ObsidianBorder)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                step.badge,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                                color = OnAccent
                                            )
                                        }
                                    }
                                }
                                val sub = if (!exact) "Not available · downloads $actualLabel" else buildList {
                                    if (opt.label.isNotBlank() && opt.label != opt.quality) add(opt.label)
                                    if (opt.format.isNotBlank()) add(opt.format.uppercase())
                                    if (opt.audioLanguage.isNotBlank()) add(opt.audioLanguage)
                                    if (opt.sizeText.isNotBlank()) add(opt.sizeText)
                                    if (!downloadable) add("streaming only")
                                }.joinToString(" • ")
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (downloadable) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isStarted) ObsidianSurface else CinetorRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isStarted) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                        contentDescription = "Download ${opt.quality}",
                                        tint = if (isStarted) CinetorRed else OnAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
