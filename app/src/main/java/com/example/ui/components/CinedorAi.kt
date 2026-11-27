package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import android.content.Context
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One chat bubble. Cinedor Ai's bubbles can carry posters the viewer can tap. */
data class AiMessage(val fromUser: Boolean, val text: String, val items: List<MediaItem> = emptyList())

/**
 * Cinedor Ai: describe a movie or series in your own words (Bangla, Banglish or English) and it
 * finds it. The worker's AI turns the description into likely titles; the app then looks those
 * titles up in Cinedor's own catalog, so every suggestion is something you can actually watch.
 */
object CinedorAi {
    private const val DEFAULT_URL = "https://fragrant-dust-19c9.khanbhai19u.workers.dev/ai"

    /** True while the chat is on screen (the bottom navigation bar hides). */
    var isOpen by mutableStateOf(false)

    /** Catalog search, set by MainActivity (the screen itself has no view model). */
    var searcher: (suspend (String) -> List<MediaItem>)? = null

    data class Answer(val reply: String, val titles: List<String>, val keywords: List<String>)

    suspend fun ask(context: Context, history: List<AiMessage>): Answer = withContext(Dispatchers.IO) {
        val token = CinedorCloud.authToken(context)
            ?: throw IllegalStateException("সাইন-ইন হচ্ছে না। ইন্টারনেট চেক করুন। (Admin: Firebase এ Anonymous sign-in চালু আছে কিনা দেখুন)")
        val msgs = JSONArray()
        history.takeLast(8).forEach { m ->
            msgs.put(JSONObject().put("role", if (m.fromUser) "user" else "assistant").put("content", m.text))
        }
        val conn = (URL(DEFAULT_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            conn.outputStream.use { it.write(JSONObject().put("messages", msgs).toString().toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val j = runCatching { JSONObject(body) }.getOrNull() ?: throw IllegalStateException("Cinedor Ai is busy. Try again.")
            if (!j.optBoolean("ok")) throw IllegalStateException(j.optString("error").ifBlank { "Cinedor Ai is busy. Try again." })
            fun list(name: String) = j.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
            Answer(j.optString("reply"), list("titles"), list("keywords"))
        } finally {
            conn.disconnect()
        }
    }

    /** Looks every suggested title up in the catalog; best match of each title first. */
    suspend fun findPosters(answer: Answer, search: suspend (String) -> List<MediaItem>): List<MediaItem> = coroutineScope {
        val queries = (answer.titles + answer.keywords).map { it.replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "").trim() }
            .filter { it.length >= 2 }.distinct().take(8)
        val results = queries.map { q ->
            async { q to (kotlinx.coroutines.withTimeoutOrNull(12_000) { runCatching { search(q) }.getOrDefault(emptyList()) } ?: emptyList()) }
        }.awaitAll()
        fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9\\u0980-\\u09ff]+"), " ").trim()
        val out = LinkedHashMap<String, MediaItem>()
        // First pass: titles that really match what the AI named.
        results.forEach { (q, list) ->
            val nq = norm(q)
            list.firstOrNull { norm(it.title).contains(nq) || nq.contains(norm(it.title)) }?.let { out.putIfAbsent(it.source + it.id, it) }
        }
        // Then a few more results from each search.
        results.forEach { (_, list) -> list.take(3).forEach { out.putIfAbsent(it.source + it.id, it) } }
        out.values.take(12).toList()
    }
}

/** Chat kept while the app is open, so leaving and coming back keeps the conversation. */
private val chatHistory = mutableStateListOf<AiMessage>()

/**
 * Full-screen Cinedor Ai chat, like a messenger: header with the Cinedor logo, chat bubbles,
 * posters you can tap, animated typing dots and a message bar that sits above the keyboard.
 */
@Composable
fun CinedorAiScreen(
    onBack: () -> Unit,
    onSearch: suspend (String) -> List<MediaItem>,
    onOpenMedia: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = chatHistory
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    androidx.activity.compose.BackHandler(onBack = onBack)
    // The bottom navigation bar hides while the chat is open.
    androidx.compose.runtime.DisposableEffect(Unit) {
        CinedorAi.isOpen = true
        onDispose { CinedorAi.isOpen = false }
    }
    LaunchedEffect(messages.size, busy) {
        val last = messages.size + (if (busy) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || busy) return
        messages.add(AiMessage(true, q))
        input = ""
        busy = true
        scope.launch {
            val reply = runCatching {
                val answer = kotlinx.coroutines.withTimeout(50_000) { CinedorAi.ask(context, messages.toList()) }
                // Poster lookup never holds the answer back for long.
                val posters = kotlinx.coroutines.withTimeoutOrNull(20_000) { CinedorAi.findPosters(answer, onSearch) }.orEmpty()
                val text2 = when {
                    posters.isNotEmpty() -> answer.reply.ifBlank { "এগুলো দেখুন 👇" }
                    answer.titles.isNotEmpty() -> answer.reply +
                        "\n\nএই নামগুলো পেয়েছি, কিন্তু এখন Cinedor এ নেই: " + answer.titles.joinToString(", ")
                    else -> answer.reply.ifBlank { "দুঃখিত, বুঝতে পারিনি। আরেকটু বিস্তারিত বলবেন?" }
                }
                AiMessage(false, text2.trim(), posters)
            }.getOrElse {
                val msg = if (it is kotlinx.coroutines.TimeoutCancellationException) "Cinedor Ai উত্তর দিতে দেরি করছে। একটু পরে আবার চেষ্টা করুন।"
                else it.message ?: "Cinedor Ai এখন ব্যস্ত। একটু পরে আবার চেষ্টা করুন।"
                AiMessage(false, "⚠️ $msg")
            }
            messages.add(reply)
            busy = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .glassBackground()
            // Shrink above the keyboard (or the phone's navigation bar), never push the header away.
            .windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars)
                    .only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom)
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .statusBarsPadding()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Box {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.cinedor_logo),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White).padding(4.dp)
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape)
                        .background(Color.White).padding(2.dp).clip(CircleShape).background(Color(0xFF22C55E))
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Cinedor Ai", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(if (busy) "লিখছে…" else "অনলাইন • মুভি খুঁজে দেয়", color = if (busy) CinetorRed else TextSecondary, fontSize = 12.sp)
            }
            if (messages.isNotEmpty()) {
                androidx.compose.material3.IconButton(onClick = {
                    if (!busy) messages.clear()
                }) { Icon(Icons.Rounded.Refresh, contentDescription = "New chat", tint = TextSecondary) }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(messages) { _, m -> AiBubble(m, onOpenMedia) }
            if (busy) item { TypingBubble() }
        }

        // Message bar, above the keyboard and the phone's navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)
                .liquidGlass(RoundedCornerShape(28.dp))
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it.take(400) },
                placeholder = { Text("মুভির নাম বা গল্প লিখুন…", color = TextMuted) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CinetorRed
                )
            )
            Box(
                Modifier.size(44.dp).glassAccent(CircleShape).clickable(enabled = !busy && input.isNotBlank()) { send(input) },
                contentAlignment = Alignment.Center
            ) {
                if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AiAvatar() {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.cinedor_logo),
        contentDescription = null,
        modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White).padding(3.dp)
    )
}

