package com.example.data.remote

import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.SeasonInfo
import com.example.data.model.StreamOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * "Server 2": the user's own Cloudflare worker (worker/server2-worker.js in the Console repo)
 * that lists titles, searches and returns playable links. Its address comes from the Console's
 * config ("server2Url"), so it can be changed without a new app build. Items it returns use
 * source = "movielinkbd" so the rest of the app treats them as Server 2 titles.
 */
object Server2Client {
    const val SOURCE = "movielinkbd"
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Built-in Server 2 address, used until (or unless) the Console config gives another one. */
    const val DEFAULT_URL = "https://cinedor-server2.khanbhai19u.workers.dev"

    /** From the Console config (RemoteConsole); falls back to [DEFAULT_URL] so Server 2 always works. */
    @Volatile var baseUrl: String = DEFAULT_URL
        set(v) { field = v.trim().ifBlank { DEFAULT_URL } }

    val enabled: Boolean get() = baseUrl.startsWith("https://")

    data class Detail(
        val title: String,
        val poster: String,
        val synopsis: String,
        val referer: String,
        val streams: List<StreamOption>,
        val episodes: List<EpisodeInfo>,
        val episodePaths: Map<Pair<Int, Int>, String>,
        val episodeStreams: Map<Pair<Int, Int>, List<StreamOption>> = emptyMap()
    )

