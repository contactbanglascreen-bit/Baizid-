package com.example.data.remote

import com.example.data.model.ApiStatusInfo
import com.example.data.model.CaptionTrack
import com.example.data.model.CastMember
import com.example.data.model.CategoryChip
import com.example.data.model.CommentItem
import com.example.data.model.EpisodeInfo
import com.example.data.model.FilterOptions
import com.example.data.model.HomeFeed
import com.example.data.model.HomeSection
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaDetail
import com.example.data.model.MediaItem
import com.example.data.model.ReelItem
import com.example.data.model.SeasonInfo
import com.example.data.model.SportsMatch
import com.example.data.model.StaffDetail
import com.example.data.model.StreamOption
import org.json.JSONArray
import org.json.JSONObject

object CinePulseJsonParser {

    // Phrases that mark 18+ content anywhere in a title / path / genre.
    private val matureKeywords = listOf(
        "adult18plus", "18+", "(18)", "[18]", "hentai", "uncensored", "midnight", "erotic", "erotica",
        "porn", "xxx", "nsfw", "ecchi", "taboo charming", "bible black", "inbo", "overflow",
        "seduction", "seductive", "sensual", "nude", "naked", "nudity", "uncut", "unrated",
        "softcore", "hardcore", "lust", "hot web series", "bold web series", "adult web",
        // Indian / Asian adult streaming labels
        "ullu", "kooku", "primeflix", "hotshots", "hotshot", "fliz", "rabbit movies", "hunters app",
        "besharams", "cineprime", "nuefliks", "voovi", "tadka prime", "triflicks", "atrangii",
        "bigshots", "hotx", "neonx", "mood x", "moodx", "jalva", "yessma", "kangan app",
        "18 plus", "adults only", "for adults"
    )

    // Short words matched as whole words only (so "Essex" or "Hotel" stay safe).
    private val matureWords = Regex(
        "\\b(sex|sexy|sexual|adult|adults|bdsm|fetish|milf|nsfw|jav|av idol|r18|r-18|xxx)\\b"
    )

    fun isSafeText(text: String): Boolean {
        val lower = text.lowercase()
        if (matureKeywords.any { keyword -> lower.contains(keyword) }) return false
        return !matureWords.containsMatchIn(lower)
    }

    private fun JSONObject.firstNonBlank(vararg keys: String): String {
        for (key in keys) {
            val raw = opt(key)
            when (raw) {
                is String -> if (raw.isNotBlank() && !raw.equals("null", ignoreCase = true)) return raw.trim()
                is Number -> return raw.toString()
            }
        }
        return ""
    }

    private fun JSONObject.extractImageUrl(vararg keys: String): String {
        for (key in keys) {
            val raw = opt(key)
            when (raw) {
                is String -> if (raw.startsWith("http")) return raw.trim()
                is JSONObject -> {
                    val nested = raw.firstNonBlank("url", "src", "image", "cover", "poster", "thumbnail", "high", "original")
                    if (nested.startsWith("http")) return nested
                }
            }
        }
        return ""
    }

