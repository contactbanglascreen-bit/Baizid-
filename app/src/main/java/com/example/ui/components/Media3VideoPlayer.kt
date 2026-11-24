package com.example.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.CaptionTrack
import com.example.data.model.StreamOption
import com.example.data.remote.MovieBoxDirectClient
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MediaBackground as ObsidianBackground
import com.example.ui.theme.MediaTextPrimary as TextPrimary
import com.example.ui.theme.MediaTextSecondary as TextSecondary
import kotlinx.coroutines.delay
import okhttp3.CipherSuite
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.TlsVersion
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val PLAYER_TAG = "CinePulsePlayer"

/**
 * Chrome-like TLS 1.2 cipher suite specification that avoids Android Conscrypt's default
 * TLS 1.3 + OkHttp JA3/HTTP2 fingerprint rejected with HTTP 426 on EdgeNext (enacdn.net) POPs.
 */
internal val chromeTls12Spec: ConnectionSpec by lazy {
    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .cipherSuites(
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256
        )
        .build()
}

@Volatile
internal var lastCdnErrorDiagnostic: String = ""

/**
 * Matches real Chrome <video> GET behavior on media CDNs:
 * - Always sends Range: bytes=0- when missing (ExoPlayer omits Range at position=0)
 * - Uses Chrome's Accept-Encoding: identity;q=1, *;q=0 instead of bare identity
 * - Strips unsolicited Origin header on video CDNs
 * - Captures exact CDN Server/Upgrade/Via headers on 4xx/5xx responses for clear diagnostics
 */
private val chromeVideoNetworkInterceptor = Interceptor { chain ->
    val original = chain.request()
    val host = original.url.host.lowercase()
    val isMediaCdn = host.contains("hakunaymatata.com") || host.contains("aoneroom.com")
    val builder = original.newBuilder()
    if (isMediaCdn) {
        if (original.header("Range") == null) {
            builder.header("Range", "bytes=0-")
        }
        builder.header("Accept-Encoding", "identity;q=1, *;q=0")
        builder.removeHeader("Origin")
    }
    val response = chain.proceed(builder.build())
    if (response.code >= 400) {
        val srv = response.header("Server").orEmpty().ifBlank { "unknown" }
        val upg = response.header("Upgrade").orEmpty()
        val via = response.header("Via").orEmpty()
        val snippet = runCatching {
            response.peekBody(256).string().replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim().take(90)
        }.getOrDefault("")
        val diag = buildString {
            append("HTTP ${response.code} ($host • Server: $srv")
            if (upg.isNotBlank()) append(" • Upgrade: $upg")
            if (via.isNotBlank()) append(" • Via: $via")
            append(")")
            if (snippet.isNotBlank()) append(" — $snippet")
        }
        lastCdnErrorDiagnostic = diag
        Log.w(PLAYER_TAG, "CDN error diagnostic: $diag")
    }
    response
}

