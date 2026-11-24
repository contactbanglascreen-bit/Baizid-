package com.example.ui.screens

import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.imageLoader
import com.example.data.local.UserPreferences
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.model.ContentLanguage
import com.example.data.model.MediaItem
import com.example.ui.components.AppSettings
import com.example.ui.components.DownloadStore
import com.example.ui.components.DownloadsPage
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.PosterCard
import com.example.ui.components.downloadedBytes
import com.example.ui.theme.CinetorBlush
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OnAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Avatar shapes
// ---------------------------------------------------------------------------

private fun polygonShape(points: Int, innerRatio: Float? = null, rotationDeg: Float = -90f): Shape =
    GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(cx, cy)
        val total = if (innerRatio != null) points * 2 else points
        for (i in 0 until total) {
            val radius = if (innerRatio != null && i % 2 == 1) r * innerRatio else r
            val angle = Math.toRadians((rotationDeg + i * 360.0 / total)).toFloat()
            val p = Offset(cx + radius * cos(angle), cy + radius * sin(angle))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

private val AvatarShapes: List<Shape> = listOf(
    CircleShape,
    RoundedCornerShape(28),
    polygonShape(6, rotationDeg = -90f),
    polygonShape(4, rotationDeg = -90f),
    polygonShape(8, innerRatio = 0.72f),
    RoundedCornerShape(topStartPercent = 50, topEndPercent = 12, bottomEndPercent = 50, bottomStartPercent = 12)
)

@Composable
fun ProfileAvatar(style: Int, name: String, size: Dp, modifier: Modifier = Modifier) {
    val shape = AvatarShapes[style.coerceIn(0, AvatarShapes.lastIndex)]
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFEF1B26), Color(0xFFC00B15)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "M",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp
            ),
            color = OnAccent
        )
    }
}

// ---------------------------------------------------------------------------
// Me hub
// ---------------------------------------------------------------------------

