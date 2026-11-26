package com.example.ui.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

/**
 * Push notifications from the Cinedor admin panel (Firebase Cloud Messaging).
 * Every phone subscribes to the "all" topic; the panel sends text or poster notifications to it.
 */
object CinedorPush {
    const val CHANNEL_ID = "cinedor_news"
    const val TOPIC_ALL = "all"
    private const val TAG = "CinedorPush"

    // Firebase project "cinedor-60aac". Used when the app has no google-services.json resource.
    private const val PROJECT_ID = "cinedor-60aac"
    private const val APP_ID = "1:337658275844:android:b228fc4737bc8acaf8ed51"
    private const val API_KEY = "AIzaSyCddEpMsakf5vHeLlHuJLMPUCC50uTYOpY"
    private const val SENDER_ID = "337658275844"
    private const val STORAGE_BUCKET = "cinedor-60aac.firebasestorage.app"

    fun ensureFirebase(context: Context): Boolean = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId(PROJECT_ID)
                .setApplicationId(APP_ID)
                .setApiKey(API_KEY)
                .setGcmSenderId(SENDER_ID)
                .setStorageBucket(STORAGE_BUCKET)
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
        }
        true
    }.onFailure { Log.w(TAG, "Firebase init failed: ${it.message}") }.getOrDefault(false)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "New movies & news", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "New releases, updates and announcements from Cinedor"
                }
            )
        }
    }

    /** Call once on app start. */
    fun start(context: Context) {
        createChannel(context)
        if (!ensureFirebase(context)) return
        runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL)
                .addOnCompleteListener { t -> Log.i(TAG, "Subscribed to '$TOPIC_ALL': ${t.isSuccessful}") }
        }.onFailure { Log.w(TAG, "FCM subscribe failed: ${it.message}") }
    }

    private fun loadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
            }
            c.inputStream.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    fun show(context: Context, title: String, body: String, imageUrl: String?, link: String?) {
        createChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!link.isNullOrBlank()) putExtra("cinedor_link", link)
        } ?: return
        val pi = PendingIntent.getActivity(
            context, (System.currentTimeMillis() % 100000).toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val poster = loadBitmap(imageUrl)
        val b = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_logo_mono)
            .setColor(0xFFE50914.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
        if (poster != null) {
            b.setLargeIcon(poster)
            b.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(poster)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }
        runCatching {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 1_000_000).toInt(), b.build())
        }
    }
}

/** Receives FCM messages. Poster (image) notifications are shown with a big picture. */
class CinedorMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val data = message.data
        val title = data["title"] ?: n?.title ?: "Cinedor"
        val body = data["body"] ?: n?.body ?: ""
        val image = data["image"] ?: n?.imageUrl?.toString()
        CinedorPush.show(applicationContext, title, body, image, data["link"])
    }

    override fun onNewToken(token: String) {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(CinedorPush.TOPIC_ALL) }
    }
}
