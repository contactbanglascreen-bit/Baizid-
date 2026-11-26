package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** "1.2 MB/s" / "350 KB/s" from bytes per second. */
fun formatRate(bytesPerSec: Long): String = when {
    bytesPerSec >= 1024L * 1024L -> "%.1f MB/s".format(bytesPerSec / 1048576.0)
    bytesPerSec >= 1024L -> "${bytesPerSec / 1024} KB/s"
    else -> "$bytesPerSec B/s"
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.2f GB".format(bytes / 1073741824.0)
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / 1048576.0)
    bytes >= 1024L -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

object NetSpeed {
    private const val TEST_HOST = "https://speed.cloudflare.com"

    fun networkName(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Unknown"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    /** Ping in ms (best of 3). */
    suspend fun ping(): Long = withContext(Dispatchers.IO) {
        (1..3).mapNotNull {
            runCatching {
                val t = System.nanoTime()
                val c = (URL("$TEST_HOST/__down?bytes=0").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000; readTimeout = 5000; useCaches = false
                }
                c.inputStream.use { it.readBytes() }
                c.disconnect()
                (System.nanoTime() - t) / 1_000_000
            }.getOrNull()
        }.minOrNull() ?: -1L
    }

    /** Downloads for up to [seconds]; reports live bytes/sec; returns average bytes/sec. */
    suspend fun download(seconds: Int = 8, onRate: (Long) -> Unit): Long = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        var total = 0L
        var lastReport = start
        var lastBytes = 0L
        val deadline = start + seconds * 1_000_000_000L
        runCatching {
            while (System.nanoTime() < deadline) {
                val c = (URL("$TEST_HOST/__down?bytes=50000000").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000; readTimeout = 8000; useCaches = false
                }
                c.inputStream.use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        val now = System.nanoTime()
                        if (now - lastReport > 300_000_000L) {
                            onRate(((total - lastBytes) * 1_000_000_000L) / (now - lastReport))
                            lastReport = now
                            lastBytes = total
                        }
                        if (now > deadline) break
                    }
                }
                c.disconnect()
            }
        }
        val secs = (System.nanoTime() - start) / 1_000_000_000.0
        if (secs > 0) (total / secs).toLong() else 0L
    }

    /** Uploads ~4 MB chunks for up to [seconds]; returns bytes/sec. */
    suspend fun upload(seconds: Int = 5, onRate: (Long) -> Unit): Long = withContext(Dispatchers.IO) {
        val chunk = ByteArray(2 * 1024 * 1024)
        val start = System.nanoTime()
        var total = 0L
        val deadline = start + seconds * 1_000_000_000L
        runCatching {
            while (System.nanoTime() < deadline) {
                val t = System.nanoTime()
                val c = (URL("$TEST_HOST/__up").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 6000; readTimeout = 10000
                    setFixedLengthStreamingMode(chunk.size)
                    setRequestProperty("Content-Type", "application/octet-stream")
                }
                c.outputStream.use { it.write(chunk) }
                c.responseCode
                c.disconnect()
                total += chunk.size
                val dt = System.nanoTime() - t
                if (dt > 0) onRate(chunk.size * 1_000_000_000L / dt)
            }
        }
        val secs = (System.nanoTime() - start) / 1_000_000_000.0
        if (secs > 0) (total / secs).toLong() else 0L
    }
}

private fun mbps(bytesPerSec: Long) = bytesPerSec * 8 / 1_000_000.0

private fun qualityAdvice(bytesPerSec: Long): String {
    val m = mbps(bytesPerSec)
    return when {
        m >= 25 -> "Great — 4K & 1080p will play smoothly"
        m >= 8 -> "Good — 1080p plays smoothly"
        m >= 4 -> "OK — 720p recommended"
        m >= 1.5 -> "Slow — choose 480p for no buffering"
        m > 0 -> "Very slow — 360p recommended"
        else -> ""
    }
}