private enum class MePage { HUB, LIBRARY, HISTORY, DOWNLOADS, SETTINGS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeHubScreen(
    preferences: UserPreferences,
    watchlist: List<WatchlistItem>,
    history: List<WatchHistoryItem>,
    onOpenMedia: (MediaItem) -> Unit,
    onResumeHistory: (WatchHistoryItem) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onUpdateLanguage: (ContentLanguage) -> Unit,
    onUpdateSafeMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(MePage.HUB) }
    var editOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = page != MePage.HUB) { page = MePage.HUB }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CinetorBlush, ObsidianBackground, ObsidianBackground)))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        // Page header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page != MePage.HUB) {
                IconButton(onClick = { page = MePage.HUB }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when (page) {
                    MePage.HUB -> "Me"
                    MePage.LIBRARY -> "Library"
                    MePage.HISTORY -> "Watch history"
                    MePage.DOWNLOADS -> "Downloads"
                    MePage.SETTINGS -> "Settings"
                    MePage.ABOUT -> "About"
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (page == MePage.HISTORY && history.isNotEmpty()) {
                var confirm by remember { mutableStateOf(false) }
                TextButton(onClick = { confirm = true }) {
                    Text("Clear all", color = CinetorRed, fontWeight = FontWeight.SemiBold)
                }
                if (confirm) {
                    ConfirmDialog(
                        title = "Clear watch history?",
                        body = "Continue Watching and saved positions will be removed.",
                        confirm = "Clear",
                        onConfirm = { onClearHistory(); confirm = false },
                        onDismiss = { confirm = false }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (page) {
            MePage.HUB -> {
                ProfileCard(
                    watchlistCount = watchlist.size,
                    watchedCount = history.size,
                    onEdit = { editOpen = true }
                )
                Spacer(modifier = Modifier.height(14.dp))
                val downloadedCount = remember(page) {
                    DownloadStore.entries(context).count { it.state == com.example.ui.components.DownloadState.DONE }
                }
                MenuCard {
                    MenuRow(Icons.Rounded.Bookmarks, "Library", "My List (${watchlist.size})") { page = MePage.LIBRARY }
                    MenuDivider()
                    MenuRow(Icons.Rounded.History, "Watch history", "${history.size} watched") { page = MePage.HISTORY }
                    MenuDivider()
                    MenuRow(Icons.Rounded.Download, "Downloads", "$downloadedCount downloaded") { page = MePage.DOWNLOADS }
                    MenuDivider()
                    MenuRow(Icons.Rounded.Settings, "Settings", "Player, downloads & privacy") { page = MePage.SETTINGS }
                    MenuDivider()
                    MenuRow(Icons.Rounded.Info, "About", "Cinedor · version & info") { page = MePage.ABOUT }
                }
            }

            MePage.LIBRARY -> {
                if (watchlist.isEmpty()) {
                    EmptyLibraryCard(
                        icon = Icons.Rounded.Bookmarks,
                        title = "Your List is Empty",
                        subtitle = "Tap + My List on any movie or series to save it here."
                    )
                } else {
                    watchlist.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { w ->
                                PosterCard(
                                    item = w.toMediaItem(),
                                    onClick = { onOpenMedia(w.toMediaItem()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }

            MePage.HISTORY -> HistoryPage(history, onResumeHistory, onRemoveHistory)

            MePage.DOWNLOADS -> DownloadsPage()

            MePage.SETTINGS -> SettingsPage(
                preferences = preferences,
                onUpdateLanguage = onUpdateLanguage,
                onUpdateSafeMode = onUpdateSafeMode,
                onClearHistory = onClearHistory
            )

            MePage.ABOUT -> AboutPage()
        }
    }

    if (editOpen) {
        EditProfileSheet(onDismiss = { editOpen = false })
    }
}

@Composable
private fun ProfileCard(watchlistCount: Int, watchedCount: Int, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(3.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(style = AppSettings.avatarStyle, name = AppSettings.profileName, size = 60.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppSettings.profileName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$watchlistCount in My List · $watchedCount watched",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = TextSecondary
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, ObsidianBorder, CircleShape)
                .clickable(onClick = onEdit)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Edit", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
        }
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 4.dp)
    ) { content() }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = ObsidianBorder.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 62.dp, end = 16.dp)
    )
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CinetorRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = CinetorRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        containerColor = ObsidianCard
    )
}

// ---------------------------------------------------------------------------
// Edit profile sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(AppSettings.profileName) }
    var style by remember { mutableIntStateOf(AppSettings.avatarStyle) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF6F1EF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(style = style, name = name, size = 58.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    "Edit profile",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    AppSettings.setProfile(context, name, style)
                    onDismiss()
                }) {
                    Text("Save", color = CinetorRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Display name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
            Spacer(modifier = Modifier.height(18.dp))
            Text("Choose an avatar", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            AvatarShapes.indices.chunked(4).forEach { rowIdx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowIdx.forEach { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (style == i) CinetorRedSoft else Color.Transparent)
                                .border(
                                    2.dp,
                                    if (style == i) CinetorRed else Color.Transparent,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { style = i }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatar(style = i, name = name, size = 58.dp)
                        }
                    }
                    repeat(4 - rowIdx.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Watch history page
// ---------------------------------------------------------------------------

@Composable
private fun HistoryPage(
    history: List<WatchHistoryItem>,
    onResume: (WatchHistoryItem) -> Unit,
    onRemove: (String) -> Unit
) {
    if (history.isEmpty()) {
        EmptyLibraryCard(
            icon = Icons.Rounded.History,
            title = "No Watch History Yet",
            subtitle = "Start playing anything and your progress is saved here."
        )
        return
    }
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    val groups = history.sortedByDescending { it.updatedAtMillis }.groupBy {
        val age = now - it.updatedAtMillis
        when {
            age < day -> "Today"
            age < 7 * day -> "This week"
            else -> "Earlier"
        }
    }
    listOf("Today", "This week", "Earlier").forEach { label ->
        val items = groups[label].orEmpty()
        if (items.isEmpty()) return@forEach
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        items.forEach { entry -> HistoryRow(entry, onResume, onRemove) }
    }
}

@Composable
private fun HistoryRow(entry: WatchHistoryItem, onResume: (WatchHistoryItem) -> Unit, onRemove: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            .clickable { onResume(entry) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianSurfaceVariant)
        ) {
            AsyncImage(
                model = entry.posterUrl,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(entry.progressFraction.coerceIn(0.04f, 1f))
                        .height(3.dp)
                        .background(CinetorRed)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val isSeries = entry.mediaType != com.example.data.model.MediaCategoryType.MOVIE.wireValue
            val ago = DateUtils.getRelativeTimeSpanString(
                entry.updatedAtMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            Text(
                text = if (isSeries) "S${entry.season} · E${entry.episode} · $ago" else ago,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = TextSecondary)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Resume") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CinetorRed) },
                    onClick = { menuOpen = false; onResume(entry) }
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = TextSecondary) },
                    onClick = { menuOpen = false; onRemove(entry.id) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings page
// ---------------------------------------------------------------------------

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp), color = TextPrimary)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnAccent,
                checkedTrackColor = CinetorRed,
                uncheckedBorderColor = ObsidianBorder
            )
        )
    }
}

@Composable
private fun SettingLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun SmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(if (selected) CinetorRed else ObsidianCard)
            .border(1.dp, if (selected) CinetorRed else ObsidianBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (selected) OnAccent else TextPrimary
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun SettingsPage(
    preferences: UserPreferences,
    onUpdateLanguage: (ContentLanguage) -> Unit,
    onUpdateSafeMode: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var storageTick by remember { mutableIntStateOf(0) }
    val downloadsSize = remember(storageTick) { downloadedBytes(context) }
    val cacheSize = remember(storageTick) {
        runCatching { context.imageLoader.diskCache?.size ?: 0L }.getOrDefault(0L)
    }

    SettingsCard("Player") {
        Text("Double-tap skip", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 30).forEach { sec ->
                SmallChip("${sec}s", AppSettings.doubleTapSkipSec == sec) { AppSettings.setSkip(context, sec) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        SettingSwitchRow(
            title = "Hold for 2× speed",
            subtitle = "Press and hold the video to play at 2× speed",
            checked = AppSettings.holdFor2x,
            onChange = { AppSettings.setHold2x(context, it) }
        )
    }

    SettingsCard("Downloads") {
        SettingSwitchRow(
            title = "Download on Wi-Fi only",
            subtitle = "New downloads wait for Wi-Fi instead of using mobile data",
            checked = AppSettings.wifiOnlyDownloads,
            onChange = { AppSettings.setWifiOnly(context, it) }
        )
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text("Downloaded media storage", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
            Text(
                "${Formatter.formatShortFileSize(context, downloadsSize)} used by Cinedor",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text("Download folder", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
            Text("Internal storage (app)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
        }
    }

    SettingsCard("Content & Privacy") {
        SettingSwitchRow(
            title = "Family-Safe Mode",
            subtitle = "Hide mature 18+ categories and keep feeds clean",
            checked = preferences.safeMode,
            onChange = onUpdateSafeMode
        )
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Preferred language", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(ContentLanguage.entries) { lang ->
                SmallChip(lang.label, preferences.language == lang) { onUpdateLanguage(lang) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        SettingLinkRow("Clear watch history", "Remove Continue Watching and saved playback positions") {
            confirmClearHistory = true
        }
        HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.7f))
        SettingLinkRow(
            "Clear image cache",
            "Artwork cache · ${Formatter.formatShortFileSize(context, cacheSize)} used"
        ) {
            runCatching {
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
            }
            storageTick++
            Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(22.dp))
            .clickable { confirmReset = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CinetorRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = CinetorRed, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Reset settings", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = CinetorRed)
            Text(
                "Restore player and download preferences to default. History and My List are kept.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary
            )
        }
    }

    if (confirmClearHistory) {
        ConfirmDialog(
            title = "Clear watch history?",
            body = "Continue Watching and saved positions will be removed.",
            confirm = "Clear",
            onConfirm = { onClearHistory(); confirmClearHistory = false },
            onDismiss = { confirmClearHistory = false }
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = "Reset settings?",
            body = "Player and download preferences go back to default.",
            confirm = "Reset",
            onConfirm = {
                AppSettings.reset(context)
                confirmReset = false
                Toast.makeText(context, "Settings reset", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { confirmReset = false }
        )
    }
}

// ---------------------------------------------------------------------------
// About page
// ---------------------------------------------------------------------------

@Composable
private fun AboutPage() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.cinedor_logo),
            contentDescription = "Cinedor",
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Cinedor", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = TextPrimary)
        Text("Version $version", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Movies, series, anime, dramas and live sports in one place. Download in any quality and watch offline.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
