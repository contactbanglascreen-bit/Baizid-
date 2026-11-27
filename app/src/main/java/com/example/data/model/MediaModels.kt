package com.example.data.model

enum class ContentSource(val apiValue: String, val label: String) {
    // Labels are what viewers see: the content providers' names are not shown in the app.
    ALL("all", "All Servers"),
    MOVIEBOX("moviebox", "Server 1"),
    MOVIELINKBD("movielinkbd", "Server 2");

    companion object {
        fun fromApi(value: String?): ContentSource =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: ALL
    }
}

enum class ContentLanguage(val code: String, val label: String) {
    EN("en", "English"),
    HI("hi", "Hindi"),
    BN("bn", "Bengali"),
    AR("ar", "Arabic"),
    FR("fr", "French"),
    ID("id", "Indonesian"),
    UR("ur", "Urdu");

    companion object {
        fun fromCode(code: String?): ContentLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

enum class MediaCategoryType(val wireValue: String, val label: String) {
    MOVIE("MOVIE", "Movie"),
    SERIES("SERIES", "Series"),
    ANIME("ANIME", "Anime"),
    DRAMA("DRAMA", "Drama"),
    SHORT("SHORT", "Short");

    companion object {
        fun fromRaw(raw: String?): MediaCategoryType {
            val lower = raw?.lowercase().orEmpty()
            return when {
                "anime" in lower -> ANIME
                "drama" in lower || "kdrama" in lower || "cdrama" in lower -> DRAMA
                "short" in lower || "reel" in lower -> SHORT
                "tv" in lower || "series" in lower || "season" in lower || "show" in lower -> SERIES
                else -> MOVIE
            }
        }
    }
}

data class MediaItem(
    val id: String,
    val title: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val mediaType: MediaCategoryType = MediaCategoryType.MOVIE,
    val source: String = "moviebox",
    val year: String = "",
    val rating: String = "",
    val languageBadge: String = "",
    val qualityBadge: String = "",
    val genres: List<String> = emptyList(),
    val country: String = "",
    val releaseDate: String = "",
    val bookedCount: String = "",
    val rank: Int? = null,
    val detailPath: String = "",
    val description: String = ""
)

data class HomeSection(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val categoryPath: String = "",
    val isRanked: Boolean = false,
    val isUpcoming: Boolean = false
)

data class SportsMatch(
    val id: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogo: String = "",
    val awayLogo: String = "",
    val scoreText: String = "",
    val statusText: String = "",
    val startTime: String = "",
    val isLive: Boolean = false,
    val streamUrl: String = ""
)

data class HomeFeed(
    val banners: List<MediaItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val sportsMatches: List<SportsMatch> = emptyList(),
    val upcomingItems: List<MediaItem> = emptyList(),
    val trendingItems: List<MediaItem> = emptyList()
)

data class SeasonInfo(
    val seasonNumber: Int,
    val title: String,
    val episodes: List<EpisodeInfo>
)

data class EpisodeInfo(
    val season: Int,
    val episode: Int,
    val title: String,
    val thumbnailUrl: String = "",
    val durationText: String = ""
)

data class StreamOption(
    val id: String,
    val label: String,
    val quality: String,
    val url: String,
    val format: String = "",
    val audioLanguage: String = "",
    val sizeText: String = "",
    val headers: Map<String, String> = emptyMap()
)

data class CaptionTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String
)

data class CommentItem(
    val id: String,
    val author: String,
    val avatarUrl: String = "",
    val text: String,
    val timeText: String = "",
    val likes: Int = 0
)

data class CastMember(
    val id: String,
    val name: String,
    val role: String = "",
    val avatarUrl: String = ""
)

data class StaffDetail(
    val member: CastMember,
    val bio: String = "",
    val works: List<MediaItem> = emptyList()
)

data class MediaDetail(
    val item: MediaItem,
    val synopsis: String = "",
    val durationText: String = "",
    val seasonCount: Int = 0,
    val seasons: List<SeasonInfo> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val audioLanguages: List<String> = emptyList(),
    val streams: List<StreamOption> = emptyList(),
    val captions: List<CaptionTrack> = emptyList(),
    val related: List<MediaItem> = emptyList(),
    val comments: List<CommentItem> = emptyList()
)

data class ReelItem(
    val id: String,
    val title: String,
    val description: String = "",
    val videoUrl: String,
    val posterUrl: String = "",
    val subjectId: String = "",
    val source: String = "moviebox",
    val category: String = "Reel",
    val durationText: String = "",
    val likesText: String = ""
)

data class CategoryChip(
    val id: String,
    val name: String,
    val path: String,
    val group: String = "Collections",
    val imageUrl: String = ""
)

data class FilterOptions(
    val genres: List<String> = listOf("Action", "Drama", "Anime", "Sci-Fi", "Thriller", "Comedy", "Romance", "Horror", "Fantasy"),
    val years: List<String> = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020"),
    val categories: List<Pair<String, String>> = listOf(
        "movies" to "Movies",
        "tv" to "TV Series",
        "anime" to "Anime",
        "drama" to "Dramas"
    ),
    val platforms: List<Pair<String, String>> = listOf(
        "Netflix" to "Netflix",
        "Hoichoi" to "Hoichoi",
        "PrimeVideo" to "Prime Video",
        "Zee5" to "ZEE5",
        "Disney" to "Disney+",
        "AppleTV" to "Apple TV+",
        "Viu" to "Viu",
        "Hulu" to "Hulu",
        "chorki" to "Chorki",
        "Showmax" to "Showmax"
    )
)

data class ApiStatusInfo(
    val isOnline: Boolean,
    val statusText: String,
    val version: String = "",
    val latencyMs: Long = 0L
)
