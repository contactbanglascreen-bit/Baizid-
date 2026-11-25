package com.example.ui.components

import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.OpenableColumns
import android.os.Build
import android.media.AudioManager
import android.content.Intent
import android.content.ContextWrapper
import android.content.Context
import android.app.Activity
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CaptionTrack
import com.example.data.model.StreamOption
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.MediaCard as ObsidianCard
import com.example.ui.theme.MediaTextPrimary as TextPrimary
import com.example.ui.theme.MediaTextSecondary as TextSecondary
import kotlinx.coroutines.delay

enum class VideoResizeOption(val label: String) {
    FIT("Fit"),
    ZOOM("Fill"),
    CROP("Crop black bars"),
    FILL("Stretch")
}

internal fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
internal fun CinedorPlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    playbackSpeed: Float,
    resizeOption: VideoResizeOption,
    streams: List<StreamOption>,
    selectedStream: StreamOption,
    captions: List<CaptionTrack>,
    selectedCaption: CaptionTrack?,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleResize: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSelectStream: (StreamOption) -> Unit,
    onSelectCaption: (CaptionTrack?) -> Unit,
    modifier: Modifier = Modifier,
    onHoldSpeed: (Boolean) -> Unit = {},
    onSetSpeed: ((Float) -> Unit)? = null,
    audioTracks: List<PlayerTrackOption> = emptyList(),
    textTracks: List<PlayerTrackOption> = emptyList(),
    onSelectTrack: (PlayerTrackOption) -> Unit = {},
    onDisableEmbeddedText: () -> Unit = {},
    onPinchZoom: (Float) -> Unit = {},
    videoTracks: List<PlayerTrackOption> = emptyList(),
    videoAuto: Boolean = true,
    onSelectVideoTrack: (PlayerTrackOption?) -> Unit = {}
) {
    val ctx = LocalContext.current
    var containerHeightPx by remember { mutableIntStateOf(1) }
    var gestureHud by remember { mutableStateOf<Pair<ImageVector, Int>?>(null) }
    var gestureHudTick by remember { mutableIntStateOf(0) }
    var brightness by remember {
        mutableFloatStateOf(
            ctx.findHostActivityForPlayer()?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
        )
    }
    val audioManager = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var volumeAcc by remember { mutableFloatStateOf(0f) }
    val streamLanguages = remember(streams) {
        streams.map { it.audioLanguage }.filter { it.isNotBlank() }.distinct()
    }
    val subtitleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = runCatching {
                ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: "subtitle.srt"
            onSelectCaption(
                CaptionTrack(
                    id = "local_${uri.hashCode()}",
                    language = "local",
                    label = name,
                    url = uri.toString()
                )
            )
        }
    }
    LaunchedEffect(gestureHudTick) {
        if (gestureHudTick > 0) {
            delay(800L)
            gestureHud = null
        }
    }
    var openPanel by remember { mutableStateOf<PlayerPanel?>(null) }
    val translateScope = rememberCoroutineScope()
    var translateProgress by remember { mutableStateOf<Float?>(null) }
    var translateLabel by remember { mutableStateOf("") }
    var controlsVisible by remember { mutableStateOf(true) }
    var isHolding2x by remember { mutableStateOf(false) }
    val skipSec = AppSettings.doubleTapSkipSec
    val skipMs = skipSec * 1000L
    val rewindIcon = when (skipSec) {
        5 -> Icons.Rounded.Replay5
        30 -> Icons.Rounded.Replay30
        10 -> Icons.Rounded.Replay10
        else -> Icons.Rounded.Replay
    }
    val forwardIcon = when (skipSec) {
        5 -> Icons.Rounded.Forward5
        30 -> Icons.Rounded.Forward30
        10 -> Icons.Rounded.Forward10
        else -> Icons.Rounded.FastForward
    }
    var isLocked by remember(isFullscreen) { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var qualityMenuOpen by remember { mutableStateOf(false) }
    var captionMenuOpen by remember { mutableStateOf(false) }
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackRight by remember { mutableStateOf(true) }
    var seekFeedbackTick by remember { mutableIntStateOf(0) }
    var resizeToastText by remember { mutableStateOf<String?>(null) }
    var resizeToastTick by remember { mutableIntStateOf(0) }
    var containerWidthPx by remember { mutableIntStateOf(1) }

    // Auto-hide controls cleanly after 3.5s while playing (exact MovieBox clean playback look)
    LaunchedEffect(controlsVisible, isPlaying, isScrubbing, qualityMenuOpen, captionMenuOpen, interactionTick, isLocked, openPanel) {
        if (controlsVisible && isPlaying && !isScrubbing && !qualityMenuOpen && !captionMenuOpen && openPanel == null) {
            delay(if (isLocked) 2200L else 3500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekFeedbackTick) {
        if (seekFeedbackTick > 0) {
            delay(750L)
            seekFeedbackText = null
        }
    }

    LaunchedEffect(resizeToastTick) {
        if (resizeToastTick > 0) {
            delay(1100L)
            resizeToastText = null
        }
    }

    val effectivePos = if (isScrubbing) scrubPositionMs else currentPositionMs
    val safeDuration = durationMs.coerceAtLeast(0L)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                containerWidthPx = it.width.coerceAtLeast(1)
                containerHeightPx = it.height.coerceAtLeast(1)
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onPinchZoom(zoom)
                    } else if (kotlin.math.abs(pan.y) > kotlin.math.abs(pan.x) * 1.4f) {
                        val delta = -pan.y / containerHeightPx.toFloat()
                        if (centroid.x < containerWidthPx / 2f) {
                            brightness = (brightness + delta).coerceIn(0.02f, 1f)
                            ctx.findHostActivityForPlayer()?.window?.let { w ->
                                val lp = w.attributes
                                lp.screenBrightness = brightness
                                w.attributes = lp
                            }
                            gestureHud = Icons.Rounded.BrightnessMedium to (brightness * 100).toInt()
                        } else if (audioManager != null) {
                            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            volumeAcc += delta * max * 1.2f
                            val step = volumeAcc.toInt()
                            if (step != 0) {
                                volumeAcc -= step
                                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (cur + step).coerceIn(0, max),
                                    0
                                )
                            }
                            val now = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            gestureHud = (if (now == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp) to
                                (now * 100 / max)
                        }
                        gestureHudTick++
                    }
                }
            }
            .pointerInput(isLocked) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                        if (isHolding2x) {
                            isHolding2x = false
                            onHoldSpeed(false)
                        }
                    },
                    onLongPress = {
                        if (AppSettings.holdFor2x && !isLocked) {
                            isHolding2x = true
                            onHoldSpeed(true)
                        }
                    },
                    onTap = {
                        controlsVisible = !controlsVisible
                        interactionTick++
                    },
                    onDoubleTap = { offset ->
                        if (isLocked) {
                            controlsVisible = true
                            interactionTick++
                            return@detectTapGestures
                        }
                        val fraction = offset.x / containerWidthPx.toFloat()
                        when {
                            fraction < 0.36f -> {
                                onSeekBy(-AppSettings.doubleTapSkipSec * 1000L)
                                seekFeedbackRight = false
                                seekFeedbackText = "-${AppSettings.doubleTapSkipSec}s"
                                seekFeedbackTick++
                            }
                            fraction > 0.64f -> {
                                onSeekBy(AppSettings.doubleTapSkipSec * 1000L)
                                seekFeedbackRight = true
                                seekFeedbackText = "+${AppSettings.doubleTapSkipSec}s"
                                seekFeedbackTick++
                            }
                            else -> {
                                onTogglePlayPause()
                                controlsVisible = true
                                interactionTick++
                            }
                        }
                    }
                )
            }
    ) {
        // Double-tap -10s / +10s animated feedback badge
        AnimatedVisibility(
            visible = seekFeedbackText != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(240)),
            modifier = Modifier
                .align(if (seekFeedbackRight) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = if (isFullscreen) 72.dp else 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.62f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (seekFeedbackRight) forwardIcon else rewindIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = seekFeedbackText.orEmpty(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }

        // Aspect-ratio / Scrubbing preview pill
        AnimatedVisibility(
            visible = resizeToastText != null || isScrubbing,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFullscreen) 58.dp else 44.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.68f)
            ) {
                Text(
                    text = if (isScrubbing) {
                        "${formatPlayerTime(scrubPositionMs)} / ${formatPlayerTime(safeDuration)}"
                    } else {
                        resizeToastText.orEmpty()
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // AI translation progress pill
        val tp = translateProgress
        if (tp != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isFullscreen) 70.dp else 46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { tp.coerceIn(0.02f, 1f) },
                    color = CinedorMagenta,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI translating to $translateLabel… ${(tp * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }

        // Brightness / volume HUD
        AnimatedVisibility(
            visible = gestureHud != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val hud = gestureHud
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hud != null) {
                    Icon(hud.first, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((hud.second / 100f).coerceIn(0f, 1f))
                                .height(4.dp)
                                .background(CinedorMagenta)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${hud.second}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Hold-for-2x badge
        AnimatedVisibility(
            visible = isHolding2x,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFullscreen) 24.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2x",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Rounded.FastForward, contentDescription = null, tint = CinedorMagenta, modifier = Modifier.size(18.dp))
            }
        }

        // Locked Screen Minimal Unlock Button (Fullscreen only)
        AnimatedVisibility(
            visible = controlsVisible && isLocked,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp)
        ) {
            IconButton(
                onClick = {
                    isLocked = false
                    controlsVisible = true
                    interactionTick++
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("player_unlock_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Unlock screen",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Main Sleek MovieBox-style Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim + Top Bar (Back chevron + Title in fullscreen + Clean icon+label actions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                        .padding(
                            start = if (isFullscreen) 14.dp else 4.dp,
                            end = if (isFullscreen) 18.dp else 10.dp,
                            top = if (isFullscreen) 10.dp else 4.dp,
                            bottom = 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            interactionTick++
                            onBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isFullscreen && title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 10.dp else 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopOverlayActionItem(
                            icon = Icons.Rounded.Speed,
                            label = if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x",
                            active = playbackSpeed != 1.0f,
                            onClick = {
                                interactionTick++
                                if (onSetSpeed != null) openPanel = PlayerPanel.SPEED else onCycleSpeed()
                            },
                            testTag = "player_speed_button"
                        )

                        if (streams.isNotEmpty() || videoTracks.size > 1) {
                            TopOverlayActionItem(
                                icon = Icons.Rounded.HighQuality,
                                label = when {
                                    videoTracks.size > 1 && videoAuto -> "Auto"
                                    videoTracks.size > 1 -> videoTracks.firstOrNull { it.selected }?.label ?: "Auto"
                                    else -> selectedStream.quality.ifBlank { "HD" }
                                },
                                active = false,
                                onClick = {
                                    interactionTick++
                                    openPanel = PlayerPanel.QUALITY
                                },
                                testTag = "player_quality_button"
                            )
                        }

                        if (audioTracks.size > 1 || streamLanguages.size > 1) {
                            TopOverlayActionItem(
                                icon = Icons.Rounded.Translate,
                                label = "Audio",
                                active = false,
                                onClick = {
                                    interactionTick++
                                    openPanel = PlayerPanel.AUDIO
                                },
                                testTag = "player_audio_button"
                            )
                        }

                        run {
                            TopOverlayActionItem(
                                icon = Icons.Rounded.ClosedCaption,
                                label = "CC",
                                active = selectedCaption != null,
                                onClick = {
                                    interactionTick++
                                    openPanel = PlayerPanel.CAPTIONS
                                },
                                testTag = "player_caption_button"
                            )
                        }

                        if (AppSettings.pipEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            TopOverlayActionItem(
                                icon = Icons.Rounded.PictureInPictureAlt,
                                label = "PiP",
                                active = false,
                                onClick = {
                                    interactionTick++
                                    PipState.requestPip?.invoke()
                                },
                                testTag = "player_pip_button"
                            )
                        }

                        // Lock screen toggle in Fullscreen mode
                        if (isFullscreen) {
                            TopOverlayActionItem(
                                icon = Icons.Rounded.LockOpen,
                                label = "Lock",
                                active = false,
                                onClick = {
                                    isLocked = true
                                    controlsVisible = true
                                    interactionTick++
                                },
                                testTag = "player_lock_button"
                            )
                        }
                    }
                }

                // Fullscreen Center Transport (-10s • Play/Pause • +10s); Portrait stays clean like MovieBox!
                if (!isBuffering) {
                    val sideSize = if (isFullscreen) 52.dp else 42.dp
                    val sideIcon = if (isFullscreen) 28.dp else 24.dp
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 44.dp else 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                interactionTick++
                                onSeekBy(-skipMs)
                            },
                            modifier = Modifier
                                .size(sideSize)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.38f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                .testTag("player_rewind_10")
                        ) {
                            Icon(
                                imageVector = rewindIcon,
                                contentDescription = "Rewind $skipSec seconds",
                                tint = Color.White,
                                modifier = Modifier.size(sideIcon)
                            )
                        }

                        IconButton(
                            onClick = {
                                interactionTick++
                                onTogglePlayPause()
                            },
                            modifier = Modifier
                                .size(if (isFullscreen) 66.dp else 54.dp)
                                .shadow(10.dp, CircleShape, spotColor = CinedorMagenta)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(AccentBright, CinetorRedDark))
                                )
                                .testTag("player_center_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(if (isFullscreen) 38.dp else 32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                interactionTick++
                                onSeekBy(skipMs)
                            },
                            modifier = Modifier
                                .size(sideSize)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.38f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                .testTag("player_forward_10")
                        ) {
                            Icon(
                                imageVector = forwardIcon,
                                contentDescription = "Forward $skipSec seconds",
                                tint = Color.White,
                                modifier = Modifier.size(sideIcon)
                            )
                        }
                    }
                }

                // Bottom Gradient Scrim + Single Sleek MovieBox Control Bar:
                // [Play/Pause] [━━●━━━━━━] [00:04/1:04:49] [Aspect] [Fullscreen]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.88f))
                            )
                        )
                        .padding(
                            start = if (isFullscreen) 14.dp else 4.dp,
                            end = if (isFullscreen) 14.dp else 4.dp,
                            top = 18.dp,
                            bottom = if (isFullscreen) 12.dp else 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatPlayerTime(effectivePos),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.testTag("player_play_pause")
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    SleekVideoSeekBar(
                        positionMs = effectivePos,
                        bufferedMs = bufferedPositionMs,
                        durationMs = safeDuration,
                        isScrubbing = isScrubbing,
                        onScrubStart = { targetMs ->
                            isScrubbing = true
                            scrubPositionMs = targetMs
                            interactionTick++
                        },
                        onScrubMove = { targetMs ->
                            scrubPositionMs = targetMs
                        },
                        onScrubEnd = { targetMs ->
                            isScrubbing = false
                            onSeekTo(targetMs)
                            interactionTick++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatPlayerTime(safeDuration),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(
                        onClick = {
                            interactionTick++
                            val nextIdx = (resizeOption.ordinal + 1) % VideoResizeOption.entries.size
                            resizeToastText = "Display: ${VideoResizeOption.entries[nextIdx].label}"
                            resizeToastTick++
                            onCycleResize()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("player_resize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Change aspect ratio",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            onToggleFullscreen()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Options panel (quality / speed / subtitles)
        AnimatedVisibility(
            visible = openPanel != null,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize()
        ) {
            val panel = openPanel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .pointerInput(Unit) { detectTapGestures { openPanel = null; interactionTick++ } },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (panel) {
                            PlayerPanel.QUALITY -> "Video quality"
                            PlayerPanel.SPEED -> "Playback speed"
                            PlayerPanel.CAPTIONS -> "Subtitles"
                            PlayerPanel.AUDIO -> "Audio language"
                            PlayerPanel.TRANSLATE -> "AI translate subtitles to…"
                            null -> ""
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(if (isFullscreen) 18.dp else 10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        when (panel) {
                            PlayerPanel.QUALITY -> {
                                val useTracks = videoTracks.size > 1
                                if (useTracks) {
                                    item(key = "vt_auto") {
                                        PanelOptionCard(
                                            title = "Auto",
                                            badge = null,
                                            subtitle = "Best for your internet",
                                            selected = videoAuto,
                                            compact = !isFullscreen,
                                            onClick = {
                                                openPanel = null
                                                interactionTick++
                                                onSelectVideoTrack(null)
                                            }
                                        )
                                    }
                                }
                                val availableSteps = if (useTracks) {
                                    videoTracks.mapNotNull { QualityLadder.stepFor(QualityLadder.valueOf(it.label))?.value }.toSet()
                                } else QualityLadder.available(streams)
                                val currentStep = if (useTracks) {
                                    if (videoAuto) null
                                    else videoTracks.firstOrNull { it.selected }?.let { QualityLadder.stepFor(QualityLadder.valueOf(it.label))?.value }
                                } else QualityLadder.stepFor(QualityLadder.valueOf(selectedStream))?.value
                                items(QualityLadder.steps, key = { "qs_" + it.value }) { step ->
                                    val available = step.value in availableSteps
                                    val fallbackLabel = if (useTracks) {
                                        QualityLadder.pickTrack(videoTracks, step.value)?.label
                                    } else {
                                        QualityLadder.pick(streams, step.value, selectedStream.audioLanguage)
                                            ?.let { QualityLadder.stepFor(QualityLadder.valueOf(it))?.label }
                                    }
                                    PanelOptionCard(
                                        title = step.label,
                                        badge = step.badge,
                                        subtitle = if (available) "Available" else "→ ${fallbackLabel ?: "auto"}",
                                        selected = currentStep == step.value,
                                        compact = !isFullscreen,
                                        dimmed = !available,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            if (useTracks) {
                                                QualityLadder.pickTrack(videoTracks, step.value)?.let { onSelectVideoTrack(it) }
                                            } else {
                                                val match = QualityLadder.pick(streams, step.value, selectedStream.audioLanguage)
                                                if (match != null && match.url != selectedStream.url) onSelectStream(match)
                                            }
                                            if (!available) {
                                                android.widget.Toast.makeText(
                                                    ctx,
                                                    "${step.label} isn't available for this video — using ${fallbackLabel ?: "the closest quality"}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            }
                            PlayerPanel.SPEED -> items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)) { sp ->
                                PanelOptionCard(
                                    title = if (sp == 1.0f) "Normal" else "${sp}x",
                                    badge = null,
                                    subtitle = if (sp == 1.0f) "1.0x" else "",
                                    selected = sp == playbackSpeed,
                                    compact = !isFullscreen,
                                    onClick = {
                                        openPanel = null
                                        interactionTick++
                                        onSetSpeed?.invoke(sp)
                                    }
                                )
                            }
                            PlayerPanel.CAPTIONS -> {
                                item(key = "cc_off") {
                                    PanelOptionCard(
                                        title = "Off",
                                        badge = null,
                                        subtitle = "",
                                        selected = selectedCaption == null && textTracks.none { it.selected },
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            onSelectCaption(null)
                                            onDisableEmbeddedText()
                                        }
                                    )
                                }
                                items(captions, key = { "cc_" + it.url }) { track ->
                                    PanelOptionCard(
                                        title = track.label.ifBlank { track.language },
                                        badge = null,
                                        subtitle = track.language.uppercase().take(6),
                                        selected = selectedCaption?.url == track.url,
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            onSelectCaption(track)
                                        }
                                    )
                                }
                                val selectedLocal = selectedCaption?.takeIf { sc -> captions.none { it.url == sc.url } }
                                if (selectedLocal != null) {
                                    item(key = "cc_local_sel") {
                                        PanelOptionCard(
                                            title = selectedLocal.label.substringBeforeLast('.').take(14),
                                            badge = "FILE",
                                            subtitle = "From phone",
                                            selected = true,
                                            compact = !isFullscreen,
                                            onClick = { openPanel = null; interactionTick++ }
                                        )
                                    }
                                }
                                val embedded = textTracks.filter { t -> selectedCaption == null || t.label != selectedCaption.label }
                                items(embedded, key = { "et_" + it.key }) { t ->
                                    PanelOptionCard(
                                        title = t.label,
                                        badge = "IN",
                                        subtitle = "Built-in",
                                        selected = t.selected && selectedCaption == null,
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            if (selectedCaption != null) onSelectCaption(null)
                                            onSelectTrack(t)
                                        }
                                    )
                                }
                                item(key = "cc_ai") {
                                    PanelOptionCard(
                                        title = "AI Translate",
                                        badge = "AI",
                                        subtitle = "Any language",
                                        selected = false,
                                        compact = !isFullscreen,
                                        onClick = {
                                            interactionTick++
                                            val hasSource = selectedCaption != null || captions.isNotEmpty()
                                            if (hasSource) {
                                                openPanel = PlayerPanel.TRANSLATE
                                            } else {
                                                openPanel = null
                                                android.widget.Toast.makeText(
                                                    ctx,
                                                    "This video has no subtitle to translate. Add a subtitle file first.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    )
                                }
                                item(key = "cc_pick") {
                                    PanelOptionCard(
                                        title = "+ Add file",
                                        badge = null,
                                        subtitle = ".srt .vtt .ass",
                                        selected = false,
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            runCatching { subtitleFilePicker.launch(arrayOf("*/*")) }
                                        }
                                    )
                                }
                            }
                            PlayerPanel.TRANSLATE -> {
                                items(SubtitleTranslator.languages, key = { "tl_" + it.code }) { lang ->
                                    PanelOptionCard(
                                        title = lang.name,
                                        badge = null,
                                        subtitle = "AI",
                                        selected = false,
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            val source = selectedCaption
                                                ?: captions.firstOrNull {
                                                    it.language.startsWith("en", true) || it.label.contains("english", true)
                                                }
                                                ?: captions.firstOrNull()
                                            if (source != null && translateProgress == null) {
                                                translateLabel = lang.name
                                                translateProgress = 0f
                                                translateScope.launch {
                                                    try {
                                                        val track = SubtitleTranslator.translate(ctx, source, lang) { p ->
                                                            translateProgress = p
                                                        }
                                                        onSelectCaption(track)
                                                        android.widget.Toast.makeText(ctx, "${lang.name} subtitles ready", android.widget.Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(
                                                            ctx,
                                                            "Translation failed: ${e.message ?: "check internet for the first download"}",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    } finally {
                                                        translateProgress = null
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            PlayerPanel.AUDIO -> {
                                items(audioTracks, key = { "at_" + it.key }) { t ->
                                    PanelOptionCard(
                                        title = t.label,
                                        badge = null,
                                        subtitle = "Built-in audio",
                                        selected = t.selected,
                                        compact = !isFullscreen,
                                        onClick = {
                                            openPanel = null
                                            interactionTick++
                                            onSelectTrack(t)
                                        }
                                    )
                                }
                                if (streamLanguages.size > 1) {
                                    items(streamLanguages, key = { "sl_$it" }) { lang ->
                                        PanelOptionCard(
                                            title = lang,
                                            badge = null,
                                            subtitle = "Dubbed version",
                                            selected = selectedStream.audioLanguage == lang,
                                            compact = !isFullscreen,
                                            onClick = {
                                                openPanel = null
                                                interactionTick++
                                                if (selectedStream.audioLanguage != lang) {
                                                    val target = streams.filter { it.audioLanguage == lang }
                                                    val same = target.firstOrNull { it.quality == selectedStream.quality } ?: target.firstOrNull()
                                                    if (same != null) onSelectStream(same)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            null -> {}
                        }
                    }
                }
                if (panel == PlayerPanel.CAPTIONS) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (isFullscreen) 28.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        listOf("S", "M", "L", "XL").forEachIndexed { idx, lbl ->
                            val sel = AppSettings.subtitleSize == idx
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) CinedorMagenta else Color.White.copy(alpha = 0.14f))
                                    .clickable { AppSettings.setSubtitleSize(ctx, idx); interactionTick++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (AppSettings.subtitleBackground) CinedorMagenta else Color.White.copy(alpha = 0.14f))
                                .clickable { AppSettings.setSubtitleBackground(ctx, !AppSettings.subtitleBackground); interactionTick++ }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Background", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
                IconButton(
                    onClick = { openPanel = null; interactionTick++ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TopOverlayActionItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) CinedorMagenta else Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = if (active) 0f else 0.16f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun SleekVideoSeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    isScrubbing: Boolean,
    onScrubStart: (Long) -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val bufferedFraction = if (durationMs > 0L) {
        (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0L) {
                        val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                        onScrubEnd((frac * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0L) {
                            dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                            onScrubStart((dragFraction * durationMs).toLong())
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (durationMs > 0L) {
                            change.consume()
                            dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            onScrubMove((dragFraction * durationMs).toLong())
                        }
                    },
                    onDragEnd = {
                        if (durationMs > 0L) {
                            onScrubEnd((dragFraction * durationMs).toLong())
                        }
                    },
                    onDragCancel = {
                        if (durationMs > 0L) {
                            onScrubEnd((dragFraction * durationMs).toLong())
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val centerY = size.height / 2f
            val trackHeight = if (isScrubbing) 5.dp.toPx() else 3.5.dp.toPx()
            val thumbRadius = if (isScrubbing) 8.dp.toPx() else 6.dp.toPx()
            val trackTop = centerY - trackHeight / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // Unplayed track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.24f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // Buffered track
            val bufWidth = (size.width * bufferedFraction).coerceIn(0f, size.width)
            if (bufWidth > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.48f),
                    topLeft = Offset(0f, trackTop),
                    size = Size(bufWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Played track (crisp white with subtle Cinedor accent glow while scrubbing)
            val playedWidth = (size.width * progressFraction).coerceIn(0f, size.width)
            if (playedWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CinetorRedDark, CinedorMagenta, CinedorPink)
                    ),
                    topLeft = Offset(0f, trackTop),
                    size = Size(playedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Crisp white circular thumb
            val thumbX = playedWidth.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
            if (isScrubbing) {
                drawCircle(
                    color = CinedorMagenta.copy(alpha = 0.35f),
                    radius = thumbRadius * 1.65f,
                    center = Offset(thumbX, centerY)
                )
            }
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = CinedorMagenta,
                radius = thumbRadius * 0.55f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

internal enum class PlayerPanel { QUALITY, SPEED, CAPTIONS, AUDIO, TRANSLATE }

/** A built-in audio or subtitle track found inside the video stream. */
data class PlayerTrackOption(
    val key: String,
    val label: String,
    val isAudio: Boolean,
    val selected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

internal fun Context.findHostActivityForPlayer(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

internal fun qualityBadge(label: String): String? {
    val n = Regex("(\\d{3,4})").find(label)?.value?.toIntOrNull() ?: return null
    return when {
        n >= 2000 -> "4K"
        n >= 1080 -> "FHD"
        n >= 720 -> "HD"
        else -> "SD"
    }
}

@Composable
private fun PanelOptionCard(
    title: String,
    badge: String?,
    subtitle: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    dimmed: Boolean = false
) {
    Column(
        modifier = Modifier
            .alpha(if (dimmed && !selected) 0.55f else 1f)
            .width(if (compact) 96.dp else 124.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(AccentBright, CinetorRedDark))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.08f)))
                }
            )
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 14.sp else 16.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badge != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) Color.White else CinedorMagenta)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                    color = if (selected) CinedorMagenta else Color.White
                )
            }
        }
        if (subtitle.isNotBlank() && !compact) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
