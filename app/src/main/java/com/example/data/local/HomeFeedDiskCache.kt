package com.example.data.local

import com.example.data.model.HomeFeed
import com.example.data.model.HomeSection
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import com.example.data.model.SportsMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Saves the last successful Home feed on the phone so the app can show it instantly
 * on the next launch (posters start loading right away) while a fresh copy downloads.
 */
class HomeFeedDiskCache(dir: File) {
    private val file = File(dir, "home_feed_cache.json")

    suspend fun read(key: String): HomeFeed? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching null
            val root = JSONObject(file.readText())
            if (root.optString("key") != key) return@runCatching null
            val f = root.getJSONObject("feed")
            HomeFeed(
                banners = f.optJSONArray("banners").toItems(),
                sections = f.optJSONArray("sections").let { arr ->
                    if (arr == null) emptyList() else List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        HomeSection(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            items = o.optJSONArray("items").toItems(),
                            categoryPath = o.optString("categoryPath"),
                            isRanked = o.optBoolean("isRanked"),
                            isUpcoming = o.optBoolean("isUpcoming")
                        )
                    }
                },
                sportsMatches = f.optJSONArray("sports").let { arr ->
                    if (arr == null) emptyList() else List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        SportsMatch(
                            id = o.optString("id"),
                            league = o.optString("league"),
                            homeTeam = o.optString("homeTeam"),
                            awayTeam = o.optString("awayTeam"),
                            homeLogo = o.optString("homeLogo"),
                            awayLogo = o.optString("awayLogo"),
                            scoreText = o.optString("scoreText"),
                            statusText = o.optString("statusText"),
                            startTime = o.optString("startTime"),
                            isLive = o.optBoolean("isLive"),
                            streamUrl = o.optString("streamUrl")
                        )
                    }
                },
                upcomingItems = f.optJSONArray("upcoming").toItems(),
                trendingItems = f.optJSONArray("trending").toItems()
            )
        }.getOrNull()
    }

    suspend fun write(key: String, feed: HomeFeed) = withContext(Dispatchers.IO) {
        runCatching {
            val f = JSONObject()
                .put("banners", feed.banners.toJson())
                .put("sections", JSONArray().apply {
                    feed.sections.forEach { s ->
                        put(
                            JSONObject()
                                .put("id", s.id)
                                .put("title", s.title)
                                .put("items", s.items.toJson())
                                .put("categoryPath", s.categoryPath)
                                .put("isRanked", s.isRanked)
                                .put("isUpcoming", s.isUpcoming)
                        )
                    }
                })
                .put("sports", JSONArray().apply {
                    feed.sportsMatches.forEach { m ->
                        put(
                            JSONObject()
                                .put("id", m.id)
                                .put("league", m.league)
                                .put("homeTeam", m.homeTeam)
                                .put("awayTeam", m.awayTeam)
                                .put("homeLogo", m.homeLogo)
                                .put("awayLogo", m.awayLogo)
                                .put("scoreText", m.scoreText)
                                .put("statusText", m.statusText)
                                .put("startTime", m.startTime)
                                .put("isLive", m.isLive)
                                .put("streamUrl", m.streamUrl)
                        )
                    }
                })
                .put("upcoming", feed.upcomingItems.toJson())
                .put("trending", feed.trendingItems.toJson())
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(JSONObject().put("key", key).put("feed", f).toString())
            tmp.renameTo(file)
        }
    }

    private fun List<MediaItem>.toJson(): JSONArray = JSONArray().apply {
        forEach { m ->
            put(
                JSONObject()
                    .put("id", m.id)
                    .put("title", m.title)
                    .put("posterUrl", m.posterUrl)
                    .put("backdropUrl", m.backdropUrl)
                    .put("mediaType", m.mediaType.name)
                    .put("source", m.source)
                    .put("year", m.year)
                    .put("rating", m.rating)
                    .put("languageBadge", m.languageBadge)
                    .put("qualityBadge", m.qualityBadge)
                    .put("genres", JSONArray(m.genres))
                    .put("country", m.country)
                    .put("releaseDate", m.releaseDate)
                    .put("bookedCount", m.bookedCount)
                    .put("rank", m.rank ?: -1)
                    .put("detailPath", m.detailPath)
                    .put("description", m.description)
            )
        }
    }

    private fun JSONArray?.toItems(): List<MediaItem> {
        if (this == null) return emptyList()
        return List(length()) { i ->
            val o = getJSONObject(i)
            val genres = o.optJSONArray("genres")
            MediaItem(
                id = o.optString("id"),
                title = o.optString("title"),
                posterUrl = o.optString("posterUrl"),
                backdropUrl = o.optString("backdropUrl"),
                mediaType = runCatching { MediaCategoryType.valueOf(o.optString("mediaType")) }
                    .getOrDefault(MediaCategoryType.MOVIE),
                source = o.optString("source", "moviebox"),
                year = o.optString("year"),
                rating = o.optString("rating"),
                languageBadge = o.optString("languageBadge"),
                qualityBadge = o.optString("qualityBadge"),
                genres = if (genres == null) emptyList() else List(genres.length()) { g -> genres.optString(g) },
                country = o.optString("country"),
                releaseDate = o.optString("releaseDate"),
                bookedCount = o.optString("bookedCount"),
                rank = o.optInt("rank", -1).takeIf { it >= 0 },
                detailPath = o.optString("detailPath"),
                description = o.optString("description")
            )
        }
    }
}
