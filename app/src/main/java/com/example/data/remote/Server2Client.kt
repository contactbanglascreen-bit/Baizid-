package com.example.data.remote

import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.SeasonInfo
import com.example.data.model.StreamOption
import kotlinx.coroutines.Dispatchers
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

    /** Set from the Console config (RemoteConsole). Blank = Server 2 worker not set up. */
    @Volatile var baseUrl: String = ""

    val enabled: Boolean get() = baseUrl.startsWith("https://")

    data class Detail(
        val title: String,
        val poster: String,
        val synopsis: String,
        val referer: String,
        val streams: List<StreamOption>,
        val episodes: List<EpisodeInfo>,
        val episodePaths: Map<Pair<Int, Int>, String>
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
            val series = Regex("(?i)season|episode|\\bS\\d+|web.?series|\\bseries\\b").containsMatchIn(title + " " + path)
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
        }.distinctBy { it.id }
    }

    suspend fun home(): List<MediaItem> = withContext(Dispatchers.IO) { items(get("/home")) }

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
        val sArr = j.optJSONArray("streams") ?: JSONArray()
        val streams = (0 until sArr.length()).mapNotNull { i ->
            val o = sArr.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url")
            if (!url.startsWith("http")) return@mapNotNull null
            val hls = o.optString("type").equals("hls", true) || url.contains(".m3u8")
            val quality = o.optString("quality").ifBlank { if (hls) "Auto" else "HD" }
            StreamOption(
                id = "s2_$i",
                label = "$quality • Server 2",
                quality = quality,
                url = url,
                format = if (hls) "HLS" else "MP4",
                headers = headers
            )
        }
        val eArr = j.optJSONArray("episodes") ?: JSONArray()
        val paths = LinkedHashMap<Pair<Int, Int>, String>()
        val episodes = (0 until eArr.length()).mapNotNull { i ->
            val o = eArr.optJSONObject(i) ?: return@mapNotNull null
            val se = o.optInt("season", 1).coerceAtLeast(1)
            val ep = o.optInt("episode", 0)
            if (ep <= 0) return@mapNotNull null
            paths[se to ep] = o.optString("path")
            EpisodeInfo(season = se, episode = ep, title = o.optString("title").ifBlank { "Episode $ep" })
        }
        Detail(j.optString("title"), j.optString("poster"), j.optString("synopsis"), referer, streams, episodes, paths)
    }

    /** Detail page for the app's title screen. */
    suspend fun mediaDetail(item: MediaItem): MediaDetail? {
        val d = detail(item.detailPath.ifBlank { item.id }) ?: return null
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
        val main = detail(item.detailPath.ifBlank { item.id }) ?: return emptyList()
        if (episode <= 0 || main.episodePaths.isEmpty()) return main.streams
        val epPath = main.episodePaths[season.coerceAtLeast(1) to episode] ?: return main.streams
        return detail(epPath)?.streams?.ifEmpty { main.streams } ?: main.streams
    }
}
