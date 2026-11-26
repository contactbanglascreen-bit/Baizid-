package com.example.ui.components

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves the details of a crash to a small file. On the next start the report is sent to the
 * admin's Feedback list (category "Crash report") and the file is removed.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { File(app.filesDir, FILE).writeText(describe(thread, error)) }
            // Let Android close the app as usual.
            previous?.uncaughtException(thread, error)
        }
    }

    /** Sends a saved report, if there is one. Called from a background thread at start-up. */
    suspend fun sendPending(context: Context) {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return
        val report = runCatching { file.readText() }.getOrDefault("").trim()
        if (report.isEmpty()) { file.delete(); return }
        val sent = CinedorCloud.sendFeedback(context, "Crash report", report.take(2000), "")
        if (sent) file.delete()
    }

    private fun describe(thread: Thread, error: Throwable): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val sb = StringBuilder()
        sb.append("CRASH ").append(time)
            .append(" · app ").append(com.example.BuildConfig.VERSION_NAME)
            .append(" (").append(com.example.BuildConfig.VERSION_CODE).append(")")
            .append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(" · Android ").append(Build.VERSION.RELEASE)
            .append(" · thread ").append(thread.name).append('\n')
        appendThrowable(sb, error, frames = 18)
        // The deepest "Caused by" usually names the real problem.
        var root = error.cause
        var depth = 0
        while (root?.cause != null && root.cause !== root && depth < 8) { root = root.cause; depth++ }
        if (root != null) {
            sb.append("\nRoot cause: ")
            appendThrowable(sb, root, frames = 12)
        }
        return sb.toString()
    }

    private fun appendThrowable(sb: StringBuilder, t: Throwable, frames: Int) {
        sb.append(t.javaClass.name).append(": ").append(t.message?.take(300) ?: "").append('\n')
        t.stackTrace.take(frames).forEach { f ->
            sb.append("  at ").append(f.className.removePrefix("com.example.")).append('.')
                .append(f.methodName).append(':').append(f.lineNumber).append('\n')
        }
    }
}
