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
    val showEveryHours: Int = 24
)

data class ConsoleConfig(
    val premium: PopupConfig?,
    val announcement: PopupConfig?,
    val maintenanceOn: Boolean,
    val maintenanceMessage: String,
    val minVersionCode: Int
)

/**
 * Cinedor Console: the app reads console/app-config.json from the GitHub repo at start-up,
 * so editing that file on GitHub controls popups, announcements, maintenance and forced updates.
 */
object RemoteConsole {
    private val CONFIG_URLS = listOf(
        "https://raw.githubusercontent.com/contactbanglascreen-bit/Baizid-/main/console/app-config.json",
        "https://raw.githubusercontent.com/contactbanglascreen-bit/Baizid-/ccr-c4b9015d-7d590g/console/app-config.json"
    )
    private const val PREFS = "cinedor_console"

    var config by mutableStateOf<ConsoleConfig?>(null)
        private set

    suspend fun refresh() {
        val json = withContext(Dispatchers.IO) {
            CONFIG_URLS.firstNotNullOfOrNull { u ->
                runCatching {
                    val c = (URL("$u?t=${System.currentTimeMillis() / 60000}").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        useCaches = false
                    }
                    try {
                        if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() } else null
                    } finally {
                        c.disconnect()
                    }
                }.getOrNull()
            }
        } ?: return
        config = runCatching { parse(JSONObject(json)) }.getOrNull()
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
            showEveryHours = o.optInt("showEveryHours", 24)
        )
    }

    private fun parse(root: JSONObject): ConsoleConfig {
        val m = root.optJSONObject("maintenance")
        return ConsoleConfig(
            premium = popup(root.optJSONObject("premium")),
            announcement = popup(root.optJSONObject("announcement")),
            maintenanceOn = m?.optBoolean("enabled", false) == true,
            maintenanceMessage = m?.optString("message").orEmpty(),
            minVersionCode = root.optInt("minVersionCode", 0)
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