/** Settings card: live phone speed, speed test (ping / download / upload) and Cinedor's data usage. */
@Composable
fun NetSpeedCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var liveDown by remember { mutableLongStateOf(0L) }
    var liveUp by remember { mutableLongStateOf(0L) }
    var appUsed by remember { mutableLongStateOf(0L) }
    var network by remember { mutableStateOf(NetSpeed.networkName(context)) }

    var testing by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("") }
    var gauge by remember { mutableLongStateOf(0L) }
    var ping by remember { mutableLongStateOf(-1L) }
    var down by remember { mutableLongStateOf(0L) }
    var up by remember { mutableLongStateOf(0L) }
    var maxSeen by remember { mutableDoubleStateOf(10.0) }

    // Live speed of the whole phone, updated every second.
    LaunchedEffect(Unit) {
        val uid = Process.myUid()
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(1000)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (rx >= 0 && lastRx >= 0) liveDown = (rx - lastRx).coerceAtLeast(0)
            if (tx >= 0 && lastTx >= 0) liveUp = (tx - lastTx).coerceAtLeast(0)
            lastRx = rx; lastTx = tx
            val a = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid)
            if (a > 0) appUsed = a
            network = NetSpeed.networkName(context)
        }
    }

    val shown = if (testing) gauge else (if (down > 0) down else liveDown)
    val frac by animateFloatAsState(
        targetValue = (mbps(shown) / maxSeen).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(400),
        label = "gauge"
    )

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (network == "Wi-Fi") Icons.Rounded.Wifi else Icons.Rounded.SignalCellularAlt,
                contentDescription = null, tint = CinetorRed, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(network, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))

        // Gauge
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(170.dp)) {
                val stroke = 14.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val tl = Offset(stroke / 2, stroke / 2)
                drawArc(ObsidianBorder, 150f, 240f, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(listOf(AccentBright, CinetorRed, CinetorRedDark, AccentBright)),
                    150f, 240f * frac, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f".format(mbps(shown)), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 32.sp)
                Text("Mbps · ${formatRate(shown)}", color = TextSecondary, fontSize = 12.sp)
                if (testing) Text(phase, color = CinetorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(Icons.Rounded.ArrowDownward, "Download now", formatRate(liveDown), Modifier.weight(1f))
            MiniStat(Icons.Rounded.ArrowUpward, "Upload now", formatRate(liveUp), Modifier.weight(1f))
        }
        if (down > 0 || ping >= 0) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Icons.Rounded.NetworkCheck, "Ping", if (ping >= 0) "$ping ms" else "—", Modifier.weight(1f))
                MiniStat(Icons.Rounded.ArrowDownward, "Download", if (down > 0) "%.1f Mbps".format(mbps(down)) else "—", Modifier.weight(1f))
                MiniStat(Icons.Rounded.ArrowUpward, "Upload", if (up > 0) "%.1f Mbps".format(mbps(up)) else "—", Modifier.weight(1f))
            }
            if (down > 0) {
                Spacer(Modifier.height(8.dp))
                Text("${formatRate(down)} download · ${qualityAdvice(down)}", color = TextSecondary, fontSize = 12.5.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(AccentBright, CinetorRedDark)))
                .clickable(enabled = !testing) {
                    testing = true
                    down = 0; up = 0; ping = -1; gauge = 0; maxSeen = 10.0
                    scope.launch {
                        phase = "Checking ping…"
                        ping = NetSpeed.ping()
                        phase = "Testing download…"
                        down = NetSpeed.download(8) { r ->
                            gauge = r
                            if (mbps(r) > maxSeen * 0.9) maxSeen = mbps(r) * 1.4
                        }
                        phase = "Testing upload…"
                        gauge = 0
                        up = NetSpeed.upload(5) { r -> gauge = r }
                        gauge = down
                        testing = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (testing) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(phase, color = Color.White, fontWeight = FontWeight.Bold)
            } else Text(if (down > 0) "Test again" else "Run speed test", color = Color.White, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CinetorRedSoft).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Data used by Cinedor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Since the phone was last restarted", color = TextMuted, fontSize = 11.5.sp)
            }
            Text(formatBytes(appUsed), color = CinetorRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(CinetorRedSoft), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text(label, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
        Spacer(Modifier.height(3.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}