    private fun get(path: String): JSONObject? {
        if (!enabled) return null
        val req = Request.Builder().url(baseUrl.trimEnd('/') + path).header("Accept", "application/json").build()
        return runCatching {
            http.newCall(req).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) null else JSONObject(body)
            }
        }.getOrNull()
    }

    private fun items(j: JSONObject?): List<MediaItem> {
        val arr = j?.optJSONArray("items") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val path = o.optString("path")
            val title = o.optString("title")
            if (path.isBlank() || title.isBlank()) return@mapNotNull null
            val series = !path.startsWith("/movie/") &&
                (path.startsWith("/series/") || path.startsWith("/drama/") || path.startsWith("/anime/") ||
                    Regex("(?i)season|episode|\\bS\\d+|web.?series").containsMatchIn(title))
            MediaItem(
                id = path,
                title = title,
                posterUrl = o.optString("poster"),
                backdropUrl = o.optString("poster"),
                mediaType = if (series) MediaCategoryType.SERIES else MediaCategoryType.MOVIE,
                source = SOURCE,
                detailPath = path,
                year = Regex("(19|20)\\d{2}").find(title)?.value.orEmpty()
            )
        }.distinctBy { it.id }.distinctBy { it.title.lowercase().trim() }
    }

    suspend fun home(): List<MediaItem> = withContext(Dispatchers.IO) { items(get("/home")) }

    /** One page of the full catalog (page 1 = newest). */
    suspend fun list(page: Int): List<MediaItem> = withContext(Dispatchers.IO) { items(get("/list?page=${page.coerceAtLeast(1)}")) }

    /** Which kind a title is, from its address on the site (/movie/, /series/, /anime/, /drama/). */
    fun kindOf(item: MediaItem): String = when {
        item.id.startsWith("/series/") -> "tv"
        item.id.startsWith("/anime/") -> "anime"
        item.id.startsWith("/drama/") -> "drama"
        else -> "movies"
    }

    /**
     * A page of titles of one kind for the endless category tabs. Several site pages are read
     * together because each site page mixes movies, series, anime and dramas.
     */
    suspend fun catalog(kind: String, page: Int): List<MediaItem> = kotlinx.coroutines.coroutineScope {
        val per = 3
        val first = (page.coerceAtLeast(1) - 1) * per + 1
        (first until first + per)
            .map { p -> async { runCatching { list(p) }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
            .filter { kind.isBlank() || kindOf(it) == kind || (kind == "tv" && kindOf(it) == "drama") }
            .distinctBy { it.id }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList() else items(get("/search?q=" + URLEncoder.encode(query.trim(), "UTF-8")))
    }

    suspend fun detail(path: String): Detail? = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext null
        val j = get("/detail?path=" + URLEncoder.encode(path, "UTF-8")) ?: return@withContext null
        if (j.has("error")) return@withContext null
        val referer = j.optString("referer").trimEnd('/')
        val headers = buildMap {
            put("User-Agent", UA)
            if (referer.isNotBlank()) put("Referer", "$referer/")
        }
        fun parseStreams(arr: JSONArray?, prefix: String): List<StreamOption> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val url = o.optString("url")
                if (!url.startsWith("http")) return@mapNotNull null
                val hls = o.optString("type").equals("hls", true) || url.contains(".m3u8") || url.contains("/m/")
                val quality = o.optString("quality").ifBlank { if (hls) "Auto" else "HD" }
                StreamOption(
                    id = "${prefix}_$i",
                    label = "$quality • Server 2",
                    quality = quality,
                    url = url,
                    format = if (hls) "HLS" else "MP4",
                    headers = headers
                )
            }
        }
        val streams = parseStreams(j.optJSONArray("streams"), "s2")
        val eArr = j.optJSONArray("episodes") ?: JSONArray()
        val paths = LinkedHashMap<Pair<Int, Int>, String>()
        val epStreams = LinkedHashMap<Pair<Int, Int>, List<StreamOption>>()
        val episodes = (0 until eArr.length()).mapNotNull { i ->
            val o = eArr.optJSONObject(i) ?: return@mapNotNull null
            val se = o.optInt("season", 1).coerceAtLeast(1)
            val ep = o.optInt("episode", 0)
            if (ep <= 0) return@mapNotNull null
            o.optString("path").takeIf { it.isNotBlank() }?.let { paths[se to ep] = it }
            // The worker gives each episode's own video addresses, so no extra lookup is needed.
            parseStreams(o.optJSONArray("streams"), "s2e${se}_$ep").takeIf { it.isNotEmpty() }?.let { epStreams[se to ep] = it }
            EpisodeInfo(season = se, episode = ep, title = o.optString("title").ifBlank { "Episode $ep" })
        }.distinctBy { it.season to it.episode }
        Detail(j.optString("title"), j.optString("poster"), j.optString("synopsis"), referer, streams, episodes, paths, epStreams)
    }

    /**
     * The site gives every title a fresh address now and then (/movie/mKs_…), so an address saved
     * earlier (My List, history, a notification) can stop working. Then the title is looked up
     * again by its name and the new address is used.
     */
    private suspend fun detailFor(item: MediaItem): Detail? {
        detail(item.detailPath.ifBlank { item.id })?.let { if (it.title.isNotBlank() || it.streams.isNotEmpty()) return it }
        val name = item.title.substringBefore(" (").substringBefore(" [").trim()
        if (name.length < 2) return null
        val hits = runCatching { search(name) }.getOrDefault(emptyList())
        val norm = { t: String -> t.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim() }
        val best = hits.firstOrNull { norm(it.title) == norm(item.title) }
            ?: hits.firstOrNull { norm(it.title).startsWith(norm(name)) && kindOf(it) == kindOf(item) }
            ?: return null
        return detail(best.id)
    }

    /** Detail page for the app's title screen. */
    suspend fun mediaDetail(item: MediaItem): MediaDetail? {
        val d = detailFor(item) ?: return null
        val seasons = d.episodes.groupBy { it.season }.toSortedMap().map { (se, eps) ->
            SeasonInfo(seasonNumber = se, title = "Season $se", episodes = eps.sortedBy { it.episode })
        }
        val fixed = item.copy(
            title = item.title.ifBlank { d.title },
            posterUrl = item.posterUrl.ifBlank { d.poster },
            backdropUrl = item.backdropUrl.ifBlank { d.poster },
            description = item.description.ifBlank { d.synopsis },
            mediaType = if (seasons.isNotEmpty()) MediaCategoryType.SERIES else item.mediaType
        )
        return MediaDetail(
            item = fixed,
            synopsis = d.synopsis,
            seasonCount = seasons.size,
            seasons = seasons,
            streams = d.streams
        )
    }

    /** Playable links for a movie, or for one episode of a series. */
    suspend fun streams(item: MediaItem, season: Int, episode: Int): List<StreamOption> {
        val main = detailFor(item) ?: return emptyList()
        if (episode > 0) {
            main.episodeStreams[season.coerceAtLeast(1) to episode]?.let { return it }
            // Seasons on this server can start at any number (e.g. season 13, episode 1221).
            main.episodeStreams.entries.firstOrNull { it.key.second == episode }?.let { return it.value }
            main.episodes.getOrNull(episode - 1)?.let { e -> main.episodeStreams[e.season to e.episode]?.let { return it } }
        }
        if (episode <= 0 || main.episodePaths.isEmpty()) return main.streams
        val epPath = main.episodePaths[season.coerceAtLeast(1) to episode] ?: return main.streams
        return detail(epPath)?.streams?.ifEmpty { main.streams } ?: main.streams
    }
}
