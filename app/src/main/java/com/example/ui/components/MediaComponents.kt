package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import com.example.data.model.SportsMatch
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.CinedorTintedSurface
import com.example.ui.theme.CinedorViolet
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * 100% MovieBox-faithful Hero Banner with overlapping floating mini-poster play card carousel,
 * styled with Cinedor's Violet -> Magenta -> Pink gradient accent.
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

    val currentItem = banners[pagerState.currentPage.coerceIn(0, banners.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(352.dp)
    ) {
        // Full-bleed Hero Artwork synced with active card
        AsyncImage(
            model = currentItem.backdropUrl.ifBlank { currentItem.posterUrl },
            contentDescription = currentItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clickable { onPlayClick(currentItem) }
        )

        // Top & bottom MovieBox atmospheric vignette blending cleanly into dark background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to ObsidianBackground.copy(alpha = 0.72f),
                            0.25f to Color.Transparent,
                            0.62f to Color.Transparent,
                            0.88f to ObsidianBackground.copy(alpha = 0.88f),
                            1.0f to ObsidianBackground
                        )
                    )
                )
        )

        // Overlapping MovieBox Floating Mini-Poster Card Carousel (peeking next card on the right!)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 12.dp, end = 52.dp),
            pageSpacing = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) { page ->
            val item = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clickable { onPlayClick(item) }
                    .testTag("hero_play_button")
            ) {
                // Dark rounded card body (sits slightly below the protruding mini-poster top)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianCard.copy(alpha = 0.94f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(68.dp)
                        .border(0.75.dp, ObsidianBorder.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 74.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
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
                                    imageVector = if (item.mediaType == MediaCategoryType.MOVIE) {
                                        Icons.Rounded.Movie
                                    } else {
                                        Icons.Rounded.LiveTv
                                    },
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                val metaParts = buildList {
                                    if (item.year.isNotBlank()) add(item.year)
                                    add(
                                        item.genres.firstOrNull()?.takeIf { it.isNotBlank() }
                                            ?: item.mediaType.label
                                    )
                                }
                                Text(
                                    text = "| ${metaParts.joinToString(" | ")}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Cinedor Logo-Gradient Circular Play Button (replaces MovieBox green circle)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(CinedorGradientColors))
                                .clickable { onPlayClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.play_now),
                                tint = ObsidianBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Protruding 3D Mini-Poster Thumbnail on Left
                AsyncImage(
                    model = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 6.dp)
                        .size(width = 52.dp, height = 74.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(ObsidianSurfaceVariant)
                        .border(0.75.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(7.dp))
                )
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
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = 0.2f),
        modifier = modifier.border(0.75.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
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
 * 100% MovieBox-faithful Poster Card:
 * - Rounded card fusing the 2:3 poster image + dark #1B1E23 caption footer strip
 * - Top-right translucent dark language/dub badge ("Hindi", "Punjabi", "Bengali")
 * - Giant semi-transparent rank watermark ("1", "2", "3") at bottom-right of poster
 * - Clean single-line title inside the fused dark footer
 */
@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rankNumber: Int? = null
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = ObsidianCard,
        modifier = modifier
            .width(116.dp)
            .clickable(onClick = onClick)
            .testTag("poster_card_${item.id.take(16)}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .background(ObsidianSurfaceVariant)
            ) {
                AsyncImage(
                    model = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-right dark translucent language/quality badge (MovieBox exact style)
                val badgeText = item.languageBadge.ifBlank { item.qualityBadge }
                if (badgeText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText.take(10),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Giant Translucent Rank Watermark at bottom-right (MovieBox "1", "2", "3" style)
                if (rankNumber != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                )
                            )
                    )
                    Text(
                        text = rankNumber.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 54.sp,
                            lineHeight = 54.sp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.62f),
                                    CinedorMagenta.copy(alpha = 0.28f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = 6.dp)
                    )
                }
            }

            // Fused MovieBox Dark Caption Footer Strip
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                ),
                color = Color(0xFFDCE0E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianCard)
                    .padding(horizontal = 7.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * MovieBox-style wide landscape card (used for Viral Shorts / Music / Kids / Continue Watching).
 */
@Composable
fun ContinueWatchingCard(
    history: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ObsidianCard,
        modifier = modifier
            .width(184.dp)
            .clickable(onClick = onClick)
            .testTag("continue_watching_${history.id.take(16)}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
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
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(CinedorGradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume",
                            tint = ObsidianBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (history.season > 0L && history.episode > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "S${history.season} E${history.episode}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White
                        )
                    }
                }
            }

            // Cinedor Gradient Progress Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(ObsidianSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(history.progressFraction.coerceIn(0.05f, 1f))
                        .height(2.5.dp)
                        .background(Brush.horizontalGradient(CinedorGradientColors))
                )
            }

            Text(
                text = history.title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFDCE0E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}

