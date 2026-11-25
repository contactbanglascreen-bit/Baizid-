package com.example.ui.components

import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.WatchHistoryItem
import com.example.data.model.MediaItem
import com.example.data.model.SportsMatch
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.RankOutline
import com.example.ui.theme.ChipBorder
import com.example.ui.theme.AccentBright
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinetorBlush
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedDark
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MediaCardDark
import com.example.ui.theme.MediaCardDarkEnd
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.OnAccent
import com.example.ui.theme.OnMediaText
import com.example.ui.theme.OnMediaTextSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

/** Red gradient used by every primary "Play" style button. */
private val RedButtonBrush: Brush get() = Brush.verticalGradient(listOf(AccentBright, CinetorRedDark))

/** Small dark translucent pill with a red star + rating (e.g. "★ 7.9"). */
@Composable
fun RatingBadge(rating: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Black.copy(alpha = 0.45f)))
            )
            .border(0.8.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = CinetorRed,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = rating,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = OnMediaText,
            maxLines = 1
        )
    }
}

/**
 * Liquid-glass finish drawn over a poster: a soft diagonal light reflection, a glossy top edge
 * and a thin bright rim, like the poster sits behind curved glass.
 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.GlassSheen(cornerRadius: androidx.compose.ui.unit.Dp = 16.dp) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = 0.26f),
                    0.28f to Color.White.copy(alpha = 0.06f),
                    0.29f to Color.Transparent,
                    0.78f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.45f)
                    )
                ),
                shape = shape
            )
    )
}

/** Translucent outlined chip drawn on top of artwork ("Action", "2022"...). */
@Composable
private fun GlassMetaChip(text: String, modifier: Modifier = Modifier, solid: Boolean = false) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (solid) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = if (solid) 0f else 0.35f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (solid) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (solid) TextPrimary else OnMediaText,
            maxLines = 1
        )
    }
}

/**
 * Hero carousel: large rounded centre poster card with peeking neighbours,
 * rating/year/genre chips, a red "Play" pill and a white "My List" pill,
 * plus a page indicator (active page = long red pill).
 */
