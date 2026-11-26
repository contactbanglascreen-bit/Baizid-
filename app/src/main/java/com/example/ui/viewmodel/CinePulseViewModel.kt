package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalLibraryRepository
import com.example.data.local.UpcomingReminder
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.local.sanitizeLocalDocId
import com.example.data.model.ApiStatusInfo
import com.example.data.model.CaptionTrack
import com.example.data.model.CastMember
import com.example.data.model.CategoryChip
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentSource
import com.example.data.model.FilterOptions
import com.example.data.model.HomeFeed
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.MediaCategoryType
import com.example.data.model.ReelItem
import com.example.data.model.SportsMatch
import com.example.data.model.StaffDetail
import com.example.data.model.StreamOption
import com.example.data.remote.CinePulseApiClient
import com.example.data.remote.CinePulseApiException
import com.example.data.remote.MovieBoxDirectClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val isApiKeyMissing: Boolean = false) : UiState<Nothing>
}

data class HomeUiState(
    val selectedTab: String = "All",
    val feedState: UiState<HomeFeed> = UiState.Loading,
    val filteredItems: List<MediaItem> = emptyList(),
    val isCategoryLoading: Boolean = false,
    val categoryPage: Int = 1,
    val categoryLoadingMore: Boolean = false,
    val categoryEnd: Boolean = false,
    val updates24h: List<MediaItem> = emptyList(),
    val ranking: List<MediaItem> = emptyList()
)