/**
 * 100% MovieBox-faithful Live Sports Match Card with Cinedor Violet-Magenta accent gradient!
 */
@Composable
fun SportsMatchCard(
    match: SportsMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBrush = if (match.isLive) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF3A1230),
                Color(0xFF1F1634),
                Color(0xFF142238)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF1A1C24),
                Color(0xFF1E1B2B)
            )
        )
    }

    Box(
        modifier = modifier
            .width(276.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBrush)
            .border(0.75.dp, ObsidianBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            // Centered League Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (match.isLive) CinedorPink else CinedorViolet)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = match.league,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = Color(0xFFD5D8E2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams & Big Center Score
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = if (match.isLive) CinedorMagenta else TextPrimary
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
                            color = TextSecondary
                        )
                    }
                }

                TeamColumn(name = match.awayTeam, logoUrl = match.awayLogo, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Full-Width Action Pill ("Details" or "Notify Me" in Cinedor accent)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (match.isLive) {
                            Color.White.copy(alpha = 0.12f)
                        } else {
                            CinedorTintedSurface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!match.isLive) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = CinedorMagenta,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (match.isLive) "Details" else "Notify Me",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (match.isLive) TextPrimary else CinedorMagenta
                    )
                }
            }
        }
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
                .size(34.dp)
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
                    color = TextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color(0xFFD5D8E2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 100% MovieBox-faithful Categories Photo-Backed Banner Chip ("All", "All Movies", "All Dramas", etc.)
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
            .height(46.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.horizontalGradient(accentColors))
            .clickable(onClick = onClick)
    ) {
        if (!previewImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = previewImageUrl,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColors.first().copy(alpha = 0.85f),
                                accentColors.last().copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        )
    }
}

/**
 * 100% MovieBox-faithful Coming Soon Card:
 * - Top-left 2-line Cinedor-tinted date block (e.g. "Dec\n12")
 * - Top-right outline/filled reminder bell button
 * - Bottom-center booked/release text over fade
 * - Fused dark caption strip underneath
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

    Surface(
        shape = RoundedCornerShape(9.dp),
        color = ObsidianCard,
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .background(ObsidianSurfaceVariant)
            ) {
                AsyncImage(
                    model = item.posterUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left MovieBox stacked Date Ribbon (in Cinedor deep violet-magenta!)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(bottomEnd = 8.dp))
                        .background(CinedorTintedSurface.copy(alpha = 0.92f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = topDateLine,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                    if (bottomDateLine.isNotBlank()) {
                        Text(
                            text = bottomDateLine,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = CinedorMagenta
                        )
                    }
                }

                // Top-right Reminder Bell Icon
                IconButton(
                    onClick = onToggleReminder,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isReminded) {
                                CinedorMagenta
                            } else {
                                Color.Black.copy(alpha = 0.45f)
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (isReminded) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.NotificationsNone
                        },
                        contentDescription = if (isReminded) "Remove reminder" else "Remind me",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Bottom-center subtle fade + status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isReminded) "Reminded ✓" else item.releaseDate.ifBlank { "Coming Soon" }.take(14),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isReminded) CinedorMagenta else Color.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                }
            }

            // Fused MovieBox dark footer caption
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFFDCE0E8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianCard)
                    .padding(horizontal = 7.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 100% MovieBox-faithful Section Header: Clean bold white title left + "All >" chevron link right
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
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
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
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ObsidianCard.copy(alpha = alpha))
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
        shape = RoundedCornerShape(16.dp),
        color = ObsidianCard,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                1.dp,
                if (isApiKeyMissing) CinemaGold.copy(alpha = 0.6f) else ObsidianBorder,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isApiKeyMissing) CinemaGold else CrimsonPrimary).copy(alpha = 0.16f)
                    ),
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
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isApiKeyMissing) stringResource(R.string.api_key_missing_body) else message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
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
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCard,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
