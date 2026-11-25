package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PopupConfig(
    val enabled: Boolean,
    val id: String,
    val title: String,
    val message: String,
    val buttonText: String,
    val buttonUrl: String,
    val perks: List<String> = emptyList(),
    val price: String = "",
    val showEveryHours: Int = 24,
    val secondaryText: String = "",
    val secondaryUrl: String = "",
    val afterMinutes: Int = 20,
    val shareMessage: String = ""
)

data class ConsoleConfig(
    val premium: PopupConfig?,
    val announcement: PopupConfig?,
    val maintenanceOn: Boolean,
    val maintenanceMessage: String,
    val minVersionCode: Int,
    val community: PopupConfig? = null,
    val sharePrompt: PopupConfig? = null,
    val audioDubsOn: Boolean = true,
    val audioDubsTitle: String = "Audio & Dubs"
)

/**
 * Cinedor Console: the app reads console/app-config.json from the GitHub repo at start-up,
 * so editing that file on GitHub controls popups, announcements, maintenance and forced updates.
 */
object RemoteConsole {
    const val REPO = "contactbanglascreen-bit/Baizid-"
    const val PATH = "console/app-config.json"
    private val REFS = listOf("main", "ccr-c4b9015d-7d590g")
    private const val PREFS = "cinedor_console"

    var config by mutableStateOf<ConsoleConfig?>(null)
        private set

    /** Raw JSON text and the branch it came from (used by the admin Console to save back). */
    var rawJson by mutableStateOf<String?>(null)
        private set
    var loadedRef by mutableStateOf(REFS.last())
        private set

    private fun get(url: String, accept: String? = null): String? = runCatching {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            useCaches = false
            setRequestProperty("User-Agent", "Cinedor-App")
            if (accept != null) setRequestProperty("Accept", accept)
        }
        try {
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) {
            // GitHub API first (no CDN delay), then raw files as a fallback.
            REFS.firstNotNullOfOrNull { ref ->
                get("https://api.github.com/repos/$REPO/contents/$PATH?ref=$ref", "application/vnd.github.raw+json")
                    ?.let { ref to it }
            } ?: REFS.firstNotNullOfOrNull { ref ->
                get("https://raw.githubusercontent.com/$REPO/$ref/$PATH?t=${System.currentTimeMillis() / 60000}")
                    ?.let { ref to it }
            }
        } ?: return
        loadedRef = result.first
        rawJson = result.second
        config = runCatching { parse(JSONObject(result.second)) }.getOrNull()
    }

    /**
     * Publishes new config JSON to GitHub (needs a GitHub token with "Contents: write"
     * on this repository). Returns null on success or an error message.
     */
    suspend fun save(token: String, json: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ref = loadedRef
            val api = "https://api.github.com/repos/$REPO/contents/$PATH"
            val shaConn = (URL("$api?ref=$ref").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Cinedor-App")
            }
            val sha = try {
                if (shaConn.responseCode in 200..299) {
                    JSONObject(shaConn.inputStream.bufferedReader().use { it.readText() }).optString("sha")
                } else ""
            } finally {
                shaConn.disconnect()
            }
            val body = JSONObject()
                .put("message", "Cinedor Console: update app config")
                .put("content", android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.NO_WRAP))
                .put("branch", ref)
            if (sha.isNotBlank()) body.put("sha", sha)
            val put = (URL(api).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Cinedor-App")
            }
            try {
                put.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = put.responseCode
                if (code in 200..299) {
                    rawJson = json
                    config = runCatching { parse(JSONObject(json)) }.getOrNull()
                    null
                } else {
                    val err = runCatching { put.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
                    "GitHub error $code ${JSONObject(err.ifBlank { "{}" }).optString("message")}".trim()
                }
            } finally {
                put.disconnect()
            }
        }.getOrElse { "Couldn't save: ${it.message}" }
    }

    private fun popup(o: JSONObject?): PopupConfig? {
        if (o == null) return null
        val perks = o.optJSONArray("perks")
        return PopupConfig(
            enabled = o.optBoolean("enabled", false),
            id = o.optString("id", "default"),
            title = o.optString("title"),
            message = o.optString("message"),
            buttonText = o.optString("buttonText", "OK"),
            buttonUrl = o.optString("buttonUrl"),
            perks = if (perks == null) emptyList() else List(perks.length()) { perks.optString(it) }.filter { it.isNotBlank() },
            price = o.optString("price"),
            showEveryHours = o.optInt("showEveryHours", 24),
            secondaryText = o.optString("secondaryText"),
            secondaryUrl = o.optString("secondaryUrl"),
            afterMinutes = o.optInt("afterMinutes", 20),
            shareMessage = o.optString("shareMessage")
        )
    }

    private fun parse(root: JSONObject): ConsoleConfig {
        val m = root.optJSONObject("maintenance")
        return ConsoleConfig(
            premium = popup(root.optJSONObject("premium")),
            announcement = popup(root.optJSONObject("announcement")),
            maintenanceOn = m?.optBoolean("enabled", false) == true,
            maintenanceMessage = m?.optString("message").orEmpty(),
            minVersionCode = root.optInt("minVersionCode", 0),
            community = popup(root.optJSONObject("community")),
            sharePrompt = popup(root.optJSONObject("sharePrompt")),
            audioDubsOn = root.optJSONObject("audioDubs")?.optBoolean("enabled", true) ?: true,
            audioDubsTitle = root.optJSONObject("audioDubs")?.optString("title", "Audio & Dubs") ?: "Audio & Dubs"
        )
    }

    fun shouldShow(context: Context, kind: String, p: PopupConfig): Boolean {
        if (!p.enabled || p.title.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong("${kind}_${p.id}", 0L)
        if (last == 0L) return true
        if (kind == "announcement") return false
        return System.currentTimeMillis() - last > p.showEveryHours.coerceAtLeast(1) * 3_600_000L
    }

    fun markShown(context: Context, kind: String, p: PopupConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("${kind}_${p.id}", System.currentTimeMillis()).apply()
    }
}

