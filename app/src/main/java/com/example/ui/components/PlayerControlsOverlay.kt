package com.example.ui.components

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
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class VideoResizeOption(val label: String) {
    FIT("Fit"),
    ZOOM("Fill"),
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
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
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
    LaunchedEffect(controlsVisible, isPlaying, isScrubbing, qualityMenuOpen, captionMenuOpen, interactionTick, isLocked) {
        if (controlsVisible && isPlaying && !isScrubbing && !qualityMenuOpen && !captionMenuOpen) {
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
            .onSizeChanged { containerWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(isLocked) {
                detectTapGestures(
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
                                onSeekBy(-10_000L)
                                seekFeedbackRight = false
                                seekFeedbackText = "-10s"
                                seekFeedbackTick++
                            }
                            fraction > 0.64f -> {
                                onSeekBy(10_000L)
                                seekFeedbackRight = true
                                seekFeedbackText = "+10s"
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
                        imageVector = if (seekFeedbackRight) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
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
                        horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 14.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed control (icon + clean label, matching MovieBox top-right style)
                        TopOverlayActionItem(
                            icon = Icons.Rounded.Speed,
                            label = if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x",
                            active = playbackSpeed != 1.0f,
                            onClick = {
                                interactionTick++
                                onCycleSpeed()
                            },
                            testTag = "player_speed_button"
                        )

                        // Quality selector (when multiple streams exist)
                        if (streams.size > 1) {
                            Box {
                                TopOverlayActionItem(
                                    icon = Icons.Rounded.HighQuality,
                                    label = selectedStream.quality.ifBlank { "HD" },
                                    active = qualityMenuOpen,
                                    onClick = {
                                        interactionTick++
                                        qualityMenuOpen = true
                                    },
                                    testTag = "player_quality_button"
                                )
                                DropdownMenu(
                                    expanded = qualityMenuOpen,
                                    onDismissRequest = {
                                        qualityMenuOpen = false
                                        interactionTick++
                                    },
                                    modifier = Modifier.background(ObsidianCard)
                                ) {
                                    streams.forEach { opt ->
                                        val isCurrent = opt.url == selectedStream.url
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${opt.label} (${opt.format})",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isCurrent) CinedorMagenta else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                qualityMenuOpen = false
                                                interactionTick++
                                                onSelectStream(opt)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Subtitles selector (when captions exist)
                        if (captions.isNotEmpty()) {
                            Box {
                                TopOverlayActionItem(
                                    icon = Icons.Rounded.ClosedCaption,
                                    label = "CC",
                                    active = selectedCaption != null,
                                    onClick = {
                                        interactionTick++
                                        captionMenuOpen = true
                                    },
                                    testTag = "player_caption_button"
                                )
                                DropdownMenu(
                                    expanded = captionMenuOpen,
                                    onDismissRequest = {
                                        captionMenuOpen = false
                                        interactionTick++
                                    },
                                    modifier = Modifier.background(ObsidianCard)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Off",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selectedCaption == null) CinedorMagenta else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            captionMenuOpen = false
                                            interactionTick++
                                            onSelectCaption(null)
                                        }
                                    )
                                    captions.forEach { track ->
                                        val isCurrent = selectedCaption?.url == track.url
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = track.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (isCurrent) CinedorMagenta else TextPrimary
                                                )
                                            },
                                            onClick = {
                                                captionMenuOpen = false
                                                interactionTick++
                                                onSelectCaption(track)
                                            }
                                        )
                                    }
                                }
                            }
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
                if (isFullscreen && !isBuffering) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                interactionTick++
                                onSeekBy(-10_000L)
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.36f))
                                .testTag("player_rewind_10")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                interactionTick++
                                onTogglePlayPause()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("player_center_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                interactionTick++
                                onSeekBy(10_000L)
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.36f))
                                .testTag("player_forward_10")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
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
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
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
                    IconButton(
                        onClick = {
                            interactionTick++
                            onTogglePlayPause()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

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

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${formatPlayerTime(effectivePos)}/${formatPlayerTime(safeDuration)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White.copy(alpha = 0.94f),
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
    val tint = if (active) CinedorPink else Color.White
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = tint.copy(alpha = 0.92f),
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
            val trackHeight = if (isScrubbing) 4.5.dp.toPx() else 2.8.dp.toPx()
            val thumbRadius = if (isScrubbing) 7.5.dp.toPx() else 5.5.dp.toPx()
            val trackTop = centerY - trackHeight / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // Unplayed track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
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
                        colors = if (isScrubbing) listOf(CinedorMagenta, CinedorPink, Color.White)
                        else listOf(Color.White.copy(alpha = 0.92f), Color.White)
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
        }
    }
}
