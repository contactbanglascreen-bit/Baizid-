package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CaptionTrack
import com.example.data.model.CastMember
import com.example.data.model.CommentItem
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.StaffDetail
import com.example.data.model.StreamOption
import com.example.ui.components.ApiErrorOrKeyCard
import com.example.ui.components.CinePulseVideoPlayer
import com.example.ui.components.PosterCard
import androidx.compose.material.icons.rounded.Download
import com.example.ui.components.DownloadQualitySheet
import com.example.ui.theme.OnAccent
import com.example.ui.theme.OnMediaText
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.CinedorTintedSurface
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DetailUiState
import com.example.ui.viewmodel.UiState

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 100% MovieBox-faithful Detail & Video Player Screen (matching screenshots 132502 & 132504),
 * using Cinedor's Violet -> Magenta -> Pink logo accents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPlayerScreen(
    state: DetailUiState,
    isInWatchlist: Boolean,
    staffState: UiState<StaffDetail>? = null,
    onBack: () -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onSelectEpisode: (season: Int, episode: Int) -> Unit,
    onSelectStream: (StreamOption) -> Unit,
    onSelectCaption: (CaptionTrack?) -> Unit,
    onRetryStream: () -> Unit,
    onRetryDetail: () -> Unit,
    onOpenRelated: (MediaItem) -> Unit,
    onOpenStaff: (CastMember) -> Unit = {},
    onCloseStaff: () -> Unit = {},
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onStreamExhausted: (StreamOption, Int?) -> Boolean
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var infoExpanded by remember { mutableStateOf(false) }
    var subtitlesExpanded by remember { mutableStateOf(false) }
    var allEpisodesExpanded by remember { mutableStateOf(false) }
    var qualityDropdownExpanded by remember { mutableStateOf(false) }
    var seasonDropdownExpanded by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var downloadSheetOpen by remember { mutableStateOf(false) }

    DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    val item = state.selectedItem ?: return

    if (downloadSheetOpen) {
        val dlDetail: MediaDetail? = (state.detailState as? UiState.Success)?.data
        val dlTitle = buildString {
            append((dlDetail?.item ?: item).title)
            if (dlDetail?.seasons?.isNotEmpty() == true) {
                append(" S${state.selectedSeason}E${state.selectedEpisode}")
            }
        }
        DownloadQualitySheet(
            title = dlTitle,
            streams = state.streams,
            onDismiss = { downloadSheetOpen = false },
            posterUrl = (dlDetail?.item ?: item).posterUrl.ifBlank { item.posterUrl }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else ObsidianBackground)
            .then(
                if (isFullscreen) Modifier else Modifier.statusBarsPadding().navigationBarsPadding()
            )
    ) {
        // Single unified Media3 Video Player Area — stays mounted across fullscreen toggles without reloading!
        Box(
            modifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }
        ) {
            val activeStream = state.selectedStream
            val detailForTitle: MediaDetail? = (state.detailState as? UiState.Success)?.data
            val playerTitle = buildString {
                append((detailForTitle?.item ?: item).title)
                if ((detailForTitle?.seasons?.isNotEmpty() == true) || state.selectedSeason > 1 || state.selectedEpisode > 1) {
                    append(" • S${state.selectedSeason} E${state.selectedEpisode}")
                }
            }
            when {
                activeStream != null -> {
                    CinePulseVideoPlayer(
                        title = playerTitle,
                        stream = activeStream,
                        streams = state.streams,
                        caption = state.selectedCaption,
                        captions = state.captions,
                        initialPositionMs = state.resumePositionMs,
                        isFullscreen = isFullscreen,
                        onBack = {
                            if (isFullscreen) isFullscreen = false else onBack()
                        },
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        onSelectStream = onSelectStream,
                        onSelectCaption = onSelectCaption,
                        onProgressUpdate = onProgressUpdate,
                        onStreamExhausted = onStreamExhausted,
                        onRefreshStream = onRetryStream,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    AsyncImage(
                        model = item.backdropUrl.ifBlank { item.posterUrl },
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isStreamLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CinedorMagenta, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Fetching HD stream…",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OnMediaText
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = state.streamError ?: "Tap retry to load stream",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnMediaText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Brush.horizontalGradient(CinedorGradientColors))
                                        .clickable(onClick = onRetryStream)
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = OnAccent)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Load Stream", style = MaterialTheme.typography.labelLarge, color = OnAccent)
                                    }
                                }
                            }
                        }
                    }

                    // Back chevron shown while stream is loading/unavailable (during playback, player overlay handles it & auto-hides cleanly)
                    IconButton(
                        onClick = {
                            if (isFullscreen) isFullscreen = false else onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(if (isFullscreen) 14.dp else 4.dp)
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Back",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (isFullscreen) return@Column

        // Scrollable MovieBox Detail Body
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item(key = "header_meta") {
                val detail: MediaDetail? = (state.detailState as? UiState.Success)?.data
                val displayItem = detail?.item ?: item

                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    // MovieBox Title + inline "Info >" Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayItem.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp
                            ),
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { infoExpanded = !infoExpanded }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Info",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = TextSecondary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Toggle info",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Exact MovieBox Pipe-Separated Metadata Line: 📺 | ⭐ 5.8 | 2019 | Japan | Anime | 1 seasons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (displayItem.mediaType == MediaCategoryType.MOVIE) {
                                Icons.Rounded.Movie
                            } else {
                                Icons.Rounded.LiveTv
                            },
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        if (displayItem.rating.isNotBlank()) {
                            Text("|", color = ObsidianBorder, fontSize = 12.sp)
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = CinemaGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = displayItem.rating,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = CinemaGold
                            )
                        }
                        val metaTail = buildList {
                            if (displayItem.year.isNotBlank()) add(displayItem.year)
                            if (displayItem.country.isNotBlank()) add(displayItem.country)
                            displayItem.genres.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
                            val seasonCount = detail?.seasons?.size ?: 0
                            if (seasonCount > 0) {
                                add("$seasonCount season${if (seasonCount > 1) "s" else ""}")
                            } else if (detail?.durationText?.isNotBlank() == true) {
                                add(detail.durationText)
                            }
                        }
                        if (metaTail.isNotEmpty()) {
                            Text(
                                text = "| ${metaTail.joinToString(" | ")}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Expandable Info Drawer (Synopsis + Cast, hidden by default just like MovieBox!)
                    AnimatedVisibility(visible = infoExpanded) {
                        val synopsis = detail?.synopsis?.ifBlank { displayItem.description }.orEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianCard)
                                .padding(12.dp)
                        ) {
                            if (displayItem.genres.isNotEmpty()) {
                                Text(
                                    text = displayItem.genres.joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CinedorMagenta
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (synopsis.isNotBlank()) {
                                Text(
                                    text = synopsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (detail != null && detail.cast.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Cast & Stars",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(detail.cast, key = { it.id }) { member ->
                                        Column(
                                            modifier = Modifier
                                                .width(68.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onOpenStaff(member) }
                                                .padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(ObsidianSurfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (member.avatarUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = member.avatarUrl,
                                                        contentDescription = member.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = member.name.take(2).uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = member.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Exact MovieBox Horizontal Rounded Action Pills Row
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            MovieBoxActionPill(
                                icon = Icons.Rounded.Download,
                                label = "Download",
                                active = true,
                                onClick = { downloadSheetOpen = true },
                                testTag = "detail_download_button"
                            )
                        }
                        item {
                            MovieBoxActionPill(
                                icon = if (isInWatchlist) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PlaylistAdd,
                                label = if (isInWatchlist) "In list" else "Add to list",
                                active = isInWatchlist,
                                onClick = { onToggleWatchlist(displayItem) },
                                testTag = "detail_watchlist_button"
                            )
                        }
                        item {
                            MovieBoxActionPill(
                                icon = Icons.Rounded.Share,
                                label = stringResource(R.string.share),
                                active = false,
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Watch ${displayItem.title} on Cinedor!")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                                },
                                testTag = "detail_share_button"
                            )
                        }
                        item {
                            MovieBoxActionPill(
                                icon = Icons.Rounded.Refresh,
                                label = "Switch Server",
                                active = false,
                                onClick = onRetryStream,
                                testTag = "detail_refresh_stream"
                            )
                        }
                        if (state.captions.isNotEmpty()) {
                            item {
                                MovieBoxActionPill(
                                    icon = Icons.Rounded.ClosedCaption,
                                    label = state.selectedCaption?.label ?: "Subtitles",
                                    active = state.selectedCaption != null,
                                    onClick = { subtitlesExpanded = !subtitlesExpanded },
                                    testTag = "detail_subtitles_toggle"
                                )
                            }
                        }
                    }
                }
            }

            // MovieBox "Resources" Section (Quality/Audio dropdown + Season dropdown + Subtitles + Episode Squares!)
            item(key = "resources_section") {
                val detail: MediaDetail? = (state.detailState as? UiState.Success)?.data
                val seasons = detail?.seasons.orEmpty()
                val currentSeason = seasons.firstOrNull { it.seasonNumber == state.selectedSeason }
                    ?: seasons.firstOrNull()

                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Resources",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Streamed via ${(detail?.item ?: item).source.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dropdown selectors row (MovieBox "Original Audio v" style bordered dark boxes)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.streams.isNotEmpty()) {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianSurface,
                                    modifier = Modifier
                                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                        .clickable { qualityDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.HighQuality,
                                            contentDescription = null,
                                            tint = CinedorMagenta,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = state.selectedStream?.let { "${it.label} (${it.format})" }
                                                ?: "Select Quality",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                            color = TextPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ExpandMore,
                                            contentDescription = "Select quality",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = qualityDropdownExpanded,
                                    onDismissRequest = { qualityDropdownExpanded = false }
                                ) {
                                    state.streams.forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${opt.label} (${opt.format})",
                                                    color = if (state.selectedStream?.url == opt.url) CinedorMagenta else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                qualityDropdownExpanded = false
                                                onSelectStream(opt)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (seasons.size > 1 && currentSeason != null) {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianSurface,
                                    modifier = Modifier
                                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                        .clickable { seasonDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = currentSeason.title,
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                            color = TextPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.ExpandMore,
                                            contentDescription = "Select season",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = seasonDropdownExpanded,
                                    onDismissRequest = { seasonDropdownExpanded = false }
                                ) {
                                    seasons.forEach { se ->
                                        DropdownMenuItem(
                                            text = { Text(se.title) },
                                            onClick = {
                                                seasonDropdownExpanded = false
                                                val firstEp = se.episodes.firstOrNull()?.episode ?: 1
                                                onSelectEpisode(se.seasonNumber, firstEp)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Subtitles Pill Strip
                    AnimatedVisibility(visible = subtitlesExpanded && state.captions.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCaption == null,
                                    onClick = { onSelectCaption(null) },
                                    label = { Text("Off", style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                            items(state.captions, key = { it.url }) { track ->
                                FilterChip(
                                    selected = state.selectedCaption?.url == track.url,
                                    onClick = { onSelectCaption(track) },
                                    label = { Text(track.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CinedorTintedSurface,
                                        selectedLabelColor = CinedorMagenta,
                                        containerColor = ObsidianCard,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Exact MovieBox Square Episode Row ("All" | "01" | "02" | "03"...) + Expandable Full Episode Grid
                    if (currentSeason != null && currentSeason.episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (allEpisodesExpanded) CinedorTintedSurface else ObsidianCard)
                                        .then(
                                            if (allEpisodesExpanded) {
                                                Modifier.border(
                                                    1.dp,
                                                    CinedorMagenta.copy(alpha = 0.55f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { allEpisodesExpanded = !allEpisodesExpanded }
                                        .testTag("episodes_all_toggle"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (allEpisodesExpanded) "Hide" else "All",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (allEpisodesExpanded) CinedorMagenta else TextPrimary
                                    )
                                }
                            }
                            items(currentSeason.episodes, key = { "${it.season}_${it.episode}" }) { ep ->
                                val selected = state.selectedSeason == ep.season && state.selectedEpisode == ep.episode
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) CinedorTintedSurface else ObsidianCard
                                        )
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    1.dp,
                                                    CinedorMagenta.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onSelectEpisode(ep.season, ep.episode) }
                                        .testTag("episode_chip_${ep.season}_${ep.episode}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ep.episode.toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) CinedorMagenta else TextPrimary
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = allEpisodesExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurface)
                                    .border(1.dp, ObsidianBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${currentSeason.title} • ${currentSeason.episodes.size} Episodes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                currentSeason.episodes.chunked(6).forEach { rowEps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowEps.forEach { ep ->
                                            val selected = state.selectedSeason == ep.season && state.selectedEpisode == ep.episode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (selected) CinedorTintedSurface else ObsidianCard)
                                                    .then(
                                                        if (selected) {
                                                            Modifier.border(
                                                                1.dp,
                                                                CinedorMagenta.copy(alpha = 0.55f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clickable {
                                                        onSelectEpisode(ep.season, ep.episode)
                                                        allEpisodesExpanded = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ep.episode.toString().padStart(2, '0'),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (selected) CinedorMagenta else TextPrimary
                                                )
                                            }
                                        }
                                        repeat(6 - rowEps.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (val detailState = state.detailState) {
                is UiState.Loading -> {
                    item(key = "detail_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CinedorMagenta, strokeWidth = 2.5.dp)
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "detail_error") {
                        ApiErrorOrKeyCard(
                            message = detailState.message,
                            isApiKeyMissing = detailState.isApiKeyMissing,
                            onRetry = onRetryDetail
                        )
                    }
                }

                is UiState.Success -> {
                    val detail = detailState.data

                    // Left-aligned MovieBox Tabs: "For you" (white underline) | "Comments(71)•"
                    item(key = "detail_tabs") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .clickable { selectedBottomTab = 0 }
                                        .padding(bottom = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "For you",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (selectedBottomTab == 0) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 17.sp
                                        ),
                                        color = if (selectedBottomTab == 0) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(54.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedBottomTab == 0) CinedorMagenta else Color.Transparent
                                            )
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .clickable { selectedBottomTab = 1 }
                                        .padding(bottom = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "Comments(${detail.comments.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = if (selectedBottomTab == 1) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 17.sp
                                            ),
                                            color = if (selectedBottomTab == 1) TextPrimary else TextSecondary
                                        )
                                        if (detail.comments.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 3.dp, top = 2.dp)
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(CinedorPink)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedBottomTab == 1) CinedorMagenta else Color.Transparent
                                            )
                                    )
                                }
                            }
                            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.5f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (selectedBottomTab == 0) {
                        if (detail.related.isEmpty()) {
                            item(key = "related_empty") {
                                Text(
                                    text = "No similar titles listed yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(detail.related.chunked(3)) { rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { rel ->
                                        PosterCard(
                                            item = rel,
                                            onClick = { onOpenRelated(rel) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        if (detail.comments.isEmpty()) {
                            item(key = "comments_empty") {
                                Text(
                                    text = "No viewer comments yet for this title.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(detail.comments, key = { it.id }) { comment ->
                                CommentCard(comment = comment)
                            }
                        }
                    }
                }
            }
        }
    }

    if (staffState != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseStaff,
            containerColor = ObsidianSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (staffState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CinedorMagenta)
                        }
                    }

                    is UiState.Error -> {
                        Text(
                            text = staffState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }

                    is UiState.Success -> {
                        val staff = staffState.data
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianCard),
                                contentAlignment = Alignment.Center
                            ) {
                                if (staff.member.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = staff.member.avatarUrl,
                                        contentDescription = staff.member.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = staff.member.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = staff.member.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                if (staff.member.role.isNotBlank()) {
                                    Text(
                                        text = staff.member.role,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CinedorMagenta
                                    )
                                }
                            }
                        }

                        if (staff.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = staff.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Known For",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (staff.works.isEmpty()) {
                            Text(
                                text = "No filmography titles found yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(staff.works.take(12), key = { "${it.source}:${it.id}:${it.title}" }) { work ->
                                    PosterCard(
                                        item = work,
                                        onClick = {
                                            onCloseStaff()
                                            onOpenRelated(work)
                                        },
                                        modifier = Modifier.width(114.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieBoxActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = if (active) CinedorTintedSurface else ObsidianCard,
        shadowElevation = if (active) 0.dp else 2.dp,
        modifier = Modifier
            .height(36.dp)
            .border(1.dp, if (active) CinedorMagenta.copy(alpha = 0.45f) else ObsidianBorder, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) CinedorMagenta else TextPrimary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (active) CinedorMagenta else TextPrimary
            )
        }
    }
}

@Composable
private fun CommentCard(comment: CommentItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                if (comment.timeText.isNotBlank()) {
                    Text(
                        text = comment.timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