private fun openLink(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Shows maintenance screen, Premium popup and announcements controlled from the Console file. */
@Composable
fun ConsolePrompts() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { RemoteConsole.refresh() }
    val cfg = RemoteConsole.config ?: return

    if (cfg.maintenanceOn) {
        MaintenanceScreen(cfg.maintenanceMessage)
        return
    }

    var premiumOpen by remember(cfg.premium) {
        mutableStateOf(cfg.premium != null && RemoteConsole.shouldShow(context, "premium", cfg.premium))
    }
    var noticeOpen by remember(cfg.announcement) {
        mutableStateOf(cfg.announcement != null && RemoteConsole.shouldShow(context, "announcement", cfg.announcement))
    }
    var communityOpen by remember(cfg.community) {
        mutableStateOf(cfg.community != null && RemoteConsole.shouldShow(context, "community", cfg.community))
    }
    val community = cfg.community

    val premium = cfg.premium
    val notice = cfg.announcement
    if (premiumOpen && premium != null) {
        PremiumPopup(
            p = premium,
            onClose = {
                RemoteConsole.markShown(context, "premium", premium)
                premiumOpen = false
            },
            onAction = {
                RemoteConsole.markShown(context, "premium", premium)
                premiumOpen = false
                openLink(context, premium.buttonUrl)
            }
        )
    } else if (noticeOpen && notice != null) {
        AnnouncementPopup(
            p = notice,
            onClose = {
                RemoteConsole.markShown(context, "announcement", notice)
                noticeOpen = false
            },
            onAction = {
                RemoteConsole.markShown(context, "announcement", notice)
                noticeOpen = false
                openLink(context, notice.buttonUrl)
            }
        )
    } else if (communityOpen && community != null) {
        CommunityPopup(
            p = community,
            onClose = {
                RemoteConsole.markShown(context, "community", community)
                communityOpen = false
            },
            onLink = { url ->
                RemoteConsole.markShown(context, "community", community)
                communityOpen = false
                openLink(context, url)
            }
        )
    }
}

/** "Join us" popup: Telegram + Facebook (or any two links), text set from the Console. */
@Composable
private fun CommunityPopup(p: PopupConfig, onClose: () -> Unit, onLink: (String) -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0D10), Color(0xFF0C0809))))
                .border(1.dp, CinetorRed.copy(alpha = 0.55f), RoundedCornerShape(30.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 18.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CinetorRed.copy(alpha = 0.45f), Color.Transparent)))
                        .border(2.dp, CinetorRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = AccentBright, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                p.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = 22.sp),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (p.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    p.message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (p.buttonUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF2AABEE), Color(0xFF1B6FE0))))
                        .clickable { onLink(p.buttonUrl) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.buttonText.ifBlank { "Join our Telegram channel" } + "  ↗",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
                        color = Color.White
                    )
                }
            }
            if (p.secondaryUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.2.dp, Color(0xFF4C8DF6), RoundedCornerShape(18.dp))
                        .clickable { onLink(p.secondaryUrl) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.secondaryText.ifBlank { "Visit our Facebook page" } + "  ↗",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
                        color = Color(0xFF6FA6FF)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Close",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * "Enjoying the movie?" share popup: appears after the viewer has watched the number of minutes
 * set in the Console. WhatsApp share + share to any other app.
 */
object WatchTimeTracker {
    private const val PREFS = "cinedor_watch_time"
    fun addSecond(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = p.getInt("seconds", 0) + 1
        p.edit().putInt("seconds", v).apply()
        return v
    }
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("seconds", 0).apply()
    }
}