private fun buildMediaOkHttpClient(
    dns: Dns,
    protocols: List<Protocol>,
    useChromeTls12: Boolean
): OkHttpClient {
    return OkHttpClient.Builder()
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .protocols(protocols)
        .apply {
            if (useChromeTls12) {
                connectionSpecs(listOf(chromeTls12Spec, ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            }
        }
        .dns(dns)
        .addNetworkInterceptor(chromeVideoNetworkInterceptor)
        .build()
}

// Primary streaming OkHttp client: Chrome TLS 1.2 + HTTP/1.1 + Chrome <video> Range interceptor
internal val sharedMediaOkHttpClient: OkHttpClient by lazy {
    buildMediaOkHttpClient(
        dns = MovieBoxDirectClient.TENGINE_CDN_DNS,
        protocols = listOf(Protocol.HTTP_1_1),
        useChromeTls12 = true
    )
}

// Global EdgeNext POP failover OkHttp client (SG / US / BD / JP POPs with Chrome TLS 1.2 + HTTP/1.1)
internal val edgePopMediaOkHttpClient: OkHttpClient by lazy {
    buildMediaOkHttpClient(
        dns = MovieBoxDirectClient.EDGE_POP_FALLBACK_DNS,
        protocols = listOf(Protocol.HTTP_1_1),
        useChromeTls12 = true
    )
}

// HTTP/2 fallback OkHttp client for servers that prefer H2
internal val h2MediaOkHttpClient: OkHttpClient by lazy {
    buildMediaOkHttpClient(
        dns = MovieBoxDirectClient.TENGINE_CDN_DNS,
        protocols = listOf(Protocol.HTTP_2, Protocol.HTTP_1_1),
        useChromeTls12 = true
    )
}

private enum class TransportEngine {
    OKHTTP_CHROME_H1,
    NATIVE_HTTP_URL_CONN,
    OKHTTP_EDGE_POP_H1,
    OKHTTP_H2,
    CHROMIUM_WEBVIEW
}

private data class PlaybackProfile(
    val name: String,
    val url: String,
    val referer: String?,
    val userAgent: String,
    val engine: TransportEngine
)

private fun isBlockedCdnReferer(ref: String?): Boolean {
    if (ref.isNullOrBlank()) return false
    return ref.contains("fmoviesunblocked", ignoreCase = true) ||
        ref.contains("aoneroom.com", ignoreCase = true) ||
        ref.contains("moviebox.ph", ignoreCase = true)
}

private fun buildPlaybackProfiles(stream: StreamOption): List<PlaybackProfile> {
    val ua = stream.headers["User-Agent"] ?: MovieBoxDirectClient.CHROME_USER_AGENT
    val isHakunaCdn = stream.url.contains(".hakunaymatata.com", ignoreCase = true) ||
        stream.url.contains(".aoneroom.com", ignoreCase = true)
    val explicitReferer = stream.headers["Referer"]?.let { ref ->
        if (isHakunaCdn && isBlockedCdnReferer(ref)) null else ref
    }
    // Only inject MovieBox's movie-box.co Referer on MovieBox/Hakuna CDNs — NEVER corrupt MovieLinkBD streams!
    val primaryRef = if (isHakunaCdn) {
        explicitReferer ?: MovieBoxDirectClient.DEFAULT_REFERER
    } else {
        explicitReferer
    }
    val secondaryRef = if (isHakunaCdn) {
        MovieBoxDirectClient.WHITELISTED_STREAM_REFERERS
            .firstOrNull { !it.equals(primaryRef, ignoreCase = true) }
            ?: "https://movie-box.co/"
    } else {
        explicitReferer
    }
    val isProgressiveMp4 = !stream.url.contains(".m3u8", ignoreCase = true) &&
        !stream.url.contains(".mpd", ignoreCase = true)

    return buildList {
        // 1. Chrome TLS 1.2 + HTTP/1.1 + Range: bytes=0- via OkHttp (fastest & avoids Conscrypt TLS 1.3 426)
        add(
            PlaybackProfile(
                name = "Chrome-H1",
                url = stream.url,
                referer = primaryRef,
                userAgent = ua,
                engine = TransportEngine.OKHTTP_CHROME_H1
            )
        )
        // 2. Android native HttpsURLConnection (DefaultHttpDataSource — separate TLS stack)
        add(
            PlaybackProfile(
                name = "Native-HTTP",
                url = stream.url,
                referer = secondaryRef,
                userAgent = ua,
                engine = TransportEngine.NATIVE_HTTP_URL_CONN
            )
        )
        // 3. Global EdgeNext POP failover (SG/US/BD/JP POPs) for hakunaymatata.com streams
        if (isHakunaCdn) {
            add(
                PlaybackProfile(
                    name = "Global-CDN",
                    url = stream.url,
                    referer = primaryRef,
                    userAgent = ua,
                    engine = TransportEngine.OKHTTP_EDGE_POP_H1
                )
            )
        }
        // 4. HTTP/2 fallback
        add(
            PlaybackProfile(
                name = "Direct-H2",
                url = stream.url,
                referer = if (isHakunaCdn) "https://123movienow.cc/" else primaryRef,
                userAgent = ua,
                engine = TransportEngine.OKHTTP_H2
            )
        )
        // 5. Real Chromium Blink HTML5 <video> fallback (genuine Chrome TLS/JA3/HTTP2 + Referer)
        if (isProgressiveMp4) {
            add(
                PlaybackProfile(
                    name = "Chromium-Player",
                    url = stream.url,
                    referer = primaryRef,
                    userAgent = ua,
                    engine = TransportEngine.CHROMIUM_WEBVIEW
                )
            )
        }
    }
}

/**
 * Prevents ExoPlayer from stalling on HTTP 426/428/429/401/403/404/410 CDN rejections,
 * handing control immediately to our multi-transport failover.
 */
@OptIn(UnstableApi::class)
class CinedorLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy(2) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val httpCode = findHttpStatusCode(loadErrorInfo.exception)
        if (httpCode in setOf(426, 428, 429, 403, 401, 404, 410)) {
            return C.TIME_UNSET
        }
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}

@OptIn(UnstableApi::class)
internal fun findHttpStatusCode(throwable: Throwable?): Int? {
    var cause: Throwable? = throwable
    while (cause != null) {
        if (cause is HttpDataSource.InvalidResponseCodeException) {
            return cause.responseCode
        }
        cause = cause.cause
    }
    return null
}

@OptIn(UnstableApi::class)
private fun findFailedRequestUrl(throwable: Throwable?): String? {
    var cause: Throwable? = throwable
    while (cause != null) {
        if (cause is HttpDataSource.HttpDataSourceException) {
            return cause.dataSpec.uri.toString()
        }
        cause = cause.cause
    }
    return null
}

private fun describePlaybackError(error: PlaybackException): Triple<String, String, Int?> {
    val httpCode = findHttpStatusCode(error)
    return when (httpCode) {
        429, 428, 426 -> Triple(
            "CDN Mirror Rejected Link (HTTP $httpCode)",
            "This CDN edge requires a fresh signed link. Tap Refresh Stream or switch quality below.",
            httpCode
        )
        403, 401 -> Triple(
            "Stream Access Restricted (HTTP $httpCode)",
            "This CDN mirror rejected the signed link. Tap Refresh Stream to fetch a fresh mirror.",
            httpCode
        )
        else -> {
            val rootMsg = error.cause?.localizedMessage?.takeIf { it.isNotBlank() }
            val title = if (httpCode != null) "CDN returned HTTP $httpCode" else (error.localizedMessage ?: error.errorCodeName)
            val subtitle = rootMsg ?: "Try switching server/quality below or tap Refresh Stream."
            Triple(title, subtitle, httpCode)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CinePulseVideoPlayer(
    title: String,
    stream: StreamOption,
    streams: List<StreamOption>,
    caption: CaptionTrack?,
    captions: List<CaptionTrack>,
    initialPositionMs: Long,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSelectStream: (StreamOption) -> Unit,
    onSelectCaption: (CaptionTrack?) -> Unit,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    onStreamExhausted: (StreamOption, Int?) -> Boolean = { _, _ -> false },
    onRefreshStream: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnStreamExhausted by rememberUpdatedState(onStreamExhausted)
    val latestOnRefreshStream by rememberUpdatedState(onRefreshStream)
    val latestOnProgressUpdate by rememberUpdatedState(onProgressUpdate)

    val profiles = remember(stream.url, stream.headers) { buildPlaybackProfiles(stream) }

    var profileIndex by remember(stream.url) { mutableIntStateOf(0) }
    var captionBypassed by remember(stream.url, caption?.url) { mutableStateOf(false) }
    var retryNonce by remember(stream.url) { mutableIntStateOf(0) }
    var isBuffering by remember(stream.url, profileIndex, captionBypassed, retryNonce) { mutableStateOf(true) }
    var isPlaying by remember(stream.url, profileIndex, retryNonce) { mutableStateOf(true) }
    var currentPositionMs by remember(stream.url) { mutableLongStateOf(initialPositionMs.coerceAtLeast(0L)) }
    var bufferedPositionMs by remember(stream.url) { mutableLongStateOf(0L) }
    var durationMs by remember(stream.url) { mutableLongStateOf(0L) }
    var playbackError by remember(stream.url, profileIndex, captionBypassed, retryNonce) {
        mutableStateOf<Triple<String, String, Int?>?>(null)
    }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeOption by remember { mutableStateOf(VideoResizeOption.FIT) }
    var preservedPositionMs by remember(initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val speeds = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    val activeProfile = profiles[profileIndex.coerceIn(0, profiles.lastIndex)]
    val isWebViewMode = activeProfile.engine == TransportEngine.CHROMIUM_WEBVIEW
    val effectiveCaption = if (captionBypassed) null else caption

    val exoPlayer = remember(stream.url, effectiveCaption?.url, profileIndex, retryNonce, isWebViewMode) {
        if (isWebViewMode) {
            null
        } else {
            val isHakunaUrl = activeProfile.url.contains(".hakunaymatata.com", ignoreCase = true) ||
                activeProfile.url.contains(".aoneroom.com", ignoreCase = true)
            val requestHeaders = LinkedHashMap<String, String>().apply {
                if (isHakunaUrl) {
                    putAll(MovieBoxDirectClient.CHROME_VIDEO_HEADERS)
                }
                if (!activeProfile.referer.isNullOrBlank()) {
                    put("Referer", activeProfile.referer)
                }
                stream.headers.forEach { (k, v) ->
                    val skipOrigin = isHakunaUrl && k.equals("Origin", ignoreCase = true)
                    if (!k.equals("User-Agent", ignoreCase = true) &&
                        !k.equals("Referer", ignoreCase = true) &&
                        !skipOrigin &&
                        v.isNotBlank()
                    ) {
                        put(k, v)
                    }
                }
            }

            val dataSourceFactory: DataSource.Factory = when (activeProfile.engine) {
                TransportEngine.NATIVE_HTTP_URL_CONN -> {
                    DefaultHttpDataSource.Factory()
                        .setUserAgent(activeProfile.userAgent)
                        .setDefaultRequestProperties(requestHeaders)
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(8_000)
                        .setReadTimeoutMs(18_000)
                }
                TransportEngine.OKHTTP_EDGE_POP_H1 -> {
                    OkHttpDataSource.Factory(edgePopMediaOkHttpClient)
                        .setUserAgent(activeProfile.userAgent)
                        .setDefaultRequestProperties(requestHeaders)
                }
                TransportEngine.OKHTTP_H2 -> {
                    OkHttpDataSource.Factory(h2MediaOkHttpClient)
                        .setUserAgent(activeProfile.userAgent)
                        .setDefaultRequestProperties(requestHeaders)
                }
                else -> {
                    OkHttpDataSource.Factory(sharedMediaOkHttpClient)
                        .setUserAgent(activeProfile.userAgent)
                        .setDefaultRequestProperties(requestHeaders)
                }
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(CinedorLoadErrorHandlingPolicy())
            val fastLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000, // minBufferMs
                    50_000, // maxBufferMs
                    500,    // bufferForPlaybackMs — MovieBox-grade instant ~0.5s first frame!
                    1_200   // bufferForPlaybackAfterRebufferMs
                )
                .build()

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(fastLoadControl)
                .build()
                .apply {
                    val itemBuilder = MediaItem.Builder().setUri(Uri.parse(activeProfile.url))
                    when {
                        activeProfile.url.contains(".m3u8", ignoreCase = true) -> itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                        activeProfile.url.contains(".mpd", ignoreCase = true) -> itemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                    }
                    if (effectiveCaption != null && effectiveCaption.url.startsWith("http")) {
                        val mime = when {
                            effectiveCaption.url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                            effectiveCaption.url.contains(".ass", ignoreCase = true) ||
                                effectiveCaption.url.contains(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                            else -> MimeTypes.APPLICATION_SUBRIP
                        }
                        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(effectiveCaption.url))
                            .setMimeType(mime)
                            .setLanguage(effectiveCaption.language)
                            .setLabel(effectiveCaption.label)
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build()
                        itemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                    }
                    setMediaItem(itemBuilder.build())
                    val seekTarget = preservedPositionMs.coerceAtLeast(initialPositionMs)
                    if (seekTarget > 0L) {
                        seekTo(seekTarget)
                    }
                    playbackParameters = PlaybackParameters(playbackSpeed)
                    playWhenReady = true
                    prepare()
                }
        }
    }

    DisposableEffect(exoPlayer) {
        if (exoPlayer == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackError = null
                    val dur = exoPlayer.duration
                    if (dur > 0L) durationMs = dur
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                val httpCode = findHttpStatusCode(error)
                val failedUrl = findFailedRequestUrl(error)
                val detailed = describePlaybackError(error)
                Log.w(PLAYER_TAG, "Playback error on ${activeProfile.name} (${stream.url}): HTTP=$httpCode ${detailed.first}")

                // Bypass subtitle if the error came from the caption track itself
                val failedOnCaption = effectiveCaption != null &&
                    (failedUrl == effectiveCaption.url || (httpCode !in setOf(426, 428, 429) && failedUrl != activeProfile.url))
                if (effectiveCaption != null && !captionBypassed && failedOnCaption) {
                    captionBypassed = true
                    return
                }

                // Rotate across Chrome-H1 -> Native-HTTP -> Global-CDN -> Direct-H2 -> Chromium WebView!
                if ((httpCode == null || httpCode in setOf(426, 428, 429, 403, 401, 502, 503)) &&
                    profileIndex < profiles.lastIndex
                ) {
                    profileIndex += 1
                    return
                }

                // Switch to an alternate host/mirror if available
                if (latestOnStreamExhausted(stream, httpCode)) {
                    return
                }

                isBuffering = false
                isPlaying = false
                playbackError = detailed
            }
        }
        exoPlayer.addListener(listener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (pos > 0L) {
                preservedPositionMs = pos
            }
            if (dur > 0L && pos > 0L) {
                latestOnProgressUpdate(pos, dur)
            }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Smooth 250ms timeline ticker for custom Compose seekbar + 10s watch-history persistence
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        var lastSavedAtMs = System.currentTimeMillis()
        while (true) {
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            currentPositionMs = pos
            bufferedPositionMs = buf
            if (dur > 0L) durationMs = dur
            if (pos > 0L) preservedPositionMs = pos

            val now = System.currentTimeMillis()
            if (exoPlayer.isPlaying && dur > 0L && now - lastSavedAtMs >= 10_000L) {
                lastSavedAtMs = now
                latestOnProgressUpdate(pos, dur)
            }
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        if (isWebViewMode) {
            ChromiumFallbackVideoView(
                streamUrl = activeProfile.url,
                referer = activeProfile.referer ?: MovieBoxDirectClient.DEFAULT_REFERER,
                initialPositionMs = preservedPositionMs.coerceAtLeast(initialPositionMs),
                playbackSpeed = playbackSpeed,
                onWebViewReady = { webViewRef = it },
                onPlaying = {
                    isBuffering = false
                    playbackError = null
                },
                onBuffering = { buffering ->
                    isBuffering = buffering
                },
                onProgress = { posMs, durMs ->
                    if (posMs > 0L) preservedPositionMs = posMs
                    if (durMs > 0L && posMs > 0L) latestOnProgressUpdate(posMs, durMs)
                },
                onError = { errDetail ->
                    Log.w(PLAYER_TAG, "Chromium fallback player failed on ${stream.url}: $errDetail | lastDiag=$lastCdnErrorDiagnostic")
                    if (!latestOnStreamExhausted(stream, 426)) {
                        isBuffering = false
                        val diagLine = lastCdnErrorDiagnostic.ifBlank { "Chromium error: $errDetail" }
                        playbackError = Triple(
                            "CDN Blocked This Network (HTTP 426)",
                            "MovieBox CDN rejected this cloud preview IP. Install APK on your phone or add CINEPULSE_API_KEY in Secrets.\n$diagLine",
                            426
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (exoPlayer != null) {
            val targetResizeMode = when (resizeOption) {
                VideoResizeOption.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoResizeOption.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoResizeOption.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.cinedor_player_view, null, false) as PlayerView).apply {
                        player = exoPlayer
                        keepScreenOn = true
                        useController = false
                        resizeMode = targetResizeMode
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.keepScreenOn = playbackError == null
                    view.useController = false
                    if (view.resizeMode != targetResizeMode) {
                        view.resizeMode = targetResizeMode
                    }
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isBuffering && playbackError == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = CrimsonPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(38.dp)
                )
                if (profileIndex > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connecting via ${activeProfile.name} (${profileIndex + 1}/${profiles.size})…",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        val err = playbackError
        if (err != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.84f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ObsidianBackground.copy(alpha = 0.95f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = err.first,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = err.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val needsFreshLink = err.third in setOf(429, 428, 426, 403, 401)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            onClick = {
                                if (needsFreshLink) {
                                    latestOnRefreshStream()
                                } else {
                                    profileIndex = 0
                                    captionBypassed = false
                                    retryNonce += 1
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Brush.horizontalGradient(CinedorGradientColors))
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = if (needsFreshLink) "Refresh stream" else "Retry playback",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (needsFreshLink) "Refresh Stream" else "Retry",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom MovieBox-faithful auto-hiding controls overlay (zero permanent clutter!)
        if (!isWebViewMode && playbackError == null && exoPlayer != null) {
            CinedorPlayerControlsOverlay(
                title = title,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                currentPositionMs = currentPositionMs,
                bufferedPositionMs = bufferedPositionMs,
                durationMs = durationMs,
                isFullscreen = isFullscreen,
                playbackSpeed = playbackSpeed,
                resizeOption = resizeOption,
                streams = streams,
                selectedStream = stream,
                captions = captions,
                selectedCaption = caption,
                onBack = onBack,
                onTogglePlayPause = {
                    if (exoPlayer.playbackState == Player.STATE_ENDED) {
                        exoPlayer.seekTo(0L)
                        exoPlayer.play()
                    } else if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeekTo = { targetMs ->
                    val clamped = if (durationMs > 0L) targetMs.coerceIn(0L, durationMs) else targetMs.coerceAtLeast(0L)
                    exoPlayer.seekTo(clamped)
                    currentPositionMs = clamped
                },
                onSeekBy = { deltaMs ->
                    val target = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
                    val clamped = if (durationMs > 0L) target.coerceAtMost(durationMs) else target
                    exoPlayer.seekTo(clamped)
                    currentPositionMs = clamped
                },
                onCycleSpeed = {
                    val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                    playbackSpeed = speeds[nextIdx]
                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                },
                onCycleResize = {
                    val nextIdx = (resizeOption.ordinal + 1) % VideoResizeOption.entries.size
                    resizeOption = VideoResizeOption.entries[nextIdx]
                },
                onToggleFullscreen = onToggleFullscreen,
                onSelectStream = onSelectStream,
                onSelectCaption = onSelectCaption,
                modifier = Modifier.fillMaxSize(),
                onHoldSpeed = { holding ->
                    exoPlayer.playbackParameters = PlaybackParameters(if (holding) 2f else playbackSpeed)
                }
            )
        } else if (isWebViewMode && playbackError == null) {
            // Minimal unobtrusive back + speed + fullscreen bar when Chromium HTML5 fallback is active
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    onClick = {
                        val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                        playbackSpeed = speeds[nextIdx]
                        webViewRef?.evaluateJavascript(
                            "var v=document.getElementById('v');if(v){v.playbackRate=$playbackSpeed;}",
                            null
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Playback speed",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (playbackError != null) {
            // Ensure user can always exit cleanly when an error overlay is shown
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = if (isFullscreen) "Exit fullscreen" else "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Genuine Chromium Blink HTML5 <video> fallback player.
 * Used automatically when CDN WAFs (such as EdgeNext HTTP 426) reject non-browser TLS/JA3 stacks:
 * loading with baseUrl="https://moviebox01.com/" lets Android System WebView (real Chrome) send
 * authentic Chrome BoringSSL TLS/JA3, HTTP/2 frames, Referer, Sec-Fetch-Dest: video, and Range headers.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ChromiumFallbackVideoView(
    streamUrl: String,
    referer: String,
    initialPositionMs: Long,
    playbackSpeed: Float,
    onWebViewReady: (WebView?) -> Unit,
    onPlaying: () -> Unit,
    onBuffering: (Boolean) -> Unit,
    onProgress: (Long, Long) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val latestOnPlaying by rememberUpdatedState(onPlaying)
    val latestOnBuffering by rememberUpdatedState(onBuffering)
    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnError by rememberUpdatedState(onError)

    var holderWebView by remember { mutableStateOf<WebView?>(null) }

    val safeBaseUrl = remember(referer) {
        val trimmed = referer.trim()
        if (trimmed.startsWith("https://")) trimmed else MovieBoxDirectClient.DEFAULT_REFERER
    }

    val htmlContent = remember(streamUrl, initialPositionMs, playbackSpeed) {
        val quotedUrl = JSONObject.quote(streamUrl)
        val startSeconds = (initialPositionMs / 1000.0).coerceAtLeast(0.0)
        """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"/>
          <meta name="referrer" content="unsafe-url"/>
          <style>
            html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden;display:flex;align-items:center;justify-content:center;}
            video{width:100%;height:100%;object-fit:contain;background:#000;}
          </style>
        </head>
        <body>
          <video id="v" controls autoplay playsinline preload="auto" controlsList="nodownload" referrerpolicy="unsafe-url"></video>
          <script>
            (function(){
              var v = document.getElementById('v');
              var started = false;
              var lastSent = 0;
              v.src = $quotedUrl;
              v.playbackRate = $playbackSpeed;
              v.addEventListener('loadedmetadata', function(){
                if ($startSeconds > 1 && v.duration > $startSeconds) {
                  try { v.currentTime = $startSeconds; } catch(e){}
                }
                v.play().catch(function(){});
              });
              v.addEventListener('playing', function(){
                started = true;
                if (window.CinedorBridge) CinedorBridge.onPlaying();
              });
              v.addEventListener('canplay', function(){
                if (window.CinedorBridge) CinedorBridge.onBuffering(false);
              });
              v.addEventListener('waiting', function(){
                if (window.CinedorBridge) CinedorBridge.onBuffering(true);
              });
              v.addEventListener('timeupdate', function(){
                var now = Date.now();
                if (now - lastSent > 8000 && v.duration > 0) {
                  lastSent = now;
                  if (window.CinedorBridge) CinedorBridge.onProgress(Math.floor(v.currentTime*1000), Math.floor(v.duration*1000));
                }
              });
              v.addEventListener('error', function(){
                var code = v.error ? v.error.code : 0;
                if (window.CinedorBridge) CinedorBridge.onError('media_err_' + code);
              });
              v.play().catch(function(){});
              setTimeout(function(){
                if (!started && v.readyState < 2 && window.CinedorBridge) {
                  CinedorBridge.onBuffering(false);
                }
              }, 12000);
            })();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    DisposableEffect(lifecycleOwner, holderWebView) {
        val observer = LifecycleEventObserver { _, event ->
            val wv = holderWebView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wv.evaluateJavascript("var v=document.getElementById('v');if(v){v.pause();}", null)
                    wv.onPause()
                }
                Lifecycle.Event.ON_RESUME -> wv.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                keepScreenOn = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    allowFileAccess = false
                    allowContentAccess = false
                    userAgentString = MovieBoxDirectClient.CHROME_USER_AGENT
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPlaying() {
                            mainHandler.post { latestOnPlaying() }
                        }

                        @JavascriptInterface
                        fun onBuffering(buffering: Boolean) {
                            mainHandler.post { latestOnBuffering(buffering) }
                        }

                        @JavascriptInterface
                        fun onProgress(posMs: Long, durMs: Long) {
                            mainHandler.post { latestOnProgress(posMs, durMs) }
                        }

                        @JavascriptInterface
                        fun onError(detail: String) {
                            mainHandler.post { latestOnError(detail) }
                        }
                    },
                    "CinedorBridge"
                )
                tag = streamUrl
                loadDataWithBaseURL(safeBaseUrl, htmlContent, "text/html", "utf-8", null)
                holderWebView = this
                onWebViewReady(this)
            }
        },
        update = { wv ->
            if (wv.tag != streamUrl) {
                wv.tag = streamUrl
                wv.loadDataWithBaseURL(safeBaseUrl, htmlContent, "text/html", "utf-8", null)
            }
        },
        onRelease = { wv ->
            onWebViewReady(null)
            holderWebView = null
            wv.stopLoading()
            wv.removeJavascriptInterface("CinedorBridge")
            wv.loadUrl("about:blank")
            wv.destroy()
        },
        modifier = modifier
    )
}
