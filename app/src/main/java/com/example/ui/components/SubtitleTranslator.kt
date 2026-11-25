package com.example.ui.components

import android.content.Context
import android.net.Uri
import com.example.data.model.CaptionTrack
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A language the user can translate subtitles into. */
data class SubtitleLanguage(val code: String, val name: String)

/**
 * AI subtitle translation: takes any existing subtitle (SRT / VTT), translates every line on the
 * phone with Google ML Kit, and returns a new subtitle track in the chosen language.
 */
object SubtitleTranslator {
    val languages = listOf(
        SubtitleLanguage(TranslateLanguage.BENGALI, "Bengali"),
        SubtitleLanguage(TranslateLanguage.HINDI, "Hindi"),
        SubtitleLanguage(TranslateLanguage.ENGLISH, "English"),
        SubtitleLanguage(TranslateLanguage.URDU, "Urdu"),
        SubtitleLanguage(TranslateLanguage.ARABIC, "Arabic"),
        SubtitleLanguage(TranslateLanguage.TAMIL, "Tamil"),
        SubtitleLanguage(TranslateLanguage.TELUGU, "Telugu"),
        SubtitleLanguage(TranslateLanguage.MARATHI, "Marathi"),
        SubtitleLanguage(TranslateLanguage.INDONESIAN, "Indonesian"),
        SubtitleLanguage(TranslateLanguage.MALAY, "Malay"),
        SubtitleLanguage(TranslateLanguage.SPANISH, "Spanish"),
        SubtitleLanguage(TranslateLanguage.FRENCH, "French"),
        SubtitleLanguage(TranslateLanguage.PORTUGUESE, "Portuguese"),
        SubtitleLanguage(TranslateLanguage.GERMAN, "German"),
        SubtitleLanguage(TranslateLanguage.TURKISH, "Turkish"),
        SubtitleLanguage(TranslateLanguage.RUSSIAN, "Russian"),
        SubtitleLanguage(TranslateLanguage.CHINESE, "Chinese"),
        SubtitleLanguage(TranslateLanguage.JAPANESE, "Japanese"),
        SubtitleLanguage(TranslateLanguage.KOREAN, "Korean"),
        SubtitleLanguage(TranslateLanguage.THAI, "Thai"),
        SubtitleLanguage(TranslateLanguage.VIETNAMESE, "Vietnamese")
    )

    private data class Cue(val index: String, val timing: String, val text: String)

    private fun sourceCode(track: CaptionTrack): String {
        val raw = (track.language.ifBlank { track.label }).lowercase()
        val byName = languages.firstOrNull { raw.contains(it.name.lowercase()) }?.code
        if (byName != null) return byName
        val tag = raw.take(2)
        return TranslateLanguage.fromLanguageTag(tag) ?: TranslateLanguage.ENGLISH
    }

    private fun readText(context: Context, url: String): String {
        if (url.startsWith("content://") || url.startsWith("file://")) {
            return context.contentResolver.openInputStream(Uri.parse(url))!!.bufferedReader().use { it.readText() }
        }
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 20000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) Chrome/124.0 Mobile Safari/537.36")
        }
        try {
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Parses SRT or WebVTT into cues (timing kept as-is, converted to SRT format). */
    private fun parse(text: String): List<Cue> {
        val blocks = text.replace("\r\n", "\n").replace("\r", "\n").split(Regex("\n\\s*\n"))
        val cues = mutableListOf<Cue>()
        blocks.forEach { block ->
            val lines = block.trim().lines().filter { it.isNotBlank() }
            val ti = lines.indexOfFirst { "-->" in it }
            if (ti < 0) return@forEach
            val timing = lines[ti]
                .replace(Regex("(\\d{2}:\\d{2})\\.(\\d{3})"), "$1,$2") // VTT dot -> SRT comma
                .replace(Regex("^(\\d{2}:\\d{2},\\d{3})"), "00:$1")
                .replace(Regex("--> (\\d{2}:\\d{2},\\d{3})"), "--> 00:$1")
                .replace(Regex("\\s+(align|line|position|size|vertical):\\S+"), "")
                .trim()
            val body = lines.drop(ti + 1).joinToString("\n")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\\{\\\\[^}]*\\}"), "")
                .trim()
            if (body.isNotBlank()) cues.add(Cue((cues.size + 1).toString(), timing, body))
        }
        return cues
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * Translates [source] into [target]. [onProgress] gets 0..1 (model download counts as the start).
     * Returns a ready-to-use subtitle track.
     */
    suspend fun translate(
        context: Context,
        source: CaptionTrack,
        target: SubtitleLanguage,
        onProgress: (Float) -> Unit
    ): CaptionTrack = withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, "ai_subtitles").apply { mkdirs() }
        val outFile = File(outDir, "${source.url.hashCode()}_${target.code}.srt")
        if (outFile.exists() && outFile.length() > 0) {
            onProgress(1f)
            return@withContext CaptionTrack(
                id = "ai_${target.code}_${source.id}",
                language = target.code,
                label = "${target.name} (AI)",
                url = Uri.fromFile(outFile).toString()
            )
        }
        val cues = parse(readText(context, source.url))
        if (cues.isEmpty()) throw IllegalStateException("This subtitle couldn't be read")
        val src = sourceCode(source)
        if (src == target.code) throw IllegalStateException("Subtitle is already in ${target.name}")
        val translator: Translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(target.code).build()
        )
        try {
            onProgress(0.02f)
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitTask()
            val sb = StringBuilder()
            cues.forEachIndexed { i, cue ->
                val translated = runCatching { translator.translate(cue.text).awaitTask() }.getOrDefault(cue.text)
                sb.append(i + 1).append('\n').append(cue.timing).append('\n').append(translated).append("\n\n")
                if (i % 10 == 0) onProgress(0.05f + 0.95f * (i + 1) / cues.size)
            }
            outFile.writeText(sb.toString())
            onProgress(1f)
        } finally {
            translator.close()
        }
        CaptionTrack(
            id = "ai_${target.code}_${source.id}",
            language = target.code,
            label = "${target.name} (AI)",
            url = Uri.fromFile(outFile).toString()
        )
    }
}
