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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    /** Rounded-corner copy of a poster so it looks like a card in the notification. */
    private fun rounded(src: Bitmap, radius: Float = 18f): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.shader = android.graphics.BitmapShader(src, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        canvas.drawRoundRect(android.graphics.RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), radius, radius, paint)
        return out
    }

    private fun scaled(b: Bitmap, maxH: Int = 300): Bitmap =
        if (b.height <= maxH) b else Bitmap.createScaledBitmap(b, (b.width * maxH.toFloat() / b.height).toInt().coerceAtLeast(1), maxH, true)

    /** Centre-crops a poster to the 2:3 shape of the notification tiles so rounded corners stay visible. */
    private fun crop23(b: Bitmap): Bitmap {
        val targetW = (b.height * 2f / 3f).toInt()
        return when {
            targetW in 1 until b.width -> Bitmap.createBitmap(b, (b.width - targetW) / 2, 0, targetW, b.height)
            b.width < targetW -> {
                val targetH = (b.width * 3f / 2f).toInt().coerceAtMost(b.height)
                Bitmap.createBitmap(b, 0, (b.height - targetH) / 2, b.width, targetH)
            }
            else -> b
        }
    }

    /** Small rounded tile; kept small because a notification can only carry about 1 MB of pictures. */
    private fun tile(b: Bitmap, h: Int): Bitmap = rounded(scaled(crop23(b), h), 14f)

    /** Downloads several posters at the same time (keeps their order). */
    private fun loadAll(urls: List<String>): List<Bitmap?> {
        if (urls.isEmpty()) return emptyList()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(minOf(5, urls.size))
        return try {
            urls.map { u -> pool.submit<Bitmap?> { loadBitmap(u) } }
                .map { f -> runCatching { f.get(15, java.util.concurrent.TimeUnit.SECONDS) }.getOrNull() }
        } finally {
            pool.shutdownNow()
        }
    }

    /**
     * Shows a notification like the big streaming apps:
     * - one poster: title with a small poster on the right; expanded, the description and a big poster
     * - several posters ([images]): title with three posters in a row; expanded, up to ten posters
     *   in two rows of five, the tenth marked "More"
     */
    fun show(context: Context, title: String, body: String, imageUrl: String?, link: String?, images: List<String> = emptyList()) {
        createChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!link.isNullOrBlank()) putExtra("cinedor_link", link)
        } ?: return
        val pi = PendingIntent.getActivity(
            context, (System.currentTimeMillis() % 100000).toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun base() = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_logo_mono)
            .setColor(0xFFE50914.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)

        val urls = images.filter { it.startsWith("http") }.distinct().take(10)
        val id = (System.currentTimeMillis() % 1_000_000).toInt()
        val nm = NotificationManagerCompat.from(context)
        val gone = android.view.View.GONE
        val visible = android.view.View.VISIBLE

        if (urls.size >= 2) {
            val bitmaps = loadAll(urls).filterNotNull()
            if (bitmaps.size >= 2) {
                val small = android.widget.RemoteViews(context.packageName, com.example.R.layout.notif_multi)
                small.setTextViewText(com.example.R.id.n_title, title)
                val smallIds = listOf(com.example.R.id.n_p1, com.example.R.id.n_p2, com.example.R.id.n_p3)
                smallIds.forEachIndexed { i, vid ->
                    val bmp = bitmaps.getOrNull(i)
                    if (bmp != null) small.setImageViewBitmap(vid, tile(bmp, 120)) else small.setViewVisibility(vid, gone)
                }

                fun bigView(maxTiles: Int, tileH: Int): android.widget.RemoteViews {
                    val big = android.widget.RemoteViews(context.packageName, com.example.R.layout.notif_multi_big)
                    big.setTextViewText(com.example.R.id.n_title, title)
                    if (body.isBlank()) big.setViewVisibility(com.example.R.id.n_body, gone)
                    else big.setTextViewText(com.example.R.id.n_body, body)
                    val gridIds = listOf(
                        com.example.R.id.n_g1, com.example.R.id.n_g2, com.example.R.id.n_g3, com.example.R.id.n_g4, com.example.R.id.n_g5,
                        com.example.R.id.n_g6, com.example.R.id.n_g7, com.example.R.id.n_g8, com.example.R.id.n_g9, com.example.R.id.n_g10
                    )
                    val shown = bitmaps.take(maxTiles)
                    // A full row of five looks tidy; with fewer than five, empty slots stay as space.
                    gridIds.forEachIndexed { i, vid ->
                        val bmp = shown.getOrNull(i)
                        if (bmp != null) big.setImageViewBitmap(vid, tile(bmp, tileH))
                        else big.setViewVisibility(vid, android.view.View.INVISIBLE)
                    }
                    if (shown.size <= 5) big.setViewVisibility(com.example.R.id.n_row2, gone)
                    if (shown.size >= 10) big.setViewVisibility(com.example.R.id.n_more, visible)
                    else big.setViewVisibility(com.example.R.id.n_g10_box, if (shown.size > 9) visible else android.view.View.INVISIBLE)
                    return big
                }

                // Try the full picture grid first; if the phone refuses it as too large, use smaller tiles.
                for ((maxTiles, tileH) in listOf(10 to 132, 10 to 96, 5 to 96)) {
                    val ok = runCatching {
                        nm.notify(
                            id, base()
                                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                                .setCustomContentView(small)
                                .setCustomBigContentView(bigView(maxTiles, tileH))
                                .build()
                        )
                    }.isSuccess
                    if (ok) return
                }
                runCatching { nm.notify(id, base().setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).build()) }
                return
            }
        }

        val poster = loadBitmap(imageUrl?.ifBlank { null } ?: urls.firstOrNull())
        if (poster != null) {
            val small = android.widget.RemoteViews(context.packageName, com.example.R.layout.notif_poster)
            small.setTextViewText(com.example.R.id.n_title, title)
            small.setImageViewBitmap(com.example.R.id.n_poster, tile(poster, 120))
            val big = android.widget.RemoteViews(context.packageName, com.example.R.layout.notif_poster_big)
            big.setTextViewText(com.example.R.id.n_title, title)
            if (body.isBlank()) big.setViewVisibility(com.example.R.id.n_body, gone)
            else big.setTextViewText(com.example.R.id.n_body, body)
            big.setImageViewBitmap(com.example.R.id.n_poster, rounded(scaled(crop23(poster), 300), 20f))
            runCatching {
                nm.notify(
                    id, base()
                        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(small)
                        .setCustomBigContentView(big)
                        .build()
                )
            }
        } else {
            runCatching { nm.notify(id, base().setStyle(NotificationCompat.BigTextStyle().bigText(body)).build()) }
        }
    }
}

