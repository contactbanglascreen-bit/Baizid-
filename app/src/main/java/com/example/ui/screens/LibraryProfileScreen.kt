package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UpcomingReminder
import com.example.data.local.UserPreferences
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.model.ApiStatusInfo
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentSource
import com.example.data.model.MediaItem
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.PosterCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class LibraryMode { MY_LIST, ME }

@Composable
fun LibraryProfileScreen(
    mode: LibraryMode = LibraryMode.MY_LIST,
    preferences: UserPreferences,
    watchlist: List<WatchlistItem>,
    history: List<WatchHistoryItem>,
    reminders: List<UpcomingReminder>,
    apiStatus: ApiStatusInfo?,
    isDualSiteActive: Boolean = false,
    onOpenMedia: (MediaItem) -> Unit,
    onResumeHistory: (WatchHistoryItem) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveReminder: (UpcomingReminder) -> Unit,
    onUpdateSource: (ContentSource) -> Unit,
    onUpdateLanguage: (ContentLanguage) -> Unit,
    onUpdateSafeMode: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit = {},
    onCheckApiStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(mode) { mutableIntStateOf(if (mode == LibraryMode.ME) 1 else 0) }

    LaunchedEffect(Unit) {
        onCheckApiStatus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Branded On-Device Library Summary Card (No Login Required)
        item(key = "library_header") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                CinedorGradientColors.first().copy(alpha = 0.45f),
                                ObsidianBorder,
                                CinedorGradientColors.last().copy(alpha = 0.45f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(ObsidianSurface)
                                .border(1.5.dp, Brush.linearGradient(CinedorGradientColors), RoundedCornerShape(15.dp))
                                .padding(7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cinedor_logo),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.library_header_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = LiveEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.library_local_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LiveEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LibraryStatChip(
                            count = watchlist.size,
                            label = "Saved",
                            accent = CrimsonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        LibraryStatChip(
                            count = history.size,
                            label = "Watched",
                            accent = CinemaGold,
                            modifier = Modifier.weight(1f)
                        )
                        LibraryStatChip(
                            count = reminders.size,
                            label = "Reminders",
                            accent = LiveEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // When in ME mode, show Streaming Preferences & Engine Status right at the top!
        if (mode == LibraryMode.ME) {
            item(key = "me_preferences_top") {
                StreamingPreferencesCard(
                    preferences = preferences,
                    apiStatus = apiStatus,
                    isDualSiteActive = isDualSiteActive,
                    onUpdateSource = onUpdateSource,
                    onUpdateLanguage = onUpdateLanguage,
                    onUpdateSafeMode = onUpdateSafeMode,
                    onSaveApiKey = onSaveApiKey,
                    onCheckApiStatus = onCheckApiStatus
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Library Segmented Tabs
        item(key = "library_tabs") {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianBackground,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CrimsonPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My List (${watchlist.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History (${history.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Reminders (${reminders.size})") }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when (selectedTab) {
            0 -> {
                if (watchlist.isEmpty()) {
                    item(key = "watchlist_empty") {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.Bookmark,
                            title = "Your Watchlist is Empty",
                            subtitle = "Tap + My List on any movie, series, or anime to save it on your device."
                        )
                    }
                } else {
                    items(watchlist.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { w ->
                                PosterCard(
                                    item = w.toMediaItem(),
                                    onClick = { onOpenMedia(w.toMediaItem()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            1 -> {
                if (history.isEmpty()) {
                    item(key = "history_empty") {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.History,
                            title = "No Watch History Yet",
                            subtitle = "Start playing any episode or movie and your resume progress saves automatically."
                        )
                    }
                } else {
                    item(key = "history_clear_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onClearHistory,
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear History", color = CrimsonPrimary)
                            }
                        }
                    }
                    items(history, key = { it.id }) { h ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                .clickable { onResumeHistory(h) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = h.posterUrl,
                                    contentDescription = h.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianSurface)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = h.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "S${h.season} E${h.episode} • ${(h.progressFraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { h.progressFraction.coerceAtLeast(0.04f) },
                                        color = CrimsonPrimary,
                                        trackColor = ObsidianSurfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                    )
                                }
                                IconButton(onClick = { onResumeHistory(h) }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = CrimsonPrimary)
                                }
                                IconButton(onClick = { onRemoveHistory(h.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                if (reminders.isEmpty()) {
                    item(key = "reminders_empty") {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "No Release Reminders",
                            subtitle = "Tap the bell icon on Coming Soon titles to track upcoming premieres."
                        )
                    }
                } else {
                    items(reminders, key = { it.id }) { rem ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = rem.posterUrl,
                                    contentDescription = rem.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 52.dp, height = 70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rem.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rem.releaseDate.ifBlank { "Upcoming Release" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LiveEmerald
                                    )
                                }
                                IconButton(onClick = { onRemoveReminder(rem) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove reminder", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Streaming & Preferences Section (shown at bottom in MY_LIST mode)
        if (mode == LibraryMode.MY_LIST) {
            item(key = "preferences_section") {
                Spacer(modifier = Modifier.height(22.dp))
                StreamingPreferencesCard(
                    preferences = preferences,
                    apiStatus = apiStatus,
                    isDualSiteActive = isDualSiteActive,
                    onUpdateSource = onUpdateSource,
                    onUpdateLanguage = onUpdateLanguage,
                    onUpdateSafeMode = onUpdateSafeMode,
                    onSaveApiKey = onSaveApiKey,
                    onCheckApiStatus = onCheckApiStatus
                )
            }
        }
    }
}

@Composable
private fun StreamingPreferencesCard(
    preferences: UserPreferences,
    apiStatus: ApiStatusInfo?,
    isDualSiteActive: Boolean,
    onUpdateSource: (ContentSource) -> Unit,
    onUpdateLanguage: (ContentLanguage) -> Unit,
    onUpdateSafeMode: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onCheckApiStatus: () -> Unit
) {
    var apiKeyDraft by remember(preferences.customApiKey) { mutableStateOf(preferences.customApiKey) }
    Column {
        SectionHeaderRow(title = "Streaming Preferences")
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Content Source", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ContentSource.entries) { src ->
                        val selected = preferences.source == src
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateSource(src) },
                            label = { Text(src.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CrimsonPrimary,
                                selectedLabelColor = TextPrimary,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Preferred Language", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ContentLanguage.entries) { lang ->
                        val selected = preferences.language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateLanguage(lang) },
                            label = { Text(lang.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGold,
                                selectedLabelColor = ObsidianBackground,
                                containerColor = ObsidianSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = LiveEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Family-Safe Mode", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "Hides mature 18+ categories and keeps feeds clean",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = preferences.safeMode,
                        onCheckedChange = onUpdateSafeMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBackground,
                            checkedTrackColor = LiveEmerald
                        ),
                        modifier = Modifier.testTag("safe_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = if (isDualSiteActive) LiveEmerald else CinemaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CinePulse Dual-Site Key",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isDualSiteActive) {
                                    "Active • MovieBox + MovieLinkBD unlocked"
                                } else {
                                    "Paste cp_live_... key to unlock MovieLinkBD & 2nd site streams"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDualSiteActive) LiveEmerald else CinemaGold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        placeholder = {
                            Text(
                                text = "cp_live_...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianSurface,
                            unfocusedContainerColor = ObsidianSurface,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("api_key_input")
                    )
                    Button(
                        onClick = { onSaveApiKey(apiKeyDraft) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonPrimary,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = if (apiStatus?.isOnline == true) LiveEmerald else CinemaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Cinedor Turbo Engine", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = apiStatus?.let {
                                    if (it.isOnline) "${it.statusText} • ${it.latencyMs}ms" else it.statusText
                                } ?: "Checking connection…",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (apiStatus?.isOnline == true) LiveEmerald else TextSecondary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onCheckApiStatus,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ping", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryStatChip(
    count: Int,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurface,
        modifier = modifier.border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
