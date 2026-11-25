package com.example.ui.components

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * Shows Cinedor's own download notifications (progress bar, speed, time left,
 * and "Download complete") while the app is running.
 */
object DownloadNotifier {
    private const val CHANNEL = "cinedor_downloads"
    private const val BASE_ID = 7_000
    private val seenRunning = HashSet<Long>()
    private val completedShown = HashSet<Long>()

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

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Runs while the app is open; updates notifications every second. */
    suspend fun run(context: Context) {
        val app = context.applicationContext
        ensureChannel(app)
        val nm = NotificationManagerCompat.from(app)
        while (true) {
            val entries = runCatching { DownloadStore.entries(app) }.getOrDefault(emptyList())
            DownloadSpeedTracker.update(entries)
            if (canNotify(app)) {
                entries.forEach { e ->
                    val id = e.record.id
                    val nid = BASE_ID + (id % 100_000).toInt()
                    when (e.state) {
                        DownloadState.RUNNING, DownloadState.QUEUED, DownloadState.PAUSED -> {
                            seenRunning.add(id)
                            val bps = DownloadSpeedTracker.speeds[id] ?: 0L
                            val pct = (e.progress * 100).toInt()
                            val left = e.totalBytes - e.downloadedBytes
                            val text = when (e.state) {
                                DownloadState.QUEUED -> "Waiting…"
                                DownloadState.PAUSED -> "Paused · waiting for network"
                                else -> buildString {
                                    append("$pct%")
                                    if (bps > 0) append(" · ${Formatter.formatShortFileSize(app, bps)}/s")
                                    if (bps > 0 && left > 0) append(" · ${formatEta(left / bps)} left")
                                }
                            }
                            val n = NotificationCompat.Builder(app, CHANNEL)
                                .setSmallIcon(android.R.drawable.stat_sys_download)
                                .setContentTitle("${e.record.title} · ${e.record.quality}")
                                .setContentText(text)
                                .setSubText(
                                    if (e.totalBytes > 0)
                                        "${Formatter.formatShortFileSize(app, e.downloadedBytes)} / ${Formatter.formatShortFileSize(app, e.totalBytes)}"
                                    else null
                                )
                                .setProgress(100, pct, e.totalBytes <= 0)
                                .setOngoing(true)
                                .setOnlyAlertOnce(true)
                                .setContentIntent(openAppIntent(app))
                                .build()
                            runCatching { nm.notify(nid, n) }
                        }
                        DownloadState.DONE -> {
                            if (id in seenRunning && id !in completedShown) {
                                completedShown.add(id)
                                val n = NotificationCompat.Builder(app, CHANNEL)
                                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                    .setContentTitle("Download complete")
                                    .setContentText("${e.record.title} · ${e.record.quality} — watch it in Me › Downloads")
                                    .setAutoCancel(true)
                                    .setContentIntent(openAppIntent(app))
                                    .build()
                                runCatching { nm.notify(nid, n) }
                            }
                        }
                        DownloadState.FAILED -> {
                            if (id in seenRunning) {
                                seenRunning.remove(id)
                                val n = NotificationCompat.Builder(app, CHANNEL)
                                    .setSmallIcon(android.R.drawable.stat_notify_error)
                                    .setContentTitle("Download failed")
                                    .setContentText("${e.record.title} · ${e.record.quality}")
                                    .setAutoCancel(true)
                                    .setContentIntent(openAppIntent(app))
                                    .build()
                                runCatching { nm.notify(nid, n) }
                            }
                        }
                    }
                }
            }
            val active = entries.any { it.state != DownloadState.DONE && it.state != DownloadState.FAILED }
            delay(if (active) 1000L else 3000L)
        }
    }
}
