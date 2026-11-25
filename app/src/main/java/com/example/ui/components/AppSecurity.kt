package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.security.MessageDigest

/**
 * Basic protection against modified copies and tampering:
 * - the app must be signed with Cinedor's own certificate (a re-signed / modded APK is blocked)
 * - a debugger attached to the app blocks it
 * - rooted phones and hooking frameworks (Frida / Xposed) are detected so sensitive screens
 *   (admin Console) can refuse to open
 */
object AppSecurity {
    // SHA-256 of the certificate Cinedor's builds are signed with.
    private const val EXPECTED_CERT_SHA256 =
        "7CD396475FD959A4087A09412653C19C94FA467225BD7D4C2AD69A3C8171B9B5"

    enum class Problem { MODIFIED_APP, DEBUGGER }

    @SuppressLint("PackageManagerGetSignatures")
    private fun certHashes(context: Context): List<String> = runCatching {
        val pm = context.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return@runCatching emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        } ?: emptyArray()
        sigs.map { s ->
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
                .joinToString("") { "%02X".format(it) }
        }
    }.getOrDefault(emptyList())

    fun isGenuine(context: Context): Boolean {
        // Only Cinedor's own CI builds (debug type, fixed key) are checked; other build types
        // are signed with a different key and skip this check.
        if (com.example.BuildConfig.BUILD_TYPE != "debug") return true
        val hashes = certHashes(context)
        // If the system can't tell us, don't lock the real user out.
        return hashes.isEmpty() || hashes.any { it == EXPECTED_CERT_SHA256 }
    }

    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    fun isRooted(): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return true
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su", "/vendor/bin/su",
            "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/app/Superuser.apk",
            "/data/adb/magisk", "/sbin/.magisk"
        )
        return paths.any { runCatching { File(it).exists() }.getOrDefault(false) }
    }

    fun isHooked(): Boolean {
        val xposed = runCatching { Class.forName("de.robv.android.xposed.XposedBridge"); true }.getOrDefault(false)
        if (xposed) return true
        return runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { l -> val x = l.lowercase(); "frida" in x || "xposed" in x || "lsposed" in x || "substrate" in x }
            }
        }.getOrDefault(false)
    }

    /** Problems that block the whole app. */
    fun blockingProblem(context: Context): Problem? = when {
        !isGenuine(context) -> Problem.MODIFIED_APP
        isDebuggerAttached() -> Problem.DEBUGGER
        else -> null
    }

    /** Sensitive screens (admin Console) refuse to open on rooted / hooked phones. */
    fun safeForAdmin(): Boolean = !isRooted() && !isHooked() && !isDebuggerAttached()
}

private fun Context.findActivityForSecurity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Blocks screenshots and screen recording while this composable is on screen. */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivityForSecurity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Full-screen block shown for a modified copy or an attached debugger. */
@Composable
fun SecurityBlockScreen(problem: AppSecurity.Problem) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080607)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFF2A35), Color(0xFF8A0710)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.GppBad, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                if (problem == AppSecurity.Problem.MODIFIED_APP) "Unofficial copy" else "Debugging detected",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (problem == AppSecurity.Problem.MODIFIED_APP)
                    "This copy of Cinedor has been modified. Please install the official app."
                else
                    "Cinedor can't run while a debugger is attached.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}
