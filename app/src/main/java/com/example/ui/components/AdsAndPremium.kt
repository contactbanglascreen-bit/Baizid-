package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Premium = no ads. Status comes from a code redeemed in the app (made in the admin app). */
object PremiumState {
    var until by mutableLongStateOf(0L)
    val isActive: Boolean get() = until > System.currentTimeMillis()
    fun load(context: Context) {
        until = CinedorCloud.cachedPremiumUntil(context)
    }
}

/** Opens an ad / popup link: cinedor://item links open a title, others open the browser. */
fun openCinedorLink(context: Context, link: String) {
    if (link.isBlank()) return
    if (link.startsWith("cinedor://")) {
        PushLink.pending = link
        return
    }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Banner ad on Home (rotates every 8 s when there are several). Hidden for Premium users. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ads = RemoteConsole.config?.ads?.banners.orEmpty()
    if (ads.isEmpty() || PremiumState.isActive) return
    var index by remember(ads) { mutableIntStateOf((ads.indices).random()) }
    LaunchedEffect(ads) {
        while (ads.size > 1) { delay(8000); index = (index + 1) % ads.size }
    }
    val ad = ads[index.coerceIn(0, ads.size - 1)]
    Box(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .aspectRatio(3.2f)
            .glassBubble(RoundedCornerShape(18.dp))
            .clickable { openCinedorLink(context, ad.link) }
    ) {
        AsyncImage(model = ad.image, contentDescription = ad.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(
            "Ad",
            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/** Full-screen ad shown before a video, at most once every few minutes. Hidden for Premium users. */
object FullscreenAds {
    private var lastShownAt = 0L
    var showing by mutableStateOf<AdItem?>(null)

    /** Call when a title is opened. */
    fun maybeShow() {
        val cfg = RemoteConsole.config?.ads ?: return
        val list = cfg.fullscreens
        if (list.isEmpty() || PremiumState.isActive) return
        val now = System.currentTimeMillis()
        if (now - lastShownAt < cfg.fullscreenEveryMinutes.coerceAtLeast(1) * 60_000L) return
        lastShownAt = now
        showing = list.random()
    }
}

@Composable
fun FullscreenAdHost() {
    val context = LocalContext.current
    val ad = FullscreenAds.showing ?: return
    val skipAfter = RemoteConsole.config?.ads?.skipAfterSeconds ?: 5
    var left by remember(ad) { mutableIntStateOf(skipAfter.coerceIn(0, 30)) }
    LaunchedEffect(ad) { while (left > 0) { delay(1000); left-- } }
    Dialog(
        onDismissRequest = { if (left <= 0) FullscreenAds.showing = null },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = ad.image,
                contentDescription = ad.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clickable { openCinedorLink(context, ad.link); FullscreenAds.showing = null }
            )
            Row(
                Modifier.statusBarsPadding().padding(14.dp).align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ad",
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable(enabled = left <= 0) { FullscreenAds.showing = null }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (left > 0) Text("Skip in $left", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Skip", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Close, contentDescription = "Close ad", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (PremiumCta.visible) {
                Text(
                    "Remove ads with Premium",
                    color = Color(0xFFF5C451), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFFF5C451).copy(alpha = 0.6f), CircleShape)
                        .clickable { FullscreenAds.showing = null; PremiumCta.open = true }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/** Lets an ad open the Premium page in Me. */
object PremiumCta {
    val visible: Boolean get() = true
    var open by mutableStateOf(false)
}

/** Me › Premium: status, redeem a code, buy button (link set in the admin app). */
@Composable
fun PremiumPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { CinedorCloud.refreshPremium(context) }
    val gold = Color(0xFFF5C451)
    val premium = RemoteConsole.config?.premium
    val active = PremiumState.isActive

    Column(Modifier.fillMaxWidth().padding(bottom = 120.dp)) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2B1B05), Color(0xFF120C04))))
                .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFE39A), gold, Color(0xFFC8901A)))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color(0xFF3A2500), modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (active) "Premium active" else "Cinedor Premium", color = gold, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(
                            if (active) "Until " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(PremiumState.until))
                            else "No ads · support Cinedor",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val perks = premium?.perks?.ifEmpty { null } ?: listOf("No ads anywhere", "Faster downloads", "Support new features")
                perks.forEach { perk ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(perk, color = Color.White, fontSize = 14.sp)
                    }
                }
                if (!active && premium != null && premium.buttonUrl.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFE39A), gold, Color(0xFFC8901A))))
                            .clickable { openCinedorLink(context, premium.buttonUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (premium.buttonText.ifBlank { "Get Premium" }) + if (premium.price.isNotBlank()) " · ${premium.price}" else "",
                            color = Color(0xFF3A2500), fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Have a code?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text("Enter the Premium code you received from Cinedor.", color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().take(24) },
                placeholder = { Text("CINE-XXXX-XXXX") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = gold, unfocusedBorderColor = ObsidianBorder, cursorColor = gold),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CinetorRed)
                    .clickable(enabled = !busy) {
                        busy = true
                        scope.launch {
                            val (ok, msg) = CinedorCloud.redeem(context, code)
                            busy = false
                            if (ok) code = ""
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Activate", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Premium is linked to this phone.", color = TextMuted, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 16.dp))
    }
}
