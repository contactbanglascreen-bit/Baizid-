package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CinedorCloud
import com.example.ui.components.FeedbackItem
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val FEEDBACK_TYPES = listOf(
    "🎬 Video not playing",
    "💬 Subtitles",
    "⬇️ Download",
    "🐞 App problem",
    "🙏 Request a title",
    "💡 Idea",
    "✍️ Other"
)

/** Me › Feedback & help: send a problem, see whether the team fixed it. */
@Composable
internal fun FeedbackPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(FEEDBACK_TYPES.first()) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<FeedbackItem>?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) { items = CinedorCloud.myFeedback(context) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CinetorRed,
        unfocusedBorderColor = ObsidianBorder,
        focusedContainerColor = ObsidianCard,
        unfocusedContainerColor = ObsidianCard,
        cursorColor = CinetorRed,
        focusedLabelColor = CinetorRed
    )

    Column(Modifier.fillMaxWidth().padding(bottom = 120.dp)) {
        // Header card
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(AccentBright, CinetorRedDark)))
                .padding(18.dp)
        ) {
            Column {
                Text("Tell us what's wrong", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(
                    "We read every message. When it's fixed you'll get a notification.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Text("What is it about?", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FEEDBACK_TYPES) { t ->
                val sel = t == type
                Text(
                    t,
                    color = if (sel) OnAccent else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (sel) CinetorRed else ObsidianCard)
                        .border(1.dp, if (sel) CinetorRed else ObsidianBorder, CircleShape)
                        .clickable { type = t }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(2000) },
                label = { Text(if (type.contains("Request")) "Which movie or series?" else "Describe the problem") },
                minLines = 4,
                colors = fieldColors,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it.take(200) },
                label = { Text("Email or Telegram (optional)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(AccentBright, CinetorRedDark)))
                    .clickable(enabled = !sending) {
                        if (message.isBlank()) {
                            Toast.makeText(context, "Please write a few words", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        sending = true
                        scope.launch {
                            val ok = CinedorCloud.sendFeedback(context, type, message.trim(), contact.trim())
                            sending = false
                            if (ok) {
                                message = ""
                                Toast.makeText(context, "Thanks! We got your message ✓", Toast.LENGTH_SHORT).show()
                                reload++
                            } else {
                                Toast.makeText(context, "Couldn't send. Check the internet and try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (sending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send feedback", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("My feedback", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinetorRed, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            list.isEmpty() -> Text(
                "Nothing sent yet.",
                color = TextMuted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            else -> list.forEach { f -> FeedbackCard(f) }
        }
    }
}

@Composable
private fun FeedbackCard(f: FeedbackItem) {
    val fixed = f.isFixed
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianCard)
            .border(1.dp, if (fixed) Color(0xFF22C55E).copy(alpha = 0.5f) else ObsidianBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (fixed) Color(0xFF22C55E).copy(alpha = 0.15f) else CinetorRedSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (fixed) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = if (fixed) Color(0xFF22C55E) else CinetorRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (fixed) "Fixed" else "Received",
                    color = if (fixed) Color(0xFF22C55E) else CinetorRed,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(f.message, color = TextSecondary, fontSize = 13.5.sp)
        if (f.reply.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceVariant)
                    .padding(10.dp)
            ) {
                Text("Cinedor team", color = CinetorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(f.reply, color = TextPrimary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(f.createdAt.take(10), color = TextMuted, fontSize = 11.sp)
    }
}
