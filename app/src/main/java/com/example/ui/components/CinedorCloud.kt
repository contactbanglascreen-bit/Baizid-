package com.example.ui.components

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FeedbackItem(
    val id: String,
    val category: String,
    val message: String,
    val status: String,
    val reply: String,
    val createdAt: String,
    val fixedAt: String
) {
    val isFixed: Boolean get() = status == "fixed"
}

/**
 * Small cloud features on Firebase (free Spark plan), without extra SDKs:
 * - anonymous sign-in per phone (Firebase Auth REST)
 * - device stats for the admin (country + phone model, no personal data)
 * - feedback: users send problems, the admin marks them fixed and the user is told
 */
object CinedorCloud {
    private const val TAG = "CinedorCloud"
    private const val PROJECT = "cinedor-60aac"
    private const val API_KEY = "AIzaSyCddEpMsakf5vHeLlHuJLMPUCC50uTYOpY"
    private const val PREFS = "cinedor_cloud"
    private const val DOCS = "https://firestore.googleapis.com/v1/projects/$PROJECT/databases/(default)/documents"

    private var idToken: String? = null
    private var expiresAt = 0L

    private data class Res(val code: Int, val body: String)

    private fun http(url: String, method: String = "GET", body: String? = null, bearer: String? = null, form: Boolean = false): Res {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
            connectTimeout = 12000
            readTimeout = 15000
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", if (form) "application/x-www-form-urlencoded" else "application/json")
            }
        }
        try {
            if (body != null) c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Res(code, text)
        } finally {
            c.disconnect()
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun uid(context: Context): String = prefs(context).getString("uid", "").orEmpty()

    /** Signs this phone in anonymously (once) and keeps a fresh ID token. */
    private fun token(context: Context): String? {
        if (idToken != null && System.currentTimeMillis() < expiresAt) return idToken
        val p = prefs(context)
        val refresh = p.getString("refresh", "").orEmpty()
        val r = if (refresh.isNotBlank()) {
            http(
                "https://securetoken.googleapis.com/v1/token?key=$API_KEY", "POST",
                "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refresh, "UTF-8"), form = true
            )
        } else {
            http("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$API_KEY", "POST", "{\"returnSecureToken\":true}")
        }
        if (r.code !in 200..299) {
            Log.w(TAG, "Sign-in failed ${r.code}: ${r.body.take(200)}")
            if (refresh.isNotBlank() && r.code == 400) p.edit().remove("refresh").apply()
            return null
        }
        val j = JSONObject(r.body)
        val tok = j.optString("idToken").ifBlank { j.optString("id_token") }
        val newRefresh = j.optString("refreshToken").ifBlank { j.optString("refresh_token") }
        val uid = j.optString("localId").ifBlank { j.optString("user_id") }
        val exp = (j.optString("expiresIn").ifBlank { j.optString("expires_in") }).toLongOrNull() ?: 3600L
        p.edit().putString("refresh", newRefresh).putString("uid", uid).apply()
        idToken = tok
        expiresAt = System.currentTimeMillis() + (exp - 120) * 1000L
        return tok
    }

    private fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    private fun str(v: String) = JSONObject().put("stringValue", v)

    private fun countryCode(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val net = runCatching { tm?.networkCountryIso }.getOrNull().orEmpty()
        val sim = runCatching { tm?.simCountryIso }.getOrNull().orEmpty()
        return (net.ifBlank { sim }.ifBlank { Locale.getDefault().country }).uppercase(Locale.US)
    }

    /** Called on app start: saves country + phone model (at most every 6 hours). */
    suspend fun reportDevice(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val p = prefs(context)
            val last = p.getLong("reported_at", 0L)
            if (System.currentTimeMillis() - last < 6 * 3600_000L) return@runCatching
            val tok = token(context) ?: return@runCatching
            val uid = uid(context)
            val cc = countryCode(context)
            val fields = JSONObject()
                .put("country", str(cc))
                .put("countryName", str(Locale("", cc).getDisplayCountry(Locale.ENGLISH).ifBlank { cc }))
                .put("brand", str(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }))
                .put("model", str(Build.MODEL))
                .put("android", str(Build.VERSION.RELEASE ?: ""))
                .put("appVersion", str(com.example.BuildConfig.VERSION_NAME))
                .put("lastSeen", JSONObject().put("timestampValue", nowIso()))
            val mask = listOf("country", "countryName", "brand", "model", "android", "appVersion", "lastSeen")
                .joinToString("&") { "updateMask.fieldPaths=$it" }
            val r = http("$DOCS/devices/$uid?$mask", "PATCH", JSONObject().put("fields", fields).toString(), tok)
            if (r.code in 200..299) p.edit().putLong("reported_at", System.currentTimeMillis()).apply()
            else Log.w(TAG, "Device report failed ${r.code}: ${r.body.take(200)}")
            // Personal topic so the admin can tell this phone when its feedback is fixed.
            runCatching { com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("u_$uid") }
            Unit
        }
    }

    suspend fun sendFeedback(context: Context, category: String, message: String, contact: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val tok = token(context) ?: return@runCatching false
            val fields = JSONObject()
                .put("uid", str(uid(context)))
                .put("category", str(category))
                .put("message", str(message.take(2000)))
                .put("contact", str(contact.take(200)))
                .put("status", str("open"))
                .put("reply", str(""))
                .put("device", str("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"))
                .put("country", str(countryCode(context)))
                .put("appVersion", str(com.example.BuildConfig.VERSION_NAME))
                .put("createdAt", JSONObject().put("timestampValue", nowIso()))
            val r = http("$DOCS/feedback", "POST", JSONObject().put("fields", fields).toString(), tok)
            if (r.code !in 200..299) Log.w(TAG, "Feedback failed ${r.code}: ${r.body.take(200)}")
            r.code in 200..299
        }.getOrDefault(false)
    }

    private fun field(o: JSONObject?, name: String): String {
        val f = o?.optJSONObject(name) ?: return ""
        return f.optString("stringValue").ifBlank { f.optString("timestampValue") }
    }

    /** This phone's own feedback, newest first. */
    suspend fun myFeedback(context: Context): List<FeedbackItem> = withContext(Dispatchers.IO) {
        runCatching {
            val tok = token(context) ?: return@runCatching emptyList()
            val query = JSONObject().put(
                "structuredQuery", JSONObject()
                    .put("from", JSONArray().put(JSONObject().put("collectionId", "feedback")))
                    .put(
                        "where", JSONObject().put(
                            "fieldFilter", JSONObject()
                                .put("field", JSONObject().put("fieldPath", "uid"))
                                .put("op", "EQUAL")
                                .put("value", str(uid(context)))
                        )
                    )
                    .put("limit", 50)
            )
            val r = http("$DOCS:runQuery", "POST", query.toString(), tok)
            if (r.code !in 200..299) return@runCatching emptyList()
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { i ->
                val doc = arr.optJSONObject(i)?.optJSONObject("document") ?: return@mapNotNull null
                val f = doc.optJSONObject("fields")
                FeedbackItem(
                    id = doc.optString("name").substringAfterLast('/'),
                    category = field(f, "category"),
                    message = field(f, "message"),
                    status = field(f, "status"),
                    reply = field(f, "reply"),
                    createdAt = field(f, "createdAt"),
                    fixedAt = field(f, "fixedAt")
                )
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    /** Notifications the admin sent (stored in Firestore), newest first, for the in-app inbox. */
    suspend fun announcements(context: Context): List<InboxItem> = withContext(Dispatchers.IO) {
        runCatching {
            // Public list (rules allow reading notifications without login); token only as a fallback.
            val tok: String? = null
            val query = JSONObject().put(
                "structuredQuery", JSONObject()
                    .put("from", JSONArray().put(JSONObject().put("collectionId", "notifications")))
                    .put("orderBy", JSONArray().put(JSONObject().put("field", JSONObject().put("fieldPath", "at")).put("direction", "DESCENDING")))
                    .put("limit", 40)
            )
            var r = http("$DOCS:runQuery?key=$API_KEY", "POST", query.toString(), tok)
            if (r.code !in 200..299) {
                val t = token(context) ?: return@runCatching emptyList()
                r = http("$DOCS:runQuery", "POST", query.toString(), t)
            }
            if (r.code !in 200..299) return@runCatching emptyList()
            val arr = JSONArray(r.body)
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            (0 until arr.length()).mapNotNull { i ->
                val f = arr.optJSONObject(i)?.optJSONObject("document")?.optJSONObject("fields") ?: return@mapNotNull null
                val at = runCatching { iso.parse(field(f, "at").take(19))?.time }.getOrNull() ?: 0L
                InboxItem(field(f, "title"), field(f, "body"), field(f, "image"), field(f, "link"), at)
            }
        }.getOrDefault(emptyList())
    }

    /** On app start: counts admin notifications the user hasn't seen yet (for the 🔔 badge). */
    suspend fun refreshInboxBadge(context: Context) {
        val list = announcements(context)
        if (list.isEmpty()) return
        val p = prefs(context)
        val seenAt = p.getLong("inbox_seen_at", 0L)
        val fresh = list.count { it.at > seenAt }
        NotificationInbox.setRemoteUnread(context, fresh)
    }

    /** On app start: tells the user (notification) about feedback the admin marked as fixed. */
    suspend fun checkFixed(context: Context) {
        val items = myFeedback(context).filter { it.isFixed }
        if (items.isEmpty()) return
        val p = prefs(context)
        val seen = p.getStringSet("seen_fixed", emptySet()).orEmpty()
        val fresh = items.filter { it.id !in seen }
        fresh.firstOrNull()?.let { f ->
            NotificationInbox.add(context, "✅ Your problem is fixed", f.reply.ifBlank { f.message.take(80) }, null, null)
            CinedorPush.show(
                context,
                "✅ Your problem is fixed",
                f.reply.ifBlank { "\"${f.message.take(60)}\" — thanks for telling us!" },
                null, null
            )
        }
        if (fresh.isNotEmpty()) p.edit().putStringSet("seen_fixed", seen + fresh.map { it.id }).apply()
    }
}
