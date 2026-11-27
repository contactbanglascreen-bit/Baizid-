package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** One link of a channel (a channel can have several links; the next one is tried if one fails). */
data class TvSource(val url: String, val quality: String, val userAgent: String)

data class TvChannel(
    val id: String,
    val name: String,
    val group: String,
    val logo: String,
    val sources: List<TvSource>
)

/**
 * Live TV channels from an M3U playlist. The playlist ships inside the app (assets/tv_channels.m3u)
 * and can be replaced without a new build by setting "tvPlaylistUrl" in the Console config.
 */
object TvChannels {
    /** Set from the Console config (RemoteConsole). Blank = use the playlist inside the app. */
    @Volatile var playlistUrl: String = ""

    val groups = listOf("All", "News", "Entertainment", "Sports", "Music", "Kids", "Movies")

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile private var cache: List<TvChannel>? = null

    suspend fun load(context: Context, force: Boolean = false): List<TvChannel> = withContext(Dispatchers.IO) {
        if (!force) cache?.let { return@withContext it }
        val remote = playlistUrl.takeIf { it.startsWith("http") }?.let { url ->
            runCatching {
                http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (r.isSuccessful) r.body?.string() else null
                }
            }.getOrNull()
        }
        val text = remote?.takeIf { it.contains("#EXTINF") }
            ?: runCatching { context.assets.open("tv_channels.m3u").bufferedReader().use { it.readText() } }.getOrDefault("")
        parse(text).also { if (it.isNotEmpty()) cache = it }
    }

    /** Reads an M3U playlist; links of the same channel are joined, best quality first. */
    fun parse(text: String): List<TvChannel> {
        data class Entry(val name: String, val tvgId: String, val group: String, val logo: String, val quality: String, val ua: String, val url: String)
        val entries = mutableListOf<Entry>()
        var info: String? = null
        var ua = ""
        text.lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> { info = line; ua = "" }
                line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter("=").trim()
                line.startsWith("http", true) && info != null -> {
                    val inf = info!!
                    val title = inf.substringAfterLast(",").trim()
                    val quality = Regex("\\((\\d{3,4}p)\\)").find(title)?.groupValues?.get(1).orEmpty()
                    val name = title.replace(Regex("\\s*\\(\\d{3,4}p\\)\\s*"), " ").replace(Regex("\\s*\\[[^]]*]"), "").trim()
                    fun attr(key: String) = Regex("$key=\"([^\"]*)\"").find(inf)?.groupValues?.get(1).orEmpty()
                    entries += Entry(name, attr("tvg-id"), attr("group-title"), attr("tvg-logo"), quality, ua, line)
                    info = null
                    ua = ""
                }
            }
        }
        return entries.groupBy { it.tvgId.substringBefore("@").ifBlank { it.name.lowercase() } }
            .map { (key, list) ->
                val first = list.first()
                val sources = list
                    .sortedWith(compareByDescending<Entry> { it.url.startsWith("https") }.thenByDescending { it.quality.removeSuffix("p").toIntOrNull() ?: 0 })
                    .map { TvSource(it.url, it.quality.ifBlank { "Live" }, it.ua) }
                    .distinctBy { it.url }
                TvChannel(
                    id = "tv_" + key.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                    name = first.name,
                    group = first.group.ifBlank { groupOf(first.name) },
                    logo = list.firstOrNull { it.logo.isNotBlank() }?.logo.orEmpty(),
                    sources = sources
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    /** Sorts a channel into a group from its name (playlists often have no group). */
    fun groupOf(name: String): String {
        val n = name.lowercase()
        return when {
            listOf("news", "ekhon", "dbc", "somoy", "jamuna", "independent", "ekattor", "channel 24", "71").any { it in n } -> "News"
            listOf("sport", "gazi", "ten ", "star sports", "ptv").any { it in n } -> "Sports"
            "music" in n || "gaan" in n -> "Music"
            listOf("duronto", "kids", "cartoon", "pogo", "nick").any { it in n } -> "Kids"
            "movie" in n || "cinema" in n -> "Movies"
            else -> "Entertainment"
        }
    }
}
