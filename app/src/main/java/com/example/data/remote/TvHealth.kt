package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Checks which TV links are working right now, from the user's own phone and network, so only
 * active channels are shown. Results are remembered for a few hours (working links longer than
 * broken ones) so opening the TV tab again is instant.
 */
object TvHealth {
    private const val ALIVE_TTL_MS = 6L * 60 * 60 * 1000
    private const val DEAD_TTL_MS = 2L * 60 * 60 * 1000
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** url -> (alive, checked at). */
    private val results = ConcurrentHashMap<String, Pair<Boolean, Long>>()
    @Volatile private var loaded = false

    /** The checked list from this app session, so the TV tab opens instantly the next time. */
    @Volatile var lastChecked: List<TvChannel>? = null

    /** Turned off in screenshot tests, which have no real internet. */
    @Volatile var enabled = true

    private fun file(context: Context) = File(context.filesDir, "tv_health.txt")

    private fun loadSaved(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            file(context).takeIf { it.exists() }?.readLines()?.forEach { line ->
                val p = line.split('\t')
                if (p.size == 3) p[2].toLongOrNull()?.let { results[p[0]] = (p[1] == "1") to it }
            }
        }
    }

    private fun save(context: Context) {
        runCatching {
            val now = System.currentTimeMillis()
            val tmp = File(context.filesDir, "tv_health.txt.tmp")
            tmp.bufferedWriter().use { w ->
                results.forEach { (url, r) ->
                    if (now - r.second < ALIVE_TTL_MS) w.append(url).append('\t').append(if (r.first) "1" else "0").append('\t').append(r.second.toString()).append('\n')
                }
            }
            tmp.renameTo(file(context))
        }
    }

    /** Remembered answer for a link, or null when it hasn't been checked lately. */
    private fun known(url: String): Boolean? {
        val r = results[url] ?: return null
        val age = System.currentTimeMillis() - r.second
        return if (age < (if (r.first) ALIVE_TTL_MS else DEAD_TTL_MS)) r.first else null
    }

    /** Opens the link and looks at the first bytes: a playlist, a DASH file or video data means it works. */
    private fun probe(src: TvSource): Boolean = runCatching {
        val req = Request.Builder().url(src.url)
            .header("User-Agent", src.userAgent.ifBlank { UA })
            .apply { if (src.referer.isNotBlank()) header("Referer", src.referer) }
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@use false
            val type = r.header("Content-Type").orEmpty().lowercase()
            val buf = ByteArray(2048)
            val n = r.body?.byteStream()?.let { input ->
                var total = 0
                while (total < buf.size) {
                    val read = input.read(buf, total, buf.size - total)
                    if (read <= 0) break
                    total += read
                }
                total
            } ?: 0
            if (n <= 0) return@use false
            val head = String(buf, 0, n, Charsets.ISO_8859_1)
            head.contains("#EXTM3U") || head.contains("<MPD") ||
                buf[0] == 0x47.toByte() || // MPEG-TS video
                type.contains("video") || type.contains("mp2t")
        }
    }.getOrDefault(false)

    private fun alive(src: TvSource): Boolean {
        known(src.url)?.let { return it }
        val ok = probe(src)
        results[src.url] = ok to System.currentTimeMillis()
        return ok
    }

    /**
     * Keeps channels as they are, but drops links already known to be broken and channels whose
     * links are all known to be broken. Used to show the list straight away.
     */
    fun applyKnown(context: Context, channels: List<TvChannel>): List<TvChannel> {
        loadSaved(context)
        return channels.mapNotNull { ch ->
            val left = ch.sources.filter { known(it.url) != false }
            if (left.isEmpty()) null else ch.copy(sources = left.sortedByDescending { known(it.url) == true })
        }
    }

    /**
     * Checks every channel (a few at a time). For each channel the links are tried in order until
     * one works; broken links are removed and the working one goes first. Channels with no working
     * link are left out. [onProgress] gets the count checked so far and the list as it stands.
     */
    suspend fun check(
        context: Context,
        channels: List<TvChannel>,
        onProgress: suspend (done: Int, total: Int, current: List<TvChannel>) -> Unit
    ): List<TvChannel> = withContext(Dispatchers.IO) {
        loadSaved(context)
        val total = channels.size
        val done = AtomicInteger(0)
        val working = ConcurrentHashMap<String, TvChannel>()
        val dead = ConcurrentHashMap.newKeySet<String>()
        val gate = Semaphore(12)
        fun current() = channels.mapNotNull { ch -> if (ch.id in dead) null else working[ch.id] ?: ch }
        coroutineScope {
            channels.map { ch ->
                async {
                    gate.withPermit {
                        val idx = ch.sources.indexOfFirst { alive(it) }
                        if (idx < 0) dead.add(ch.id) else {
                            val rest = ch.sources.drop(idx + 1).filter { known(it.url) != false }
                            working[ch.id] = ch.copy(sources = listOf(ch.sources[idx]) + rest)
                        }
                        val d = done.incrementAndGet()
                        if (d % 40 == 0 || d == total) onProgress(d, total, current())
                    }
                }
            }.awaitAll()
        }
        save(context)
        current().also { lastChecked = it }
    }
}