    private fun unwrapRoot(jsonString: String): Any? {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            obj.opt("data") ?: obj.opt("result") ?: obj.opt("results") ?: obj
        }
    }

    fun parseMediaItem(obj: JSONObject, defaultSource: String = "moviebox"): MediaItem? {
        val title = obj.firstNonBlank("title", "name", "subjectTitle", "movieTitle", "label")
        if (title.isBlank()) return null

        val id = obj.firstNonBlank("subjectId", "id", "movieId", "slug", "detailPath", "url")
            .ifBlank { title }

        val poster = obj.extractImageUrl(
            "poster", "posterUrl", "cover", "coverUrl", "thumbnail", "thumb", "image", "img", "verticalCover", "pic"
        )
        val backdrop = obj.extractImageUrl(
            "backdrop", "backdropUrl", "banner", "bannerUrl", "horizontalCover", "still", "cover"
        ).ifBlank { poster }

        val source = obj.firstNonBlank("source", "provider", "site").ifBlank { defaultSource }
        val rawType = obj.firstNonBlank("subjectType", "type", "mediaType", "category", "contentType")
        val type = when (rawType) {
            "1" -> MediaCategoryType.MOVIE
            "2" -> MediaCategoryType.SERIES
            "6" -> MediaCategoryType.SHORT
            else -> MediaCategoryType.fromRaw(rawType.ifBlank { obj.firstNonBlank("genre", "genres") })
        }

        val ratingRaw = obj.firstNonBlank("imdbRatingValue", "rating", "score", "imdb", "voteAverage")
        val rating = if (ratingRaw == "0" || ratingRaw == "0.0") "" else ratingRaw.take(5)

        val releaseDate = obj.firstNonBlank("releaseDate", "release_date", "airDate", "date")
        val year = obj.firstNonBlank("year", "releaseYear")
            .ifBlank { releaseDate.take(4).takeIf { it.all(Char::isDigit) && it.length == 4 }.orEmpty() }

        val languageBadge = obj.firstNonBlank("dubs", "language", "lang", "audio", "subtitles", "badge", "corner")
            .ifBlank {
                val bracket = Regex("\\[(Hindi|Bengali|English|Dual Audio|Sub|Dub|Tamil|Telugu|Punjabi)]", RegexOption.IGNORE_CASE)
                    .find(title)?.groupValues?.getOrNull(1).orEmpty()
                bracket
            }
            .take(28)

        val qualityBadge = obj.firstNonBlank("quality", "resolution", "hd", "tag").take(16)
        val detailPath = obj.firstNonBlank("detailPath", "url", "link", "slug")
        val description = obj.firstNonBlank("description", "overview", "synopsis", "plot", "intro")
        val country = obj.firstNonBlank("countryName", "country", "region")
        val booked = obj.firstNonBlank("bookedCount", "bookings", "appointments")

        val genres = mutableListOf<String>()
        val genresRaw = obj.opt("genres") ?: obj.opt("genre") ?: obj.opt("tags")
        when (genresRaw) {
            is JSONArray -> {
                for (i in 0 until genresRaw.length()) {
                    val item = genresRaw.opt(i)
                    val g = when (item) {
                        is String -> item.trim()
                        is JSONObject -> item.firstNonBlank("name", "title", "label")
                        else -> ""
                    }
                    if (g.isNotBlank()) genres.add(g)
                }
            }
            is String -> {
                genresRaw.split(",", "/", "|").map { it.trim() }.filter { it.isNotBlank() }.forEach { genres.add(it) }
            }
        }

        val rankVal = obj.optInt("rank", obj.optInt("ranking", -1)).takeIf { it > 0 }

        return MediaItem(
            id = id,
            title = title,
            posterUrl = poster,
            backdropUrl = backdrop,
            mediaType = type,
            source = source,
            year = year,
            rating = rating,
            languageBadge = languageBadge,
            qualityBadge = qualityBadge,
            genres = genres,
            country = country,
            releaseDate = releaseDate,
            bookedCount = booked,
            rank = rankVal,
            detailPath = detailPath,
            description = description
        )
    }

    fun parseMediaList(jsonString: String, safeMode: Boolean, defaultSource: String = "moviebox"): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val seen = mutableSetOf<String>()

        fun collectFromArray(arr: JSONArray, sourceHint: String) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val nested = obj.optJSONObject("subject") ?: obj.optJSONObject("item") ?: obj
                val item = parseMediaItem(nested, sourceHint) ?: continue
                if (safeMode && (!isSafeText(item.title) || !isSafeText(item.detailPath) || !isSafeText(item.genres.joinToString(" ")))) {
                    continue
                }
                val key = "${item.source}:${item.id.lowercase()}:${item.title.lowercase()}"
                if (seen.add(key)) {
                    result.add(item)
                }
            }
        }

        fun walk(node: Any?, sourceHint: String, depth: Int) {
            if (node == null || depth > 5) return
            when (node) {
                is JSONArray -> collectFromArray(node, sourceHint)
                is JSONObject -> {
                    val listKeys = listOf("subjectList", "items", "subjects", "results", "list", "movies", "series", "data", "records", "catalog")
                    for (k in listKeys) {
                        val arr = node.optJSONArray(k)
                        if (arr != null && arr.length() > 0) {
                            collectFromArray(arr, sourceHint)
                        }
                    }
                    // Handle MovieBox platform/play-list monthList[].subjects[] structure
                    node.optJSONArray("monthList")?.let { months ->
                        for (m in 0 until months.length()) {
                            months.optJSONObject(m)?.optJSONArray("subjects")?.let { subjArr ->
                                collectFromArray(subjArr, sourceHint)
                            }
                        }
                    }
                    // Handle dual-site separated blocks (moviebox / movielinkbd)
                    listOf("moviebox" to "moviebox", "movielinkbd" to "movielinkbd", "sources" to sourceHint, "data" to sourceHint)
                        .forEach { (key, nextSource) ->
                            val child = node.opt(key)
                            if (child is JSONObject || child is JSONArray) {
                                walk(child, nextSource, depth + 1)
                            }
                        }
                }
            }
        }

        val root = runCatching { JSONObject(jsonString) }.getOrNull()
        if (root != null) {
            walk(root, defaultSource, 0)
        } else {
            runCatching { JSONArray(jsonString) }.getOrNull()?.let { collectFromArray(it, defaultSource) }
        }
        return result
    }

    fun parseHomeFeed(jsonString: String, safeMode: Boolean): HomeFeed {
        val banners = mutableListOf<MediaItem>()
        val sections = mutableListOf<HomeSection>()
        val sports = mutableListOf<SportsMatch>()
        val seenSectionTitles = mutableSetOf<String>()

        fun addSection(title: String, rawItems: JSONArray?, sourceHint: String, categoryPath: String = "") {
            if (rawItems == null || rawItems.length() == 0) return
            if (safeMode && (!isSafeText(title) || !isSafeText(categoryPath))) return

            val parsed = mutableListOf<MediaItem>()
            for (i in 0 until rawItems.length()) {
                val obj = rawItems.optJSONObject(i) ?: continue
                val target = obj.optJSONObject("subject") ?: obj
                val item = parseMediaItem(target, sourceHint) ?: continue
                if (safeMode && (!isSafeText(item.title) || !isSafeText(item.detailPath))) continue
                parsed.add(item)
            }
            if (parsed.isEmpty()) return

            val cleanTitle = title.ifBlank { "Featured" }.trim()
            val dedupKey = "${sourceHint.lowercase()}:${cleanTitle.lowercase()}"
            if (seenSectionTitles.add(dedupKey)) {
                val lower = cleanTitle.lowercase()
                sections.add(
                    HomeSection(
                        id = dedupKey,
                        title = cleanTitle,
                        items = parsed,
                        categoryPath = categoryPath,
                        isRanked = "top" in lower || "trending" in lower || "rank" in lower,
                        isUpcoming = "coming" in lower || "upcoming" in lower
                    )
                )
            }
        }

        fun extractFromBlock(block: JSONObject, sourceHint: String) {
            // Banners
            val bannerCandidates = listOf("banners", "banner", "hero", "featured", "slider", "carousel")
            for (key in bannerCandidates) {
                val node = block.opt(key)
                val arr = when (node) {
                    is JSONArray -> node
                    is JSONObject -> node.optJSONArray("items") ?: node.optJSONArray("list")
                    else -> null
                }
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val target = obj.optJSONObject("subject") ?: obj
                        val item = parseMediaItem(target, sourceHint) ?: continue
                        val bannerImg = obj.extractImageUrl("image", "banner", "backdrop", "cover", "poster")
                            .ifBlank { item.backdropUrl }
                        val merged = item.copy(backdropUrl = bannerImg, posterUrl = item.posterUrl.ifBlank { bannerImg })
                        if (!safeMode || isSafeText(merged.title)) {
                            banners.add(merged)
                        }
                    }
                }
            }

            // Sections / operatingList / rows
            val sectionArrays = listOf("operatingList", "sections", "rows", "shelves", "categories", "blocks", "lists")
            for (key in sectionArrays) {
                val arr = block.optJSONArray(key) ?: continue
                for (i in 0 until arr.length()) {
                    val sec = arr.optJSONObject(i) ?: continue
                    val secType = sec.firstNonBlank("type").uppercase()
                    val secTitle = sec.firstNonBlank("title", "name", "heading", "label")
                    val catPath = sec.firstNonBlank("path", "category", "url", "moreUrl")

                    if (secType == "BANNER") {
                        val bObj = sec.optJSONObject("banner")
                        val bItems = bObj?.optJSONArray("items") ?: sec.optJSONArray("items")
                        if (bItems != null) {
                            for (j in 0 until bItems.length()) {
                                val bo = bItems.optJSONObject(j) ?: continue
                                val subj = bo.optJSONObject("subject") ?: bo
                                val item = parseMediaItem(subj, sourceHint) ?: continue
                                val img = bo.extractImageUrl("image", "cover", "banner").ifBlank { item.backdropUrl }
                                if (!safeMode || isSafeText(item.title)) {
                                    banners.add(item.copy(backdropUrl = img, posterUrl = item.posterUrl.ifBlank { img }))
                                }
                            }
                        }
                        continue
                    }

                    if (secType == "SPORT_LIVE" || secType == "LIVE") {
                        val liveArr = sec.optJSONArray("liveList") ?: sec.optJSONArray("matches") ?: sec.optJSONArray("items")
                        if (liveArr != null) {
                            sports.addAll(parseSportsArray(liveArr))
                        }
                        continue
                    }

                    val itemsArr = sec.optJSONArray("subjects")
                        ?: sec.optJSONArray("items")
                        ?: sec.optJSONArray("list")
                        ?: sec.optJSONArray("movies")
                        ?: sec.optJSONArray("data")
                    addSection(secTitle, itemsArr, sourceHint, catPath)
                }
            }

            // Direct named arrays on the block
            val directRows = listOf(
                "trending" to "Trending Now",
                "trendingMovies" to "Trending Movies",
                "trendingSeries" to "Trending Series",
                "popular" to "Popular Picks",
                "latest" to "Latest Releases",
                "recent" to "Recently Added",
                "anime" to "Top Anime",
                "dramas" to "Asian Dramas",
                "movies" to "Featured Movies",
                "series" to "TV Series",
                "items" to if (sourceHint == "movielinkbd") "MovieLinkBD Picks" else "Featured Picks"
            )
            for ((k, fallbackTitle) in directRows) {
                val arr = block.optJSONArray(k)
                if (arr != null && arr.length() > 0) {
                    addSection(fallbackTitle, arr, sourceHint)
                }
            }
        }

        val root = runCatching { JSONObject(jsonString) }.getOrNull() ?: return HomeFeed()
        val dataNode = root.optJSONObject("data") ?: root

        extractFromBlock(dataNode, "moviebox")

        listOf("moviebox" to "moviebox", "movielinkbd" to "movielinkbd").forEach { (key, src) ->
            val sub = dataNode.optJSONObject(key) ?: root.optJSONObject(key)
            if (sub != null) {
                extractFromBlock(sub, src)
            }
        }

        // If banners empty, promote top 5 backdrop/poster items from first section
        val finalBanners = if (banners.isNotEmpty()) {
            banners.distinctBy { it.title.lowercase() }.take(8)
        } else {
            sections.firstOrNull()?.items?.take(6).orEmpty()
        }

        return HomeFeed(
            banners = finalBanners,
            sections = sections,
            sportsMatches = sports.distinctBy { it.id }
        )
    }

    fun parseSportsMatches(jsonString: String): List<SportsMatch> {
        val root = unwrapRoot(jsonString) ?: return emptyList()
        return when (root) {
            is JSONArray -> parseSportsArray(root)
            is JSONObject -> {
                val arr = root.optJSONArray("matches")
                    ?: root.optJSONArray("liveList")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONArray("list")
                    ?: root.optJSONArray("data")
                if (arr != null) parseSportsArray(arr) else emptyList()
            }
            else -> emptyList()
        }
    }

    /** Searches nested objects/arrays of a match for a playable live stream link. */
    private fun findStreamUrl(node: Any?, depth: Int): String {
        if (depth > 5 || node == null) return ""
        when (node) {
            is JSONObject -> {
                val keys = node.keys().asSequence().toList()
                // Prefer keys that sound like a stream
                val ordered = keys.sortedByDescending { k ->
                    val l = k.lowercase()
                    listOf("m3u8", "hls", "stream", "live", "play", "source", "url", "link").count { it in l }
                }
                for (k in ordered) {
                    val v = node.opt(k)
                    if (v is String && v.startsWith("http")) {
                        val l = v.lowercase()
                        val kl = k.lowercase()
                        val looksLikeVideo = ".m3u8" in l || ".flv" in l || ".mp4" in l || "/hls" in l || "/live" in l
                        val keySaysStream = listOf("stream", "play", "live", "hls", "m3u8").any { it in kl }
                        if (looksLikeVideo || (keySaysStream && !(".png" in l || ".jpg" in l || ".webp" in l))) return v
                    }
                }
                for (k in ordered) {
                    val found = findStreamUrl(node.opt(k), depth + 1)
                    if (found.isNotBlank()) return found
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                val found = findStreamUrl(node.opt(i), depth + 1)
                if (found.isNotBlank()) return found
            }
        }
        return ""
    }

    private fun parseSportsArray(arr: JSONArray): List<SportsMatch> {
        val out = mutableListOf<SportsMatch>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val team1Obj = obj.optJSONObject("team1") ?: obj.optJSONObject("homeTeam") ?: obj.optJSONObject("home")
            val team2Obj = obj.optJSONObject("team2") ?: obj.optJSONObject("awayTeam") ?: obj.optJSONObject("away")

            val homeName = team1Obj?.firstNonBlank("name", "title", "shortName")
                ?: obj.firstNonBlank("homeTeamName", "team1Name", "home", "title")
            val awayName = team2Obj?.firstNonBlank("name", "title", "shortName")
                ?: obj.firstNonBlank("awayTeamName", "team2Name", "away")

            if (homeName.isBlank() && awayName.isBlank()) continue

            val score1 = team1Obj?.firstNonBlank("score") ?: obj.firstNonBlank("homeScore", "score1")
            val score2 = team2Obj?.firstNonBlank("score") ?: obj.firstNonBlank("awayScore", "score2")
            val scoreText = obj.firstNonBlank("score").ifBlank {
                if (score1.isNotBlank() || score2.isNotBlank()) "${score1.ifBlank { "0" }} - ${score2.ifBlank { "0" }}" else "VS"
            }

            val status = obj.firstNonBlank("statusText", "status", "matchStatus", "state")
            val isLive = obj.optBoolean("isLive", false) ||
                status.contains("live", ignoreCase = true) ||
                status.contains("playing", ignoreCase = true)

            out.add(
                SportsMatch(
                    id = obj.firstNonBlank("id", "matchId").ifBlank { "${homeName}_${awayName}_$i" },
                    league = obj.firstNonBlank("league", "leagueName", "tournament", "competition").ifBlank { "Live Sports" },
                    homeTeam = homeName.ifBlank { "Home" },
                    awayTeam = awayName.ifBlank { "Away" },
                    homeLogo = team1Obj?.extractImageUrl("logo", "avatar", "icon", "image").orEmpty(),
                    awayLogo = team2Obj?.extractImageUrl("logo", "avatar", "icon", "image").orEmpty(),
                    scoreText = scoreText,
                    statusText = status.ifBlank { if (isLive) "LIVE" else "Upcoming" },
                    startTime = obj.firstNonBlank("startTime", "time", "kickoff", "date"),
                    isLive = isLive,
                    streamUrl = obj.firstNonBlank("streamUrl", "url", "playUrl", "liveUrl", "hlsUrl", "m3u8", "playPath")
                        .takeIf { it.startsWith("http") } ?: findStreamUrl(obj, 0)
                )
            )
        }
        return out
    }

    fun parseReels(jsonString: String, safeMode: Boolean, workerBaseUrl: String = ""): List<ReelItem> {
        val list = mutableListOf<ReelItem>()
        val root = unwrapRoot(jsonString) ?: return emptyList()

        fun resolveVideoUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
            if (trimmed.startsWith("/") && workerBaseUrl.startsWith("http")) {
                return workerBaseUrl.trimEnd('/') + trimmed
            }
            return ""
        }

        fun parseArr(arr: JSONArray, categoryHint: String = "Trailer Reel") {
            for (i in 0 until arr.length()) {
                val rawObj = arr.optJSONObject(i) ?: continue
                val obj = rawObj.optJSONObject("subject") ?: rawObj
                val title = obj.firstNonBlank("title", "name", "subjectTitle").ifBlank { "Cinedor Reel" }
                val rawVideoUrl = obj.firstNonBlank(
                    "proxyUrl", "videoUrl", "playUrl", "streamUrl", "mp4", "url", "video", "trailerUrl", "mediaUrl"
                ).ifBlank {
                    obj.optJSONObject("video")?.firstNonBlank("url", "playUrl", "mp4")
                        ?: obj.optJSONObject("trailer")?.optJSONObject("videoAddress")?.firstNonBlank("url", "playUrl", "mp4")
                        ?: ""
                }
                val videoUrl = resolveVideoUrl(rawVideoUrl)
                if (videoUrl.isBlank()) continue
                if (safeMode && !isSafeText(title)) continue

                list.add(
                    ReelItem(
                        id = obj.firstNonBlank("id", "reelId", "subjectId").ifBlank { "reel_${list.size}" },
                        title = title,
                        description = obj.firstNonBlank("description", "overview", "caption", "subtitle"),
                        videoUrl = videoUrl,
                        posterUrl = obj.extractImageUrl("poster", "cover", "thumbnail", "image", "verticalCover"),
                        subjectId = obj.firstNonBlank("subjectId", "movieId", "id"),
                        source = obj.firstNonBlank("source").ifBlank { "moviebox" },
                        category = obj.firstNonBlank("type", "category", "tag").ifBlank { categoryHint },
                        durationText = obj.firstNonBlank("duration", "durationText"),
                        likesText = obj.firstNonBlank("likes", "likeCount", "views")
                    )
                )
            }
        }

        when (root) {
            is JSONArray -> parseArr(root)
            is JSONObject -> {
                val keys = listOf(
                    "items" to "Trailer Reel",
                    "reels" to "Trailer Reel",
                    "trailers" to "Trailer Reel",
                    "shorts" to "ShortTV Drama",
                    "shorttv" to "ShortTV Drama",
                    "shortTv" to "ShortTV Drama",
                    "dramas" to "ShortTV Drama",
                    "list" to "Trailer Reel",
                    "results" to "Trailer Reel",
                    "data" to "Trailer Reel"
                )
                for ((k, hint) in keys) {
                    val node = root.opt(k)
                    when (node) {
                        is JSONArray -> parseArr(node, hint)
                        is JSONObject -> {
                            listOf("items", "reels", "list", "trailers", "shorts").forEach { subKey ->
                                node.optJSONArray(subKey)?.let { parseArr(it, hint) }
                            }
                        }
                    }
                }
            }
        }
        return list.distinctBy { it.videoUrl }
    }

    /**
     * Extracts playable MP4 trailer/short reels directly from MovieBox subject lists
     * (trending / upcoming / home) when the edge worker is unavailable.
     */
    fun parseDirectReels(jsonString: String, safeMode: Boolean): List<ReelItem> {
        val list = mutableListOf<ReelItem>()
        val seenUrls = mutableSetOf<String>()
        val root = runCatching { JSONObject(jsonString) }.getOrNull() ?: return emptyList()
        val data = root.optJSONObject("data") ?: root

        fun collectFromArray(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val raw = arr.optJSONObject(i) ?: continue
                val obj = raw.optJSONObject("subject") ?: raw
                val title = obj.firstNonBlank("title", "name", "subjectTitle")
                if (title.isBlank()) continue
                if (safeMode && !isSafeText(title)) continue

                val trailerObj = obj.optJSONObject("trailer")
                val videoAddr = trailerObj?.optJSONObject("videoAddress")
                val videoUrl = videoAddr?.firstNonBlank("url", "playUrl", "mp4")
                    ?.ifBlank { trailerObj.firstNonBlank("url", "videoUrl", "playUrl") }
                    .orEmpty()
                if (!videoUrl.startsWith("http") || !seenUrls.add(videoUrl)) continue

                val poster = obj.extractImageUrl("cover", "poster", "posterUrl", "verticalCover", "thumbnail", "image")
                val subjectId = obj.firstNonBlank("subjectId", "id")
                val durationSec = videoAddr?.optInt("duration", 0) ?: 0
                val durationText = if (durationSec > 0) "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}" else ""
                val genre = obj.firstNonBlank("genre").split(",").firstOrNull()?.trim().orEmpty()

                list.add(
                    ReelItem(
                        id = subjectId.ifBlank { "mb_reel_$i" },
                        title = title,
                        description = obj.firstNonBlank("description", "overview", "intro"),
                        videoUrl = videoUrl,
                        posterUrl = poster,
                        subjectId = subjectId,
                        source = "moviebox",
                        category = genre.ifBlank { "Trailer Reel" },
                        durationText = durationText
                    )
                )
            }
        }

        listOf("subjectList", "items", "subjects", "list", "results").forEach { key ->
            data.optJSONArray(key)?.let { collectFromArray(it) }
        }
        return list
    }

    fun parseCategories(jsonString: String, safeMode: Boolean): List<CategoryChip> {
        val out = mutableListOf<CategoryChip>()
        val root = unwrapRoot(jsonString) ?: return emptyList()

        fun addChip(obj: JSONObject, group: String) {
            val name = obj.firstNonBlank("name", "title", "label")
            val path = obj.firstNonBlank("path", "slug", "category", "url", "id").ifBlank { name }
            if (name.isBlank()) return
            if (safeMode && (!isSafeText(name) || !isSafeText(path))) return
            out.add(
                CategoryChip(
                    id = "${group}_$path",
                    name = name,
                    path = path,
                    group = group,
                    imageUrl = obj.extractImageUrl("image", "icon", "logo", "cover")
                )
            )
        }

        when (root) {
            is JSONArray -> {
                for (i in 0 until root.length()) {
                    root.optJSONObject(i)?.let { addChip(it, "Categories") }
                }
            }
            is JSONObject -> {
                val groups = listOf(
                    "otts" to "OTT Platforms",
                    "ott" to "OTT Platforms",
                    "platforms" to "OTT Platforms",
                    "collections" to "Collections",
                    "genres" to "Genres",
                    "languages" to "Languages",
                    "special" to "Featured",
                    "wwe" to "WWE",
                    "adult" to "18+",
                    "categories" to "Categories",
                    "items" to "Categories"
                )
                for ((key, groupLabel) in groups) {
                    val arr = root.optJSONArray(key) ?: continue
                    for (i in 0 until arr.length()) {
                        val elem = arr.opt(i)
                        when (elem) {
                            is JSONObject -> addChip(elem, groupLabel)
                            is String -> {
                                if (!safeMode || isSafeText(elem)) {
                                    out.add(CategoryChip("${groupLabel}_$elem", elem, elem, groupLabel))
                                }
                            }
                        }
                    }
                }
            }
        }
        return out.distinctBy { "${it.group}:${it.path.lowercase()}" }
    }

    fun parseSuggestions(jsonString: String, safeMode: Boolean): List<String> {
        val out = mutableListOf<String>()
        val root = unwrapRoot(jsonString) ?: return emptyList()

        fun extract(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val elem = arr.opt(i)
                val text = when (elem) {
                    is String -> elem.trim()
                    is JSONObject -> elem.firstNonBlank("title", "keyword", "word", "name", "query", "text")
                    else -> ""
                }
                if (text.isNotBlank() && (!safeMode || isSafeText(text))) {
                    out.add(text)
                }
            }
        }

        when (root) {
            is JSONArray -> extract(root)
            is JSONObject -> {
                listOf("everyoneSearch", "suggestions", "items", "list", "keywords", "hotSearch", "results", "data", "moviebox", "movielinkbd")
                    .forEach { k ->
                        val node = root.opt(k)
                        when (node) {
                            is JSONArray -> extract(node)
                            is JSONObject -> node.optJSONArray("items")?.let { extract(it) }
                        }
                    }
            }
        }
        return out.distinct().take(15)
    }

    fun parseFilterOptions(jsonString: String, safeMode: Boolean): FilterOptions {
        val defaults = FilterOptions()
        val root = unwrapRoot(jsonString) as? JSONObject ?: return defaults

        fun stringList(vararg keys: String): List<String> {
            for (k in keys) {
                val arr = root.optJSONArray(k) ?: continue
                val items = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val elem = arr.opt(i)
                    val s = when (elem) {
                        is String -> elem.trim()
                        is JSONObject -> elem.firstNonBlank("name", "label", "title", "value")
                        else -> ""
                    }
                    if (s.isNotBlank() && (!safeMode || isSafeText(s))) items.add(s)
                }
                if (items.isNotEmpty()) return items.distinct()
            }
            return emptyList()
        }

        val genres = stringList("genres", "genre").ifEmpty { defaults.genres }
        val years = stringList("years", "year").ifEmpty { defaults.years }
        return defaults.copy(genres = genres, years = years)
    }

    fun parseMediaDetail(jsonString: String, fallbackItem: MediaItem): MediaDetail {
        val rootObj = runCatching { JSONObject(jsonString) }.getOrNull()
            ?: return MediaDetail(item = fallbackItem)
        val data = rootObj.optJSONObject("data") ?: rootObj
        val subject = data.optJSONObject("subject")
            ?: data.optJSONObject("movie")
            ?: data.optJSONObject("details")
            ?: data

        val parsedItem = parseMediaItem(subject, fallbackItem.source)?.let { p ->
            p.copy(
                id = p.id.ifBlank { fallbackItem.id },
                title = p.title.ifBlank { fallbackItem.title },
                posterUrl = p.posterUrl.ifBlank { fallbackItem.posterUrl },
                backdropUrl = p.backdropUrl.ifBlank { fallbackItem.backdropUrl.ifBlank { p.posterUrl } },
                rating = p.rating.ifBlank { fallbackItem.rating },
                year = p.year.ifBlank { fallbackItem.year },
                languageBadge = p.languageBadge.ifBlank { fallbackItem.languageBadge }
            )
        } ?: fallbackItem

        val synopsis = subject.firstNonBlank("description", "overview", "synopsis", "plot", "intro")
            .ifBlank { data.firstNonBlank("description", "overview", "synopsis") }
            .ifBlank { fallbackItem.description }

        val durationSeconds = subject.optInt("duration", 0)
        val durationText = subject.firstNonBlank("durationText", "runtime").ifBlank {
            if (durationSeconds > 60) "${durationSeconds / 60} min" else ""
        }

        // Parse seasons & episodes
        val seasons = mutableListOf<SeasonInfo>()
        val resourceObj = data.optJSONObject("resource") ?: subject.optJSONObject("resource")
        val seasonsArr = resourceObj?.optJSONArray("seasons")
            ?: data.optJSONArray("seasons")
            ?: subject.optJSONArray("seasons")
            ?: data.optJSONArray("episodesBySeason")

        if (seasonsArr != null) {
            for (i in 0 until seasonsArr.length()) {
                val sObj = seasonsArr.optJSONObject(i) ?: continue
                val rawSe = sObj.optInt("se", sObj.optInt("season", sObj.optInt("seasonNumber", i + 1)))
                val maxEp = sObj.optInt("maxEp", sObj.optInt("episodeCount", sObj.optInt("totalEpisodes", 0)))
                // MovieBox returns a single placeholder season with se=0 & maxEp=0 for movies; skip it so movies stay movies.
                if (rawSe == 0 && maxEp <= 0) continue
                val seNum = rawSe.coerceAtLeast(1)
                val epArr = sObj.optJSONArray("episodes") ?: sObj.optJSONArray("list")
                val episodes = mutableListOf<EpisodeInfo>()

                if (epArr != null && epArr.length() > 0) {
                    for (j in 0 until epArr.length()) {
                        val eObj = epArr.optJSONObject(j)
                        if (eObj != null) {
                            val epNum = eObj.optInt("ep", eObj.optInt("episode", eObj.optInt("number", j + 1)))
                            episodes.add(
                                EpisodeInfo(
                                    season = seNum,
                                    episode = epNum,
                                    title = eObj.firstNonBlank("title", "name").ifBlank { "Episode $epNum" },
                                    thumbnailUrl = eObj.extractImageUrl("thumbnail", "still", "image", "cover"),
                                    durationText = eObj.firstNonBlank("duration", "runtime")
                                )
                            )
                        } else {
                            val epNum = epArr.optInt(j, j + 1)
                            episodes.add(EpisodeInfo(seNum, epNum, "Episode $epNum"))
                        }
                    }
                } else if (maxEp > 0) {
                    for (epNum in 1..maxEp.coerceAtMost(200)) {
                        episodes.add(EpisodeInfo(seNum, epNum, "Episode $epNum"))
                    }
                }

                if (episodes.isNotEmpty()) {
                    seasons.add(
                        SeasonInfo(
                            seasonNumber = seNum,
                            title = sObj.firstNonBlank("title", "name").ifBlank { "Season $seNum" },
                            episodes = episodes
                        )
                    )
                }
            }
        }

        // If flat episodes array exists without seasons
        if (seasons.isEmpty()) {
            val flatEps = data.optJSONArray("episodes") ?: subject.optJSONArray("episodes")
            if (flatEps != null && flatEps.length() > 0) {
                val eps = (0 until flatEps.length()).mapNotNull { idx ->
                    val eObj = flatEps.optJSONObject(idx) ?: return@mapNotNull null
                    val epNum = eObj.optInt("ep", eObj.optInt("episode", idx + 1))
                    val seNum = eObj.optInt("se", eObj.optInt("season", 1))
                    EpisodeInfo(
                        season = seNum,
                        episode = epNum,
                        title = eObj.firstNonBlank("title", "name").ifBlank { "Episode $epNum" },
                        thumbnailUrl = eObj.extractImageUrl("thumbnail", "still", "image")
                    )
                }
                if (eps.isNotEmpty()) {
                    seasons.addAll(
                        eps.groupBy { it.season }.entries.sortedBy { it.key }.map { (se, list) ->
                            SeasonInfo(se, "Season $se", list)
                        }
                    )
                }
            }
        }

        // Parse cast/stars
        val cast = mutableListOf<CastMember>()
        val castArr = data.optJSONArray("stars")
            ?: data.optJSONArray("cast")
            ?: subject.optJSONArray("stars")
            ?: subject.optJSONArray("cast")
        if (castArr != null) {
            for (i in 0 until castArr.length()) {
                val cObj = castArr.optJSONObject(i) ?: continue
                val name = cObj.firstNonBlank("name", "title")
                if (name.isNotBlank()) {
                    cast.add(
                        CastMember(
                            id = cObj.firstNonBlank("staffId", "id").ifBlank { "cast_$i" },
                            name = name,
                            role = cObj.firstNonBlank("character", "role", "job"),
                            avatarUrl = cObj.extractImageUrl("avatarUrl", "avatar", "photo", "image")
                        )
                    )
                }
            }
        }

        // Parse audio/dubs list
        val audioLangs = mutableListOf<String>()
        val dubsArr = data.optJSONArray("dubs") ?: subject.optJSONArray("dubs") ?: resourceObj?.optJSONArray("dubs")
        if (dubsArr != null) {
            for (i in 0 until dubsArr.length()) {
                val elem = dubsArr.opt(i)
                val label = when (elem) {
                    is String -> elem.trim()
                    is JSONObject -> elem.firstNonBlank("lanName", "language", "name", "title")
                    else -> ""
                }
                if (label.isNotBlank()) audioLangs.add(label)
            }
        }

        val embeddedStreams = parseStreamsFromObject(data).toMutableList()
        // If no full stream is embedded, also extract the official MovieBox trailer as a playable fallback
        val trailerAddr = (subject.optJSONObject("trailer") ?: data.optJSONObject("trailer"))
            ?.optJSONObject("videoAddress")
        val trailerUrl = trailerAddr?.firstNonBlank("url", "playUrl", "mp4").orEmpty()
        if (embeddedStreams.isEmpty() && trailerUrl.startsWith("http")) {
            embeddedStreams.add(
                StreamOption(
                    id = "trailer_${parsedItem.id}",
                    label = "Official Trailer",
                    quality = "Trailer",
                    url = trailerUrl,
                    format = "MP4",
                    headers = mapOf(
                        "User-Agent" to MovieBoxDirectClient.CHROME_USER_AGENT,
                        "Referer" to MovieBoxDirectClient.DEFAULT_REFERER
                    )
                )
            )
        }
        val embeddedCaptions = parseCaptionsFromObject(data)

        return MediaDetail(
            item = parsedItem,
            synopsis = synopsis,
            durationText = durationText,
            seasonCount = seasons.size.coerceAtLeast(subject.optInt("seasonCount", 0)),
            seasons = seasons,
            cast = cast.take(16),
            audioLanguages = audioLangs.distinct(),
            streams = embeddedStreams,
            captions = embeddedCaptions
        )
    }

    fun parseStreams(
        jsonString: String,
        workerBaseUrl: String = ""
    ): Pair<List<StreamOption>, List<CaptionTrack>> {
        val root = runCatching { JSONObject(jsonString) }.getOrNull() ?: return emptyList<StreamOption>() to emptyList()
        val data = root.optJSONObject("data") ?: root
        return parseStreamsFromObject(data, workerBaseUrl) to parseCaptionsFromObject(data)
    }

    private fun parseStreamsFromObject(
        data: JSONObject,
        workerBaseUrl: String = ""
    ): List<StreamOption> {
        val out = mutableListOf<StreamOption>()
        val seenUrls = mutableSetOf<String>()

        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
            if (trimmed.startsWith("/") && workerBaseUrl.startsWith("http")) {
                return workerBaseUrl.trimEnd('/') + trimmed
            }
            return ""
        }

        fun extractHeaders(obj: JSONObject): Map<String, String> {
            val map = LinkedHashMap<String, String>()
            val rootRef = data.firstNonBlank("referer", "Referer")
            if (rootRef.startsWith("http")) map["Referer"] = rootRef
            val objRef = obj.firstNonBlank("referer", "Referer")
            if (objRef.startsWith("http")) map["Referer"] = objRef
            val hObj = obj.optJSONObject("headers") ?: data.optJSONObject("headers")
            if (hObj != null) {
                val keys = hObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = hObj.optString(k)
                    if (v.isNotBlank()) map[k] = v
                }
            }
            return map
        }

        fun addStream(
            rawUrl: String,
            quality: String,
            label: String,
            id: String,
            format: String,
            audio: String,
            size: String,
            headers: Map<String, String>
        ) {
            val url = normalizeUrl(rawUrl)
            if (url.isBlank() || !seenUrls.add(url)) return
            out.add(
                StreamOption(
                    id = id.ifBlank { "stream_${out.size}" },
                    label = label.ifBlank { quality.ifBlank { "Auto HD" } },
                    quality = quality.ifBlank { "HD" },
                    url = url,
                    format = format.ifBlank {
                        when {
                            url.contains(".m3u8", ignoreCase = true) -> "HLS"
                            url.contains(".mpd", ignoreCase = true) -> "DASH"
                            else -> "MP4"
                        }
                    },
                    audioLanguage = audio,
                    sizeText = size,
                    headers = headers
                )
            )
        }

        fun parseArray(arr: JSONArray, sourceTag: String = "") {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val quality = obj.firstNonBlank("resolutions", "resolution", "quality", "label", "name")
                    .let { if (it.all(Char::isDigit) && it.isNotBlank()) "${it}p" else it }
                val audio = obj.firstNonBlank("audio", "language", "lanName", "dub")
                val baseLabel = listOf(quality, audio, sourceTag).filter { it.isNotBlank() }.joinToString(" • ")
                val streamId = obj.firstNonBlank("id", "streamId")
                val format = obj.firstNonBlank("format", "type")
                val size = obj.firstNonBlank("size", "fileSize")
                val headers = extractHeaders(obj)

                val proxyUrl = obj.firstNonBlank("proxyUrl", "proxy", "streamProxy", "proxyStreamUrl", "workerUrl")
                if (proxyUrl.isNotBlank()) {
                    addStream(
                        rawUrl = proxyUrl,
                        quality = quality,
                        label = listOf(quality.ifBlank { "HD" }, "Proxy").joinToString(" • "),
                        id = streamId,
                        format = format,
                        audio = audio,
                        size = size,
                        headers = headers
                    )
                }

                val url = obj.firstNonBlank("url", "streamUrl", "playUrl", "file", "src", "hls", "mp4")
                if (url.isNotBlank()) {
                    addStream(
                        rawUrl = url,
                        quality = quality,
                        label = baseLabel,
                        id = streamId,
                        format = format,
                        audio = audio,
                        size = size,
                        headers = headers
                    )
                }
            }
        }

        listOf("streams", "processedSources", "sources", "links", "downloads", "qualities", "urls", "files", "items").forEach { k ->
            data.optJSONArray(k)?.let { parseArray(it) }
        }

        listOf("moviebox" to "MBox", "movielinkbd" to "MLBD").forEach { (srcKey, tag) ->
            val sub = data.optJSONObject(srcKey)
            if (sub != null) {
                listOf("streams", "processedSources", "sources", "links", "urls").forEach { k ->
                    sub.optJSONArray(k)?.let { parseArray(it, tag) }
                }
                val proxy = sub.firstNonBlank("proxyUrl", "proxy", "streamProxy")
                if (proxy.isNotBlank()) {
                    addStream(proxy, "HD", "$tag • Proxy", "${srcKey}_proxy", "", "", "", extractHeaders(sub))
                }
                val direct = sub.firstNonBlank("streamUrl", "url", "playUrl", "hls", "mp4")
                if (direct.isNotBlank()) {
                    addStream(direct, "HD", tag, srcKey, "", "", "", extractHeaders(sub))
                }
            }
        }

        val rootProxy = data.firstNonBlank("proxyUrl", "proxy", "streamProxy", "workerUrl")
        if (rootProxy.isNotBlank()) {
            addStream(rootProxy, data.firstNonBlank("quality", "resolution").ifBlank { "HD" }, "Edge Proxy", "proxy", "", "", "", extractHeaders(data))
        }

        val directUrl = data.firstNonBlank("streamUrl", "playUrl", "url", "hls", "mp4", "videoUrl")
        if (directUrl.isNotBlank()) {
            addStream(directUrl, data.firstNonBlank("quality", "resolution").ifBlank { "HD" }, "Direct Stream", "direct", "", "", "", extractHeaders(data))
        }

        return out.sortedWith(
            compareByDescending<StreamOption> {
                it.label.contains("Proxy", ignoreCase = true) ||
                    (workerBaseUrl.isNotBlank() && it.url.startsWith(workerBaseUrl))
            }.thenByDescending {
                Regex("(\\d{3,4})").find(it.quality)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
            }
        )
    }

    fun parseCaptions(jsonString: String): List<CaptionTrack> {
        val root = runCatching { JSONObject(jsonString) }.getOrNull() ?: return emptyList()
        val data = root.optJSONObject("data") ?: root
        return parseCaptionsFromObject(data)
    }

    private fun parseCaptionsFromObject(data: JSONObject): List<CaptionTrack> {
        val out = mutableListOf<CaptionTrack>()
        val seen = mutableSetOf<String>()
        val arr = data.optJSONArray("captions")
            ?: data.optJSONArray("subtitles")
            ?: data.optJSONArray("tracks")
            ?: return emptyList()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.firstNonBlank("url", "src", "file", "subtitleUrl")
            if (!url.startsWith("http") || !seen.add(url)) continue
            val lang = obj.firstNonBlank("lan", "lang", "language", "code").ifBlank { "en" }
            val label = obj.firstNonBlank("lanName", "label", "name", "language").ifBlank { lang.uppercase() }
            out.add(
                CaptionTrack(
                    id = obj.firstNonBlank("id").ifBlank { "cap_$i" },
                    language = lang,
                    label = label,
                    url = url
                )
            )
        }
        return out
    }

    fun parseComments(jsonString: String): List<CommentItem> {
        val out = mutableListOf<CommentItem>()
        val root = unwrapRoot(jsonString) ?: return emptyList()

        fun parseArr(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val text = obj.firstNonBlank("content", "text", "comment", "body", "message")
                if (text.isBlank()) continue
                val userObj = obj.optJSONObject("user") ?: obj.optJSONObject("author")
                val author = userObj?.firstNonBlank("nickname", "name", "username")
                    ?: obj.firstNonBlank("nickname", "author", "username", "userName")
                    .ifBlank { "Cinedor Viewer" }
                val avatar = userObj?.extractImageUrl("avatar", "avatarUrl", "photo")
                    ?: obj.extractImageUrl("avatar", "avatarUrl")
                out.add(
                    CommentItem(
                        id = obj.firstNonBlank("id", "commentId").ifBlank { "c_$i" },
                        author = author,
                        avatarUrl = avatar,
                        text = text,
                        timeText = obj.firstNonBlank("createdAt", "time", "date", "timeAgo"),
                        likes = obj.optInt("likeCount", obj.optInt("likes", 0))
                    )
                )
            }
        }

        when (root) {
            is JSONArray -> parseArr(root)
            is JSONObject -> {
                listOf("comments", "postList", "items", "list", "results", "data").forEach { k ->
                    root.optJSONArray(k)?.let { parseArr(it) }
                }
            }
        }
        return out
    }

    fun parseStaffDetail(
        jsonString: String,
        fallbackMember: CastMember,
        safeMode: Boolean
    ): StaffDetail {
        val root = runCatching { JSONObject(jsonString) }.getOrNull()
            ?: return StaffDetail(member = fallbackMember)
        val data = root.optJSONObject("data") ?: root
        val staffObj = data.optJSONObject("staff") ?: data.optJSONObject("person") ?: data

        val name = staffObj.firstNonBlank("name", "title").ifBlank { fallbackMember.name }
        val role = staffObj.firstNonBlank("job", "role", "profession", "character").ifBlank { fallbackMember.role }
        val avatar = staffObj.extractImageUrl("avatarUrl", "avatar", "photo", "image").ifBlank { fallbackMember.avatarUrl }
        val bio = staffObj.firstNonBlank("description", "biography", "bio", "intro", "overview")

        val works = parseMediaList(jsonString, safeMode, "moviebox")
        return StaffDetail(
            member = fallbackMember.copy(name = name, role = role, avatarUrl = avatar),
            bio = bio,
            works = works
        )
    }

    fun parseStatus(jsonString: String, latencyMs: Long): ApiStatusInfo {
        val obj = runCatching { JSONObject(jsonString) }.getOrNull()
        val statusText = obj?.firstNonBlank("status", "message", "state")?.ifBlank { "Operational" } ?: "Operational"
        val version = obj?.firstNonBlank("version", "apiVersion").orEmpty()
        return ApiStatusInfo(
            isOnline = true,
            statusText = statusText,
            version = version,
            latencyMs = latencyMs
        )
    }
}
