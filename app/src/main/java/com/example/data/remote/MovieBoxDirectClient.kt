package com.example.data.remote

import android.util.Log
import com.example.data.model.ApiStatusInfo
import com.example.data.model.CaptionTrack
import com.example.data.model.CommentItem
import com.example.data.model.FilterOptions
import com.example.data.model.HomeFeed
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.ReelItem
import com.example.data.model.SportsMatch
import com.example.data.model.StreamOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Direct MovieBox + Sports API engine with sticky fastest-host selection, 4-host failover,
 * warm HTTP/2 connection pooling, MD5 X-Client-Token signing, and Bangladesh geo headers.
 */
class MovieBoxDirectClient {

    companion object {
        private const val TAG = "MovieBoxDirect"

        val PRIMARY_HOSTS = listOf(
            "https://h5-api.aoneroom.com",
            "https://moviebox01.com",
            "https://movie-box.co",
            "https://themoviebox.xyz"
        )

        const val SPORTS_HOST = "https://h5-sport-api.aoneroom.com"

        const val CHROME_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        // Verified primary playback Referer per MovieBox upstream spec
        const val DEFAULT_REFERER = "https://movie-box.co/"

        // Verified whitelisted Referers accepted by hakunaymatata.com / Tengine video CDN (returns HTTP 206)
        val WHITELISTED_STREAM_REFERERS = listOf(
            "https://movie-box.co/",
            "https://moviebox01.com/",
            "https://123movienow.cc/",
            "https://themoviebox.xyz/"
        )

        // Verified global EdgeNext POP IPs for *.hakunaymatata.com (all serve valid TLS + HTTP 206 video/mp4)
        val EDGENEXT_FALLBACK_POPS = listOf(
            "103.137.168.117", // Singapore
            "154.85.94.28",    // US
            "103.120.202.253", // Bangladesh / South Asia
            "148.222.161.58",  // Japan
            "156.238.128.62"   // Taiwan / APAC
        )

        // Full browser <video> request headers matching real Chrome playback requests
        val CHROME_VIDEO_HEADERS = linkedMapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Sec-Fetch-Dest" to "video",
            "Sec-Fetch-Mode" to "no-cors",
            "Sec-Fetch-Site" to "cross-site",
            "sec-ch-ua" to "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\""
        )

