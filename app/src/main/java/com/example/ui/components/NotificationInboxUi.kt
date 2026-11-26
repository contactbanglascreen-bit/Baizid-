package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Round bell button with an unread badge (Home header). */
@Composable
fun NotificationBellButton(size: Int = 40) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { NotificationInbox.load(context) }
    Box {
        Box(
            modifier = Modifier
                .size(size.dp)
                .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(ObsidianSurface)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = TextPrimary, modifier = Modifier.size((size * 0.52f).dp))
        }
        val n = NotificationInbox.unread
        if (n > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(CinetorRed)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (n > 9) "9+" else n.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
    if (open) NotificationInboxDialog(onClose = { open = false })
}

@Composable
fun NotificationInboxDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(NotificationInbox.items(context)) }
    LaunchedEffect(Unit) {
        val remote = CinedorCloud.announcements(context)
        items = NotificationInbox.merge(NotificationInbox.items(context), remote)
        NotificationInbox.markAllRead(context)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(ObsidianBackground).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Text("Notifications", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    IconButton(onClick = { NotificationInbox.clear(context); items = emptyList() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all", tint = TextSecondary)
                    }
                }
            }
            if (items.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(80.dp))
                    Box(Modifier.size(84.dp).clip(CircleShape).background(CinetorRedSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(42.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("New movies, episodes and news will show up here.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    items(items, key = { it.at.toString() + it.title }) { n ->
                        Column(
                            Modifier
                                .padding(vertical = 5.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(ObsidianCard)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                                .clickable(enabled = n.link.isNotBlank()) {
                                    PushLink.pending = n.link
                                    onClose()
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(CinetorRedSoft), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(n.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    if (n.body.isNotBlank()) Text(n.body, color = TextSecondary, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        android.text.format.DateUtils.getRelativeTimeSpanString(n.at).toString(),
                                        color = TextMuted, style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (n.image.startsWith("http")) {
                                Spacer(Modifier.height(10.dp))
                                AsyncImage(
                                    model = n.image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
