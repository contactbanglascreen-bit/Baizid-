package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ChipBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PIN lock for 18+ content (parental control).
 * The PIN is stored only as a salted SHA-256 hash on the device.
 */
object AdultLock {
    private const val PREFS = "cinedor_adult_lock"
    private const val PIN_LENGTH = 4
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_MS = 30_000L

    /** True once the PIN was entered in this app session; resets when the app restarts. */
    var sessionUnlocked by mutableStateOf(false)

    var hasPin by mutableStateOf(false)
        private set
    private var loaded = false

    val pinLength: Int get() = PIN_LENGTH

    fun load(context: Context) {
        if (loaded) return
        hasPin = prefs(context).getString("hash", null) != null
        loaded = true
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun setPin(context: Context, pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        prefs(context).edit()
            .putString("salt", salt)
            .putString("hash", hash(pin, salt))
            .putInt("fails", 0)
            .putLong("lockUntil", 0L)
            .apply()
        hasPin = true
    }

    fun removePin(context: Context) {
        prefs(context).edit().clear().apply()
        hasPin = false
        sessionUnlocked = false
    }

    fun lockedUntil(context: Context): Long = prefs(context).getLong("lockUntil", 0L)

    /** Returns true if the PIN is correct. Counts failures and locks out for 30s after 5 wrong tries. */
    fun verify(context: Context, pin: String): Boolean {
        val p = prefs(context)
        if (System.currentTimeMillis() < p.getLong("lockUntil", 0L)) return false
        val salt = p.getString("salt", null) ?: return false
        val ok = hash(pin, salt) == p.getString("hash", null)
        if (ok) {
            p.edit().putInt("fails", 0).putLong("lockUntil", 0L).apply()
        } else {
            val fails = p.getInt("fails", 0) + 1
            val e = p.edit().putInt("fails", fails)
            if (fails >= MAX_ATTEMPTS) e.putInt("fails", 0).putLong("lockUntil", System.currentTimeMillis() + LOCKOUT_MS)
            e.apply()
        }
        return ok
    }
}

enum class PinMode { CREATE, VERIFY }

/**
 * Full PIN keypad dialog.
 * CREATE: asks for a new PIN twice. VERIFY: checks the saved PIN.
 */
@Composable
fun AdultPinDialog(
    mode: PinMode,
    title: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entered by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var lockedUntil by remember { mutableLongStateOf(AdultLock.lockedUntil(context)) }
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val shake = remember { Animatable(0f) }
    var errorTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            nowTick = System.currentTimeMillis()
            delay(500L)
        }
        nowTick = System.currentTimeMillis()
    }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    val isLockedOut = nowTick < lockedUntil
    val subtitle = when {
        isLockedOut -> "Too many wrong tries. Try again in ${((lockedUntil - nowTick) / 1000) + 1}s"
        message != null -> message!!
        mode == PinMode.CREATE && firstPin == null -> "Create a ${AdultLock.pinLength}-digit PIN to lock 18+ content"
        mode == PinMode.CREATE -> "Enter the same PIN again to confirm"
        else -> "Enter your ${AdultLock.pinLength}-digit PIN"
    }

    fun onComplete(pin: String) {
        if (mode == PinMode.CREATE) {
            if (firstPin == null) {
                firstPin = pin
                entered = ""
                message = null
            } else if (firstPin == pin) {
                AdultLock.setPin(context, pin)
                AdultLock.sessionUnlocked = true
                onSuccess()
            } else {
                firstPin = null
                entered = ""
                message = "PINs didn't match. Start again."
                errorTick++
            }
        } else {
            if (AdultLock.verify(context, pin)) {
                AdultLock.sessionUnlocked = true
                onSuccess()
            } else {
                entered = ""
                lockedUntil = AdultLock.lockedUntil(context)
                message = "Wrong PIN"
                errorTick++
            }
        }
    }

    fun press(d: String) {
        if (isLockedOut || entered.length >= AdultLock.pinLength) return
        entered += d
        message = null
        if (entered.length == AdultLock.pinLength) {
            val pin = entered
            scope.launch {
                delay(120L)
                onComplete(pin)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CinetorRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (message != null || isLockedOut) CinetorRed else TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.offset { IntOffset(shake.value.toInt(), 0) },
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(AdultLock.pinLength) { i ->
                    val filled = i < entered.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) CinetorRed else ChipBg)
                            .border(1.5.dp, if (filled) CinetorRed else ObsidianBorder, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<")
            keys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { k ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.6f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (k.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else ChipBg)
                                .clickable(enabled = k.isNotEmpty() && !isLockedOut) {
                                    if (k == "<") entered = entered.dropLast(1) else press(k)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "<") {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else if (k.isNotEmpty()) {
                                Text(
                                    text = k,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                                    color = if (isLockedOut) TextMuted else TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
            if (mode == PinMode.VERIFY) {
                Text(
                    text = "Forgot PIN? Clear the app's data in phone Settings to reset it.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(260.dp)
                )
            }
        }
    }
}

/**
 * Returns a safe-mode setter that asks for the PIN before 18+ content is shown.
 * Turning Family-Safe Mode back on (locking) never needs a PIN.
 */
@Composable
fun rememberGuardedSafeModeToggle(onUpdateSafeMode: (Boolean) -> Unit): (Boolean) -> Unit {
    var pending by remember { mutableStateOf<PinMode?>(null) }
    pending?.let { mode ->
        AdultPinDialog(
            mode = mode,
            title = if (mode == PinMode.CREATE) "Set 18+ PIN" else "Unlock 18+",
            onSuccess = {
                pending = null
                onUpdateSafeMode(false)
            },
            onDismiss = { pending = null }
        )
    }
    return { safeModeOn ->
        if (safeModeOn) {
            AdultLock.sessionUnlocked = false
            onUpdateSafeMode(true)
        } else if (AdultLock.sessionUnlocked && AdultLock.hasPin) {
            onUpdateSafeMode(false)
        } else {
            pending = if (AdultLock.hasPin) PinMode.VERIFY else PinMode.CREATE
        }
    }
}