data class ExploreUiState(
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val hotSearches: List<String> = emptyList(),
    val categories: List<CategoryChip> = emptyList(),
    val filterOptions: FilterOptions = FilterOptions(),
    val selectedCategory: String = "movies",
    val selectedGenre: String = "All",
    val selectedYear: String = "All",
    val selectedPlatform: String? = null,
    val selectedCategoryChip: CategoryChip? = null,
    val activeCatalogTitle: String = "Discover Catalog",
    val itemsState: UiState<List<MediaItem>> = UiState.Loading,
    val page: Int = 1,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

data class ReelsUiState(
    val reelsState: UiState<List<ReelItem>> = UiState.Loading,
    val page: Int = 1,
    val isLoadingMore: Boolean = false
)

data class DetailUiState(
    val selectedItem: MediaItem? = null,
    val detailState: UiState<MediaDetail> = UiState.Loading,
    val isMovie: Boolean = true,
    val selectedSeason: Int = 1,
    val selectedEpisode: Int = 1,
    val streams: List<StreamOption> = emptyList(),
    val selectedStream: StreamOption? = null,
    val failedStreamUrls: Set<String> = emptySet(),
    val captions: List<CaptionTrack> = emptyList(),
    val selectedCaption: CaptionTrack? = null,
    val isStreamLoading: Boolean = false,
    val streamError: String? = null,
    val resumePositionMs: Long = 0L
)

class CinePulseViewModel(
    private val apiClient: CinePulseApiClient,
    private val libraryRepository: LocalLibraryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val feedCache: com.example.data.local.HomeFeedDiskCache? = null
) : ViewModel() {

    /** Which source/language/safe-mode the feed on screen belongs to. */
    private var shownFeedKey: String? = null

    private val _isDualSiteActive = MutableStateFlow(apiClient.isApiKeyConfigured)
    val isDualSiteActive: StateFlow<Boolean> = _isDualSiteActive.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val watchlistState: StateFlow<UiState<List<WatchlistItem>>> = libraryRepository.observeWatchlist()
        .map<List<WatchlistItem>, UiState<List<WatchlistItem>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing watchlist", error)
            emit(UiState.Error(error.message ?: "Failed to load watchlist"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val historyState: StateFlow<UiState<List<WatchHistoryItem>>> = libraryRepository.observeHistory()
        .map<List<WatchHistoryItem>, UiState<List<WatchHistoryItem>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing watch history", error)
            emit(UiState.Error(error.message ?: "Failed to load history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val remindersState: StateFlow<UiState<List<UpcomingReminder>>> = libraryRepository.observeReminders()
        .map<List<UpcomingReminder>, UiState<List<UpcomingReminder>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing reminders", error)
            emit(UiState.Error(error.message ?: "Failed to load reminders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _exploreState = MutableStateFlow(ExploreUiState())
    val exploreState: StateFlow<ExploreUiState> = _exploreState.asStateFlow()

    private val _reelsState = MutableStateFlow(ReelsUiState())
    val reelsState: StateFlow<ReelsUiState> = _reelsState.asStateFlow()

    private val _detailState = MutableStateFlow(DetailUiState())
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    private val _apiStatus = MutableStateFlow<ApiStatusInfo?>(null)
    val apiStatus: StateFlow<ApiStatusInfo?> = _apiStatus.asStateFlow()

    private val _staffState = MutableStateFlow<UiState<StaffDetail>?>(null)
    val staffState: StateFlow<UiState<StaffDetail>?> = _staffState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var suggestJob: Job? = null
    private var detailJob: Job? = null
    private var streamJob: Job? = null
    private var staffJob: Job? = null
    private var lastSavedProgressMs: Long = -1L

    init {
        viewModelScope.launch {
            var initialized = false
            preferencesRepository.preferencesFlow.collect { prefs ->
                apiClient.updateRuntimeApiKey(prefs.customApiKey)
                _isDualSiteActive.value = apiClient.isApiKeyConfigured
                if (!initialized) {
                    initialized = true
                    loadHome()
                    launch {
                        delay(250)
                        loadExploreMetadata()
                        loadFilteredCatalog(reset = true)
                    }
                }
            }
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun updateCustomApiKey(key: String) {
        viewModelScope.launch {
            val cleaned = key.trim()
            preferencesRepository.setCustomApiKey(cleaned)
            apiClient.updateRuntimeApiKey(cleaned)
            val active = apiClient.isApiKeyConfigured
            _isDualSiteActive.value = active
            _toastMessage.value = if (active) {
                "Dual-Site activated (MovieBox + MovieLinkBD)"
            } else {
                "Using Direct MovieBox mode"
            }
            loadHome(forceRefresh = true)
            loadExploreMetadata()
            loadFilteredCatalog(reset = true)
            checkApiStatus()
        }
    }

    fun updateSource(source: ContentSource) {
        viewModelScope.launch {
            preferencesRepository.setSource(source)
            delay(60)
            loadHome()
            loadFilteredCatalog(reset = true)
        }
    }

    fun updateLanguage(language: ContentLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            delay(60)
            loadHome()
        }
    }

    fun updateSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSafeMode(enabled)
            delay(60)
            loadHome()
            loadExploreMetadata()
            loadFilteredCatalog(reset = true)
        }
    }

    fun loadHome(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val prefs = preferences.value
            val feedKey = "${prefs.source.name}:${prefs.language.code}:${prefs.safeMode}"
            val showingSameFeed = shownFeedKey == feedKey && _homeState.value.feedState is UiState.Success
            if (!showingSameFeed) {
                // Show the copy saved on the phone instantly, then refresh it from the internet.
                val cached = feedCache?.read(feedKey)
                if (cached != null) {
                    shownFeedKey = feedKey
                    _homeState.update { it.copy(feedState = UiState.Success(cached)) }
                } else {
                    shownFeedKey = null
                    _homeState.update { it.copy(feedState = UiState.Loading) }
                }
            }
            try {
                val updatesJob = launch {
                    val updates = runCatching { apiClient.getUpdates24h(prefs.source, prefs.safeMode) }
                        .getOrDefault(emptyList())
                    _homeState.update { it.copy(updates24h = updates) }
                }
                // Top-10 ranking list from the API (shown as its own row on Home)
                launch {
                    val ranking = runCatching { apiClient.getRanking(prefs.source, prefs.safeMode, prefs.language.code) }
                        .getOrDefault(emptyList())
                    if (ranking.isNotEmpty()) _homeState.update { it.copy(ranking = ranking) }
                }
                val feed = apiClient.getHome(prefs.source, prefs.language, prefs.safeMode, forceRefresh)
                shownFeedKey = feedKey
                _homeState.update { it.copy(feedState = UiState.Success(feed)) }
                launch { feedCache?.write(feedKey, feed) }
                updatesJob.join()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                // Keep the saved copy on screen if we have one (e.g. offline).
                if (shownFeedKey != feedKey) {
                    _homeState.update {
                        it.copy(feedState = UiState.Error(e.message ?: "API error", e.isAuthMissing))
                    }
                }
            } catch (e: Exception) {
                if (shownFeedKey != feedKey) {
                    _homeState.update {
                        it.copy(feedState = UiState.Error(e.localizedMessage ?: "Failed to load home feed"))
                    }
                }
            }
        }
    }

    fun selectHomeTab(tab: String) {
        _homeState.update { it.copy(selectedTab = tab) }
        if (tab == "All" || tab == "Live Sports") {
            _homeState.update { it.copy(filteredItems = emptyList(), isCategoryLoading = false) }
            return
        }
        viewModelScope.launch {
            val prefs = preferences.value
            _homeState.update { it.copy(isCategoryLoading = true, filteredItems = emptyList()) }
            if (tab == "Upcoming") {
                // Coming soon: server list first, MovieBox list next, then this year's newest titles.
                val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val upcoming = runCatching { apiClient.getUpcoming(prefs.source, prefs.safeMode, prefs.language.code) }
                    .getOrDefault(emptyList())
                    .ifEmpty {
                        runCatching { apiClient.getUpcoming(ContentSource.MOVIEBOX, prefs.safeMode, prefs.language.code) }
                            .getOrDefault(emptyList())
                    }
                    .ifEmpty {
                        runCatching {
                            apiClient.filterCatalog(prefs.source, "movies", "", thisYear.toString(), 1, prefs.safeMode)
                        }.getOrDefault(emptyList())
                    }
                _homeState.update { it.copy(filteredItems = upcoming.distinctBy { it.id }, isCategoryLoading = false) }
                return@launch
            }
            val items = fetchCategoryPage(tab, 1)
            _homeState.update { it.copy(filteredItems = items, isCategoryLoading = false, categoryPage = 1, categoryEnd = items.isEmpty()) }
        }
    }

    /** One page of a Home category tab (Movies, Series, Anime, Dramas). */
    private suspend fun fetchCategoryPage(tab: String, page: Int): List<MediaItem> {
        val prefs = preferences.value
        val catParam = when (tab) {
            "Movies" -> "movies"
            "Series" -> "tv"
            "Anime" -> "anime"
            "Dramas" -> "drama"
            else -> "movies"
        }
        return if (tab == "Anime") {
            // The worker's anime list mixes in music videos and other clips, so use MovieBox's real
            // anime channel and keep only animated titles.
            // MovieLinkBD's anime is fetched at the same time and added after MovieBox's.
            kotlinx.coroutines.coroutineScope {
                val directJob = async {
                    runCatching { apiClient.filterCatalog(ContentSource.MOVIEBOX, "anime", "", "", page, prefs.safeMode) }
                        .getOrDefault(emptyList())
                }
                val mlbdJob = async {
                    if (prefs.source == ContentSource.MOVIEBOX) emptyList<MediaItem>()
                    else runCatching { apiClient.filterCatalog(ContentSource.MOVIELINKBD, "anime", "", "", page, prefs.safeMode) }
                        .getOrDefault(emptyList())
                }
                val direct = directJob.await()
                val mlbd = mlbdJob.await().filter { it.source.equals("movielinkbd", ignoreCase = true) && looksLikeAnime(it) }
                val pool = direct.ifEmpty {
                    runCatching { apiClient.filterCatalog(prefs.source, "anime", "", "", page, prefs.safeMode) }
                        .getOrDefault(emptyList())
                }
                (pool.filter { looksLikeAnime(it) }.ifEmpty { direct } + mlbd).distinctBy { "${it.source}:${it.id}" }
            }
        } else {
            runCatching {
                apiClient.filterCatalog(
                    source = prefs.source,
                    category = catParam,
                    genre = "",
                    year = "",
                    page = page,
                    safeMode = prefs.safeMode
                )
            }.getOrDefault(emptyList())
        }
    }

    /** Endless scrolling for the Home category tabs: loads the next page and adds new titles. */
    fun loadMoreCategory() {
        val st = _homeState.value
        if (st.categoryLoadingMore || st.categoryEnd || st.isCategoryLoading) return
        val tab = st.selectedTab
        if (tab !in listOf("Movies", "Series", "Anime", "Dramas")) return
        _homeState.update { it.copy(categoryLoadingMore = true) }
        viewModelScope.launch {
            val next = st.categoryPage + 1
            val more = fetchCategoryPage(tab, next)
            _homeState.update { cur ->
                if (cur.selectedTab != tab) cur.copy(categoryLoadingMore = false)
                else {
                    val merged = (cur.filteredItems + more).distinctBy { it.title.lowercase().trim() + it.year }
                    cur.copy(
                        filteredItems = merged,
                        categoryPage = next,
                        categoryLoadingMore = false,
                        categoryEnd = merged.size == cur.filteredItems.size
                    )
                }
            }
        }
    }

    private fun looksLikeAnime(item: MediaItem): Boolean {
        val t = item.title.lowercase()
        val junk = listOf("song", "full video", "remix", "jukebox", "lyrics", "audio", "| ", "massage", "desi ", "hot ", "romance video")
        if (junk.any { it in t }) return false
        val g = item.genres.joinToString(" ").lowercase()
        return "anim" in g || item.country.contains("japan", true) || "anime" in t ||
            item.mediaType == MediaCategoryType.ANIME
    }

    fun loadExploreMetadata() {
        viewModelScope.launch {
            val prefs = preferences.value
            val hot = runCatching { apiClient.getHotSearch(prefs.safeMode, prefs.language.code) }.getOrDefault(emptyList())
            val cats = runCatching { apiClient.getCategories(prefs.safeMode) }.getOrDefault(emptyList())
            val filterOpts = runCatching { apiClient.getFilterOptions(prefs.safeMode, prefs.language.code) }.getOrDefault(FilterOptions())
            _exploreState.update {
                it.copy(
                    hotSearches = hot,
                    categories = cats,
                    filterOptions = filterOpts
                )
            }
        }
    }

    /**
     * Category pages by language or country (MovieBox-style): asks the catalog for that country,
     * searches the language name, and keeps only matching titles.
     */
    suspend fun browse(kind: String, value: String, type: String, page: Int): List<MediaItem> = kotlinx.coroutines.supervisorScope {
        val prefs = preferences.value
        val cats = when (type) { "Movies" -> listOf("movies"); "Series" -> listOf("tv"); else -> listOf("movies", "tv") }
        val info = com.example.ui.components.BrowseCatalog.find(kind, value)
        val filterJobs = cats.flatMap { c ->
            listOf(page * 2 - 1, page * 2).map { p ->
                async {
                    runCatching {
                        apiClient.filterCatalog(ContentSource.MOVIEBOX, c, "", "", p, prefs.safeMode, prefs.language.code, info?.country.orEmpty())
                    }.getOrDefault(emptyList())
                }
            }
        }
        val searchJobs = (info?.keywords ?: listOf(value)).map { k ->
            async { runCatching { apiClient.search(k, page, prefs.source, prefs.safeMode, prefs.language.code) }.getOrDefault(emptyList()) }
        }
        // MovieLinkBD has no country filter, so its catalog and its own search are asked separately
        // and the same language/country matching below keeps only the right titles.
        val mlbdJobs = if (prefs.source == ContentSource.MOVIEBOX) emptyList<kotlinx.coroutines.Deferred<List<MediaItem>>>() else {
            cats.map { c ->
                async {
                    runCatching { apiClient.filterCatalog(ContentSource.MOVIELINKBD, c, "", "", page, prefs.safeMode, prefs.language.code) }
                        .getOrDefault(emptyList())
                        .filter { it.source.equals("movielinkbd", ignoreCase = true) }
                }
            } + (info?.keywords ?: listOf(value)).map { k ->
                async { runCatching { apiClient.search(k, page, ContentSource.MOVIELINKBD, prefs.safeMode, prefs.language.code) }.getOrDefault(emptyList()) }
            }
        }
        // Titles already loaded on Home are sorted in automatically too.
        val homeFeed = (_homeState.value.feedState as? UiState.Success)?.data
        val loaded = if (page == 1 && homeFeed != null)
            homeFeed.banners + homeFeed.trendingItems + homeFeed.sections.flatMap { it.items } + _homeState.value.updates24h + _homeState.value.ranking
        else emptyList()
        val pool = loaded + (filterJobs + searchJobs + mlbdJobs).flatMap { it.await() }
        val matched = pool.filter { item -> info?.matches(item) ?: true }
            .filter { item ->
                when (type) {
                    "Movies" -> item.mediaType == MediaCategoryType.MOVIE
                    "Series" -> item.mediaType != MediaCategoryType.MOVIE
                    else -> true
                }
            }
        matched.distinctBy { it.title.lowercase().trim() }
            .sortedByDescending { it.year.toIntOrNull() ?: 0 }
    }

    /** Builds the exact title from a cinedor://item link sent by the admin (notification or popup). */
    fun itemFromLink(link: String): MediaItem? {
        if (!link.startsWith("cinedor://item")) return null
        val uri = runCatching { android.net.Uri.parse(link) }.getOrNull() ?: return null
        val id = uri.getQueryParameter("id").orEmpty()
        val title = uri.getQueryParameter("t").orEmpty()
        if (id.isBlank() || title.isBlank()) return null
        return MediaItem(
            id = id,
            title = title,
            posterUrl = uri.getQueryParameter("p").orEmpty(),
            backdropUrl = uri.getQueryParameter("p").orEmpty(),
            mediaType = MediaCategoryType.fromRaw(uri.getQueryParameter("type")),
            source = uri.getQueryParameter("src").orEmpty().ifBlank { "moviebox" },
            detailPath = uri.getQueryParameter("path").orEmpty()
        )
    }

    /** Finds the title a notification points to, so tapping it opens that movie/series page directly. */
    suspend fun findForPush(query: String): MediaItem? {
        itemFromLink(query.trim())?.let { return it }
        val q = query.trim().let { l ->
            if (l.startsWith("cinedor://")) runCatching { android.net.Uri.parse(l).getQueryParameter("t") }.getOrNull().orEmpty() else l
        }
        if (q.isBlank()) return null
        val prefs = preferences.value
        val clean = q.replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "").trim()
        val results = runCatching { apiClient.search(clean, 1, prefs.source, prefs.safeMode, prefs.language.code) }
            .getOrDefault(emptyList())
        if (results.isEmpty()) return null
        fun norm(t: String) = t.lowercase().replace(Regex("[^a-z0-9]"), "")
        val target = norm(clean)
        return results.firstOrNull { norm(it.title) == target }
            ?: results.firstOrNull { norm(it.title).startsWith(target) || target.startsWith(norm(it.title)) }
            ?: results.first()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _exploreState.update { it.copy(query = newQuery) }
        suggestJob?.cancel()
        if (newQuery.trim().length < 2) {
            _exploreState.update { it.copy(suggestions = emptyList()) }
            if (newQuery.isBlank()) {
                loadFilteredCatalog(reset = true)
            }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(280)
            val prefs = preferences.value
            val suggestions = runCatching {
                apiClient.suggest(newQuery.trim(), prefs.source, prefs.safeMode, prefs.language.code)
            }.getOrDefault(emptyList())
            _exploreState.update { it.copy(suggestions = suggestions) }
        }
    }

    fun executeSearch(explicitQuery: String? = null) {
        val q = (explicitQuery ?: _exploreState.value.query).trim()
        if (q.isBlank()) {
            loadFilteredCatalog(reset = true)
            return
        }
        _exploreState.update {
            it.copy(
                query = q,
                suggestions = emptyList(),
                selectedPlatform = null,
                selectedCategoryChip = null,
                activeCatalogTitle = "Results for \"$q\"",
                itemsState = UiState.Loading,
                page = 1
            )
        }
        viewModelScope.launch {
            val prefs = preferences.value
            try {
                val results = apiClient.search(q, 1, prefs.source, prefs.safeMode, prefs.language.code)
                _exploreState.update {
                    it.copy(
                        itemsState = UiState.Success(results),
                        canLoadMore = results.size >= 10
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.message ?: "Search failed", e.isAuthMissing))
                }
            } catch (e: Exception) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.localizedMessage ?: "Search failed"))
                }
            }
        }
    }

    fun selectPlatform(platformKey: String?, platformLabel: String) {
        _exploreState.update {
            it.copy(
                query = "",
                suggestions = emptyList(),
                selectedPlatform = platformKey,
                selectedCategoryChip = null,
                activeCatalogTitle = if (platformKey != null) "$platformLabel Catalog" else "Discover Catalog",
                page = 1
            )
        }
        if (platformKey == null) {
            loadFilteredCatalog(reset = true)
            return
        }
        viewModelScope.launch {
            val prefs = preferences.value
            _exploreState.update { it.copy(itemsState = UiState.Loading) }
            try {
                val items = apiClient.getPlatformCatalog(platformKey, 1, prefs.safeMode, prefs.language.code)
                _exploreState.update {
                    it.copy(itemsState = UiState.Success(items), canLoadMore = items.size >= 10)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.message ?: "Failed to load platform", e.isAuthMissing))
                }
            } catch (e: Exception) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.localizedMessage ?: "Failed to load platform"))
                }
            }
        }
    }

    fun selectExploreCategoryChip(chip: CategoryChip) {
        val togglingOff = _exploreState.value.selectedCategoryChip?.id == chip.id
        if (togglingOff) {
            _exploreState.update {
                it.copy(selectedCategoryChip = null, activeCatalogTitle = "Discover Catalog")
            }
            loadFilteredCatalog(reset = true)
            return
        }
        _exploreState.update {
            it.copy(
                query = "",
                suggestions = emptyList(),
                selectedPlatform = null,
                selectedCategoryChip = chip,
                activeCatalogTitle = chip.name,
                itemsState = UiState.Loading,
                page = 1
            )
        }
        viewModelScope.launch {
            val prefs = preferences.value
            try {
                val items = apiClient.getCatalog(chip.path, 1, prefs.source, prefs.safeMode)
                _exploreState.update {
                    it.copy(itemsState = UiState.Success(items), canLoadMore = items.size >= 10)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.message ?: "Failed to load catalog", e.isAuthMissing))
                }
            } catch (e: Exception) {
                _exploreState.update {
                    it.copy(itemsState = UiState.Error(e.localizedMessage ?: "Failed to load catalog"))
                }
            }
        }
    }

    fun updateFilter(category: String? = null, genre: String? = null, year: String? = null) {
        _exploreState.update {
            it.copy(
                query = "",
                selectedPlatform = null,
                selectedCategoryChip = null,
                selectedCategory = category ?: it.selectedCategory,
                selectedGenre = genre ?: it.selectedGenre,
                selectedYear = year ?: it.selectedYear,
                activeCatalogTitle = "Discover Catalog"
            )
        }
        loadFilteredCatalog(reset = true)
    }

    fun loadFilteredCatalog(reset: Boolean) {
        val state = _exploreState.value
        val nextPage = if (reset) 1 else state.page + 1
        if (!reset && (state.isLoadingMore || !state.canLoadMore)) return

        viewModelScope.launch {
            val prefs = preferences.value
            if (reset) {
                _exploreState.update { it.copy(itemsState = UiState.Loading, page = 1) }
            } else {
                _exploreState.update { it.copy(isLoadingMore = true) }
            }

            try {
                val fetched = when {
                    state.query.isNotBlank() -> apiClient.search(state.query, nextPage, prefs.source, prefs.safeMode, prefs.language.code)
                    state.selectedPlatform != null -> apiClient.getPlatformCatalog(state.selectedPlatform, nextPage, prefs.safeMode, prefs.language.code)
                    state.selectedCategoryChip != null -> apiClient.getCatalog(state.selectedCategoryChip.path, nextPage, prefs.source, prefs.safeMode)
                    else -> apiClient.filterCatalog(
                        source = prefs.source,
                        category = state.selectedCategory,
                        genre = state.selectedGenre,
                        year = state.selectedYear,
                        page = nextPage,
                        safeMode = prefs.safeMode,
                        lang = prefs.language.code
                    )
                }
                val currentList = if (reset) emptyList() else (state.itemsState as? UiState.Success)?.data.orEmpty()
                val merged = (currentList + fetched).distinctBy { "${it.source}:${it.id}:${it.title}" }
                _exploreState.update {
                    it.copy(
                        itemsState = UiState.Success(merged),
                        page = nextPage,
                        canLoadMore = fetched.size >= 8,
                        isLoadingMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _exploreState.update {
                    it.copy(
                        itemsState = if (reset) UiState.Error(e.message ?: "Filter failed", e.isAuthMissing) else it.itemsState,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                _exploreState.update {
                    it.copy(
                        itemsState = if (reset) UiState.Error(e.localizedMessage ?: "Failed to load catalog") else it.itemsState,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    fun ensureReelsLoaded() {
        val current = _reelsState.value.reelsState
        if (current is UiState.Success && current.data.isNotEmpty()) return
        loadReels(reset = true)
    }

    fun loadReels(reset: Boolean) {
        val state = _reelsState.value
        val nextPage = if (reset) 1 else state.page + 1
        if (!reset && state.isLoadingMore) return

        viewModelScope.launch {
            if (reset) {
                _reelsState.update { it.copy(reelsState = UiState.Loading, page = 1) }
            } else {
                _reelsState.update { it.copy(isLoadingMore = true) }
            }
            try {
                val items = apiClient.getReels(nextPage, preferences.value.safeMode, preferences.value.language.code)
                val existing = if (reset) emptyList() else (state.reelsState as? UiState.Success)?.data.orEmpty()
                _reelsState.update {
                    it.copy(
                        reelsState = UiState.Success((existing + items).distinctBy { r -> r.videoUrl }),
                        page = nextPage,
                        isLoadingMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _reelsState.update {
                    it.copy(
                        reelsState = if (reset) UiState.Error(e.message ?: "Reels error", e.isAuthMissing) else it.reelsState,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                _reelsState.update {
                    it.copy(
                        reelsState = if (reset) UiState.Error(e.localizedMessage ?: "Failed to load reels") else it.reelsState,
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    fun openSportsMatch(match: SportsMatch) {
        val status = match.statusText.lowercase()
        val ended = listOf("ended", "finished", "full time", "fulltime", "final", "ft").any { status.contains(it) } &&
            !match.isLive
        if (ended) {
            _toastMessage.value = "This match has ended — the live stream is no longer available."
            return
        }
        if (match.streamUrl.startsWith("http")) {
            openMediaDetail(
                item = MediaItem(
                    id = match.id,
                    title = "${match.homeTeam} vs ${match.awayTeam}",
                    backdropUrl = match.homeLogo.ifBlank { match.awayLogo },
                    posterUrl = match.homeLogo.ifBlank { match.awayLogo },
                    mediaType = com.example.data.model.MediaCategoryType.MOVIE,
                    source = "sports",
                    description = "${match.league} • ${match.statusText}"
                ),
                directStreamUrl = match.streamUrl
            )
        } else if (match.isLive) {
            _toastMessage.value = "No live stream link for this match yet. Try again in a moment."
        } else {
            toggleReminder(
                MediaItem(
                    id = "sport_${match.id}",
                    title = "${match.homeTeam} vs ${match.awayTeam}",
                    posterUrl = match.homeLogo.ifBlank { match.awayLogo },
                    releaseDate = match.league
                )
            )
        }
    }

    fun openMediaDetail(
        item: MediaItem,
        initialSeason: Int = 1,
        initialEpisode: Int = 1,
        resumeMs: Long = 0L,
        directStreamUrl: String = ""
    ) {
        detailJob?.cancel()
        streamJob?.cancel()
        lastSavedProgressMs = -1L

        val historyMatch = (historyState.value as? UiState.Success)?.data?.firstOrNull {
            it.id == sanitizeLocalDocId(item.id, item.title) || it.contentId == item.id
        }
        val initialIsMovie = item.mediaType == com.example.data.model.MediaCategoryType.MOVIE
        val startSeason = if (initialSeason > 1) initialSeason else (historyMatch?.season?.coerceAtLeast(1) ?: 1)
        val startEpisode = if (initialEpisode > 1) initialEpisode else (historyMatch?.episode?.coerceAtLeast(1) ?: 1)
        // Resume where the viewer stopped, unless they had basically finished it.
        val savedPos = historyMatch?.let { h ->
            if (h.durationMs > 0L && h.positionMs > h.durationMs - 60_000L) 0L else h.positionMs
        } ?: 0L
        val startPos = if (resumeMs > 0L) resumeMs else savedPos

        if (directStreamUrl.startsWith("http")) {
            val isHls = !(directStreamUrl.contains(".mp4", ignoreCase = true) &&
                !directStreamUrl.contains(".m3u8", ignoreCase = true))
            if (item.source == "sports") {
                // Live CDNs are picky about Referer/Origin: try several header sets automatically.
                val variants = listOf(
                    "Live • Server 1" to mapOf(
                        "User-Agent" to MovieBoxDirectClient.CHROME_USER_AGENT
                    ),
                    "Live • Server 2" to mapOf(
                        "User-Agent" to MovieBoxDirectClient.CHROME_USER_AGENT,
                        "Referer" to "https://h5-sport.aoneroom.com/",
                        "Origin" to "https://h5-sport.aoneroom.com"
                    ),
                    "Live • Server 3" to mapOf(
                        "User-Agent" to MovieBoxDirectClient.CHROME_USER_AGENT,
                        "Referer" to MovieBoxDirectClient.DEFAULT_REFERER
                    )
                )
                val options = variants.mapIndexed { i, (label, headers) ->
                    StreamOption(
                        id = "live_${item.id}_$i",
                        label = label,
                        quality = "Auto",
                        // Same link; the fragment only keeps the servers apart for automatic fallback.
                        url = if (i == 0) directStreamUrl else "$directStreamUrl#cinedor-s${i + 1}",
                        format = if (isHls) "HLS" else "MP4",
                        headers = headers
                    )
                }
                _detailState.value = DetailUiState(
                    selectedItem = item,
                    detailState = UiState.Success(MediaDetail(item = item, synopsis = item.description)),
                    isMovie = true,
                    streams = options,
                    selectedStream = options.first(),
                    isStreamLoading = false,
                    resumePositionMs = 0L
                )
                return
            }
            val directOption = StreamOption(
                id = "direct_${item.id}",
                label = "Live Stream • HD",
                quality = "HD",
                url = directStreamUrl,
                // Live links often have no ".m3u8" in the URL; treat everything that isn't an .mp4 as HLS.
                format = if (directStreamUrl.contains(".mp4", ignoreCase = true) &&
                    !directStreamUrl.contains(".m3u8", ignoreCase = true)
                ) "MP4" else "HLS",
                headers = mapOf(
                    "User-Agent" to MovieBoxDirectClient.CHROME_USER_AGENT,
                    "Referer" to MovieBoxDirectClient.DEFAULT_REFERER
                )
            )
            _detailState.value = DetailUiState(
                selectedItem = item,
                detailState = UiState.Success(MediaDetail(item = item, synopsis = item.description)),
                isMovie = true,
                streams = listOf(directOption),
                selectedStream = directOption,
                isStreamLoading = false,
                resumePositionMs = startPos
            )
            return
        }

        _detailState.value = DetailUiState(
            selectedItem = item,
            detailState = UiState.Loading,
            isMovie = initialIsMovie,
            selectedSeason = startSeason,
            selectedEpisode = startEpisode,
            isStreamLoading = true,
            resumePositionMs = startPos
        )

        val speculativeSe = if (initialIsMovie) 0 else startSeason
        val speculativeEp = if (initialIsMovie) 0 else startEpisode
        // Start fetching playable stream IMMEDIATELY in parallel with details for instant start
        val speculativeStarted = item.id.isNotBlank() && item.id.all(Char::isDigit)
        if (speculativeStarted) {
            loadStreamForEpisode(
                item = item,
                season = speculativeSe,
                episode = speculativeEp,
                isMovie = initialIsMovie
            )
        }

        detailJob = viewModelScope.launch {
            val lang = preferences.value.language.code
            try {
                val detail = apiClient.getDetails(item, preferences.value.safeMode, lang)
                val resolvedIsMovie = detail.seasons.isEmpty() &&
                    detail.item.mediaType == com.example.data.model.MediaCategoryType.MOVIE
                val firstSeasonNum = detail.seasons.firstOrNull()?.seasonNumber ?: 1
                val firstEpNum = detail.seasons.firstOrNull()?.episodes?.firstOrNull()?.episode ?: 1
                val effectiveSeason = if (resolvedIsMovie) 0 else if (detail.seasons.any { it.seasonNumber == startSeason }) startSeason else firstSeasonNum
                val effectiveEpisode = if (resolvedIsMovie) 0 else if (startEpisode > 1) startEpisode else firstEpNum

                _detailState.update {
                    it.copy(
                        selectedItem = detail.item,
                        detailState = UiState.Success(detail),
                        isMovie = resolvedIsMovie,
                        selectedSeason = effectiveSeason.coerceAtLeast(1),
                        selectedEpisode = effectiveEpisode.coerceAtLeast(1)
                    )
                }

                val currentStreams = _detailState.value.streams
                val paramsMismatch = speculativeStarted && (
                    resolvedIsMovie != initialIsMovie ||
                        effectiveSeason != speculativeSe ||
                        effectiveEpisode != speculativeEp ||
                        (currentStreams.isEmpty() && !_detailState.value.isStreamLoading && item.detailPath.isBlank() && detail.item.detailPath.isNotBlank())
                    )

                if (paramsMismatch) {
                    loadStreamForEpisode(
                        item = detail.item,
                        season = effectiveSeason,
                        episode = effectiveEpisode,
                        isMovie = resolvedIsMovie,
                        fallbackStreams = detail.streams
                    )
                } else if (detail.streams.isNotEmpty() && currentStreams.isEmpty() && !_detailState.value.isStreamLoading) {
                    _detailState.update {
                        it.copy(
                            streams = detail.streams,
                            selectedStream = detail.streams.first(),
                            captions = detail.captions,
                            selectedCaption = null,
                            isStreamLoading = false,
                            streamError = null
                        )
                    }
                } else if (!speculativeStarted || (currentStreams.isEmpty() && !_detailState.value.isStreamLoading)) {
                    loadStreamForEpisode(
                        item = detail.item,
                        season = effectiveSeason,
                        episode = effectiveEpisode,
                        isMovie = resolvedIsMovie,
                        fallbackStreams = detail.streams
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CinePulseApiException) {
                _detailState.update {
                    it.copy(
                        detailState = UiState.Error(e.message ?: "Failed to load details", e.isAuthMissing),
                        isStreamLoading = false
                    )
                }
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        detailState = UiState.Error(e.localizedMessage ?: "Failed to load details"),
                        isStreamLoading = false
                    )
                }
            }
        }
    }

    fun selectEpisode(season: Int, episode: Int) {
        val item = _detailState.value.selectedItem ?: return
        _detailState.update {
            it.copy(
                isMovie = false,
                selectedSeason = season,
                selectedEpisode = episode,
                resumePositionMs = 0L
            )
        }
        loadStreamForEpisode(item, season, episode, isMovie = false)
    }

    fun loadStreamForEpisode(
        item: MediaItem,
        season: Int,
        episode: Int,
        isMovie: Boolean = _detailState.value.isMovie,
        fallbackStreams: List<StreamOption> = emptyList(),
        forceRefresh: Boolean = false
    ) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _detailState.update { it.copy(isStreamLoading = true, streamError = null) }
            try {
                val lang = preferences.value.language.code
                val (fetchedStreams, captions) = apiClient.getStreams(
                    item = item,
                    isMovie = isMovie,
                    season = season,
                    episode = episode,
                    lang = lang,
                    forceRefresh = forceRefresh
                )
                val detailFallback = fallbackStreams.ifEmpty {
                    (_detailState.value.detailState as? UiState.Success)?.data?.streams.orEmpty()
                }
                val resolvedStreams = fetchedStreams.ifEmpty { detailFallback }
                _detailState.update {
                    it.copy(
                        streams = resolvedStreams,
                        selectedStream = resolvedStreams.firstOrNull(),
                        failedStreamUrls = emptySet(),
                        captions = captions,
                        selectedCaption = null,
                        isStreamLoading = false,
                        streamError = if (resolvedStreams.isEmpty()) {
                            if (isMovie) {
                                "No playable stream available yet for this movie."
                            } else {
                                "No playable stream found for S${season} E${episode}. Try another episode."
                            }
                        } else {
                            null
                        }
                    )
                }

                // Load subtitles asynchronously in background so video playback starts with ZERO delay!
                if (captions.isEmpty() && resolvedStreams.isNotEmpty() && item.id.isNotBlank()) {
                    val firstStreamId = resolvedStreams.firstOrNull { !it.id.startsWith("stream_") }?.id.orEmpty()
                    if (firstStreamId.isNotBlank()) {
                        launch {
                            val bgCaptions = runCatching {
                                apiClient.getCaptions(item.id, firstStreamId, item.detailPath)
                            }.getOrDefault(emptyList())
                            if (bgCaptions.isNotEmpty()) {
                                _detailState.update { cur ->
                                    if (cur.selectedItem?.id == item.id && cur.captions.isEmpty()) {
                                        cur.copy(captions = bgCaptions)
                                    } else {
                                        cur
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isStreamLoading = false,
                        streamError = e.localizedMessage ?: "Failed to fetch stream URL"
                    )
                }
            }
        }
    }

    fun selectStreamOption(option: StreamOption) {
        _detailState.update {
            it.copy(
                selectedStream = option,
                failedStreamUrls = it.failedStreamUrls - option.url
            )
        }
    }

    fun refreshCurrentStream() {
        val current = _detailState.value
        val item = current.selectedItem ?: return
        apiClient.rotateDirectHost()
        loadStreamForEpisode(
            item = item,
            season = current.selectedSeason,
            episode = current.selectedEpisode,
            isMovie = current.isMovie,
            forceRefresh = true
        )
    }

    /**
     * Host-aware stream failover:
     * - On HTTP 429 (CDN rate-limit), marks ALL streams on that exact throttled host as skipped
     *   and ONLY auto-switches to a stream on a DIFFERENT host (e.g., Worker proxy or MLBD mirror),
     *   preventing rapid 1080p -> 720p -> 480p -> 360p retry storms on the same 429 host.
     * - On other errors, prefers a different host first, then falls back to the next untried stream.
     */
    fun fallbackToNextStream(failedStream: StreamOption, httpCode: Int? = null): Boolean {
        val current = _detailState.value
        val failedHost = runCatching { Uri.parse(failedStream.url).host?.lowercase().orEmpty() }.getOrDefault("")
        val updatedFailed = current.failedStreamUrls + failedStream.url
        val remaining = current.streams.filter { it.url !in updatedFailed }

        // Always prefer a genuinely different CDN host first, then try remaining stream URLs
        val next = remaining.firstOrNull { candidate ->
            val candidateHost = runCatching { Uri.parse(candidate.url).host?.lowercase().orEmpty() }.getOrDefault("")
            candidateHost.isNotBlank() && candidateHost != failedHost
        } ?: remaining.firstOrNull()

        return if (next != null) {
            _detailState.update {
                it.copy(
                    failedStreamUrls = updatedFailed,
                    selectedStream = next
                )
            }
            _toastMessage.value = "Switching server to ${next.label}…"
            true
        } else {
            _detailState.update { it.copy(failedStreamUrls = updatedFailed) }
            false
        }
    }

    fun selectCaptionTrack(track: CaptionTrack?) {
        _detailState.update { it.copy(selectedCaption = track) }
    }

    fun openStaff(member: CastMember) {
        staffJob?.cancel()
        _staffState.value = UiState.Loading
        staffJob = viewModelScope.launch {
            try {
                val detail = apiClient.getStaffDetail(member, preferences.value.safeMode)
                _staffState.value = UiState.Success(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _staffState.value = UiState.Error(e.localizedMessage ?: "Failed to load cast profile")
            }
        }
    }

    fun closeStaff() {
        staffJob?.cancel()
        _staffState.value = null
    }

    fun closeDetail() {
        detailJob?.cancel()
        streamJob?.cancel()
        staffJob?.cancel()
        _staffState.value = null
        _detailState.update { it.copy(selectedItem = null) }
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        val state = _detailState.value
        val item = state.selectedItem ?: return
        if (durationMs <= 0L || positionMs < 3000L) return
        if (lastSavedProgressMs >= 0L && kotlin.math.abs(positionMs - lastSavedProgressMs) < 3_000L) return
        lastSavedProgressMs = positionMs
        viewModelScope.launch {
            libraryRepository.saveWatchProgress(
                item = item,
                season = state.selectedSeason,
                episode = state.selectedEpisode,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun toggleWatchlist(item: MediaItem) {
        val docId = sanitizeLocalDocId(item.id, item.title)
        val currentlyInList = (watchlistState.value as? UiState.Success)?.data
            ?.any { it.id == docId || it.contentId == item.id } == true
        viewModelScope.launch {
            val result = libraryRepository.toggleWatchlist(item, currentlyInList)
            result.onSuccess { added ->
                _toastMessage.value = if (added) "Added \"${item.title}\" to My List" else "Removed from My List"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Could not update watchlist"
            }
        }
    }

    fun removeHistoryItem(docId: String) {
        viewModelScope.launch {
            libraryRepository.removeHistoryItem(docId).onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Could not remove history item"
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            libraryRepository.clearAllHistory().onSuccess {
                _toastMessage.value = "Watch history cleared"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Could not clear history"
            }
        }
    }

    fun toggleReminder(item: MediaItem) {
        val docId = sanitizeLocalDocId(item.id, item.title)
        val booked = (remindersState.value as? UiState.Success)?.data
            ?.any { it.id == docId || it.contentId == item.id } == true
        viewModelScope.launch {
            libraryRepository.toggleReminder(item, booked).onSuccess { nowBooked ->
                _toastMessage.value = if (nowBooked) "Reminder set for ${item.title}" else "Reminder removed"
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Could not update reminder"
            }
        }
    }

    fun checkApiStatus() {
        viewModelScope.launch {
            try {
                _apiStatus.value = apiClient.getStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _apiStatus.value = ApiStatusInfo(
                    isOnline = false,
                    statusText = e.localizedMessage ?: "Offline / Key Missing"
                )
            }
        }
    }

    private companion object {
        const val TAG = "CinePulseVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
