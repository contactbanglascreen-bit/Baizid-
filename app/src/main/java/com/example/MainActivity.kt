package com.example

import android.graphics.Color
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.CinePulseDatabase
import com.example.data.local.LocalLibraryRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.sanitizeLocalDocId
import com.example.data.model.MediaItem
import com.example.data.remote.CinePulseApiClient
import com.example.ui.components.CinedorNavDestination
import com.example.ui.components.CinedorPremium3DNavBar
import com.example.ui.screens.DetailPlayerScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryMode
import com.example.ui.screens.LibraryProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.CinePulseTheme
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.CinePulseViewModel
import com.example.ui.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.components.AppSettings.load(applicationContext)
        com.example.ui.components.AdultLock.load(applicationContext)
        com.example.ui.components.PipState.requestPip = { enterPipIfAllowed() }

        // Lazy Coil ImageLoaderFactory: avoids synchronous DiskCache I/O on Main Thread during onCreate
        Coil.setImageLoader {
            ImageLoader.Builder(applicationContext)
                .memoryCache {
                    MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("cinedor_image_cache"))
                        .maxSizeBytes(100L * 1024L * 1024L)
                        .build()
                }
                .respectCacheHeaders(false)
                .crossfade(160)
                .build()
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            CinePulseTheme {
                CinedorMainShell()
            }
        }
    }

    private fun enterPipIfAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!com.example.ui.components.AppSettings.pipEnabled) return
        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (com.example.ui.components.PipState.videoPlaying) enterPipIfAllowed()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        com.example.ui.components.PipState.inPip = isInPictureInPictureMode
    }
}

private enum class CinedorTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("Explore", Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_explore"),
    REELS("Shorts", Icons.Rounded.PlayCircle, Icons.Outlined.PlayCircle, "nav_reels"),
    MY_LIST("My List", Icons.Rounded.Bookmarks, Icons.Outlined.Bookmarks, "nav_library"),
    ME("Me", Icons.Rounded.Person, Icons.Outlined.Person, "nav_me")
}

