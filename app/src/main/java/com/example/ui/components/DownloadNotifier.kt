package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One Cinedor download notification (progress bar, speed, time left, then "complete").
 * Runs in a foreground service so it keeps updating even after the app is closed.
 * The system Download Manager's own notification is hidden, so there is only one.
 */
object DownloadNotifier {
    internal const val CHANNEL = "cinedor_downloads"
    internal const val FG_ID = 6_999
    private const val BASE_ID = 7_000

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Video download progress"
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    internal fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun openAppIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Starts the notification service if any download is in progress. Safe to call often. */
    fun ensureRunning(context: Context) {
        val app = context.applicationContext
        val active = runCatching { DownloadStore.entries(app) }.getOrDefault(emptyList())
            .any { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED }
        if (!active || DownloadWatchService.running) return
        ensureChannel(app)
        runCatching {
            ContextCompat.startForegroundService(app, Intent(app, DownloadWatchService::class.java))
        }
    }

    internal fun progressNotification(context: Context, e: DownloadEntry, othersActive: Int): android.app.Notification {
        val bps = DownloadSpeedTracker.speeds[e.record.id] ?: 0L
        val pct = (e.progress * 100).toInt()
        val left = e.totalBytes - e.downloadedBytes
        val text = when (e.state) {
            DownloadState.QUEUED -> "Waiting…"
            DownloadState.PAUSED -> "Paused · waiting for network"
            else -> buildString {
                append("$pct%")
                if (bps > 0) append(" · ${Formatter.formatShortFileSize(context, bps)}/s")
                if (bps > 0 && left > 0) append(" · ${formatEta(left / bps)} left")
            }
        }
        val sub = buildString {
            if (e.totalBytes > 0) {
                append(Formatter.formatShortFileSize(context, e.downloadedBytes))
                append(" / ")
                append(Formatter.formatShortFileSize(context, e.totalBytes))
            }
            if (othersActive > 0) {
                if (isNotEmpty()) append(" · ")
                append("+$othersActive more")
            }
        }
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("${e.record.title} · ${e.record.quality}")
            .setContentText(text)
            .setSubText(sub.ifBlank { null })
            .setProgress(100, pct, e.totalBytes <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    internal fun resultNotification(context: Context, e: DownloadEntry, ok: Boolean): android.app.Notification =
        NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (ok) "Download complete" else "Download failed")
            .setContentText(
                if (ok) "${e.record.title} · ${e.record.quality} — watch it in Me › Downloads"
                else "${e.record.title} · ${e.record.quality}"
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()

    internal fun resultId(id: Long): Int = BASE_ID + (id % 100_000).toInt()

    /** Kept for compatibility: just makes sure the service runs while the app is open. */
    suspend fun run(context: Context) {
        while (true) {
            ensureRunning(context)
            delay(4000L)
        }
    }
}

/** Foreground service that keeps the single download notification updated. */
class DownloadWatchService : Service() {
    companion object {
        @Volatile
        var running = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val seenActive = HashSet<Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        CinedorDownloader.resumeInterrupted(this)
        val placeholder = NotificationCompat.Builder(this, DownloadNotifier.CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Cinedor downloads")
            .setContentText("Preparing…")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(DownloadNotifier.openAppIntent(this))
            .build()
        runCatching {
            ServiceCompat.startForeground(
                this,
                DownloadNotifier.FG_ID,
                placeholder,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
        }.onFailure {
            running = false
            stopSelf()
            return START_NOT_STICKY
        }
        if (job == null) job = scope.launch { loop() }
        return START_STICKY
    }

    private suspend fun loop() {
        val nm = NotificationManagerCompat.from(this)
        while (true) {
            val entries = runCatching { DownloadStore.entries(this) }.getOrDefault(emptyList())
            DownloadSpeedTracker.update(entries)
            // Paused downloads don't keep the notification/service running.
            val active = entries.filter { it.state == DownloadState.RUNNING || it.state == DownloadState.QUEUED }
            active.forEach { seenActive.add(it.record.id) }

            if (DownloadNotifier.canNotify(this)) {
                entries.forEach { e ->
                    val id = e.record.id
                    if (id in seenActive && e.state == DownloadState.DONE) {
                        seenActive.remove(id)
                        runCatching { nm.notify(DownloadNotifier.resultId(id), DownloadNotifier.resultNotification(this, e, true)) }
                    } else if (id in seenActive && e.state == DownloadState.FAILED) {
                        seenActive.remove(id)
                        runCatching { nm.notify(DownloadNotifier.resultId(id), DownloadNotifier.resultNotification(this, e, false)) }
                    }
                }
            }

            if (active.isEmpty()) break
            val main = active.firstOrNull { it.state == DownloadState.RUNNING } ?: active.first()
            runCatching {
                nm.notify(DownloadNotifier.FG_ID, DownloadNotifier.progressNotification(this, main, active.size - 1))
            }
            delay(1000L)
        }
        running = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }
}