        /**
         * Primary CDN DNS: uses fast system resolution without wasting time on NXDOMAIN .w.cdngslb.com
         * lookups for bcdnxw/bcdnw, while still preferring Alibaba Cloud GSLB for valiw when needed.
         */
        val TENGINE_CDN_DNS: Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (hostname.equals("valiw.hakunaymatata.com", ignoreCase = true)) {
                    val aliIps = runCatching {
                        InetAddress.getAllByName("$hostname.w.cdngslb.com").toList()
                    }.getOrDefault(emptyList())
                    if (aliIps.isNotEmpty()) return aliIps
                }
                return Dns.SYSTEM.lookup(hostname)
            }
        }

        /**
         * Alternate global EdgeNext POP DNS: routes *.hakunaymatata.com through verified SG/US/BD/JP
         * EdgeNext POPs (preserving exact TLS SNI, Host header, and signed URL) when the local POP
         * returns HTTP 426/429.
         */
        val EDGE_POP_FALLBACK_DNS: Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (hostname.endsWith(".hakunaymatata.com", ignoreCase = true) &&
                    !hostname.startsWith("valiw.", ignoreCase = true) &&
                    !hostname.startsWith("cacdn.", ignoreCase = true)
                ) {
                    val popAddrs = EDGENEXT_FALLBACK_POPS.mapNotNull { ip ->
                        runCatching { InetAddress.getByName(ip) }.getOrNull()
                    }
                    if (popAddrs.isNotEmpty()) return popAddrs
                }
                return Dns.SYSTEM.lookup(hostname)
            }
        }

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun buildDetailReferer(subjectId: String, detailPath: String): String {
            val cleanPath = detailPath.trim().trimStart('/').ifBlank { "movie" }
            return if (subjectId.isNotBlank()) {
                "https://movie-box.co/movies/$cleanPath?id=$subjectId"
            } else {
                DEFAULT_REFERER
            }
        }

        fun attachPlaybackHeaders(
            streams: List<StreamOption>,
            subjectId: String,
            detailPath: String,
            preferredReferer: String = DEFAULT_REFERER
        ): List<StreamOption> {
            return streams.map { option ->
                val merged = LinkedHashMap<String, String>()
                merged["User-Agent"] = CHROME_USER_AGENT
                if (preferredReferer.isNotBlank()) {
                    merged["Referer"] = preferredReferer
                }
                merged.putAll(CHROME_VIDEO_HEADERS)
                option.headers.forEach { (k, v) ->
                    // Never send unsolicited Origin or blocked referers on <video> GET requests (triggers CDN 429/426/403)
                    val isBlockedRef = k.equals("Referer", ignoreCase = true) && (
                        v.contains("fmoviesunblocked", ignoreCase = true) ||
                            v.contains("aoneroom.com", ignoreCase = true) ||
                            v.contains("moviebox.ph", ignoreCase = true)
                        )
                    if (!k.equals("Origin", ignoreCase = true) && !isBlockedRef && v.isNotBlank()) {
                        merged[k] = v
                    }
                }
                option.copy(headers = merged)
            }
        }

        fun cleanSearchTitle(rawTitle: String): String {
            return rawTitle
                .replace(Regex("\\[[^\\]]*]"), " ")
                .replace(Regex("\\(\\d{4}\\)"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    @Volatile
    private var preferredHostIndex: Int = 0

    @Volatile
    private var sessionBearerToken: String = ""

    fun rotatePreferredHost() {
        preferredHostIndex = (preferredHostIndex + 1) % PRIMARY_HOSTS.size
    }

    private fun captureSessionToken(res: okhttp3.Response) {
        val xUser = res.header("x-user").orEmpty()
        if (xUser.isNotBlank()) {
            val tok = runCatching { JSONObject(xUser).optString("token") }.getOrDefault("")
            if (tok.isNotBlank()) {
                sessionBearerToken = tok
                return
            }
        }
        val cookies = res.headers("Set-Cookie")
        for (c in cookies) {
            val match = Regex("""(?:^|;\s*)token=([^;]+)""").find(c)
            val tok = match?.groupValues?.getOrNull(1).orEmpty()
            if (tok.isNotBlank()) {
                sessionBearerToken = tok
                return
            }
        }
    }

    private suspend fun bootstrapSessionToken(lang: String = "en") = withContext(Dispatchers.IO) {
        for ((hostIdx, host) in orderedHosts()) {
            try {
                val req = Request.Builder()
                    .url("$host/wefeed-h5api-bff/subject/everyone-search")
                    .header("User-Agent", CHROME_USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Referer", DEFAULT_REFERER)
                    .header("X-Client-Info", """{"timezone":"Asia/Dhaka"}""")
                    .header("X-Vip-Restrict", "0")
                    .header("X-No-High-Risk-Restrict", "1")
                    .header("X-Request-Lang", lang.ifBlank { "en" })
                    .header("X-Client-Token", generateClientToken())
                    .header("X-Forwarded-For", "103.106.238.10")
                    .header("X-Real-IP", "103.106.238.10")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { res ->
                    captureSessionToken(res)
                    if (sessionBearerToken.isNotBlank()) {
                        preferredHostIndex = hostIdx
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Session bootstrap failed on $host: ${e.message}")
            }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun generateClientToken(clockOffsetSec: Long = 0L): String {
        val ts = (System.currentTimeMillis() / 1000L) + clockOffsetSec
        val reversed = ts.toString().reversed()
        val digest = MessageDigest.getInstance("MD5")
            .digest(reversed.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "$ts,$digest"
    }

    private fun orderedHosts(): List<Pair<Int, String>> {
        val start = preferredHostIndex.coerceIn(0, PRIMARY_HOSTS.lastIndex)
        return buildList {
            for (offset in PRIMARY_HOSTS.indices) {
                val idx = (start + offset) % PRIMARY_HOSTS.size
                add(idx to PRIMARY_HOSTS[idx])
            }
        }
    }

    private suspend fun mbRequest(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        method: String = "GET",
        jsonBody: JSONObject? = null,
        lang: String = "en",
        referer: String = DEFAULT_REFERER,
        sports: Boolean = false,
        useRealIp: Boolean = false,
        requireBearer: Boolean = false,
        customHosts: List<String>? = null
    ): Pair<String, Long> = withContext(Dispatchers.IO) {
        if (requireBearer && !sports && sessionBearerToken.isBlank()) {
            bootstrapSessionToken(lang)
        }

        val hosts = when {
            customHosts != null -> customHosts.mapIndexed { idx, h -> idx to h }
            sports -> listOf(0 to SPORTS_HOST)
            else -> orderedHosts()
        }
        var lastError: Exception? = null
        var retriedTokenRefresh = false

        fun buildReq(host: String): Request {
            val urlBuilder = (host + path).toHttpUrl().newBuilder()
            queryParams.forEach { (k, v) ->
                if (v.isNotBlank()) urlBuilder.addQueryParameter(k, v)
            }

            val reqBuilder = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", CHROME_USER_AGENT)
                .header("Accept", "application/json")
                .header("Referer", referer)
                .header("Origin", if (referer.startsWith("https://")) {
                    runCatching {
                        val u = referer.toHttpUrl()
                        "${u.scheme}://${u.host}"
                    }.getOrDefault("https://moviebox01.com")
                } else "https://moviebox01.com")
                .header("X-Client-Info", """{"timezone":"Asia/Dhaka"}""")
                .header("X-Vip-Restrict", "0")
                .header("X-No-High-Risk-Restrict", "1")
                .header("X-Request-Lang", lang.ifBlank { "en" })

            val bearer = sessionBearerToken
            if (!sports && bearer.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $bearer")
            } else {
                reqBuilder.header("X-Client-Token", generateClientToken())
            }

            if (!useRealIp) {
                reqBuilder
                    .header("X-Forwarded-For", "103.106.238.10")
                    .header("X-Real-IP", "103.106.238.10")
            }

            if (method.equals("POST", ignoreCase = true)) {
                val payload = (jsonBody ?: JSONObject()).toString()
                reqBuilder.post(payload.toRequestBody(JSON_MEDIA_TYPE))
            } else {
                reqBuilder.get()
            }
            return reqBuilder.build()
        }

        for ((hostIdx, host) in hosts) {
            try {
                var attempt = 0
                while (attempt < 2) {
                    attempt++
                    val start = System.currentTimeMillis()
                    var shouldRetryWithFreshToken = false
                    httpClient.newCall(buildReq(host)).execute().use { res ->
                        val latency = System.currentTimeMillis() - start
                        captureSessionToken(res)
                        val body = res.body?.string().orEmpty()
                        if (!res.isSuccessful || body.isBlank()) {
                            val tokenRejected = !sports &&
                                res.code in setOf(400, 401) &&
                                body.contains("invalid token", ignoreCase = true)
                            if (tokenRejected && !retriedTokenRefresh) {
                                retriedTokenRefresh = true
                                sessionBearerToken = ""
                                shouldRetryWithFreshToken = true
                            } else {
                                lastError = IOException("Host $host returned HTTP ${res.code}")
                            }
                            return@use
                        }
                        val parsed = runCatching { JSONObject(body) }.getOrNull()
                        if (parsed != null && (parsed.optInt("code", -1) == 0 || parsed.has("data"))) {
                            if (!sports && customHosts == null) preferredHostIndex = hostIdx
                            return@withContext body to latency
                        }
                        lastError = IOException("Host $host returned unexpected payload")
                    }
                    if (shouldRetryWithFreshToken) {
                        bootstrapSessionToken(lang)
                        if (sessionBearerToken.isBlank()) break
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "MovieBox host $host failed for $path: ${e.message}")
                lastError = e
            }
        }

        throw lastError ?: IOException("All MovieBox hosts failed for $path")
    }

    suspend fun getHome(lang: String, safeMode: Boolean): HomeFeed = supervisorScope {
        val homeDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/home",
                    queryParams = mapOf("host" to "movie-box.co"),
                    lang = lang
                )
                CinePulseJsonParser.parseHomeFeed(body, safeMode)
            }.getOrNull()
        }
        val sportsDeferred = async { runCatching { getSportsMatches() }.getOrDefault(emptyList()) }
        val upcomingDeferred = async { runCatching { getUpcoming(1, lang, safeMode) }.getOrDefault(emptyList()) }
        val trendingDeferred = async { runCatching { getTrending(1, lang, safeMode) }.getOrDefault(emptyList()) }

        val parsed = homeDeferred.await()
        val trending = withTimeoutOrNull(2200L) { trendingDeferred.await() }.orEmpty()
        val sports = parsed?.sportsMatches.orEmpty().ifEmpty {
            withTimeoutOrNull(1800L) { sportsDeferred.await() }.orEmpty()
        }
        val upcoming = parsed?.upcomingItems.orEmpty().ifEmpty {
            withTimeoutOrNull(1800L) { upcomingDeferred.await() }.orEmpty()
        }

        if (parsed != null && (parsed.banners.isNotEmpty() || parsed.sections.isNotEmpty())) {
            parsed.copy(
                sportsMatches = sports,
                upcomingItems = upcoming,
                trendingItems = trending
            )
        } else if (trending.isNotEmpty()) {
            HomeFeed(
                banners = trending.take(6),
                sportsMatches = sports,
                upcomingItems = upcoming,
                trendingItems = trending
            )
        } else {
            throw IOException("Direct MovieBox hosts returned empty feed")
        }
    }

    suspend fun getTrending(page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<MediaItem> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/trending",
            queryParams = mapOf("page" to page.toString(), "perPage" to "28"),
            lang = lang
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getRanking(page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<MediaItem> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/ranking-list/content",
            queryParams = mapOf("id" to "872031290915189720", "page" to page.toString(), "perPage" to "20"),
            lang = lang
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getUpcoming(page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<MediaItem> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/upcoming-subject-list",
            queryParams = mapOf("page" to page.toString(), "perPage" to "24"),
            lang = lang
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getReels(page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<ReelItem> = supervisorScope {
        val trendingDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/subject/trending",
                    queryParams = mapOf("page" to page.toString(), "perPage" to "24"),
                    lang = lang
                )
                CinePulseJsonParser.parseDirectReels(body, safeMode)
            }.getOrDefault(emptyList())
        }
        val upcomingDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/upcoming-subject-list",
                    queryParams = mapOf("page" to page.toString(), "perPage" to "20"),
                    lang = lang
                )
                CinePulseJsonParser.parseDirectReels(body, safeMode)
            }.getOrDefault(emptyList())
        }
        (trendingDeferred.await() + upcomingDeferred.await()).distinctBy { it.videoUrl }
    }

    suspend fun getSportsMatches(): List<SportsMatch> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/live/match-list-v5",
            sports = true
        )
        return CinePulseJsonParser.parseSportsMatches(body)
    }

    suspend fun getHotSearch(lang: String = "en", safeMode: Boolean = true): List<String> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/everyone-search",
            lang = lang
        )
        return CinePulseJsonParser.parseSuggestions(body, safeMode)
    }

    suspend fun search(keyword: String, page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<MediaItem> {
        val payload = JSONObject().apply {
            put("keyword", keyword)
            put("page", page)
            put("perPage", 24)
            put("subjectType", 0)
        }
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/search",
            method = "POST",
            jsonBody = payload,
            lang = lang,
            requireBearer = true
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun suggest(keyword: String, lang: String = "en", safeMode: Boolean = true): List<String> {
        val payload = JSONObject().apply {
            put("keyword", keyword)
            put("perPage", 10)
        }
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/search-suggest",
            method = "POST",
            jsonBody = payload,
            lang = lang
        )
        return CinePulseJsonParser.parseSuggestions(body, safeMode)
    }

    suspend fun filter(
        category: String,
        genre: String,
        year: String,
        page: Int = 1,
        lang: String = "en",
        safeMode: Boolean = true
    ): List<MediaItem> {
        val channelId = when (category.lowercase()) {
            "tv", "series", "drama", "dramas" -> 2
            "anime" -> 6
            "short", "shorts" -> 7
            else -> 1
        }
        val payload = JSONObject().apply {
            put("channelId", channelId)
            put("page", page)
            put("perPage", 24)
            put("sort", "ForYou")
            if (genre.isNotBlank() && !genre.equals("All", ignoreCase = true)) {
                put("genre", genre)
            }
            if (year.isNotBlank() && !year.equals("All", ignoreCase = true)) {
                put("year", year)
            }
        }
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/filter",
            method = "POST",
            jsonBody = payload,
            lang = lang
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getFilterOptions(lang: String = "en", safeMode: Boolean = true): FilterOptions {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/filter",
            lang = lang
        )
        return CinePulseJsonParser.parseFilterOptions(body, safeMode)
    }

    suspend fun getPlatformCatalog(platform: String, page: Int = 1, lang: String = "en", safeMode: Boolean = true): List<MediaItem> {
        val displayPlatform = when (platform.lowercase().replace(" ", "").replace("+", "")) {
            "netflix" -> "Netflix"
            "prime", "primevideo" -> "PrimeVideo"
            "disney" -> "Disney"
            "apple", "appletv" -> "AppleTV"
            "viu" -> "Viu"
            "hulu" -> "Hulu"
            "zee5" -> "Zee5"
            "vivamax" -> "Vivamax"
            "hoichoi" -> "Hoichoi"
            "showmax" -> "Showmax"
            else -> platform
        }
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/platform/play-list",
            queryParams = mapOf(
                "page" to page.toString(),
                "perPage" to "6", // Upstream strict max: perPage > 6 returns HTTP 400
                "platform" to displayPlatform
            ),
            lang = lang
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getDetails(item: MediaItem, lang: String = "en", safeMode: Boolean = true): MediaDetail = supervisorScope {
        val cleanPath = item.detailPath.trim().trimStart('/').ifBlank { "movie" }
        val detailDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/detail",
                    queryParams = mapOf(
                        "detailPath" to cleanPath,
                        "subjectId" to item.id
                    ),
                    lang = lang,
                    referer = buildDetailReferer(item.id, cleanPath)
                )
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

        val detail = detailDeferred.await() ?: MediaDetail(item = item)
        detail.copy(
            related = relatedDeferred.await(),
            comments = commentsDeferred.await()
        )
    }

    suspend fun getStreams(
        subjectId: String,
        detailPath: String,
        se: Int,
        ep: Int,
        lang: String = "en"
    ): Pair<List<StreamOption>, List<CaptionTrack>> = supervisorScope {
        val cleanPath = detailPath.trim().trimStart('/').ifBlank { "movie" }
        val detailReferer = buildDetailReferer(subjectId, cleanPath)
        val queryParams = mapOf(
            "subjectId" to subjectId,
            "se" to se.toString(),
            "ep" to ep.toString(),
            "detailPath" to cleanPath,
            "streamSignType" to "1"
        )

        // Query /subject/play and /subject/download in parallel using verified upstream Referer: https://movie-box.co/
        val playDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/subject/play",
                    queryParams = queryParams,
                    lang = lang,
                    referer = detailReferer,
                    useRealIp = false
                )
                val (raw, caps) = CinePulseJsonParser.parseStreams(body)
                attachPlaybackHeaders(raw, subjectId, cleanPath, "https://movie-box.co/") to caps
            }.getOrNull()
        }

        val downloadDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/subject/download",
                    queryParams = queryParams,
                    lang = lang,
                    referer = detailReferer,
                    useRealIp = false
                )
                val (raw, caps) = CinePulseJsonParser.parseStreams(body)
                attachPlaybackHeaders(
                    raw.map { it.copy(label = "${it.quality} • Fast CDN") },
                    subjectId,
                    cleanPath,
                    "https://movie-box.co/"
                ) to caps
            }.getOrNull()
        }

        val allStreams = mutableListOf<StreamOption>()
        val allCaptions = mutableListOf<CaptionTrack>()

        // Return immediately as soon as /subject/play succeeds; give /subject/download a brief 1.1s window
        val playRes = playDeferred.await()
        if (playRes != null) {
            allStreams.addAll(playRes.first)
            allCaptions.addAll(playRes.second)
        }
        val downloadRes = if (allStreams.isNotEmpty()) {
            withTimeoutOrNull(1100L) { downloadDeferred.await() }
        } else {
            downloadDeferred.await()
        }
        if (downloadRes != null) {
            allStreams.addAll(downloadRes.first)
            allCaptions.addAll(downloadRes.second)
        }

        val streams = allStreams.distinctBy { it.url }
        val captions = allCaptions.distinctBy { it.url }
        streams to captions
    }

    suspend fun getCaptions(subjectId: String, streamId: String, detailPath: String = "movie"): List<CaptionTrack> {
        val cleanPath = detailPath.trim().trimStart('/').ifBlank { "movie" }
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/caption",
            queryParams = mapOf(
                "format" to "MP4",
                "id" to streamId,
                "subjectId" to subjectId,
                "detailPath" to cleanPath
            ),
            referer = buildDetailReferer(subjectId, cleanPath)
        )
        return CinePulseJsonParser.parseCaptions(body)
    }

    suspend fun getRelated(subjectId: String, safeMode: Boolean = true): List<MediaItem> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/subject/detail-rec",
            queryParams = mapOf(
                "subjectId" to subjectId,
                "page" to "1",
                "perPage" to "18"
            )
        )
        return CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
    }

    suspend fun getStaffDetail(
        member: com.example.data.model.CastMember,
        safeMode: Boolean = true
    ): com.example.data.model.StaffDetail = supervisorScope {
        val worksDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/staff/subject-list",
                    queryParams = mapOf(
                        "perPage" to "24",
                        "page" to "1",
                        "staffId" to member.id
                    )
                )
                CinePulseJsonParser.parseMediaList(body, safeMode, "moviebox")
            }.getOrDefault(emptyList())
        }
        val infoDeferred = async {
            runCatching {
                val (body, _) = mbRequest(
                    path = "/wefeed-h5api-bff/staff/staff-related",
                    queryParams = mapOf("staffId" to member.id)
                )
                CinePulseJsonParser.parseStaffDetail(body, member, safeMode)
            }.getOrNull()
        }
        val works = worksDeferred.await()
        val info = infoDeferred.await()
        if (info != null) {
            info.copy(works = works.ifEmpty { info.works })
        } else {
            com.example.data.model.StaffDetail(member = member, works = works)
        }
    }

    suspend fun getComments(subjectId: String): List<CommentItem> {
        val (body, _) = mbRequest(
            path = "/wefeed-h5api-bff/post/list/subject",
            queryParams = mapOf(
                "id" to subjectId,
                "page" to "0",
                "perPage" to "20"
            )
        )
        return CinePulseJsonParser.parseComments(body)
    }

    suspend fun ping(): ApiStatusInfo {
        val (_, latency) = mbRequest(
            path = "/wefeed-h5api-bff/subject/everyone-search"
        )
        return ApiStatusInfo(
            isOnline = true,
            statusText = "Cinedor Direct Active",
            version = "19-API Upstream",
            latencyMs = latency
        )
    }
}
