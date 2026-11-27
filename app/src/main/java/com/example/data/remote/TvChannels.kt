package com.example.data.remote

import android.content.Context
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** One link of a channel (a channel can have several links; the next one is tried if one fails). */
data class TvSource(val url: String, val quality: String, val userAgent: String, val referer: String = "")

data class TvChannel(
    val id: String,
    val name: String,
    val group: String,
    val logo: String,
    val sources: List<TvSource>,
    /** tvg-id without the feed part, e.g. "ATNBangla.bd" (used to find the official logo). */
    val tvgId: String = "",
    /** Filters the channel shows up under (country, language and kind), e.g. Bangladesh, Bangla, News. */
    val tags: Set<String> = emptySet()
)

/**
 * Live TV channels from M3U playlists:
 *  - the Bangladesh playlist inside the app (assets/tv_channels.m3u),
 *  - the India playlist (iptv-org), read online and kept on the phone so it also works when the site is slow,
 *  - an extra playlist set as "tvPlaylistUrl" in the Console config.
 * Official channel logos come from the iptv-org logo list, matched by tvg-id.
 */
object TvChannels {
    /** Set from the Console config (RemoteConsole). Blank = no extra playlist. */
    @Volatile var playlistUrl: String = ""

    private const val INDIA_URL = "https://iptv-org.github.io/iptv/countries/in.m3u"
    private const val LOGOS_URL = "https://iptv-org.github.io/api/logos.json"
    private const val LOGO_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    val groups = listOf(
        "All", "Bangladesh", "Bangla", "Sports", "News", "Entertainment", "Movies", "Kids", "Music",
        "Documentary", "Devotional", "India", "International"
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var cache: List<TvChannel>? = null
    @Volatile private var logoCache: Map<String, String>? = null

    private fun fetch(url: String): String? = runCatching {
        http.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    }.getOrNull()

    /** Reads a playlist online; keeps the last good copy on the phone and uses it when the site can't be reached. */
    private fun remotePlaylist(context: Context, url: String, file: String): String? {
        val saved = File(context.filesDir, file)
        val fresh = fetch(url)?.takeIf { it.contains("#EXTINF") }
        if (fresh != null) {
            runCatching { saved.writeText(fresh) }
            return fresh
        }
        return runCatching { saved.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    suspend fun load(context: Context, force: Boolean = false): List<TvChannel> = withContext(Dispatchers.IO) {
        if (!force) cache?.let { return@withContext it }
        coroutineScope {
            val india = async { remotePlaylist(context, INDIA_URL, "tv_india.m3u").orEmpty() }
            val extra = async {
                playlistUrl.takeIf { it.startsWith("http") }?.let { remotePlaylist(context, it, "tv_extra.m3u") }.orEmpty()
            }
            fun asset(name: String) = runCatching {
                context.assets.open(name).bufferedReader().use { it.readText() }
            }.getOrDefault("")
            val bundled = asset("tv_channels.m3u") + "\n" + asset("tv_more.m3u")
            val list = parse(bundled + "\n" + extra.await() + "\n" + india.await())
            list.also { if (it.isNotEmpty()) cache = it }
        }
    }

    /** Same list with the official logos filled in (logos are downloaded once a week). */
    suspend fun withLogos(context: Context, channels: List<TvChannel>): List<TvChannel> = withContext(Dispatchers.IO) {
        val logos = logos(context)
        if (logos.isEmpty()) return@withContext channels
        channels.map { ch ->
            if (ch.logo.isNotBlank()) ch
            else ch.copy(logo = findLogo(logos, ch))
        }.also { cache = it }
    }

    /** Short form used to match names: "Zee Bangla HD" and "ZeeBangla.in" both become "zeebangla". */
    private fun norm(s: String): String = s.lowercase()
        .replace("&", "and").replace("+", "plus")
        .replace(Regex("\\b(hd|sd|uhd|4k|tv|channel|television|live|india|uk|bd)\\b"), "")
        .replace(Regex("[^a-z0-9]"), "")

    /** Exact channel id first, then the same channel in another country, then by its name. */
    private fun findLogo(logos: Map<String, String>, ch: TvChannel): String {
        val id = ch.tvgId.lowercase()
        if (id.isNotBlank()) {
            logos[id]?.let { return it }
            logos["#" + id.substringBeforeLast(".")]?.let { return it }
        }
        val n = norm(ch.name)
        if (n.length >= 4) logos["~$n"]?.let { return it }
        return ""
    }

    private fun logos(context: Context): Map<String, String> {
        logoCache?.let { return it }
        val file = File(context.filesDir, "tv_logos2.txt")
        val stale = !file.exists() || System.currentTimeMillis() - file.lastModified() > LOGO_MAX_AGE_MS
        if (stale) downloadLogos(file)
        val map = runCatching {
            file.readLines().mapNotNull { line ->
                val i = line.indexOf('\t')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
            }.toMap()
        }.getOrDefault(emptyMap())
        if (map.isNotEmpty()) logoCache = map
        return map
    }

    /**
     * The logo list is large, so it is read piece by piece and only a small "channel → logo" file
     * is kept. Each logo is stored under its exact id ("zeebangla.in"), under the id without the
     * country ("#zeebangla") and under its short name ("~zeebangla"), so channels from playlists
     * with other ids still find their logo. PNG/JPG and the main logo (no feed) are preferred.
     */
    private fun downloadLogos(target: File) {
        runCatching {
            http.newCall(Request.Builder().url(LOGOS_URL).build()).execute().use { r ->
                if (!r.isSuccessful) return
                val best = HashMap<String, Pair<Int, String>>()
                fun offer(key: String, score: Int, url: String) {
                    if ((best[key]?.first ?: -1) < score) best[key] = score to url
                }
                JsonReader(r.body!!.charStream()).use { jr ->
                    jr.beginArray()
                    while (jr.hasNext()) {
                        var channel = ""; var feed = ""; var url = ""; var format = ""; var width = 0
                        jr.beginObject()
                        while (jr.hasNext()) {
                            val key = jr.nextName()
                            if (jr.peek() == android.util.JsonToken.NULL) { jr.skipValue(); continue }
                            when (key) {
                                "channel" -> channel = jr.nextString()
                                "feed" -> feed = jr.nextString()
                                "url" -> url = jr.nextString()
                                "format" -> format = jr.nextString()
                                "width" -> width = runCatching { jr.nextInt() }.getOrElse { jr.skipValue(); 0 }
                                else -> jr.skipValue()
                            }
                        }
                        jr.endObject()
                        if (channel.isBlank() || !url.startsWith("http")) continue
                        val svg = format.equals("SVG", true) || url.endsWith(".svg", true)
                        val score = (if (feed.isBlank()) 200_000 else 0) + (if (svg) 0 else 100_000) + width.coerceIn(0, 99_999)
                        val id = channel.lowercase()
                        offer(id, score, url)
                        offer("#" + id.substringBeforeLast("."), score, url)
                        // "AnandaTV" -> "Ananda TV" so it matches the playlist name "Ananda TV".
                        val words = channel.substringBeforeLast(".").replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
                        val n = norm(words)
                        if (n.length >= 4) offer("~$n", score, url)
                    }
                    jr.endArray()
                }
                if (best.isNotEmpty()) {
                    val tmp = File(target.parentFile, target.name + ".tmp")
                    tmp.bufferedWriter().use { w -> best.forEach { (k, v) -> w.append(k).append('\t').append(v.second).append('\n') } }
                    tmp.renameTo(target)
                }
            }
        }
    }

    /** Reads M3U playlists; links of the same channel are joined, best quality first. */
    fun parse(text: String): List<TvChannel> {
        data class Entry(
            val name: String, val tvgId: String, val group: String, val logo: String,
            val quality: String, val ua: String, val referer: String, val url: String
        )
        val entries = mutableListOf<Entry>()
        var info: String? = null
        var ua = ""
        var ref = ""
        text.lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> { info = line; ua = ""; ref = "" }
                line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfterLast("user-agent=").trim()
                line.startsWith("#EXTVLCOPT:http-referrer=", true) -> ref = line.substringAfter("referrer=").trim()
                line.startsWith("http", true) && info != null -> {
                    val inf = info!!
                    val title = inf.substringAfterLast(",").trim()
                    val quality = Regex("\\((\\d{3,4}[pi])\\)").find(title)?.groupValues?.get(1)?.replace('i', 'p').orEmpty()
                    val name = title.replace(Regex("\\s*\\(\\d{3,4}[pi]\\)\\s*"), " ").replace(Regex("\\s*\\[[^]]*]"), "").trim()
                    fun attr(key: String) = Regex("$key=\"([^\"]*)\"").find(inf)?.groupValues?.get(1).orEmpty()
                    if (name.isNotBlank()) entries += Entry(name, attr("tvg-id"), attr("group-title"), attr("tvg-logo"), quality, ua, ref, line)
                    info = null
                    ua = ""
                    ref = ""
                }
            }
        }
        return entries.groupBy { it.tvgId.ifBlank { it.name.lowercase() }.lowercase() }
            .map { (key, list) ->
                val first = list.first()
                val sources = list
                    .sortedWith(
                        compareByDescending<Entry> { it.url.startsWith("https") }
                            .thenByDescending { it.quality.removeSuffix("p").toIntOrNull() ?: 0 }
                    )
                    .map { TvSource(it.url, it.quality.ifBlank { "Live" }, it.ua, it.referer) }
                    .distinctBy { it.url }
                val baseId = first.tvgId.substringBefore("@")
                val country = baseId.substringAfterLast(".", "").lowercase()
                val group = first.group.takeIf { it in groups } ?: groupOf(first.name)
                TvChannel(
                    id = "tv_" + key.replace(Regex("[^a-z0-9]+"), "_"),
                    name = first.name,
                    group = group,
                    logo = list.firstOrNull { it.logo.isNotBlank() }?.logo.orEmpty(),
                    sources = sources,
                    tvgId = baseId,
                    tags = tagsOf(first.name, country, group)
                )
            }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<TvChannel> { "Bangladesh" in it.tags }
                    .thenByDescending { "Bangla" in it.tags }
                    .thenBy { it.name.lowercase() }
            )
    }

