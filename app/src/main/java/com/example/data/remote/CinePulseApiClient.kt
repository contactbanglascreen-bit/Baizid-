package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ApiStatusInfo
import com.example.data.model.CaptionTrack
import com.example.data.model.CategoryChip
import com.example.data.model.CommentItem
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentSource
import com.example.data.model.FilterOptions
import com.example.data.model.HomeFeed
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.CastMember
import com.example.data.model.ReelItem
import com.example.data.model.SportsMatch
import com.example.data.model.StaffDetail
import com.example.data.model.StreamOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CinePulseApiException(
    message: String,
    val isAuthMissing: Boolean = false,
    val httpCode: Int = 0
) : IOException(message)

private data class TimedCacheEntry<T>(
    val value: T,
    val expiresAtMs: Long
)

/**
 * Cinedor Hybrid Turbo Engine:
 * - Parallel async fetching across CinePulse Unified Edge Worker + Direct MovieBox 4-Host Failover
 * - 3-minute in-memory TTL cache for instant tab switches and revisits
 * - Warm HTTP/2 connection pooling for minimal latency
 */
class CinePulseApiClient(
    private val defaultApiKey: String = BuildConfig.CINEPULSE_API_KEY,
    private val movieBoxDirect: MovieBoxDirectClient = MovieBoxDirectClient()
) {
    companion object {
        private const val TAG = "CinedorHybrid"
        const val BASE_URL = "https://inepulse-edge.siamvai9474737.workers.dev"
        private const val PLACEHOLDER_KEY = "YOUR_CINEPULSE_API_KEY"
        private const val CACHE_TTL_MS = 180_000L
    }

    @Volatile
    private var runtimeApiKey: String = ""

    private val activeApiKey: String
        get() = runtimeApiKey.ifBlank { defaultApiKey }

    val isApiKeyConfigured: Boolean
        get() {
            val k = activeApiKey
            return k.isNotBlank() &&
                k != PLACEHOLDER_KEY &&
                !k.contains("paste_full_key", ignoreCase = true) &&
                !k.contains("...")
        }

    fun updateRuntimeApiKey(newKey: String) {
        val cleaned = newKey.trim()
        if (cleaned != runtimeApiKey) {
            runtimeApiKey = cleaned
            homeCache.clear()
            detailCache.clear()
            streamCache.clear()
        }
    }

    fun rotateDirectHost() {
        movieBoxDirect.rotatePreferredHost()
    }

    private val homeCache = ConcurrentHashMap<String, TimedCacheEntry<HomeFeed>>()
    private val detailCache = ConcurrentHashMap<String, TimedCacheEntry<MediaDetail>>()
    private val streamCache = ConcurrentHashMap<String, TimedCacheEntry<Pair<List<StreamOption>, List<CaptionTrack>>>>()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(9, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("x-api-key", activeApiKey)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private fun <T> ConcurrentHashMap<String, TimedCacheEntry<T>>.getFresh(key: String): T? {
        val entry = this[key] ?: return null
        return if (System.currentTimeMillis() < entry.expiresAtMs) entry.value else {
            remove(key)
            null
        }
    }

    private fun extractServerErrorMessage(body: String): String {
        val obj = runCatching { JSONObject(body) }.getOrNull() ?: return ""
        return listOf("error", "message", "msg", "detail")
            .firstNotNullOfOrNull { key ->
                obj.optString(key).takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            }
            .orEmpty()
    }

    private suspend fun getJson(path: String, queryParams: Map<String, String> = emptyMap()): Pair<String, Long> =
        withContext(Dispatchers.IO) {
            if (!isApiKeyConfigured) {
                throw CinePulseApiException(
                    message = "API key not configured. Add CINEPULSE_API_KEY in AI Studio Secrets.",
                    isAuthMissing = true,
                    httpCode = 401
                )
            }

            val urlBuilder = (BASE_URL + path).toHttpUrl().newBuilder()
            queryParams.forEach { (k, v) ->
                if (v.isNotBlank()) urlBuilder.addQueryParameter(k, v)
            }
            val request = Request.Builder().url(urlBuilder.build()).get().build()
            val start = System.currentTimeMillis()
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val authError = response.code == 401 || response.code == 403
                    val serverDetail = extractServerErrorMessage(body)
                    throw CinePulseApiException(
                        message = when {
                            authError -> "Authentication failed (HTTP ${response.code}). Verify CINEPULSE_API_KEY in AI Studio Secrets."
                            serverDetail.isNotBlank() -> "$serverDetail (HTTP ${response.code})"
                            else -> "Cinedor API returned HTTP ${response.code}"
                        },
                        isAuthMissing = authError,
                        httpCode = response.code
                    )
                }
                body to latency
            }
        }

    suspend fun getHome(
        source: ContentSource,
        language: ContentLanguage,
        safeMode: Boolean,
        forceRefresh: Boolean = false
    ): HomeFeed = withContext(Dispatchers.Default) {
        supervisorScope {
        val cacheKey = "${source.apiValue}:${language.code}:$safeMode"
        if (!forceRefresh) {
            homeCache.getFresh(cacheKey)?.let { return@supervisorScope it }
        }

        val feed: HomeFeed = if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
            movieBoxDirect.getHome(language.code, safeMode)
        } else {
            // Query Worker + Direct MovieBox in parallel inside supervisorScope so HTTP 502 never cancels failover!
            val workerDeferred = async {
                runCatching {
                    val (homeBody, _) = getJson(
                        "/api/v1/home",
                        mapOf("source" to source.apiValue, "lang" to language.code)
                    )
                    CinePulseJsonParser.parseHomeFeed(homeBody, safeMode)
                }.onFailure { err ->
                    Log.w(TAG, "Worker getHome failed (${err.message}); using Direct MovieBox failover")
                }.getOrNull()
            }
            val directDeferred = async {
                runCatching {
                    movieBoxDirect.getHome(language.code, safeMode)
                }.onFailure { err ->
                    Log.w(TAG, "Direct MovieBox getHome failed: ${err.message}")
                }.getOrNull()
            }
            val sportsDeferred = async { runCatching { getSportsMatches() }.getOrDefault(emptyList()) }
            val upcomingDeferred = async { runCatching { getUpcoming(source, safeMode, language.code) }.getOrDefault(emptyList()) }
            val trendingDeferred = async { runCatching { getTrending(source, safeMode, language.code) }.getOrDefault(emptyList()) }

            val workerFeed = withTimeoutOrNull(4500L) { workerDeferred.await() }
            val directFeed = withTimeoutOrNull(4500L) { directDeferred.await() }

            val primary = when {
                workerFeed != null && (workerFeed.sections.isNotEmpty() || workerFeed.banners.isNotEmpty()) -> workerFeed
                directFeed != null && (directFeed.sections.isNotEmpty() || directFeed.banners.isNotEmpty()) -> directFeed
                else -> null
            }

            if (primary != null) {
                // Keep MovieLinkBD cleanly separated from MovieBox when user explicitly selects MovieLinkBD
                val mergeDirect = source != ContentSource.MOVIELINKBD && primary === workerFeed
                val mergedBanners = if (mergeDirect) {
                    (primary.banners + directFeed?.banners.orEmpty())
                        .distinctBy { it.id }
                        .take(10)
                } else {
                    primary.banners.distinctBy { it.id }.take(10)
                }
                val mergedSections = if (mergeDirect) {
                    (primary.sections + directFeed?.sections.orEmpty())
                        .distinctBy { it.title.lowercase() }
                } else {
                    primary.sections
                }
                primary.copy(
                    banners = mergedBanners,
                    sections = mergedSections,
                    sportsMatches = primary.sportsMatches
                        .ifEmpty { if (source != ContentSource.MOVIELINKBD) directFeed?.sportsMatches.orEmpty() else emptyList() }
                        .ifEmpty { withTimeoutOrNull(1500L) { sportsDeferred.await() }.orEmpty() },
                    upcomingItems = primary.upcomingItems
                        .ifEmpty { if (source != ContentSource.MOVIELINKBD) directFeed?.upcomingItems.orEmpty() else emptyList() }
                        .ifEmpty { withTimeoutOrNull(1500L) { upcomingDeferred.await() }.orEmpty() },
                    trendingItems = primary.trendingItems
                        .ifEmpty { if (source != ContentSource.MOVIELINKBD) directFeed?.trendingItems.orEmpty() else emptyList() }
                        .ifEmpty { withTimeoutOrNull(1500L) { trendingDeferred.await() }.orEmpty() }
                )
            } else {
                // Final emergency fallback: build feed directly from trending list
                val fallbackTrending = trendingDeferred.await()
                if (fallbackTrending.isNotEmpty()) {
                    HomeFeed(
                        banners = fallbackTrending.take(6),
                        trendingItems = fallbackTrending,
                        sportsMatches = sportsDeferred.await(),
                        upcomingItems = upcomingDeferred.await()
                    )
                } else {
                    throw CinePulseApiException(
                        message = "Unable to reach Cinedor servers right now. Please tap Retry.",
                        httpCode = 502
                    )
                }
            }
        }

        if (feed.banners.isNotEmpty() || feed.sections.isNotEmpty() || feed.trendingItems.isNotEmpty()) {
            homeCache[cacheKey] = TimedCacheEntry(feed, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        feed
        }
    }

    suspend fun getTrending(source: ContentSource, safeMode: Boolean, lang: String = "en"): List<MediaItem> {
        if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
            return movieBoxDirect.getTrending(1, lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/trending", mapOf("source" to source.apiValue))
            CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
        }.getOrElse {
            movieBoxDirect.getTrending(1, lang, safeMode)
        }
    }

    suspend fun getRanking(source: ContentSource, safeMode: Boolean, lang: String = "en"): List<MediaItem> {
        if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
            return movieBoxDirect.getRanking(1, lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/ranking", mapOf("list" to "top", "source" to source.apiValue))
            CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
        }.getOrElse {
            movieBoxDirect.getRanking(1, lang, safeMode)
        }
    }

    suspend fun getUpdates24h(source: ContentSource, safeMode: Boolean): List<MediaItem> {
        if (!isApiKeyConfigured) return emptyList()
        val (body, _) = getJson("/api/v1/updates-24h", mapOf("source" to source.apiValue))
        return CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
    }

    suspend fun getUpcoming(source: ContentSource, safeMode: Boolean, lang: String = "en"): List<MediaItem> {
        if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
            return movieBoxDirect.getUpcoming(1, lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/upcoming", mapOf("source" to source.apiValue))
            CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
        }.getOrElse {
            movieBoxDirect.getUpcoming(1, lang, safeMode)
        }
    }

    suspend fun getSportsMatches(): List<SportsMatch> {
        if (isApiKeyConfigured) {
            val workerSports = runCatching {
                val (body, _) = getJson("/api/v1/sports/matches")
                CinePulseJsonParser.parseSportsMatches(body)
            }.getOrDefault(emptyList())
            if (workerSports.isNotEmpty()) return workerSports
        }
        return runCatching { movieBoxDirect.getSportsMatches() }.getOrDefault(emptyList())
    }

    suspend fun getReels(page: Int, safeMode: Boolean, lang: String = "en"): List<ReelItem> {
        if (isApiKeyConfigured) {
            val workerReels = runCatching {
                val (body, _) = getJson("/api/v1/reels", mapOf("type" to "all", "page" to page.toString()))
                CinePulseJsonParser.parseReels(body, safeMode, BASE_URL)
            }.getOrDefault(emptyList())
            if (workerReels.isNotEmpty()) return workerReels
        }
        return movieBoxDirect.getReels(page, lang, safeMode)
    }

    suspend fun getCategories(safeMode: Boolean): List<CategoryChip> {
        if (!isApiKeyConfigured) return emptyList()
        val (body, _) = getJson("/api/v1/categories")
        return CinePulseJsonParser.parseCategories(body, safeMode)
    }

    suspend fun getCatalog(category: String, page: Int, source: ContentSource, safeMode: Boolean): List<MediaItem> {
        val (body, _) = getJson(
            "/api/v1/catalog",
            mapOf("category" to category, "page" to page.toString(), "source" to source.apiValue)
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
    }

    suspend fun getPlatformCatalog(platform: String, page: Int, safeMode: Boolean, lang: String = "en"): List<MediaItem> {
        if (!isApiKeyConfigured) {
            return movieBoxDirect.getPlatformCatalog(platform, page, lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson(
                "/api/v1/platform",
                mapOf("platform" to platform, "page" to page.toString())
            )
            val parsed = CinePulseJsonParser.parseMediaList(body, safeMode)
            parsed.ifEmpty { movieBoxDirect.getPlatformCatalog(platform, page, lang, safeMode) }
        }.getOrElse {
            movieBoxDirect.getPlatformCatalog(platform, page, lang, safeMode)
        }
    }

    suspend fun filterCatalog(
        source: ContentSource,
        category: String,
        genre: String,
        year: String,
        page: Int,
        safeMode: Boolean,
        lang: String = "en",
        country: String = ""
    ): List<MediaItem> {
        if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured || country.isNotBlank()) {
            return movieBoxDirect.filter(category, genre, year, page, lang, safeMode, country)
        }
        return runCatching {
            val params = buildMap {
                put("source", source.apiValue)
                put("page", page.toString())
                if (category.isNotBlank()) put("cat", category)
                if (genre.isNotBlank() && !genre.equals("All", ignoreCase = true)) put("genre", genre.lowercase())
                if (year.isNotBlank() && !year.equals("All", ignoreCase = true)) put("year", year)
            }
            val (body, _) = getJson("/api/v1/filter", params)
            val list = CinePulseJsonParser.parseMediaList(body, safeMode, source.apiValue)
            list.ifEmpty { movieBoxDirect.filter(category, genre, year, page, lang, safeMode) }
        }.getOrElse {
            movieBoxDirect.filter(category, genre, year, page, lang, safeMode)
        }
    }

    suspend fun getFilterOptions(safeMode: Boolean, lang: String = "en"): FilterOptions {
        if (!isApiKeyConfigured) {
            return movieBoxDirect.getFilterOptions(lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/filter-options")
            CinePulseJsonParser.parseFilterOptions(body, safeMode)
        }.getOrElse {
            movieBoxDirect.getFilterOptions(lang, safeMode)
        }
    }

    suspend fun search(query: String, page: Int, source: ContentSource, safeMode: Boolean, lang: String = "en"): List<MediaItem> =
        supervisorScope {
            val cleanQuery = query.trim()
            if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
                return@supervisorScope movieBoxDirect.search(cleanQuery, page, lang, safeMode)
            }
            if (source == ContentSource.MOVIELINKBD) {
                val (body, _) = getJson(
                    "/api/v1/search",
                    mapOf("q" to cleanQuery, "page" to page.toString(), "source" to "movielinkbd")
                )
                return@supervisorScope CinePulseJsonParser.parseMediaList(body, safeMode, "movielinkbd")
            }

            // Dual-Site Parallel Search (ALL): query BOTH MovieBox Direct + CinePulse Unified Worker (MovieBox + MovieLinkBD)
            val directDeferred = async {
                runCatching { movieBoxDirect.search(cleanQuery, page, lang, safeMode) }.getOrNull()
            }
            val workerDeferred = async {
                runCatching {
                    val (body, _) = getJson(
                        "/api/v1/search",
                        mapOf("q" to cleanQuery, "page" to page.toString(), "source" to "all")
                    )
                    CinePulseJsonParser.parseMediaList(body, safeMode, "all")
                }.getOrNull()
            }

            val directList = withTimeoutOrNull(4500L) { directDeferred.await() }.orEmpty()
            val workerList = if (directList.isNotEmpty()) {
                withTimeoutOrNull(2600L) { workerDeferred.await() }.orEmpty()
            } else {
                workerDeferred.await().orEmpty()
            }

            if (directList.isEmpty() && workerList.isEmpty()) {
                // Retry once without swallowing the error if both failed
                return@supervisorScope movieBoxDirect.search(cleanQuery, page, lang, safeMode)
            }

            // Interleave MovieLinkBD + MovieBox results cleanly so BOTH sites are immediately visible!
            val mlbdItems = workerList.filter { it.source.equals("movielinkbd", ignoreCase = true) }
            val workerMbItems = workerList.filterNot { it.source.equals("movielinkbd", ignoreCase = true) }
            val combinedMb = (directList + workerMbItems).distinctBy { it.id.ifBlank { it.title.lowercase() } }

            buildList {
                val maxLen = maxOf(combinedMb.size, mlbdItems.size)
                for (i in 0 until maxLen) {
                    if (i < combinedMb.size) add(combinedMb[i])
                    if (i < mlbdItems.size) add(mlbdItems[i])
                }
            }.distinctBy { "${it.source.lowercase()}:${it.id.ifBlank { it.title.lowercase() }}" }
        }

    suspend fun suggest(query: String, source: ContentSource, safeMode: Boolean, lang: String = "en"): List<String> {
        if (source == ContentSource.MOVIEBOX || !isApiKeyConfigured) {
            return movieBoxDirect.suggest(query, lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/suggest", mapOf("q" to query, "source" to source.apiValue))
            CinePulseJsonParser.parseSuggestions(body, safeMode)
        }.getOrElse {
            movieBoxDirect.suggest(query, lang, safeMode)
        }
    }

    suspend fun getHotSearch(safeMode: Boolean, lang: String = "en"): List<String> {
        if (!isApiKeyConfigured) {
            return movieBoxDirect.getHotSearch(lang, safeMode)
        }
        return runCatching {
            val (body, _) = getJson("/api/v1/hot-search")
            val list = CinePulseJsonParser.parseSuggestions(body, safeMode)
            list.ifEmpty { movieBoxDirect.getHotSearch(lang, safeMode) }
        }.getOrElse {
            movieBoxDirect.getHotSearch(lang, safeMode)
        }
    }

    suspend fun getDetails(item: MediaItem, safeMode: Boolean, lang: String = "en"): MediaDetail =
        withContext(Dispatchers.Default) {
        supervisorScope {
        val cacheKey = "${item.source}:${item.id}:${item.title}:$lang"
        detailCache.getFresh(cacheKey)?.let { return@supervisorScope it }

        val isMovieBoxSource = item.source.equals("moviebox", ignoreCase = true) || item.source.isBlank()
        val hasNumericId = item.id.isNotBlank() && item.id.all(Char::isDigit)

        if (isMovieBoxSource && hasNumericId) {
            val directDetail = runCatching { movieBoxDirect.getDetails(item, lang, safeMode) }.getOrNull()
            if (directDetail != null) {
                detailCache[cacheKey] = TimedCacheEntry(directDetail, System.currentTimeMillis() + CACHE_TTL_MS)
                return@supervisorScope directDetail
            }
        }

        if (!isApiKeyConfigured) {
            val fallback = if (hasNumericId) {
                movieBoxDirect.getDetails(item, lang, safeMode)
            } else {
                val found = movieBoxDirect.search(MovieBoxDirectClient.cleanSearchTitle(item.title), 1, lang, safeMode)
                    .firstOrNull()
                if (found != null) movieBoxDirect.getDetails(found, lang, safeMode) else MediaDetail(item = item)
            }
            detailCache[cacheKey] = TimedCacheEntry(fallback, System.currentTimeMillis() + CACHE_TTL_MS)
            return@supervisorScope fallback
        }

        val cleanTitle = MovieBoxDirectClient.cleanSearchTitle(item.title)
        val params = buildMap {
            if (hasNumericId) {
                put("subjectId", item.id)
            } else if (cleanTitle.isNotBlank()) {
                put("title", cleanTitle)
            } else if (item.id.isNotBlank() && !item.id.contains(" ")) {
                put("subjectId", item.id)
            }
            if (item.detailPath.isNotBlank()) put("detailPath", item.detailPath)
            if (item.source.isNotBlank()) put("source", item.source)
        }
        val detailDeferred = async {
            runCatching {
                val (body, _) = getJson("/api/v1/details", params)
                CinePulseJsonParser.parseMediaDetail(body, item)
            }.getOrNull()
        }
        val relatedDeferred = async {
            if (item.id.isNotBlank()) runCatching { getRelated(item.id, safeMode) }.getOrDefault(emptyList())
            else emptyList()
        }
        val commentsDeferred = async {
            if (item.id.isNotBlank()) runCatching { getComments(item.id) }.getOrDefault(emptyList())
            else emptyList()
        }

        val workerDetail = detailDeferred.await()
        if (workerDetail != null) {
            val decoratedStreams = if (workerDetail.streams.isNotEmpty() && isMovieBoxSource) {
                MovieBoxDirectClient.attachPlaybackHeaders(workerDetail.streams, workerDetail.item.id, workerDetail.item.detailPath)
            } else {
                workerDetail.streams
            }

            val full = workerDetail.copy(
                streams = decoratedStreams,
                related = relatedDeferred.await(),
                comments = commentsDeferred.await()
            )
            detailCache[cacheKey] = TimedCacheEntry(full, System.currentTimeMillis() + CACHE_TTL_MS)
            return@supervisorScope full
        }

        // Failover to Direct MovieBox if Worker returned HTTP 502 / error
        val directFallback = if (hasNumericId) {
            runCatching { movieBoxDirect.getDetails(item, lang, safeMode) }.getOrNull()
        } else {
            runCatching {
                val found = movieBoxDirect.search(MovieBoxDirectClient.cleanSearchTitle(item.title), 1, lang, safeMode)
                    .firstOrNull()
                if (found != null) movieBoxDirect.getDetails(found, lang, safeMode) else null
            }.getOrNull()
        }

        val resolved = directFallback ?: MediaDetail(
            item = item,
            related = relatedDeferred.await(),
            comments = commentsDeferred.await()
        )
        detailCache[cacheKey] = TimedCacheEntry(resolved, System.currentTimeMillis() + CACHE_TTL_MS)
        resolved
        }
    }

    /**
     * Turbo Dual-Source Stream Resolver (MovieBox + MovieLinkBD):
     * - 3-minute in-memory stream cache for instant 0ms episode revisits
     * - Zero blocking pre-flight probe wall — hands verified streams straight to ExoPlayer for MovieBox-grade startup
     * - Full header preservation for BOTH MovieBox CDN and MovieLinkBD / Worker Proxy streams
     */
    suspend fun getStreams(
        item: MediaItem,
        isMovie: Boolean,
        season: Int,
        episode: Int,
        lang: String = "en",
        forceRefresh: Boolean = false
    ): Pair<List<StreamOption>, List<CaptionTrack>> = withContext(Dispatchers.Default) {
        supervisorScope {
        val effectiveSe = if (isMovie) 0 else season.coerceAtLeast(1)
        val effectiveEp = if (isMovie) 0 else episode.coerceAtLeast(1)
        val isMovieLinkBdItem = item.source.equals("movielinkbd", ignoreCase = true)
        val isMovieBoxItem = !isMovieLinkBdItem
        val hasDirectId = item.id.isNotBlank() && item.id.all(Char::isDigit)

        val streamCacheKey = "${item.source}:${item.id}:${item.title}:$effectiveSe:$effectiveEp:$lang"
        if (!forceRefresh) {
            streamCache.getFresh(streamCacheKey)?.let { return@supervisorScope it }
        }

        fun decorateStream(option: StreamOption): StreamOption {
            val headers = LinkedHashMap<String, String>()
            val isWorkerUrl = option.url.startsWith(BASE_URL)
            val isHakunaUrl = option.url.contains("hakunaymatata.com", ignoreCase = true) ||
                option.url.contains("aoneroom.com", ignoreCase = true)

            if (isWorkerUrl && isApiKeyConfigured) {
                headers["x-api-key"] = activeApiKey
                headers["User-Agent"] = MovieBoxDirectClient.CHROME_USER_AGENT
                headers.putAll(option.headers)
                return option.copy(headers = headers)
            }
            if (isHakunaUrl || (isMovieBoxItem && option.headers["Referer"].isNullOrBlank())) {
                headers["User-Agent"] = MovieBoxDirectClient.CHROME_USER_AGENT
                headers["Referer"] = MovieBoxDirectClient.DEFAULT_REFERER
                headers.putAll(MovieBoxDirectClient.CHROME_VIDEO_HEADERS)
                option.headers.forEach { (k, v) ->
                    val isBlockedRef = k.equals("Referer", ignoreCase = true) && (
                        v.contains("fmoviesunblocked", ignoreCase = true) ||
                            v.contains("aoneroom.com", ignoreCase = true) ||
                            v.contains("moviebox.ph", ignoreCase = true)
                        )
                    if (!k.equals("Origin", ignoreCase = true) && !isBlockedRef && v.isNotBlank()) {
                        headers[k] = v
                    }
                }
                return option.copy(headers = headers)
            }
            // MovieLinkBD or 3rd-party direct CDN stream: preserve its exact headers + clean Chrome UA
            headers["User-Agent"] = MovieBoxDirectClient.CHROME_USER_AGENT
            option.headers.forEach { (k, v) ->
                if (v.isNotBlank()) headers[k] = v
            }
            return option.copy(headers = headers)
        }

        val cleanTitle = MovieBoxDirectClient.cleanSearchTitle(item.title)

        // Launch Direct MovieBox and CinePulse Worker (MovieLinkBD + Proxy) concurrently!
        val directDeferred = async {
            if (isMovieBoxItem && hasDirectId) {
                val primary = runCatching {
                    movieBoxDirect.getStreams(item.id, item.detailPath, effectiveSe, effectiveEp, lang)
                }.getOrNull()
                if (primary != null && primary.first.isNotEmpty()) return@async primary

                if (effectiveSe == 0 || (effectiveSe == 1 && effectiveEp == 1)) {
                    val altSe = if (effectiveSe == 0) 1 else 0
                    val altEp = if (effectiveEp == 0) 1 else 0
                    runCatching {
                        movieBoxDirect.getStreams(item.id, item.detailPath, altSe, altEp, lang)
                    }.getOrNull()
                } else {
                    null
                }
            } else {
                null
            }
        }

        val workerDeferred = async {
            if (!isApiKeyConfigured) return@async null
            val mergedWorkerStreams = mutableListOf<StreamOption>()
            val mergedWorkerCaptions = mutableListOf<CaptionTrack>()

            val queries = buildList {
                if (isMovieLinkBdItem) {
                    // Site 2 (MovieLinkBD) primary stream query
                    if (cleanTitle.isNotBlank() || item.id.isNotBlank()) {
                        add(
                            buildMap {
                                if (cleanTitle.isNotBlank()) put("title", cleanTitle)
                                if (item.id.isNotBlank()) put("subjectId", item.id)
                                if (item.detailPath.isNotBlank()) put("detailPath", item.detailPath)
                                if (!isMovie) {
                                    put("se", effectiveSe.toString())
                                    put("ep", effectiveEp.toString())
                                }
                                put("source", "movielinkbd")
                            }
                        )
                        add(
                            buildMap {
                                if (cleanTitle.isNotBlank()) put("title", cleanTitle)
                                if (!isMovie) {
                                    put("se", effectiveSe.toString())
                                    put("ep", effectiveEp.toString())
                                }
                                put("source", "all")
                            }
                        )
                    }
                } else {
                    // MovieBox item: also fetch Worker proxy + MovieLinkBD mirrors in parallel
                    if (hasDirectId) {
                        add(
                            buildMap {
                                put("subjectId", item.id)
                                if (item.detailPath.isNotBlank()) put("detailPath", item.detailPath)
                                put("se", effectiveSe.toString())
                                put("ep", effectiveEp.toString())
                                put("source", "all")
                            }
                        )
                    } else if (cleanTitle.isNotBlank()) {
                        add(
                            buildMap {
                                put("title", cleanTitle)
                                if (!isMovie) {
                                    put("se", effectiveSe.toString())
                                    put("ep", effectiveEp.toString())
                                }
                                put("source", "all")
                            }
                        )
                    }
                }
            }

            for (params in queries) {
                val parsed = runCatching {
                    val (body, _) = getJson("/api/v1/stream", params)
                    CinePulseJsonParser.parseStreams(body, BASE_URL)
                }.getOrNull()
                if (parsed != null && parsed.first.isNotEmpty()) {
                    val labeled = parsed.first.map { opt ->
                        val tagged = if (isMovieLinkBdItem && !opt.label.contains("MLBD", ignoreCase = true) && !opt.label.contains("MovieLink", ignoreCase = true)) {
                            opt.copy(label = "${opt.quality} • MovieLinkBD")
                        } else {
                            opt
                        }
                        decorateStream(tagged)
                    }
                    mergedWorkerStreams.addAll(labeled)
                    mergedWorkerCaptions.addAll(parsed.second)
                    break
                }
            }

            if (mergedWorkerStreams.isNotEmpty()) {
                mergedWorkerStreams to mergedWorkerCaptions
            } else {
                null
            }
        }

        val collectedStreams = mutableListOf<StreamOption>()
        val collectedCaptions = mutableListOf<CaptionTrack>()

        if (isMovieLinkBdItem) {
            // For MovieLinkBD titles: await Worker MLBD streams first!
            workerDeferred.await()?.let { (s, c) ->
                collectedStreams.addAll(s)
                collectedCaptions.addAll(c)
            }
            // Cross-match on MovieBox Direct ONLY if MLBD returned nothing so user still gets instant playback!
            if (collectedStreams.isEmpty() && cleanTitle.length >= 2) {
                val matched = runCatching {
                    movieBoxDirect.search(cleanTitle, 1, lang, safeMode = true)
                }.getOrDefault(emptyList())
                    .firstOrNull { it.id.all(Char::isDigit) }
                if (matched != null) {
                    runCatching {
                        movieBoxDirect.getStreams(matched.id, matched.detailPath, effectiveSe, effectiveEp, lang)
                    }.getOrNull()?.let { fallbackPlay ->
                        collectedStreams.addAll(fallbackPlay.first.map(::decorateStream))
                        collectedCaptions.addAll(fallbackPlay.second)
                    }
                }
            }
        } else {
            // For MovieBox titles: Direct MovieBox responds in ~300ms — NEVER block it waiting 8s for Worker!
            val directRes = directDeferred.await()
            if (directRes != null) {
                collectedStreams.addAll(directRes.first.map(::decorateStream))
                collectedCaptions.addAll(directRes.second)
            }
            val workerRes = if (collectedStreams.isNotEmpty()) {
                withTimeoutOrNull(1300L) { workerDeferred.await() }
            } else {
                workerDeferred.await()
            }
            if (workerRes != null) {
                collectedStreams.addAll(workerRes.first)
                collectedCaptions.addAll(workerRes.second)
            }

            // Cross-match clean title on Direct MovieBox if still empty
            if (collectedStreams.isEmpty() && cleanTitle.length >= 2) {
                val matched = runCatching {
                    movieBoxDirect.search(cleanTitle, 1, lang, safeMode = true)
                }.getOrDefault(emptyList())
                    .firstOrNull { it.id.all(Char::isDigit) && it.id != item.id }
                if (matched != null) {
                    runCatching {
                        movieBoxDirect.getStreams(matched.id, matched.detailPath, effectiveSe, effectiveEp, lang)
                    }.getOrNull()?.let { fallbackPlay ->
                        collectedStreams.addAll(fallbackPlay.first.map(::decorateStream))
                        collectedCaptions.addAll(fallbackPlay.second)
                    }
                }
            }
        }

        // Smart MovieBox-grade mobile quality ranking: fast-starting 720p first, then 1080p, 480p, 360p!
        fun fastStartScore(option: StreamOption): Int {
            val res = Regex("(\\d{3,4})").find(option.quality)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
            val qualityScore = when {
                res in 700..760 -> 400 // 720p: crisp + instant <1s mobile startup
                res >= 1000 -> 350     // 1080p: ultra HD one tap away (or primary if no 720p)
                res in 460..560 -> 280 // 480p: fast fallback
                res in 300..400 -> 200 // 360p: low-data
                else -> 300
            }
            val sourceBonus = when {
                isMovieLinkBdItem && !option.url.contains("hakunaymatata", ignoreCase = true) -> 200
                !isMovieLinkBdItem && option.url.contains("hakunaymatata", ignoreCase = true) -> 120
                option.url.startsWith(BASE_URL) -> 100
                else -> 80
            }
            return qualityScore + sourceBonus
        }

        val rankedStreams = collectedStreams
            .distinctBy { it.url }
            .sortedByDescending { fastStartScore(it) }
        val dedupedCaptions = collectedCaptions.distinctBy { it.url }

        val result = rankedStreams to dedupedCaptions
        if (rankedStreams.isNotEmpty()) {
            streamCache[streamCacheKey] = TimedCacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        result
        }
    }

    suspend fun getCaptions(subjectId: String, streamId: String, detailPath: String = "movie"): List<CaptionTrack> {
        val direct = runCatching { movieBoxDirect.getCaptions(subjectId, streamId, detailPath) }
            .getOrDefault(emptyList())
        if (direct.isNotEmpty()) return direct
        if (!isApiKeyConfigured) return emptyList()
        val (body, _) = getJson(
            "/api/v1/captions",
            mapOf("subjectId" to subjectId, "streamId" to streamId)
        )
        return CinePulseJsonParser.parseCaptions(body)
    }

    suspend fun getRelated(subjectId: String, safeMode: Boolean): List<MediaItem> {
        if (subjectId.all(Char::isDigit)) {
            val direct = runCatching { movieBoxDirect.getRelated(subjectId, safeMode) }.getOrDefault(emptyList())
            if (direct.isNotEmpty()) return direct
        }
        if (!isApiKeyConfigured) return emptyList()
        val (body, _) = getJson("/api/v1/related", mapOf("subjectId" to subjectId))
        return CinePulseJsonParser.parseMediaList(body, safeMode)
    }

    suspend fun getComments(subjectId: String): List<CommentItem> {
        if (subjectId.all(Char::isDigit)) {
            val direct = runCatching { movieBoxDirect.getComments(subjectId) }.getOrDefault(emptyList())
            if (direct.isNotEmpty()) return direct
        }
        if (!isApiKeyConfigured) return emptyList()
        val (body, _) = getJson("/api/v1/comments", mapOf("subjectId" to subjectId))
        return CinePulseJsonParser.parseComments(body)
    }

    suspend fun getStaffDetail(member: CastMember, safeMode: Boolean): StaffDetail {
        val hasDirectStaffId = member.id.isNotBlank() && !member.id.startsWith("cast_")
        if (hasDirectStaffId && member.id.all(Char::isDigit)) {
            val directStaff = runCatching { movieBoxDirect.getStaffDetail(member, safeMode) }.getOrNull()
            if (directStaff != null && (directStaff.works.isNotEmpty() || directStaff.bio.isNotBlank())) {
                return directStaff
            }
        }
        if (isApiKeyConfigured && hasDirectStaffId) {
            val workerStaff = runCatching {
                val (body, _) = getJson("/api/v1/staff", mapOf("staffId" to member.id))
                CinePulseJsonParser.parseStaffDetail(body, member, safeMode)
            }.getOrNull()
            if (workerStaff != null && (workerStaff.works.isNotEmpty() || workerStaff.bio.isNotBlank())) {
                return workerStaff
            }
        }
        val works = runCatching {
            search(member.name, 1, ContentSource.ALL, safeMode)
        }.getOrDefault(emptyList())
        return StaffDetail(member = member, works = works)
    }

    suspend fun getStatus(): ApiStatusInfo {
        val workerStatus = if (isApiKeyConfigured) {
            runCatching {
                val (body, latency) = getJson("/api/v1/status")
                val status = CinePulseJsonParser.parseStatus(body, latency)
                status.copy(statusText = "CinePulse Edge • ${status.statusText}")
            }.getOrNull()
        } else {
            null
        }
        return workerStatus ?: movieBoxDirect.ping()
    }
}
