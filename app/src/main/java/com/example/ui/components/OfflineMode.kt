package com.example.ui.components

import androidx.compose.runtime.LaunchedEffect
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private fun Context.isOnlineNow(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Live "is the phone online" state.
 * Some phones cut an app's network while it is in the background (battery / data saver), and
 * Android doesn't always say "online again" when you come back. So the state is re-checked every
 * time the app returns to the screen, re-checked every 2 s while offline, and "offline" is only
 * shown after it has lasted a few seconds (no false "no internet" after switching apps).
 */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    var rawOnline by remember { mutableStateOf(context.isOnlineNow()) }
    var shownOnline by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { rawOnline = true }
            override fun onLost(network: Network) { rawOnline = context.isOnlineNow() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                rawOnline = context.isOnlineNow()
            }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    // Re-check whenever the app comes back to the front.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) rawOnline = context.isOnlineNow()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(rawOnline) {
        if (rawOnline) {
            shownOnline = true
        } else {
            // Keep checking; only call it "offline" if it is still offline after ~4 seconds.
            repeat(2) {
                kotlinx.coroutines.delay(2000)
                if (context.isOnlineNow()) { rawOnline = true; return@LaunchedEffect }
            }
            shownOnline = false
            while (!context.isOnlineNow()) kotlinx.coroutines.delay(2000)
            rawOnline = true
        }
    }
    return shownOnline
}

/** Shown when there is no internet: everything else needs a connection, downloads still play. */
@Composable
fun OfflineOnlyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CinetorRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "You're offline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = TextPrimary
                )
                Text(
                    "Connect to the internet to browse and stream. Your downloads still play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        DownloadsPage(modifier = Modifier.fillMaxSize())
    }
}
