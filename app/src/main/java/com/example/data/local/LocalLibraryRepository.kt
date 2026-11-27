package com.example.data.local

import com.example.data.model.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class LocalLibraryRepository(
    private val dao: LibraryDao
) {
    fun observeWatchlist(): Flow<List<WatchlistItem>> = dao.observeWatchlist()

    fun observeHistory(): Flow<List<WatchHistoryItem>> = dao.observeHistory()

    fun observeReminders(): Flow<List<UpcomingReminder>> = dao.observeReminders()

    suspend fun toggleWatchlist(item: MediaItem, currentlyInList: Boolean): Result<Boolean> {
        val docId = sanitizeLocalDocId(item.id, item.title)
        return try {
            if (currentlyInList) {
                dao.deleteWatchlistById(docId)
                Result.success(false)
            } else {
                dao.upsertWatchlist(
                    WatchlistItem(
                        id = docId,
                        contentId = item.id.ifBlank { docId }.take(128),
                        title = item.title.ifBlank { "Untitled" }.take(200),
                        posterUrl = item.posterUrl.take(1024),
                        mediaType = item.mediaType.wireValue,
                        source = item.source.ifBlank { "moviebox" }.take(32),
                        year = item.year.take(16),
                        rating = item.rating.take(16),
                        languageBadge = item.languageBadge.take(48),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveWatchProgress(
        item: MediaItem,
        season: Int,
        episode: Int,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> {
        // Titles from the PIN-locked 18+ Zone are never saved, so they can't show up in
        // Continue Watching or history outside it.
        if (AdultTitles.contains(item)) return Result.success(Unit)
        val docId = sanitizeLocalDocId(item.id, item.title)
        return try {
            dao.upsertHistory(
                WatchHistoryItem(
                    id = docId,
                    contentId = item.id.ifBlank { docId }.take(128),
                    title = item.title.ifBlank { "Untitled" }.take(200),
                    posterUrl = item.posterUrl.take(1024),
                    mediaType = item.mediaType.wireValue,
                    source = item.source.ifBlank { "moviebox" }.take(32),
                    season = season.coerceIn(1, 999),
                    episode = episode.coerceIn(1, 9999),
                    positionMs = positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeHistoryItem(docId: String): Result<Unit> {
        return try {
            dao.deleteHistoryById(docId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllHistory(): Result<Unit> {
        return try {
            dao.clearHistory()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleReminder(item: MediaItem, currentlyBooked: Boolean): Result<Boolean> {
        val docId = sanitizeLocalDocId(item.id, item.title)
        return try {
            if (currentlyBooked) {
                dao.deleteReminderById(docId)
                Result.success(false)
            } else {
                dao.upsertReminder(
                    UpcomingReminder(
                        id = docId,
                        contentId = item.id.ifBlank { docId }.take(128),
                        title = item.title.ifBlank { "Upcoming Release" }.take(200),
                        posterUrl = item.posterUrl.take(1024),
                        releaseDate = item.releaseDate.ifBlank { item.year }.take(64),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