@Composable
private fun AiBubble(m: AiMessage, onOpenMedia: (MediaItem) -> Unit) {
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (m.fromUser) 20.dp else 6.dp, bottomEnd = if (m.fromUser) 6.dp else 20.dp
    )
    if (m.fromUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.widthIn(max = 290.dp).glassAccent(shape).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(m.text, color = Color.White, fontSize = 14.5.sp, lineHeight = 20.sp)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        AiAvatar()
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            if (m.text.isNotBlank()) {
                Box(Modifier.widthIn(max = 290.dp).glassBubble(shape).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(m.text, color = TextPrimary, fontSize = 14.5.sp, lineHeight = 20.sp)
                }
            }
            if (m.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(m.items.distinctBy { it.source + it.id }, key = { it.source + it.id }) { item ->
                        Column(Modifier.width(104.dp).clickable { onOpenMedia(item) }) {
                            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).glassRim(RoundedCornerShape(14.dp))) {
                                FastPosterImage(url = item.posterUrl, contentDescription = item.title, thumbWidth = 240, modifier = Modifier.fillMaxSize())
                            }
                            Text(item.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                            if (item.year.isNotBlank()) Text(item.year, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Three bouncing dots while Cinedor Ai thinks. */
@Composable
private fun TypingBubble() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.Bottom) {
        AiAvatar()
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.glassBubble(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val y by t.animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        androidx.compose.animation.core.keyframes {
                            durationMillis = 900
                            0f at (i * 150)
                            1f at (i * 150 + 200)
                            0f at (i * 150 + 400)
                        }
                    ),
                    label = "dot$i"
                )
                Box(Modifier.padding(bottom = (y * 6).dp).size(8.dp).clip(CircleShape).background(CinetorRed.copy(alpha = 0.55f + y * 0.45f)))
            }
        }
    }
}