/** Notifications received on this phone, shown in the app's 🔔 inbox. */
data class InboxItem(val title: String, val body: String, val image: String, val link: String, val at: Long)

object NotificationInbox {
    private const val PREFS = "cinedor_inbox"
    var unread by androidx.compose.runtime.mutableIntStateOf(0)
    private var loaded = false

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        unread = prefs(c).getInt("unread", 0)
    }

    fun items(c: Context): List<InboxItem> = runCatching {
        val arr = org.json.JSONArray(prefs(c).getString("items", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            InboxItem(o.optString("title"), o.optString("body"), o.optString("image"), o.optString("link"), o.optLong("at"))
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(c: Context, title: String, body: String, image: String?, link: String?) {
        val list = items(c).toMutableList()
        list.add(0, InboxItem(title, body, image.orEmpty(), link.orEmpty(), System.currentTimeMillis()))
        val arr = org.json.JSONArray()
        list.take(60).forEach {
            arr.put(org.json.JSONObject().put("title", it.title).put("body", it.body).put("image", it.image).put("link", it.link).put("at", it.at))
        }
        val n = prefs(c).getInt("unread", 0) + 1
        prefs(c).edit().putString("items", arr.toString()).putInt("unread", n).apply()
        unread = n
    }

    private var remoteUnread = 0

    /** Unread admin notifications from the cloud are added to the badge. */
    fun setRemoteUnread(c: Context, n: Int) {
        remoteUnread = n
        unread = prefs(c).getInt("unread", 0) + n
    }

    fun markAllRead(c: Context) {
        prefs(c).edit().putInt("unread", 0).apply()
        c.getSharedPreferences("cinedor_cloud", Context.MODE_PRIVATE).edit()
            .putLong("inbox_seen_at", System.currentTimeMillis()).apply()
        remoteUnread = 0
        unread = 0
    }

    /** Local + cloud notifications merged, newest first, without duplicates. */
    fun merge(local: List<InboxItem>, remote: List<InboxItem>): List<InboxItem> =
        (local + remote)
            .distinctBy { it.title.trim().lowercase() + "|" + (it.at / 600_000L) }
            .distinctBy { it.title.trim().lowercase() + "|" + it.body.trim().lowercase() }
            .sortedByDescending { it.at }

    fun clear(c: Context) {
        prefs(c).edit().putString("items", "[]").putInt("unread", 0).apply()
        unread = 0
    }
}

/** Text from a tapped notification ("Open in app"): the app searches for it. */
object PushLink {
    var pending by androidx.compose.runtime.mutableStateOf<String?>(null)
    /** A page inside Me (ai, downloads, premium, settings, history, feedback, about) to open. */
    var mePage by androidx.compose.runtime.mutableStateOf<String?>(null)
}

/** Receives FCM messages. Poster (image) notifications are shown with a big picture. */
class CinedorMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val data = message.data
        val title = data["title"] ?: n?.title ?: "Cinedor"
        val body = data["body"] ?: n?.body ?: ""
        val image = data["image"] ?: n?.imageUrl?.toString()
        val images = data["images"].orEmpty().split(",").map { it.trim() }.filter { it.startsWith("http") }
        NotificationInbox.add(applicationContext, title, body, image?.ifBlank { null } ?: images.firstOrNull(), data["link"])
        CinedorPush.show(applicationContext, title, body, image, data["link"], images)
    }

    override fun onNewToken(token: String) {
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(CinedorPush.TOPIC_ALL) }
    }
}