@Composable
fun HeroBannerCarousel(
    banners: List<MediaItem>,
    watchlistIds: Set<String>,
    onPlayClick: (MediaItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(5500)
                val next = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(CinetorBlush, ObsidianBackground))
            )
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 60.dp),
            pageSpacing = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
        ) { page ->
            val item = banners[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val fraction = 1f - pageOffset.coerceIn(0f, 1f)
            val inWatchlist = watchlistIds.contains(item.id)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 0.86f + 0.14f * fraction
                        scaleX = s
                        scaleY = s
                        alpha = 0.55f + 0.45f * fraction
                    }
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(MediaCardDark)
                    .clickable { onPlayClick(item) }
            ) {
                FastPosterImage(
                    url = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    thumbWidth = 720,
                    modifier = Modifier.fillMaxSize()
                )
                GlassSheen(cornerRadius = 22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.52f to Color.Transparent,
                                    0.80f to Color.Black.copy(alpha = 0.55f),
                                    1.0f to lerp(CinetorRedDark, Color.Black, 0.65f).copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.rating.isNotBlank()) RatingBadge(rating = item.rating)
                        if (item.year.isNotBlank()) GlassMetaChip(text = item.year)
                        val lang = item.languageBadge
                        if (lang.isNotBlank()) GlassMetaChip(text = lang.take(10), solid = true)
                    }
                    val genres = item.genres.filter { it.isNotBlank() }.take(3)
                    if (genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            genres.forEach { GlassMetaChip(text = it) }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .shadow(5.dp, CircleShape, spotColor = CinetorRed)
                                .clip(CircleShape)
                                .background(RedButtonBrush)
                                .clickable { onPlayClick(item) }
                                .testTag("hero_play_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.play_now),
                                tint = OnAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Play",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = OnAccent
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFEDE8E8)))
                                )
                                .clickable { onToggleWatchlist(item) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (inWatchlist) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = if (inWatchlist) "Remove from My List" else "Add to My List",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "My List",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Up to 7 dots, sliding along with the current page so every page has a dot
                val windowSize = minOf(7, banners.size)
                val firstDot = (pagerState.currentPage - windowSize / 2)
                    .coerceIn(0, banners.size - windowSize)
                repeat(windowSize) { i ->
                    val index = firstDot + i
                    val active = pagerState.currentPage == index
                    val w by animateDpAsState(if (active) 20.dp else 6.dp, label = "dot_w")
                    val c by animateColorAsState(if (active) CinetorRed else ChipBorder, label = "dot_c")
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(c)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.18f),
        modifier = modifier.border(0.75.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Poster card:
 * - Rounded 2:3 poster with a dark rating pill (red star) top-left
 *   and a white language pill top-right
 * - Title underneath on the page background
 * - Optional large outlined rank number peeking out on the left ("Top 10" rows)
 */
@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rankNumber: Int? = null
) {
    val posterWidth = 106.dp
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("poster_card_${item.id.take(16)}"),
        verticalAlignment = Alignment.Bottom
    ) {
        if (rankNumber != null) {
            Box(modifier = Modifier.offset(x = 8.dp, y = (-22).dp)) {
                Text(
                    text = rankNumber.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        brush = Brush.verticalGradient(
                            listOf(CinetorRed.copy(alpha = 0.85f), CinetorRed.copy(alpha = 0.08f))
                        )
                    )
                )
                Text(
                    text = rankNumber.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 64.sp,
                        lineHeight = 64.sp,
                        drawStyle = Stroke(width = 3f)
                    ),
                    color = CinetorRed.copy(alpha = 0.55f)
                )
            }
        }
        Column(modifier = Modifier.width(posterWidth)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                )
                FastPosterImage(
                    url = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    thumbWidth = 360,
                    modifier = Modifier.fillMaxSize()
                )
                GlassSheen()
                if (item.rating.isNotBlank()) {
                    RatingBadge(
                        rating = item.rating,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    )
                }
            }
            val (cleanTitle, tag) = remember(item.title) { splitTitleTag(item.title) }
            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, end = 2.dp, top = 7.dp)
            )
            val shownYear = item.year.take(4).ifBlank { yearFromTitle(item.title) }
            if (shownYear.isNotBlank() || tag.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (shownYear.isNotBlank()) {
                        Text(
                            text = shownYear,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                    if (shownYear.isNotBlank() && tag.isNotBlank()) Spacer(modifier = Modifier.width(5.dp))
                    if (tag.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(CinetorRedSoft)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = CinetorRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Wide landscape "Continue Watching" card with red progress bar.
 */
@Composable
fun ContinueWatchingCard(
    history: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("continue_watching_${history.id.take(16)}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianSurfaceVariant)
        ) {
            AsyncImage(
                model = history.posterUrl,
                contentDescription = history.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RedButtonBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Resume",
                        tint = OnAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (onRemove != null) {
                // Remove from Continue Watching
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove from Continue Watching",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            if (history.season > 0L && history.episode > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "S${history.season} E${history.episode}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = OnMediaText
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(history.progressFraction.coerceIn(0.05f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(CinetorRed)
                )
            }
        }

        Text(
            text = history.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
        )
    }
}

/**
 * Live sports card: dark rounded tile with a red "LIVE" pill and red round play button.
 */
@Composable
fun SportsMatchCard(
    match: SportsMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(256.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(MediaCardDark, MediaCardDarkEnd)))
            .border(
                1.dp,
                if (match.isLive) CinetorRed.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.league,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = OnMediaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (match.isLive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    LivePill()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamColumn(name = match.homeTeam, logoUrl = match.homeLogo, modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = match.scoreText.ifBlank { "VS" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = OnMediaText
                    )
                    if (!match.isLive && match.startTime.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedTime = remember(match.startTime) {
                            val raw = match.startTime.trim()
                            val numeric = raw.toLongOrNull()
                            if (numeric != null && numeric > 1_000_000_000L) {
                                val millis = if (numeric < 10_000_000_000L) numeric * 1000L else numeric
                                runCatching {
                                    java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(millis))
                                }.getOrDefault(raw.take(12))
                            } else {
                                raw.take(14)
                            }
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = OnMediaTextSecondary
                        )
                    }
                }

                TeamColumn(name = match.awayTeam, logoUrl = match.awayLogo, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (match.isLive) RedButtonBrush else Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.08f))
                        )
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (match.isLive) Icons.Rounded.PlayArrow else Icons.Rounded.NotificationsNone,
                    contentDescription = null,
                    tint = OnAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (match.isLive) "Watch live" else "Notify Me",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = OnAccent
                )
            }
        }
    }
}

