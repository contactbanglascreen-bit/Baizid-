package com.example.ui.screens

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.model.MediaCategoryType
import com.example.data.model.ReelItem
import com.example.data.remote.CinePulseApiClient
import com.example.data.remote.MovieBoxDirectClient
import com.example.ui.components.ApiErrorOrKeyCard
import com.example.ui.components.CinedorLoadErrorHandlingPolicy
import com.example.ui.components.EmptyLibraryCard
import com.example.ui.components.GlassBadge
import com.example.ui.components.edgePopMediaOkHttpClient
import com.example.ui.components.findHttpStatusCode
import com.example.ui.components.sharedMediaOkHttpClient
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ReelsUiState
import com.example.ui.viewmodel.UiState
import com.example.data.model.MediaItem as CineMediaItem

@Composable
fun ReelsScreen(
    state: ReelsUiState,
    onEnsureLoaded: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenFullMedia: (CineMediaItem) -> Unit,
    onToggleWatchlist: (CineMediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onEnsureLoaded()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val reelsState = state.reelsState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBackground)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    ApiErrorOrKeyCard(
                        message = reelsState.message,
                        isApiKeyMissing = reelsState.isApiKeyMissing,
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Success -> {
                val reels = reelsState.data
                if (reels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyLibraryCard(
                            icon = Icons.Rounded.Movie,
                            title = "No Reels Available",
                            subtitle = "Trailer reels and ShortTV dramas will appear here."
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(pageCount = { reels.size })

                    LaunchedEffect(pagerState.currentPage, reels.size) {
                        if (pagerState.currentPage >= reels.size - 2) {
                            onLoadMore()
                        }
                    }

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val reel = reels[page]
                        val isActive = pagerState.currentPage == page
                        ReelVideoPage(
                            reel = reel,
                            isActive = isActive,
                            onWatchFull = {
                                onOpenFullMedia(
                                    CineMediaItem(
                                        id = reel.subjectId.ifBlank { reel.id },
                                        title = reel.title,
                                        posterUrl = reel.posterUrl,
                                        backdropUrl = reel.posterUrl,
                                        mediaType = MediaCategoryType.SHORT,
                                        source = reel.source,
                                        description = reel.description
                                    )
                                )
                            },
                            onSaveToList = {
                                onToggleWatchlist(
                                    CineMediaItem(
                                        id = reel.subjectId.ifBlank { reel.id },
                                        title = reel.title,
                                        posterUrl = reel.posterUrl,
                                        mediaType = MediaCategoryType.SHORT,
                                        source = reel.source
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Top subtle title badge
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Cinedor Reels",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelVideoPage(
    reel: ReelItem,
    isActive: Boolean,
    onWatchFull: () -> Unit,
    onSaveToList: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val refererCandidates = remember { MovieBoxDirectClient.WHITELISTED_STREAM_REFERERS + listOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var isPausedByUser by remember { mutableStateOf(false) }
    var refererIndex by remember(reel.videoUrl) { mutableIntStateOf(0) }
    var retryNonce by remember(reel.videoUrl) { mutableIntStateOf(0) }
    var isBuffering by remember(reel.videoUrl, refererIndex, retryNonce) { mutableStateOf(true) }
    var reelError by remember(reel.videoUrl, refererIndex, retryNonce) { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (reel.posterUrl.isNotBlank()) {
            AsyncImage(
                model = reel.posterUrl,
                contentDescription = reel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isActive && reel.videoUrl.startsWith("http") && reelError == null) {
            val activeReferer = refererCandidates[refererIndex.coerceIn(0, refererCandidates.lastIndex)]
            val player = remember(reel.videoUrl, refererIndex, retryNonce) {
                val headers = buildMap {
                    putAll(MovieBoxDirectClient.CHROME_VIDEO_HEADERS)
                    if (activeReferer.isNotBlank()) put("Referer", activeReferer)
                    if (reel.videoUrl.startsWith(CinePulseApiClient.BASE_URL) &&
                        BuildConfig.CINEPULSE_API_KEY.isNotBlank() &&
                        BuildConfig.CINEPULSE_API_KEY != "YOUR_CINEPULSE_API_KEY"
                    ) {
                        put("x-api-key", BuildConfig.CINEPULSE_API_KEY)
                    }
                }
                val dataSourceFactory: DataSource.Factory = when (refererIndex) {
                    1 -> DefaultHttpDataSource.Factory()
                        .setUserAgent(MovieBoxDirectClient.CHROME_USER_AGENT)
                        .setDefaultRequestProperties(headers)
                        .setAllowCrossProtocolRedirects(true)
                    2 -> OkHttpDataSource.Factory(edgePopMediaOkHttpClient)
                        .setUserAgent(MovieBoxDirectClient.CHROME_USER_AGENT)
                        .setDefaultRequestProperties(headers)
                    else -> OkHttpDataSource.Factory(sharedMediaOkHttpClient)
                        .setUserAgent(MovieBoxDirectClient.CHROME_USER_AGENT)
                        .setDefaultRequestProperties(headers)
                }
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(CinedorLoadErrorHandlingPolicy())
                val fastLoadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(10_000, 30_000, 1_000, 2_000)
                    .build()
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(fastLoadControl)
                    .build()
                    .apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(reel.videoUrl)))
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        prepare()
                    }
            }

            LaunchedEffect(isMuted) {
                player.volume = if (isMuted) 0f else 1f
            }

            LaunchedEffect(isPausedByUser) {
                if (isPausedByUser) player.pause() else player.play()
            }

            DisposableEffect(player) {
                val playerListener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val httpCode = findHttpStatusCode(error)
                        if ((httpCode == null || httpCode in setOf(429, 428, 426, 403, 401)) &&
                            refererIndex < refererCandidates.lastIndex
                        ) {
                            refererIndex += 1
                            return
                        }
                        isBuffering = false
                        reelError = if (httpCode != null) {
                            "Preview unavailable (HTTP $httpCode) • Tap to retry"
                        } else {
                            "Preview unavailable • Tap Watch Full"
                        }
                    }
                }
                player.addListener(playerListener)
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> player.pause()
                        Lifecycle.Event.ON_RESUME -> if (!isPausedByUser) player.play()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    player.removeListener(playerListener)
                    player.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.cinedor_player_view, null, false) as PlayerView).apply {
                        this.player = player
                        keepScreenOn = true
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != player) {
                        view.player = player
                    }
                    view.keepScreenOn = isActive && !isPausedByUser
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPausedByUser = !isPausedByUser }
            )

            if (isBuffering && !isPausedByUser) {
                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
        }

        // Bottom dark legibility scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ObsidianBackground.copy(alpha = 0.92f))
                    )
                )
        )

        val errMsg = reelError
        if (errMsg != null && isActive) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.72f),
                onClick = {
                    reelError = null
                    retryNonce += 1
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry preview",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
        } else if (isPausedByUser && isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Resume reel",
                    tint = TextPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Right-side action column (cleared above floating bottom nav pill)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 112.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = TextPrimary
                )
            }

            IconButton(
                onClick = onSaveToList,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("reel_save_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdd,
                    contentDescription = "Save to My List",
                    tint = TextPrimary
                )
            }
        }

        // Bottom-left info overlay + Gradient Watch Full pill (cleared above floating nav pill)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 104.dp)
        ) {
            GlassBadge(text = reel.category.uppercase(), accent = CrimsonPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reel.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (reel.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(CinedorGradientColors))
                    .clickable(onClick = onWatchFull)
                    .padding(horizontal = 20.dp)
                    .testTag("reel_watch_full_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Full", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }
    }
}
