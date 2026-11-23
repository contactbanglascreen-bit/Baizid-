package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.MediaCategoryType
import com.example.data.model.MediaItem
import java.security.MessageDigest

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val id: String,
    val contentId: String,
    val title: String,
    val posterUrl: String = "",
    val mediaType: String = MediaCategoryType.MOVIE.wireValue,
    val source: String = "moviebox",
    val year: String = "",
    val rating: String = "",
    val languageBadge: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = contentId.ifBlank { id },
        title = title,
        posterUrl = posterUrl,
        mediaType = MediaCategoryType.entries.firstOrNull { it.wireValue == mediaType }
            ?: MediaCategoryType.MOVIE,
        source = source,
        year = year,
        rating = rating,
        languageBadge = languageBadge
    )
}

@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val id: String,
    val contentId: String,
    val title: String,
    val posterUrl: String = "",
    val mediaType: String = MediaCategoryType.MOVIE.wireValue,
    val source: String = "moviebox",
    val season: Int = 1,
    val episode: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    fun toMediaItem(): MediaItem = MediaItem(
        id = contentId.ifBlank { id },
        title = title,
        posterUrl = posterUrl,
        mediaType = MediaCategoryType.entries.firstOrNull { it.wireValue == mediaType }
            ?: MediaCategoryType.MOVIE,
        source = source
    )
}

@Entity(tableName = "upcoming_reminders")
data class UpcomingReminder(
    @PrimaryKey val id: String,
    val contentId: String,
    val title: String,
    val posterUrl: String = "",
    val releaseDate: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Produces a deterministic, safe local Room primary key for a media item.
 */
fun sanitizeLocalDocId(rawId: String, fallbackTitle: String = ""): String {
    val base = rawId.ifBlank { fallbackTitle }.trim()
    val cleaned = base.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .take(96)
    if (cleaned.isNotBlank()) return cleaned
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(base.ifBlank { "item" }.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return "cp_${digest.take(24)}"
}
