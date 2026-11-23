package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("SELECT * FROM watchlist ORDER BY updatedAtMillis DESC")
    fun observeWatchlist(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlist(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteWatchlistById(id: String)

    @Query("SELECT * FROM watch_history ORDER BY updatedAtMillis DESC LIMIT 40")
    fun observeHistory(): Flow<List<WatchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM upcoming_reminders ORDER BY updatedAtMillis DESC")
    fun observeReminders(): Flow<List<UpcomingReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(item: UpcomingReminder)

    @Query("DELETE FROM upcoming_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)
}