/** Red "● LIVE" pill. */
@Composable
fun LivePill(modifier: Modifier = Modifier, label: String = "LIVE") {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(CinetorRed)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = OnAccent
        )
    }
}

@Composable
private fun TeamColumn(
    name: String,
    logoUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnMediaText
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = OnMediaTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Category chip with a faint poster preview behind a light rounded pill.
 * [accentColors] is kept for API compatibility.
 */
@Composable
fun CategoryBannerChip(
    label: String,
    previewImageUrl: String?,
    accentColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!previewImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = previewImageUrl,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                alpha = 0.18f,
                modifier = Modifier.matchParentSize()
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * Coming Soon card: rounded poster with a red date tag, reminder bell and title below.
 */
@Composable
fun UpcomingReleaseCard(
    item: MediaItem,
    isReminded: Boolean,
    onToggleReminder: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTokens = item.releaseDate.trim().split(" ", "-", "/").filter { it.isNotBlank() }
    val topDateLine = dateTokens.firstOrNull()?.take(4) ?: "Soon"
    val bottomDateLine = dateTokens.getOrNull(1)?.take(4) ?: ""

    Column(
        modifier = modifier
            .width(106.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianSurfaceVariant)
        ) {
            FastPosterImage(
                url = item.posterUrl.ifBlank { item.backdropUrl },
                contentDescription = item.title,
                thumbWidth = 360,
                modifier = Modifier.fillMaxSize()
            )
            GlassSheen()

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CinetorRed)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = topDateLine,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = OnAccent
                )
                if (bottomDateLine.isNotBlank()) {
                    Text(
                        text = bottomDateLine,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = OnAccent
                    )
                }
            }

            IconButton(
                onClick = onToggleReminder,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isReminded) CinetorRed else Color.White.copy(alpha = 0.92f))
            ) {
                Icon(
                    imageVector = if (isReminded) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                    contentDescription = if (isReminded) "Remove reminder" else "Remind me",
                    tint = if (isReminded) OnAccent else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isReminded) "Reminded ✓" else item.releaseDate.ifBlank { "Coming Soon" }.take(14),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = OnMediaText,
                    maxLines = 1
                )
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp)
        )
    }
}

/**
 * Section header: red accent bar + bold title on the left, "See all >" on the right.
 */