    private fun tagsOf(name: String, country: String, group: String): Set<String> = buildSet {
        add(group)
        if (country == "bd") add("Bangladesh")
        if (country == "in") add("India")
        if (country.isNotBlank() && country != "bd" && country != "in") add("International")
        val n = name.lowercase()
        if (country == "bd" || listOf("bangla", "kolkata", "jalsha", "ananda", "ghanta", "spondon", "aath", "aakaash", "channel s uk").any { it in n }) {
            add("Bangla")
        }
    }

    /** Sorts a channel into a kind from its name (playlists often have no group). */
    fun groupOf(name: String): String {
        val n = name.lowercase()
        fun has(vararg k: String) = k.any { it in n }
        return when {
            has("kids", "kidz", "cartoon", "pogo", "nick", "duronto", "yay", "jungle book", "green gold", "peques",
                "cbbc", "cbeebies", "tiny pop", "jimjam", "mr bean", "planet fun", "baby") || n == "pop" -> "Kids"
            has("iqra", "islam", "eman channel", "takbeer", "noor tv", "deen tv", "ahlulbayt", "madani", "peace tv") -> "Devotional"
            has("discovery", "national geographic", "nat geo", "animal planet", "bbc earth", "history", "pbs america",
                "quest", "really", "space live", "foodxp", "food food", "travelxp", "docubay", "horse & country", "master chef") -> "Documentary"
            has("sport", "kabaddi", "khel", "dazn", "fifa", "mutv", "hard knocks", "ski tv", "setanta", "pickleball", "ten ", "gazi", "ptv sports") -> "Sports"
            has("aastha", "bhakti", "bhakthi", "bhajan", "sanskar", "satsang", "gurbani", "gurbaani", "angel tv", "hosanna",
                "shalom", "calvary", "mercy tv", "svbc", "vedic", "dharm", "jinvani", "peace of mind", "harvest tv", "goodness",
                "sadhna", "divya", "krsna", "madha tv", "shubh tv", "god", "jesus", "church", "sai tv", "sairam", "nanaksar",
                "hazur", "shaheed", "darshan", "sikh", "salvation", "nambikkai", "aradana", "aaseervatham", "subhavaartha",
                "divyavani", "nireekshana", "awakening", "hope channel", "zainabia", "shekinah", "sankara", "santvani",
                "sharnam", "ishwar", "hindu dharmam", "om tv", "chintpurni", "anandpur", "dukhnivaran", "amrit gurbani", "madani",
                "sarv dharam", "sarv dharm", "lakshya", "power vision", "powervision", "joy tv", "grace tv", "mahimai", "arputhar") -> "Devotional"
            has("news", "khabar", "samachar", "aaj tak", "ndtv", "times now", "republic", "tv9", "ekhon", "dbc", "somoy",
                "jamuna", "independent", "ekattor", "channel 24", "cnbc", "wion", "zee 24", "sansad", "mirror now",
                "india today", "abp", "et now", "bharat24", "bharat express", "taaza", "ibc 24", "pratidin", "polimer news",
                "puthiya", "thanthi", "sakshi", "hmtv", "ntv telugu", "v6 ", "t news", "manorama news", "mathrubhumi",
                "reporter tv", "media one", "kairali news", "janam", "24 ghanta", "kolkata tv", "spondon", "prag ", "dy 365",
                "bansal", "india voice", "inh ", "first india", "argus", "kalinga", "kanak", "odisha tv", "jai maharashtra",
                "lokshahi", "saam tv", "pudhari", "zee business", "news18", "rt india", "toi global", "bloomberg",
                "talktv", "sky news") -> "News"
            has("music", "9xm", "mastiii", "beats", "gaane", "hits", "tunes", "sangeet", "balle balle", "isai", "zing",
                "jalwa", "jhakaas", "tashan", "mtv", "gaan", "dhoom", "raga", "songdew", "insync", "e 24", "b4u kadak",
                "totalmusic", "afrobeats") -> "Music"
            has("movie", "cinema", "cineplex", "flix", "goldmines", "b4u bhojpuri", "pitaara", "dhinchaak", "manoranjan",
                "shubh cinema", "jalsha movies", "bollygold", "the movie club", "anmol", "zee action", "big magic", "dangal 2", "epic tv",
                "talking pictures") -> "Movies"
            else -> "Entertainment"
        }
    }
}