private fun shareText(context: Context, text: String, whatsapp: Boolean): Boolean {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    return runCatching {
        if (whatsapp) {
            val wa = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)
            }
            if (wa == null) return false
            context.startActivity(Intent(send).setPackage(wa).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(Intent.createChooser(send, "Share Cinedor").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        true
    }.getOrDefault(false)
}

@Composable
fun SharePromptHost() {
    val context = LocalContext.current
    val cfg = RemoteConsole.config
    val p = cfg?.sharePrompt
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(p) {
        if (p == null || !p.enabled) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(1000L)
            if (open) continue
            if (!PipState.videoPlaying) continue
            val secs = WatchTimeTracker.addSecond(context)
            if (secs >= p.afterMinutes.coerceAtLeast(1) * 60 && RemoteConsole.shouldShow(context, "share", p)) {
                open = true
            }
        }
    }
    if (!open || p == null) return
    val text = p.shareMessage.ifBlank {
        "I'm watching movies & series free on Cinedor 🍿 Get it here: ${p.buttonUrl.ifBlank { "https://github.com/${RemoteConsole.REPO}/releases/latest" }}"
    }
    fun done() {
        RemoteConsole.markShown(context, "share", p)
        WatchTimeTracker.reset(context)
        open = false
    }
    Dialog(onDismissRequest = { done() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0D10), Color(0xFF0C0809))))
                .border(1.5.dp, Brush.linearGradient(listOf(AccentBright, CinetorRedDark)), RoundedCornerShape(30.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = CinetorRed)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(2.dp, CinetorRed, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cinedor_logo),
                    contentDescription = "Cinedor",
                    modifier = Modifier.size(66.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                p.title.ifBlank { "Enjoying the movie? 🍿" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = 23.sp),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                p.message.ifBlank { "You've enjoyed ${p.afterMinutes} free minutes! Share Cinedor with friends to keep watching." },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            val perks = p.perks.ifEmpty { listOf("✨ Full HD", "⚡ Buffer free", "🎁 100% free") }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                perks.take(3).forEach { perk ->
                    Text(
                        perk,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF25D366))
                    .clickable {
                        if (!shareText(context, text, whatsapp = true)) shareText(context, text, whatsapp = false)
                        done()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.buttonText.ifBlank { "💬  Share on WhatsApp" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(AccentBright, CinetorRedDark)))
                    .clickable {
                        shareText(context, text, whatsapp = false)
                        done()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.secondaryText.ifBlank { "Share via other apps 🚀" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { done() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PremiumPopup(p: PopupConfig, onClose: () -> Unit, onAction: () -> Unit) {
    val gold = Color(0xFFF5C451)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1206), Color(0xFF0D0A06))))
                .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(30.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.radialGradient(listOf(gold.copy(alpha = 0.55f), Color.Transparent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFE39A), gold, Color(0xFFC8901A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFF3A2500), modifier = Modifier.size(42.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    p.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = 24.sp),
                    color = gold,
                    textAlign = TextAlign.Center
                )
                if (p.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        p.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                if (p.perks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        p.perks.forEach { perk ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(perk, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                            }
                        }
                    }
                }
                if (p.price.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(p.price, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color.White)
                }
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFE39A), gold, Color(0xFFC8901A))))
                        .clickable(onClick = onAction),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.buttonText.ifBlank { "Get Premium" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFF3A2500)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Maybe later",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClose)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AnnouncementPopup(p: PopupConfig, onClose: () -> Unit, onAction: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(26.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentBright, CinetorRedDark))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = OnAccent, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(p.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(p.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(CinetorRed)
                    .clickable(onClick = if (p.buttonUrl.isNotBlank()) onAction else onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(p.buttonText.ifBlank { "OK" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = OnAccent)
            }
        }
    }
}

@Composable
private fun MaintenanceScreen(message: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF00B0C0F)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentBright, CinetorRedDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text("Under maintenance", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    message.ifBlank { "Please come back in a little while." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
