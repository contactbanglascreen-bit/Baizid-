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
import androidx.compose.material.icons.rounded.PlayArrow
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
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val remote = CinedorCloud.announcements(context)
        items = NotificationInbox.merge(NotificationInbox.items(context), remote)
        loading = false
        NotificationInbox.markAllRead(context)
    }
    val glassBorder = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
    )
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DialogBackdropBlur()
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF2A0A10), Color(0xFF0E0A0C), Color(0xFF07060A))
                    )
                )
        ) {
            // soft red glow behind the header
            Box(
                Modifier
                    .size(320.dp)
                    .offset(x = (-80).dp, y = (-120).dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(CinetorRed.copy(alpha = 0.35f), Color.Transparent)))
            )
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 10.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).border(1.dp, glassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(
                            if (items.isEmpty()) "You're all caught up" else "${items.size} updates from Cinedor",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 12.5.sp
                        )
                    }
                    if (items.isNotEmpty()) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).border(1.dp, glassBorder, CircleShape)
                                .clickable { NotificationInbox.clear(context); items = emptyList() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (items.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(90.dp))
                        Box(
                            Modifier.size(96.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, glassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (loading) androidx.compose.material3.CircularProgressIndicator(color = CinetorRed, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                            else Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(44.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(if (loading) "Loading…" else "No notifications yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (!loading) Text("New movies, episodes and news will show up here.", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 30.dp)) {
                        items(items, key = { it.at.toString() + it.title }) { n ->
                            val shape = RoundedCornerShape(24.dp)
                            Column(
                                Modifier
                                    .padding(vertical = 7.dp)
                                    .fillMaxWidth()
                                    .shadow(16.dp, shape, spotColor = Color.Black)
                                    .clip(shape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
                                        )
                                    )
                                    .border(1.dp, glassBorder, shape)
                                    .clickable(enabled = n.link.isNotBlank()) {
                                        PushLink.pending = n.link
                                        onClose()
                                    }
                            ) {
                                if (n.image.startsWith("http")) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                                        AsyncImage(
                                            model = n.image,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            Modifier.fillMaxSize().background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                        // glass time chip
                                        Text(
                                            android.text.format.DateUtils.getRelativeTimeSpanString(n.at).toString(),
                                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.35f))
                                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                        Text(
                                            n.title,
                                            color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 14.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                                Column(Modifier.padding(14.dp)) {
                                    if (!n.image.startsWith("http")) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(CinetorRed, Color(0xFF7A0710)))),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(n.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    android.text.format.DateUtils.getRelativeTimeSpanString(n.at).toString(),
                                                    color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                        if (n.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                                    }
                                    if (n.body.isNotBlank()) {
                                        Text(n.body, color = Color.White.copy(alpha = 0.75f), fontSize = 13.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (n.link.isNotBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            Modifier
                                                .clip(CircleShape)
                                                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(CinetorRed, Color(0xFF9A0A14))))
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(androidx.compose.material.icons.Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Watch now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