@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CinetorRed)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = remember(title) {
                title.replace(Regex("[\\p{So}\\p{Sk}\\uD800-\\uDFFF\\u200D\\uFE0F]"), "").trim().ifBlank { title }
            },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (actionLabel == "All") "See all" else actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ShimmerPosterRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ShimmerBase.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun ApiErrorOrKeyCard(
    message: String,
    isApiKeyMissing: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                1.dp,
                if (isApiKeyMissing) CinemaGold.copy(alpha = 0.6f) else ObsidianBorder,
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isApiKeyMissing) CinemaGold.copy(alpha = 0.16f) else CinetorRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isApiKeyMissing) Icons.Rounded.Key else Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = if (isApiKeyMissing) CinemaGold else CrimsonPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isApiKeyMissing) stringResource(R.string.api_key_missing_title) else "Unable to Load Content",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isApiKeyMissing) stringResource(R.string.api_key_missing_body) else message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary, contentColor = OnAccent),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("retry_api_button")
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun EmptyLibraryCard(
    icon: ImageVector = Icons.Rounded.Bookmark,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(CinetorRedSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CinetorRed,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}


/** Splits "Masters of the Universe [Hindi]" into ("Masters of the Universe", "Hindi"). */
internal fun yearFromTitle(raw: String): String =
    Regex("\\((\\d{4})\\)").find(raw)?.groupValues?.get(1).orEmpty()

internal fun splitTitleTag(raw: String): Pair<String, String> {
    val tags = Regex("\\[([^\\]]+)\\]").findAll(raw).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
    val clean = raw.replace(Regex("\\s*\\[[^\\]]*\\]"), "")
        .replace(Regex("\\s*\\(\\d{4}\\)"), "")
        .trim().ifBlank { raw }
    return clean to tags.joinToString(" · ")
}


private val DubKeywords = listOf(
    "hindi", "bengali", "bangla", "tamil", "telugu", "malayalam", "kannada", "urdu", "english",
    "dub", "dubbed", "dual audio", "multi audio", "arabic", "spanish", "indonesian", "french"
)

/** (has subtitles, has a dubbed audio version) guessed from the title tag and language badge. */
internal fun subDubFlags(item: MediaItem): Pair<Boolean, Boolean> {
    val tag = splitTitleTag(item.title).second.lowercase()
    val titleLower = item.title.lowercase()
    val hasDub = (tag.isNotBlank() && DubKeywords.any { it in tag }) || "dub" in titleLower || "dual audio" in titleLower
    val hasSub = item.languageBadge.isNotBlank() || "sub" in tag
    return hasSub to hasDub
}

@Composable
internal fun SubDubPill(label: String, filled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (filled) CinetorRed else Color.Black.copy(alpha = 0.62f))
            .border(0.75.dp, if (filled) Color.Transparent else Color.White.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.5.sp),
            color = Color.White
        )
    }
}


/**
 * Asks the image CDN for a small thumbnail instead of the full-size poster (much less data,
 * much faster), and falls back to the original image if the CDN doesn't support resizing.
 */
internal fun posterThumbUrl(url: String, width: Int): String {
    if (url.isBlank() || !url.startsWith("http")) return url
    val tmdb = Regex("(image\\.tmdb\\.org/t/p/)(original|w\\d+)(/)")
    if (tmdb.containsMatchIn(url)) {
        val size = if (width <= 360) "w342" else "w780"
        return url.replace(tmdb, "$1$size$3")
    }
    if (url.contains("x-oss-process", ignoreCase = true)) return url
    if (url.contains("aoneroom.com", ignoreCase = true)) {
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}x-oss-process=image/resize,w_$width/format,webp"
    }
    // Any other host: shrink big posters through a free image resizer (falls back to the
    // original if it fails), so a 1-2 MB JPG becomes a ~30 KB WebP.
    return "https://wsrv.nl/?url=" + java.net.URLEncoder.encode(url, "UTF-8") +
        "&w=$width&output=webp&q=78"
}

/**
 * Cache key that stays the same when a CDN adds changing tokens (?t=..., &sign=...) to the
 * same poster, so a poster downloaded once is never downloaded again.
 */
internal fun posterCacheKey(url: String, width: Int): String {
    val base = url.substringBefore("?").substringBefore("#")
    val original = if (url.startsWith("https://wsrv.nl/")) {
        runCatching {
            java.net.URLDecoder.decode(url.substringAfter("url=").substringBefore("&"), "UTF-8")
                .substringBefore("?")
        }.getOrDefault(base)
    } else base
    return "poster:$original@$width"
}

@Composable
internal fun FastPosterImage(
    url: String,
    contentDescription: String?,
    thumbWidth: Int,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumb = remember(url, thumbWidth) { posterThumbUrl(url, thumbWidth) }
    var useOriginal by remember(url) { mutableStateOf(thumb == url) }
    val request = remember(url, thumb, useOriginal) {
        val data = if (useOriginal) url else thumb
        val key = posterCacheKey(data, if (useOriginal) 0 else thumbWidth)
        coil.request.ImageRequest.Builder(context)
            .data(data)
            .diskCacheKey(key)
            .memoryCacheKey(key)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onError = { if (!useOriginal) useOriginal = true },
        modifier = modifier
    )
}