@Composable
fun CinedorMainShell() {
    val viewModel: CinePulseViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = CinePulseDatabase.getInstance(app)
                CinePulseViewModel(
                    apiClient = CinePulseApiClient(),
                    libraryRepository = LocalLibraryRepository(db.libraryDao()),
                    preferencesRepository = UserPreferencesRepository(app)
                )
            }
        }
    )

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val reelsState by viewModel.reelsState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val watchlistState by viewModel.watchlistState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
    val isDualSiteActive by viewModel.isDualSiteActive.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val watchlist = (watchlistState as? UiState.Success)?.data.orEmpty()
    val history = (historyState as? UiState.Success)?.data.orEmpty()
    val reminders = (remindersState as? UiState.Success)?.data.orEmpty()

    // Live poster preview for the raised circular "Shorts" center button (exact MovieBox look!)
    val shortsThumbUrl = remember(reelsState.reelsState, homeState.feedState) {
        val reelThumb = (reelsState.reelsState as? UiState.Success)?.data?.firstOrNull()?.posterUrl
        val homeThumb = (homeState.feedState as? UiState.Success)?.data?.banners?.firstOrNull()?.posterUrl
            ?: (homeState.feedState as? UiState.Success)?.data?.trendingItems?.firstOrNull()?.posterUrl
        reelThumb?.ifBlank { null } ?: homeThumb
    }

    var currentTab by rememberSaveable { mutableStateOf(CinedorTab.HOME) }

    // Ask to update when a newer build is published on GitHub Releases.
    com.example.ui.components.AppUpdatePrompt()

    // 18+ content re-locks every time the app starts: it needs the PIN again.
    LaunchedEffect(preferences.safeMode) {
        if (!preferences.safeMode && !com.example.ui.components.AdultLock.sessionUnlocked) {
            viewModel.updateSafeMode(true)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    // Full-screen overlay when a media detail/player is active
    val selectedDetail = detailState.selectedItem
    if (selectedDetail != null) {
        val detailDocId = sanitizeLocalDocId(selectedDetail.id, selectedDetail.title)
        val inWatchlist = watchlist.any { it.id == detailDocId || it.contentId == selectedDetail.id }

        Scaffold(
            containerColor = ObsidianBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                DetailPlayerScreen(
                    state = detailState,
                    isInWatchlist = inWatchlist,
                    staffState = staffState,
                    onBack = { viewModel.closeDetail() },
                    onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                    onSelectEpisode = { se, ep -> viewModel.selectEpisode(se, ep) },
                    onSelectStream = { viewModel.selectStreamOption(it) },
                    onSelectCaption = { viewModel.selectCaptionTrack(it) },
                    onRetryStream = { viewModel.refreshCurrentStream() },
                    onRetryDetail = { viewModel.openMediaDetail(selectedDetail) },
                    onOpenRelated = { viewModel.openMediaDetail(it) },
                    onOpenStaff = { viewModel.openStaff(it) },
                    onCloseStaff = { viewModel.closeStaff() },
                    onProgressUpdate = { pos, dur -> viewModel.onPlaybackProgress(pos, dur) },
                    onStreamExhausted = { failed, httpCode -> viewModel.fallbackToNextStream(failed, httpCode) }
                )
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBackground,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = if (useNavRail) 12.dp else 80.dp)
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            contentColor = TextPrimary
                        ) {
                            CinedorTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = CrimsonPrimary,
                                        selectedTextColor = TextPrimary,
                                        indicatorColor = CinetorRedSoft,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            CinedorTab.HOME -> HomeScreen(
                                homeState = homeState,
                                preferences = preferences,
                                watchlist = watchlist,
                                history = history,
                                reminders = reminders,
                                onSelectTab = { viewModel.selectHomeTab(it) },
                                onCycleSource = { viewModel.updateSource(it) },
                                onOpenSearch = { currentTab = CinedorTab.EXPLORE },
                                onOpenMedia = { viewModel.openMediaDetail(it) },
                                onResumeHistory = { entry ->
                                    viewModel.openMediaDetail(
                                        item = entry.toMediaItem(),
                                        initialSeason = entry.season.coerceAtLeast(1),
                                        initialEpisode = entry.episode.coerceAtLeast(1),
                                        resumeMs = entry.positionMs
                                    )
                                },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                onToggleReminder = { viewModel.toggleReminder(it) },
                                onSportsClick = { match -> viewModel.openSportsMatch(match) },
                                onRetry = { viewModel.loadHome(forceRefresh = true) }
                            )

                            CinedorTab.EXPLORE -> ExploreScreen(
                                state = exploreState,
                                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                onExecuteSearch = { viewModel.executeSearch(it) },
                                onSelectPlatform = { key, label -> viewModel.selectPlatform(key, label) },
                                onSelectCategoryChip = { viewModel.selectExploreCategoryChip(it) },
                                onUpdateFilter = { cat, genre, yr -> viewModel.updateFilter(cat, genre, yr) },
                                onLoadMore = { viewModel.loadFilteredCatalog(reset = false) },
                                onOpenMedia = { viewModel.openMediaDetail(it) },
                                onRetry = { viewModel.loadFilteredCatalog(reset = true) }
                            )

                            CinedorTab.REELS -> ReelsScreen(
                                state = reelsState,
                                onEnsureLoaded = { viewModel.ensureReelsLoaded() },
                                onLoadMore = { viewModel.loadReels(reset = false) },
                                onOpenFullMedia = { viewModel.openMediaDetail(it) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                onRetry = { viewModel.loadReels(reset = true) }
                            )

                            CinedorTab.MY_LIST, CinedorTab.ME -> LibraryProfileScreen(
                                mode = if (currentTab == CinedorTab.MY_LIST) LibraryMode.MY_LIST else LibraryMode.ME,
                                preferences = preferences,
                                watchlist = watchlist,
                                history = history,
                                reminders = reminders,
                                apiStatus = apiStatus,
                                isDualSiteActive = isDualSiteActive,
                                onOpenMedia = { viewModel.openMediaDetail(it) },
                                onResumeHistory = { entry ->
                                    viewModel.openMediaDetail(
                                        item = entry.toMediaItem(),
                                        initialSeason = entry.season.coerceAtLeast(1),
                                        initialEpisode = entry.episode.coerceAtLeast(1),
                                        resumeMs = entry.positionMs
                                    )
                                },
                                onRemoveHistory = { viewModel.removeHistoryItem(it) },
                                onClearHistory = { viewModel.clearAllHistory() },
                                onRemoveReminder = { rem ->
                                    viewModel.toggleReminder(
                                        MediaItem(
                                            id = rem.contentId,
                                            title = rem.title,
                                            posterUrl = rem.posterUrl,
                                            releaseDate = rem.releaseDate
                                        )
                                    )
                                },
                                onUpdateSource = { viewModel.updateSource(it) },
                                onUpdateLanguage = { viewModel.updateLanguage(it) },
                                onUpdateSafeMode = { viewModel.updateSafeMode(it) },
                                onSaveApiKey = { viewModel.updateCustomApiKey(it) },
                                onCheckApiStatus = { viewModel.checkApiStatus() },
                                onOpenCollection = { kind ->
                                    val chips = exploreState.categories
                                    fun findChip(vararg keys: String) = chips.firstOrNull { c ->
                                        keys.any { k -> c.name.contains(k, ignoreCase = true) || c.path.contains(k, ignoreCase = true) }
                                    }
                                    when (kind) {
                                        "kdrama" -> {
                                            val chip = findChip("k-drama", "kdrama", "korean drama", "korean")
                                            if (chip != null) viewModel.selectExploreCategoryChip(chip)
                                            else {
                                                viewModel.onSearchQueryChanged("Korean drama")
                                                viewModel.executeSearch("Korean drama")
                                            }
                                        }
                                        "cdrama" -> {
                                            val chip = findChip("c-drama", "cdrama", "chinese drama", "chinese")
                                            if (chip != null) viewModel.selectExploreCategoryChip(chip)
                                            else {
                                                viewModel.onSearchQueryChanged("Chinese drama")
                                                viewModel.executeSearch("Chinese drama")
                                            }
                                        }
                                        else -> viewModel.updateFilter("anime", null, null)
                                    }
                                    currentTab = CinedorTab.EXPLORE
                                }
                            )
                        }
                    }
                }

                // Premium 3D Sculpted 5-Tab Bottom Bar with Raised Domed-Glass Center "Shorts" Orb
                if (!useNavRail) {
                    val activeDestination = when (currentTab) {
                        CinedorTab.HOME -> CinedorNavDestination.HOME
                        CinedorTab.EXPLORE -> CinedorNavDestination.EXPLORE
                        CinedorTab.REELS -> CinedorNavDestination.REELS
                        CinedorTab.MY_LIST -> CinedorNavDestination.MY_LIST
                        CinedorTab.ME -> CinedorNavDestination.ME
                    }
                    CinedorPremium3DNavBar(
                        currentDestination = activeDestination,
                        shortsThumbUrl = shortsThumbUrl,
                        onSelectDestination = { dest ->
                            currentTab = when (dest) {
                                CinedorNavDestination.HOME -> CinedorTab.HOME
                                CinedorNavDestination.EXPLORE -> CinedorTab.EXPLORE
                                CinedorNavDestination.REELS -> CinedorTab.REELS
                                CinedorNavDestination.MY_LIST -> CinedorTab.MY_LIST
                                CinedorNavDestination.ME -> CinedorTab.ME
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
