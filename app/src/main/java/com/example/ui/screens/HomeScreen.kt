package com.example.ui.screens

import coil.imageLoader
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.ChipBorder
import com.example.ui.theme.ChipBg
import com.example.ui.theme.CinetorBlush
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.OnAccent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UpcomingReminder
import com.example.data.local.UserPreferences
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.local.sanitizeLocalDocId
import com.example.data.model.ContentSource
import com.example.data.model.HomeFeed
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import com.example.data.model.SportsMatch
import com.example.ui.components.ApiErrorOrKeyCard
import com.example.ui.components.CategoryBannerChip
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroBannerCarousel
import com.example.ui.components.PosterCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.ShimmerPosterRow
import com.example.ui.components.SportsMatchCard
import com.example.ui.components.UpcomingReleaseCard
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.CinedorViolet
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

// Exact MovieBox top horizontal text category tabs
private val MovieBoxTopTabs = listOf(
    "All" to "Trending",
    "Movies" to "Movies",
    "Series" to "TV Series",
    "Anime" to "Anime",
    "Dramas" to "Dramas",
    "Live Sports" to "Sports",
    "Upcoming" to "Upcoming"
)

private val MovieSubTabs = listOf("Cinema", "TOP Movies", "New Bengali", "Bollywood")
private val SeriesSubTabs = listOf("Top Series", "Anime", "Asian Drama", "Indian Drama")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeState: HomeUiState,
    preferences: UserPreferences,
    watchlist: List<WatchlistItem>,
    history: List<WatchHistoryItem>,
    reminders: List<UpcomingReminder>,
    onSelectTab: (String) -> Unit,
    onCycleSource: (ContentSource) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onResumeHistory: (WatchHistoryItem) -> Unit,
    onToggleWatchlist: (MediaItem) -> Unit,
    onToggleReminder: (MediaItem) -> Unit,
    onSportsClick: (SportsMatch) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlistIds = watchlist.map { it.contentId }.toSet() + watchlist.map { it.id }.toSet()
    val reminderIds = reminders.map { it.contentId }.toSet() + reminders.map { it.id }.toSet()

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80 }
    }

    var selectedMovieSubTab by remember { mutableStateOf("Cinema") }
    var selectedSeriesSubTab by remember { mutableStateOf("Top Series") }

    // MovieBox animated rotating search ticker titles
    val tickerTitles = remember(homeState.feedState) {
        val feed = (homeState.feedState as? UiState.Success)?.data
        val titles = (feed?.trendingItems.orEmpty() + feed?.banners.orEmpty())
            .map { it.title }
            .distinct()
            .take(10)
        titles.ifEmpty { listOf("Solo Leveling", "Squid Game", "Demon Slayer", "Avengers: Infinity War") }
    }
    var tickerIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(tickerTitles) {
        while (true) {
            delay(3400)
            tickerIndex = (tickerIndex + 1) % tickerTitles.size
        }
    }

    val isFullHomeWithHero = homeState.selectedTab == "All" &&
        (homeState.feedState as? UiState.Success)?.data?.banners?.isNotEmpty() == true

    val successFeed = (homeState.feedState as? UiState.Success)?.data
    val allPool = remember(successFeed) {
        if (successFeed == null) {
            emptyList()
        } else {
            (successFeed.trendingItems + successFeed.sections.flatMap { it.items }).distinctBy { it.id }
        }
    }
    // Warm the image disk cache with the first posters so rows appear instantly while scrolling.
    val prefetchContext = LocalContext.current
    LaunchedEffect(successFeed) {
        val feed = successFeed ?: return@LaunchedEffect
        val urls = (feed.banners + feed.trendingItems + feed.sections.flatMap { it.items.take(12) })
            .mapNotNull { it.posterUrl.ifBlank { it.backdropUrl }.ifBlank { null } }
            .map { com.example.ui.components.posterThumbUrl(it, 360) }
            .distinct()
            .take(90)
        val loader = prefetchContext.imageLoader
        urls.forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(prefetchContext)
                    .data(url)
                    .size(360, 530)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            )
        }
    }

    val trendingMovies = remember(allPool, selectedMovieSubTab) {
        val moviesOnly = allPool.filter { it.mediaType == MediaCategoryType.MOVIE }.ifEmpty { allPool }
        when (selectedMovieSubTab) {
            "New Bengali" -> moviesOnly.filter {
                it.title.contains("bengali", true) ||
                    it.title.contains("bangla", true) ||
                    it.languageBadge.contains("ben", true)
            }.ifEmpty { moviesOnly.drop(3) }
            "Bollywood" -> moviesOnly.filter {
                it.title.contains("hindi", true) ||
                    it.languageBadge.contains("hindi", true) ||
                    it.country.contains("india", true)
            }.ifEmpty { moviesOnly.drop(6) }
            "TOP Movies" -> moviesOnly.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            else -> moviesOnly
        }.take(12)
    }
    val trendingSeries = remember(allPool, selectedSeriesSubTab) {
        val seriesOnly = allPool.filter {
            it.mediaType == MediaCategoryType.SERIES ||
                it.mediaType == MediaCategoryType.ANIME ||
                it.mediaType == MediaCategoryType.DRAMA
        }.ifEmpty { allPool.reversed() }
        when (selectedSeriesSubTab) {
            "Anime" -> seriesOnly.filter {
                it.mediaType == MediaCategoryType.ANIME ||
                    it.genres.any { g -> g.contains("anim", true) }
            }.ifEmpty { seriesOnly.drop(2) }
            "Asian Drama" -> seriesOnly.filter {
                it.country.contains("korea", true) ||
                    it.country.contains("china", true) ||
                    it.genres.any { g -> g.contains("drama", true) }
            }.ifEmpty { seriesOnly.drop(4) }
            "Indian Drama" -> seriesOnly.filter {
                it.country.contains("india", true) ||
                    it.languageBadge.contains("hindi", true)
            }.ifEmpty { seriesOnly.drop(6) }
            else -> seriesOnly
        }.take(12)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        var pullRefreshing by remember { mutableStateOf(false) }
        LaunchedEffect(homeState.feedState) { pullRefreshing = false }
        LaunchedEffect(pullRefreshing) {
            if (pullRefreshing) {
                delay(8000)
                pullRefreshing = false
            }
        }
        val pullState = rememberPullToRefreshState()
        val headerOffset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 104.dp
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                onRetry()
            },
            state = pullState,
            indicator = {
                // Show the spinner just below the sticky header so it is visible.
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = pullRefreshing,
                    color = CinetorRed,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = headerOffset)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 100.dp,
                bottom = 110.dp
            )
        ) {
            when (val feedState = homeState.feedState) {
                is UiState.Loading -> {
                    item(key = "loading_hero") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(horizontal = 48.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(listOf(ShimmerBase, ShimmerBase.copy(alpha = 0.6f)))
                                )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        ShimmerPosterRow()
                        Spacer(modifier = Modifier.height(20.dp))
                        ShimmerPosterRow()
                    }
                }

                is UiState.Error -> {
                    item(key = "home_error") {
                        Spacer(modifier = Modifier.height(12.dp))
                        ApiErrorOrKeyCard(
                            message = feedState.message,
                            isApiKeyMissing = feedState.isApiKeyMissing,
                            onRetry = onRetry
                        )
                    }
                    if (history.isNotEmpty()) {
                        item(key = "continue_watching_fallback") {
                            ContinueWatchingSection(history = history, onResumeHistory = onResumeHistory)
                        }
                    }
                }

                is UiState.Success -> {
                    val feed: HomeFeed = feedState.data

                    when (homeState.selectedTab) {
                        "Live Sports" -> {
                            item(key = "sports_only_header") {
                                SectionHeaderRow(title = "Live")
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            if (feed.sportsMatches.isEmpty()) {
                                item(key = "sports_empty") {
                                    Text(
                                        text = "No live matches scheduled right now. Check back soon.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            } else {
                                items(feed.sportsMatches, key = { it.id }) { match ->
                                    SportsMatchCard(
                                        match = match,
                                        onClick = { onSportsClick(match) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        "Upcoming" -> {
                            item(key = "upcoming_only_header") {
                                SectionHeaderRow(title = "Coming Soon")
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            item(key = "upcoming_grid_rows") {
                                val chunked = feed.upcomingItems.chunked(3)
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    chunked.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { item ->
                                                val docId = sanitizeLocalDocId(item.id, item.title)
                                                UpcomingReleaseCard(
                                                    item = item,
                                                    isReminded = reminderIds.contains(item.id) || reminderIds.contains(docId),
                                                    onToggleReminder = { onToggleReminder(item) },
                                                    onClick = { onOpenMedia(item) },
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
                        }

                        "Movies", "Series", "Anime", "Dramas" -> {
                            item(key = "filtered_header") {
                                SectionHeaderRow(title = "Top ${homeState.selectedTab}")
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            if (homeState.isCategoryLoading) {
                                item(key = "cat_shimmer") {
                                    ShimmerPosterRow()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ShimmerPosterRow()
                                }
                            } else {
                                val list = homeState.filteredItems.ifEmpty {
                                    feed.sections.flatMap { it.items }.distinctBy { it.id }.take(24)
                                }
                                items(list.chunked(3)) { rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { media ->
                                            PosterCard(
                                                item = media,
                                                onClick = { onOpenMedia(media) },
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

                        else -> {
                            // 100% MovieBox Home Screen Flow!
                            // 1. Hero Banner + Overlapping Floating Mini-Poster Cards
                            if (feed.banners.isNotEmpty()) {
                                item(key = "hero_banner") {
                                    // Everything that is trending goes into the slider (trending first, then banners)
                                    val heroItems = remember(feed, homeState.updates24h) {
                                        (feed.trendingItems + homeState.updates24h + feed.banners)
                                            .filter { it.posterUrl.isNotBlank() || it.backdropUrl.isNotBlank() }
                                            .distinctBy { it.title.lowercase().replace(Regex("\\s*\\[[^\\]]*\\]"), "").trim() }
                                            .take(25)
                                            .ifEmpty { feed.banners }
                                    }
                                    HeroBannerCarousel(
                                        banners = heroItems,
                                        watchlistIds = watchlistIds,
                                        onPlayClick = onOpenMedia,
                                        onToggleWatchlist = onToggleWatchlist
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            } else {
                                item(key = "top_spacer") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // 2. Continue Watching (appears automatically once user plays anything)
                            if (history.isNotEmpty()) {
                                item(key = "continue_watching") {
                                    ContinueWatchingSection(history = history, onResumeHistory = onResumeHistory)
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // 3. "Live" Sports Section (exact MovieBox match card row)
                            if (feed.sportsMatches.isNotEmpty()) {
                                item(key = "live_sports_shelf") {
                                    SectionHeaderRow(
                                        title = "Live",
                                        actionLabel = "All",
                                        onActionClick = { onSelectTab("Live Sports") }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(feed.sportsMatches.take(10), key = { it.id }) { match ->
                                            SportsMatchCard(match = match, onClick = { onSportsClick(match) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // 4. "Trending Movies" with MovieBox Sub-Filter Pills + Giant 1/2/3 Watermarks
                            if (trendingMovies.isNotEmpty()) {
                                item(key = "trending_movies_ranked") {
                                    SectionHeaderRow(
                                        title = "Trending Movies",
                                        actionLabel = "All",
                                        onActionClick = { onSelectTab("Movies") }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MovieBoxSubFilterRow(
                                        tabs = MovieSubTabs,
                                        selected = selectedMovieSubTab,
                                        onSelect = { selectedMovieSubTab = it }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(
                                            trendingMovies,
                                            key = { idx, item -> "tm_${selectedMovieSubTab}_${item.id}_$idx" }
                                        ) { idx, item ->
                                            PosterCard(
                                                item = item,
                                                rankNumber = idx + 1,
                                                onClick = { onOpenMedia(item) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // 5. "Trending TV Series" with MovieBox Sub-Filter Pills + Giant 1/2/3 Watermarks
                            if (trendingSeries.isNotEmpty()) {
                                item(key = "trending_series_ranked") {
                                    SectionHeaderRow(
                                        title = "Trending TV Series",
                                        actionLabel = "All",
                                        onActionClick = { onSelectTab("Series") }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MovieBoxSubFilterRow(
                                        tabs = SeriesSubTabs,
                                        selected = selectedSeriesSubTab,
                                        onSelect = { selectedSeriesSubTab = it }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(
                                            trendingSeries,
                                            key = { idx, item -> "ts_${selectedSeriesSubTab}_${item.id}_$idx" }
                                        ) { idx, item ->
                                            PosterCard(
                                                item = item,
                                                rankNumber = idx + 1,
                                                onClick = { onOpenMedia(item) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // 6. "Categories" Photo-Backed Banner Chips Strip (exact MovieBox look!)
                            item(key = "moviebox_categories_strip") {
                                SectionHeaderRow(title = "Categories")
                                Spacer(modifier = Modifier.height(10.dp))
                                val previews = allPool.mapNotNull { it.posterUrl.ifBlank { null } }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        CategoryBannerChip(
                                            label = "All",
                                            previewImageUrl = previews.getOrNull(0),
                                            accentColors = listOf(Color(0xFF281938), Color(0xFF181224)),
                                            onClick = onOpenSearch
                                        )
                                    }
                                    item {
                                        CategoryBannerChip(
                                            label = "All Movies",
                                            previewImageUrl = previews.getOrNull(1),
                                            accentColors = listOf(Color(0xFF3A1D4E), Color(0xFF6B3E52)),
                                            onClick = { onSelectTab("Movies") }
                                        )
                                    }
                                    item {
                                        CategoryBannerChip(
                                            label = "All Dramas",
                                            previewImageUrl = previews.getOrNull(2),
                                            accentColors = listOf(Color(0xFF4A1838), Color(0xFF843554)),
                                            onClick = { onSelectTab("Dramas") }
                                        )
                                    }
                                    item {
                                        CategoryBannerChip(
                                            label = "Top Anime",
                                            previewImageUrl = previews.getOrNull(3),
                                            accentColors = listOf(Color(0xFF2B1C54), Color(0xFF6236A8)),
                                            onClick = { onSelectTab("Anime") }
                                        )
                                    }
                                    item {
                                        CategoryBannerChip(
                                            label = "TV Series",
                                            previewImageUrl = previews.getOrNull(4),
                                            accentColors = listOf(Color(0xFF1D2446), Color(0xFF4A2A6A)),
                                            onClick = { onSelectTab("Series") }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // 7. "🔥 Fresh in Last 24h" / Hot Picks
                            if (homeState.updates24h.isNotEmpty()) {
                                item(key = "updates_24h") {
                                    SectionHeaderRow(
                                        title = "\uD83D\uDD25 Trending now",
                                        actionLabel = "All",
                                        onActionClick = onOpenSearch
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            homeState.updates24h.take(15),
                                            key = { "u24_${it.source}_${it.id}" }
                                        ) { item ->
                                            PosterCard(item = item, onClick = { onOpenMedia(item) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            // 8. Dynamic API Sections (Gems for you, Bollywood, Hollywood, K-Drama, C-Drama...)
                            items(feed.sections, key = { it.id }) { section ->
                                SectionHeaderRow(
                                    title = section.title,
                                    actionLabel = "All",
                                    onActionClick = onOpenSearch
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(
                                        section.items.take(15),
                                        key = { idx, item -> "${section.id}_${item.id}_$idx" }
                                    ) { idx, item ->
                                        PosterCard(
                                            item = item,
                                            rankNumber = if (section.isRanked && idx < 10) idx + 1 else null,
                                            onClick = { onOpenMedia(item) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // 9. "Coming Soon" MovieBox Date Ribbon Cards
                            if (feed.upcomingItems.isNotEmpty()) {
                                item(key = "coming_soon_shelf") {
                                    SectionHeaderRow(
                                        title = "Coming Soon",
                                        actionLabel = "All",
                                        onActionClick = { onSelectTab("Upcoming") }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(feed.upcomingItems.take(12), key = { "up_${it.id}" }) { item ->
                                            val docId = sanitizeLocalDocId(item.id, item.title)
                                            UpcomingReleaseCard(
                                                item = item,
                                                isReminded = reminderIds.contains(item.id) || reminderIds.contains(docId),
                                                onToggleReminder = { onToggleReminder(item) },
                                                onClick = { onOpenMedia(item) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }

                            // Browse every video by category (genre rows built from all loaded titles)
                            val genreRows = (allPool + homeState.updates24h)
                                .distinctBy { it.id }
                                .flatMap { m -> m.genres.map { g -> g.trim().replaceFirstChar { c -> c.uppercase() } to m } }
                                .filter { (g, _) -> g.isNotBlank() && g.length < 22 }
                                .groupBy({ it.first }, { it.second })
                                .filter { it.value.size >= 4 }
                                .entries
                                .sortedByDescending { it.value.size }
                                .take(12)
                            if (genreRows.isNotEmpty()) {
                                item(key = "category_header") {
                                    SectionHeaderRow(title = "Browse by category")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(genreRows.map { it.key }) { g ->
                                            Box(
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(CircleShape)
                                                    .background(ChipBg)
                                                    .border(1.dp, ChipBorder, CircleShape)
                                                    .clickable(onClick = onOpenSearch)
                                                    .padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    g,
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                                genreRows.forEach { (genre, list) ->
                                    item(key = "genre_row_$genre") {
                                        SectionHeaderRow(
                                            title = genre,
                                            actionLabel = "All",
                                            onActionClick = onOpenSearch
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(list.take(18), key = { "g_${genre}_${it.id}" }) { m ->
                                                PosterCard(item = m, onClick = { onOpenMedia(m) })
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(18.dp))
                                    }
                                }
                            }

                            // 10. Bottom "Most Trending" 3-Column Grid (exact MovieBox bottom scroll!)
                            if (allPool.size >= 6) {
                                item(key = "most_trending_header") {
                                    SectionHeaderRow(title = "Most Trending")
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                items(allPool.take(18).chunked(3)) { rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { media ->
                                            PosterCard(
                                                item = media,
                                                onClick = { onOpenMedia(media) },
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
                    }
                }
            }
        }

        }

        // STICKY TOP HEADER (logo + wordmark + round search button, then red pill category tabs)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ObsidianBackground,
                            CinetorBlush.copy(alpha = if (isScrolled) 0.98f else 0.96f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo + wordmark (tap cycles source ALL / MovieBox / MovieLinkBD)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val next = when (preferences.source) {
                                ContentSource.ALL -> ContentSource.MOVIEBOX
                                ContentSource.MOVIEBOX -> ContentSource.MOVIELINKBD
                                ContentSource.MOVIELINKBD -> ContentSource.ALL
                            }
                            onCycleSource(next)
                        }
                        .testTag("source_switcher_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cinedor_logo),
                        contentDescription = "Cinedor",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("CINE") }
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("DOR") }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            letterSpacing = 4.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1
                    )
                }

                if (homeState.selectedTab != "All") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = MovieBoxTopTabs.firstOrNull { it.first == homeState.selectedTab }?.second
                                ?: homeState.selectedTab,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Refresh button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .clickable {
                            pullRefreshing = true
                            onRetry()
                        }
                        .testTag("home_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (pullRefreshing) {
                        CircularProgressIndicator(
                            color = CinetorRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Round white search button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .clickable(onClick = onOpenSearch)
                        .testTag("home_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search ${tickerTitles[tickerIndex % tickerTitles.size]}",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pill category tabs: selected = solid red, others = light outlined pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(MovieBoxTopTabs) { (tabKey, displayLabel) ->
                    val selected = homeState.selectedTab == tabKey
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .then(
                                if (selected) {
                                    Modifier.shadow(4.dp, CircleShape, spotColor = CinetorRed)
                                } else {
                                    Modifier
                                }
                            )
                            .clip(CircleShape)
                            .background(if (selected) CinetorRed else ChipBg)
                            .border(1.dp, if (selected) CinetorRed else ChipBorder, CircleShape)
                            .clickable { onSelectTab(tabKey) }
                            .padding(horizontal = 16.dp)
                            .testTag("home_tab_${tabKey.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tabKey == "All") "For You" else displayLabel,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = if (selected) OnAccent else TextPrimary.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieBoxSubFilterRow(
    tabs: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs) { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CinetorRed else ChipBg)
                    .border(1.dp, if (isSelected) CinetorRed else ChipBorder, CircleShape)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected) OnAccent else TextPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingSection(
    history: List<WatchHistoryItem>,
    onResumeHistory: (WatchHistoryItem) -> Unit
) {
    Column {
        SectionHeaderRow(title = "Continue Watching")
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history.take(10), key = { it.id }) { entry ->
                ContinueWatchingCard(
                    history = entry,
                    onClick = { onResumeHistory(entry) }
                )
            }
        }
    }
}
