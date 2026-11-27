package com.example.ui.screens

import com.example.ui.components.glassBubble
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppUpdater
import com.example.ui.components.RemoteConsole
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val ADMIN_PREFS = "cinedor_admin"

/**
 * The GitHub token is kept in encrypted storage (Android Keystore AES-256),
 * moving any older plain copy over once.
 */
private fun securePrefs(context: Context): android.content.SharedPreferences {
    val secure = runCatching {
        val key = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
        androidx.security.crypto.EncryptedSharedPreferences.create(
            "cinedor_admin_secure",
            key,
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull() ?: return context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
    val plain = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
    plain.getString("gh_token", null)?.let { old ->
        if (secure.getString("gh_token", null).isNullOrBlank()) secure.edit().putString("gh_token", old).apply()
        plain.edit().remove("gh_token").apply()
    }
    return secure
}

/** In-app Cinedor Console: edit the remote app config and publish it to every phone. */
@Composable
fun AdminConsoleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var latestBuild by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        RemoteConsole.refresh()
        latestBuild = AppUpdater.fetchLatest()?.versionName
        loading = false
    }

    val root = remember(RemoteConsole.rawJson) {
        runCatching { JSONObject(RemoteConsole.rawJson ?: "{}") }.getOrDefault(JSONObject())
    }
    val pr = root.optJSONObject("premium") ?: JSONObject()
    val an = root.optJSONObject("announcement") ?: JSONObject()
    val mt = root.optJSONObject("maintenance") ?: JSONObject()
    val cm = root.optJSONObject("community") ?: JSONObject()
    val sp = root.optJSONObject("sharePrompt") ?: JSONObject()
    val ad = root.optJSONObject("audioDubs") ?: JSONObject()

    var cOn by remember(root) { mutableStateOf(cm.optBoolean("enabled")) }
    var cId by remember(root) { mutableStateOf(cm.optString("id", "community-1")) }
    var cTitle by remember(root) { mutableStateOf(cm.optString("title", "🎉 Join the Cinedor family")) }
    var cMsg by remember(root) { mutableStateOf(cm.optString("message", "Get new movies, updates and requests first on our channels.")) }
    var cBtn by remember(root) { mutableStateOf(cm.optString("buttonText", "Join our Telegram channel")) }
    var cUrl by remember(root) { mutableStateOf(cm.optString("buttonUrl")) }
    var cBtn2 by remember(root) { mutableStateOf(cm.optString("secondaryText", "Visit our Facebook page")) }
    var cUrl2 by remember(root) { mutableStateOf(cm.optString("secondaryUrl")) }
    var cHours by remember(root) { mutableStateOf(cm.optInt("showEveryHours", 24).toString()) }

    var sOn by remember(root) { mutableStateOf(sp.optBoolean("enabled")) }
    var sId by remember(root) { mutableStateOf(sp.optString("id", "share-1")) }
    var sTitle by remember(root) { mutableStateOf(sp.optString("title", "Enjoying the movie? 🍿")) }
    var sMsg by remember(root) { mutableStateOf(sp.optString("message")) }
    var sMinutes by remember(root) { mutableStateOf(sp.optInt("afterMinutes", 20).toString()) }
    var sPerks by remember(root) {
        mutableStateOf(sp.optJSONArray("perks")?.let { a -> List(a.length()) { a.optString(it) }.joinToString(", ") } ?: "✨ Full HD, ⚡ Buffer free, 🎁 100% free")
    }
    var sShare by remember(root) { mutableStateOf(sp.optString("shareMessage")) }
    var sLink by remember(root) { mutableStateOf(sp.optString("buttonUrl")) }
    var sHours by remember(root) { mutableStateOf(sp.optInt("showEveryHours", 24).toString()) }

    var adOn by remember(root) { mutableStateOf(ad.optBoolean("enabled", true)) }
    var adTitle by remember(root) { mutableStateOf(ad.optString("title", "Audio & Dubs")) }

    var pOn by remember(root) { mutableStateOf(pr.optBoolean("enabled")) }
    var pId by remember(root) { mutableStateOf(pr.optString("id", "premium-1")) }
    var pTitle by remember(root) { mutableStateOf(pr.optString("title", "Go Premium")) }
    var pMsg by remember(root) { mutableStateOf(pr.optString("message")) }
    var pPerks by remember(root) {
        mutableStateOf(pr.optJSONArray("perks")?.let { a -> List(a.length()) { a.optString(it) }.joinToString(", ") } ?: "")
    }
    var pPrice by remember(root) { mutableStateOf(pr.optString("price")) }
    var pBtn by remember(root) { mutableStateOf(pr.optString("buttonText", "Get Premium")) }
    var pUrl by remember(root) { mutableStateOf(pr.optString("buttonUrl")) }
    var pHours by remember(root) { mutableStateOf(pr.optInt("showEveryHours", 24).toString()) }

    var aOn by remember(root) { mutableStateOf(an.optBoolean("enabled")) }
    var aId by remember(root) { mutableStateOf(an.optString("id", "notice-1")) }
    var aTitle by remember(root) { mutableStateOf(an.optString("title")) }
    var aMsg by remember(root) { mutableStateOf(an.optString("message")) }
    var aBtn by remember(root) { mutableStateOf(an.optString("buttonText", "OK")) }
    var aUrl by remember(root) { mutableStateOf(an.optString("buttonUrl")) }

    var mOn by remember(root) { mutableStateOf(mt.optBoolean("enabled")) }
    var mMsg by remember(root) { mutableStateOf(mt.optString("message")) }
    var minVer by remember(root) { mutableStateOf(root.optInt("minVersionCode", 0).toString()) }

    val prefs = remember { securePrefs(context) }
    var token by remember { mutableStateOf(prefs.getString("gh_token", "").orEmpty()) }

    fun buildJson(): String = JSONObject()
        .put("_help", "Cinedor Console: edit this file (or use the Console inside the app) and every phone picks up the change the next time the app opens.")
        .put(
            "premium", JSONObject()
                .put("enabled", pOn)
                .put("id", pId.ifBlank { "premium-1" })
                .put("title", pTitle)
                .put("message", pMsg)
                .put("perks", JSONArray(pPerks.split(",").map { it.trim() }.filter { it.isNotBlank() }))
                .put("price", pPrice)
                .put("buttonText", pBtn)
                .put("buttonUrl", pUrl.trim())
                .put("showEveryHours", pHours.toIntOrNull() ?: 24)
        )
        .put(
            "announcement", JSONObject()
                .put("enabled", aOn)
                .put("id", aId.ifBlank { "notice-1" })
                .put("title", aTitle)
                .put("message", aMsg)
                .put("buttonText", aBtn)
                .put("buttonUrl", aUrl.trim())
        )
        .put(
            "community", JSONObject()
                .put("enabled", cOn)
                .put("id", cId.ifBlank { "community-1" })
                .put("title", cTitle)
                .put("message", cMsg)
                .put("buttonText", cBtn)
                .put("buttonUrl", cUrl.trim())
                .put("secondaryText", cBtn2)
                .put("secondaryUrl", cUrl2.trim())
                .put("showEveryHours", cHours.toIntOrNull() ?: 24)
        )
        .put(
            "sharePrompt", JSONObject()
                .put("enabled", sOn)
                .put("id", sId.ifBlank { "share-1" })
                .put("title", sTitle)
                .put("message", sMsg)
                .put("afterMinutes", sMinutes.toIntOrNull() ?: 20)
                .put("perks", JSONArray(sPerks.split(",").map { it.trim() }.filter { it.isNotBlank() }))
                .put("shareMessage", sShare)
                .put("buttonUrl", sLink.trim())
                .put("showEveryHours", sHours.toIntOrNull() ?: 24)
        )
        .put("audioDubs", JSONObject().put("enabled", adOn).put("title", adTitle))
        .put("maintenance", JSONObject().put("enabled", mOn).put("message", mMsg))
        .put("minVersionCode", minVer.toIntOrNull() ?: 0)
        .toString(2)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(AccentBright, CinetorRedDark)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = OnAccent, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cinedor Console", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = OnAccent)
                Text(
                    if (loading) "Loading settings…" else "Changes reach every phone the next time the app opens",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnAccent.copy(alpha = 0.85f)
                )
            }
            if (loading) CircularProgressIndicator(color = OnAccent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        }

        // App overview
        AdminCard("Overview") {
            InfoLine("This phone", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
            InfoLine("Installed version", AppUpdater.currentVersionName)
            InfoLine("Latest published", latestBuild ?: "—")
            InfoLine("Settings file", "${RemoteConsole.REPO} · ${RemoteConsole.loadedRef}")
        }

        AdminCard("Users, devices & countries") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Seeing how many people use the app, their countries and phone models needs an online database " +
                        "(Firebase). Connect a Firebase project and this section will show live numbers.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }

        AdminCard("Premium popup") {
            AdminSwitch("Show Premium popup", pOn) { pOn = it }
            AdminField("Title", pTitle) { pTitle = it }
            AdminField("Message", pMsg, singleLine = false) { pMsg = it }
            AdminField("Perks (comma separated)", pPerks) { pPerks = it }
            AdminField("Price (optional)", pPrice) { pPrice = it }
            AdminField("Button text", pBtn) { pBtn = it }
            AdminField("Button link (WhatsApp, payment, website…)", pUrl) { pUrl = it }
            AdminField("Show again every (hours)", pHours, number = true) { pHours = it }
            AdminField("Popup ID (change it to show again to everyone)", pId) { pId = it }
        }

        AdminCard("Announcement") {
            AdminSwitch("Show announcement", aOn) { aOn = it }
            AdminField("Title", aTitle) { aTitle = it }
            AdminField("Message", aMsg, singleLine = false) { aMsg = it }
            AdminField("Button text", aBtn) { aBtn = it }
            AdminField("Button link (optional)", aUrl) { aUrl = it }
            AdminField("Announcement ID", aId) { aId = it }
        }

        AdminCard("Join us popup (Telegram / Facebook)") {
            AdminSwitch("Show Join us popup", cOn) { cOn = it }
            AdminField("Title", cTitle) { cTitle = it }
            AdminField("Message", cMsg, singleLine = false) { cMsg = it }
            AdminField("Button 1 text", cBtn) { cBtn = it }
            AdminField("Button 1 link (Telegram)", cUrl) { cUrl = it }
            AdminField("Button 2 text", cBtn2) { cBtn2 = it }
            AdminField("Button 2 link (Facebook)", cUrl2) { cUrl2 = it }
            AdminField("Show again every (hours)", cHours, number = true) { cHours = it }
            AdminField("Popup ID (change it to show again to everyone)", cId) { cId = it }
        }

        AdminCard("Share popup (\"Enjoying the movie?\")") {
            AdminSwitch("Ask viewers to share", sOn) { sOn = it }
            AdminField("Show after watching (minutes)", sMinutes, number = true) { sMinutes = it }
            AdminField("Title", sTitle) { sTitle = it }
            AdminField("Message (empty = automatic)", sMsg, singleLine = false) { sMsg = it }
            AdminField("Perks (comma separated, max 3)", sPerks) { sPerks = it }
            AdminField("Text that gets shared (empty = automatic)", sShare, singleLine = false) { sShare = it }
            AdminField("App download link in shared text", sLink) { sLink = it }
            AdminField("Ask again every (hours)", sHours, number = true) { sHours = it }
            AdminField("Popup ID", sId) { sId = it }
        }

        AdminCard("Audio & Dubs section") {
            AdminSwitch("Show Audio & Dubs on title pages", adOn) { adOn = it }
            AdminField("Section title", adTitle) { adTitle = it }
        }

        AdminCard("App control") {
            AdminSwitch("Maintenance mode (blocks the app)", mOn) { mOn = it }
            AdminField("Maintenance message", mMsg, singleLine = false) { mMsg = it }
            AdminField("Force update below version (e.g. 19, 0 = off)", minVer, number = true) { minVer = it }
        }

        AdminCard("Publish") {
            Text(
                "To publish, paste a GitHub token that can write to this repository " +
                    "(GitHub → Settings → Developer settings → Fine-grained tokens → this repo → Contents: Read and write). " +
                    "It is stored only on this phone.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            AdminField("GitHub token", token, password = true) {
                token = it.trim()
                prefs.edit().putString("gh_token", token).apply()
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(AccentBright, CinetorRedDark)))
                    .clickable(enabled = !saving) {
                        if (token.isBlank()) {
                            Toast.makeText(context, "Paste your GitHub token first", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        saving = true
                        scope.launch {
                            val err = RemoteConsole.save(token, buildJson())
                            saving = false
                            Toast.makeText(
                                context,
                                err ?: "Published! Phones get it the next time the app opens.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (saving) {
                        CircularProgressIndicator(color = OnAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = OnAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (saving) "Publishing…" else "Publish to all phones",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = OnAccent
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AdminCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glassBubble(RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp), color = TextPrimary)
        content()
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.width(130.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.6f))
}

@Composable
private fun AdminSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) CinetorRedSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = OnAccent, checkedTrackColor = CinetorRed)
        )
    }
}

@Composable
private fun AdminField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    number: Boolean = false,
    password: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CinetorRed,
            focusedLabelColor = CinetorRed,
            cursorColor = CinetorRed,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
