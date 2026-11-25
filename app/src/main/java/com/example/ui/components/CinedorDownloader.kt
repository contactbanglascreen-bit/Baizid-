package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * Cinedor's own video downloader.
 * - Splits big files into 4 parts downloaded at the same time (much faster than one connection)
 * - Pause / Resume at any time, and continues after the app restarts
 * - Retries broken connections automatically
 */
object CinedorDownloader {
    private const val PREFS = "cinedor_downloader"
    private const val PARTS = 4
    private const val MIN_SPLIT_BYTES = 8L * 1024 * 1024
    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val lock = Any()

    private class Part(val start: Long, val end: Long, @Volatile var pos: Long)

    private class Task(
        val id: Long,
        val url: String,
        val headers: Map<String, String>,
        val filePath: String,
        var total: Long,
        var state: DownloadState,
        var parts: MutableList<Part>,
        var single: Boolean
    )

    private val tasks = ConcurrentHashMap<Long, Task>()
    private var loaded = false

    // ---------------------------------------------------------------- persistence

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun load(context: Context) {
        synchronized(lock) {
            if (loaded) return
            val raw = prefs(context).getString("tasks", null)
            if (raw != null) runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val h = o.optJSONObject("headers") ?: JSONObject()
                    val headers = h.keys().asSequence().associateWith { h.optString(it) }
                    val pa = o.optJSONArray("parts") ?: JSONArray()
                    val parts = MutableList(pa.length()) { j ->
                        val p = pa.getJSONObject(j)
                        Part(p.getLong("s"), p.getLong("e"), p.getLong("p"))
                    }
                    val t = Task(
                        id = o.getLong("id"),
                        url = o.getString("url"),
                        headers = headers,
                        filePath = o.getString("file"),
                        total = o.optLong("total", -1L),
                        state = runCatching { DownloadState.valueOf(o.optString("state")) }.getOrDefault(DownloadState.PAUSED),
                        parts = parts,
                        single = o.optBoolean("single", false)
                    )
                    tasks[t.id] = t
                }
            }
            loaded = true
        }
    }

    private fun save(context: Context) {
        synchronized(lock) {
            val arr = JSONArray()
            tasks.values.forEach { t ->
                val h = JSONObject()
                t.headers.forEach { (k, v) -> h.put(k, v) }
                val pa = JSONArray()
                t.parts.forEach { p -> pa.put(JSONObject().put("s", p.start).put("e", p.end).put("p", p.pos)) }
                arr.put(
                    JSONObject()
                        .put("id", t.id)
                        .put("url", t.url)
                        .put("headers", h)
                        .put("file", t.filePath)
                        .put("total", t.total)
                        .put("state", t.state.name)
                        .put("parts", pa)
                        .put("single", t.single)
                )
            }
            prefs(context).edit().putString("tasks", arr.toString()).apply()
        }
    }

    // ---------------------------------------------------------------- public API

    fun isOwn(id: Long): Boolean = id < 0

    /** Creates a new download and starts it. Returns the id used in DownloadStore. */
    fun create(context: Context, url: String, headers: Map<String, String>, filePath: String): Long {
        load(context)
        val id = -System.currentTimeMillis()
        tasks[id] = Task(id, url, headers, filePath, -1L, DownloadState.QUEUED, mutableListOf(), false)
        save(context)
        start(context, id)
        return id
    }

    /** (state, downloaded bytes, total bytes) */
    fun status(context: Context, id: Long): Triple<DownloadState, Long, Long> {
        load(context)
        val t = tasks[id] ?: return Triple(DownloadState.FAILED, 0L, 0L)
        val done = if (t.single || t.parts.isEmpty()) File(t.filePath).let { if (it.exists()) it.length() else 0L }
        else t.parts.sumOf { (it.pos - it.start).coerceAtLeast(0L) }
        val state = if (t.state == DownloadState.RUNNING && jobs[id]?.isActive != true) DownloadState.PAUSED else t.state
        return Triple(state, done, t.total)
    }

    fun pause(context: Context, id: Long) {
        load(context)
        val t = tasks[id] ?: return
        jobs.remove(id)?.cancel()
        if (t.state != DownloadState.DONE) t.state = DownloadState.PAUSED
        save(context)
    }

    fun resume(context: Context, id: Long) {
        load(context)
        val t = tasks[id] ?: return
        if (t.state == DownloadState.DONE) return
        t.state = DownloadState.QUEUED
        save(context)
        start(context, id)
        DownloadNotifier.ensureRunning(context)
    }

    fun delete(context: Context, id: Long) {
        load(context)
        jobs.remove(id)?.cancel()
        tasks.remove(id)?.let { runCatching { File(it.filePath).delete() } }
        save(context)
    }

    /** Restarts downloads that were running when the app was closed. */
    fun resumeInterrupted(context: Context) {
        load(context)
        tasks.values.filter { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED }
            .forEach { if (jobs[it.id]?.isActive != true) start(context, it.id) }
    }

    fun hasActive(context: Context): Boolean {
        load(context)
        return tasks.values.any { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED }
    }

    // ---------------------------------------------------------------- engine

    private fun open(t: Task, range: String?): HttpURLConnection {
        val c = URL(t.url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.instanceFollowRedirects = true
        t.headers.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) c.setRequestProperty(k, v) }
        if (t.headers.keys.none { it.equals("User-Agent", true) }) c.setRequestProperty("User-Agent", UA)
        if (range != null) c.setRequestProperty("Range", range)
        return c
    }

    private fun isMetered(context: Context): Boolean = runCatching {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
    }.getOrDefault(false)

    private fun start(context: Context, id: Long) {
        val app = context.applicationContext
        val t = tasks[id] ?: return
        if (jobs[id]?.isActive == true) return
        jobs[id] = scope.launch {
            if (AppSettings.wifiOnlyDownloads && isMetered(app)) {
                t.state = DownloadState.PAUSED
                save(app)
                return@launch
            }
            t.state = DownloadState.RUNNING
            save(app)
            try {
                prepare(t)
                save(app)
                val saver = launch {
                    while (isActive) {
                        delay(1500L)
                        save(app)
                    }
                }
                if (t.single) runSingle(t) else {
                    t.parts.filter { it.pos <= it.end }
                        .map { p -> async { runPart(t, p) } }
                        .awaitAll()
                }
                saver.cancel()
                val complete = if (t.single) true else t.parts.all { it.pos > it.end }
                t.state = if (complete) DownloadState.DONE else DownloadState.FAILED
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (t.state != DownloadState.DONE) t.state = DownloadState.PAUSED
                save(app)
                throw e
            } catch (e: Exception) {
                t.state = DownloadState.FAILED
            }
            save(app)
            jobs.remove(id)
        }
    }

    /** Finds the file size and whether the server allows parallel parts. */
    private fun prepare(t: Task) {
        if (t.total > 0 && (t.single || t.parts.isNotEmpty())) return
        var total = -1L
        var ranges = false
        val c = open(t, "bytes=0-0")
        try {
            val code = c.responseCode
            if (code == 206) {
                ranges = true
                total = c.getHeaderField("Content-Range")?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: -1L
            } else if (code in 200..299) {
                total = c.contentLengthLong
            } else {
                throw IllegalStateException("HTTP $code")
            }
        } finally {
            c.disconnect()
        }
        t.total = total
        val file = File(t.filePath).apply { parentFile?.mkdirs() }
        if (ranges && total >= MIN_SPLIT_BYTES) {
            RandomAccessFile(file, "rw").use { it.setLength(total) }
            val size = total / PARTS
            t.parts = MutableList(PARTS) { i ->
                val s = i * size
                val e = if (i == PARTS - 1) total - 1 else (s + size - 1)
                Part(s, e, s)
            }
            t.single = false
        } else {
            t.single = true
            t.parts = mutableListOf()
        }
    }

    private suspend fun runPart(t: Task, p: Part) {
        var attempts = 0
        while (p.pos <= p.end && coroutineContext.isActive) {
            try {
                val c = open(t, "bytes=${p.pos}-${p.end}")
                try {
                    if (c.responseCode != 206) throw IllegalStateException("No range support (${c.responseCode})")
                    RandomAccessFile(t.filePath, "rw").use { raf ->
                        raf.seek(p.pos)
                        c.inputStream.use { input ->
                            val buf = ByteArray(128 * 1024)
                            while (coroutineContext.isActive && p.pos <= p.end) {
                                val n = input.read(buf)
                                if (n < 0) break
                                val take = minOf(n.toLong(), p.end - p.pos + 1).toInt()
                                raf.write(buf, 0, take)
                                p.pos += take
                            }
                        }
                    }
                } finally {
                    c.disconnect()
                }
                attempts = 0
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts > 6) throw e
                delay(1500L * attempts)
            }
        }
    }

    /** Servers without range support: one stream (resumes by appending when possible). */
    private suspend fun runSingle(t: Task) {
        var attempts = 0
        while (coroutineContext.isActive) {
            val file = File(t.filePath)
            val have = if (file.exists()) file.length() else 0L
            if (t.total > 0 && have >= t.total) return
            try {
                val c = open(t, if (have > 0) "bytes=$have-" else null)
                try {
                    val code = c.responseCode
                    val append = have > 0 && code == 206
                    if (code !in 200..299) throw IllegalStateException("HTTP $code")
                    if (t.total <= 0) t.total = if (append) have + c.contentLengthLong else c.contentLengthLong
                    java.io.FileOutputStream(file, append).use { out ->
                        c.inputStream.use { input ->
                            val buf = ByteArray(128 * 1024)
                            while (coroutineContext.isActive) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                            }
                        }
                    }
                } finally {
                    c.disconnect()
                }
                if (!coroutineContext.isActive) return
                if (t.total <= 0 || File(t.filePath).length() >= t.total) return
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts > 6) throw e
                delay(1500L * attempts)
            }
        }
    }
}
