package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MediaRecord
import com.example.data.model.SavedStream
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<MediaRecord>>

    @Query("SELECT * FROM media_history WHERE isFavorite = 1 ORDER BY lastPlayedTimestamp DESC")
    fun getFavorites(): Flow<List<MediaRecord>>

    @Query("SELECT * FROM media_history WHERE uri = :uri LIMIT 1")
    suspend fun getRecord(uri: String): MediaRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: MediaRecord)

    @Query("UPDATE media_history SET lastPositionMs = :positionMs, durationMs = :durationMs, lastPlayedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updatePosition(uri: String, positionMs: Long, durationMs: Long, timestamp: Long)

    @Query("UPDATE media_history SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query("DELETE FROM media_history WHERE uri = :uri")
    suspend fun deleteHistory(uri: String)

    @Query("DELETE FROM media_history")
    suspend fun clearAllHistory()

    // Streams
    @Query("SELECT * FROM saved_streams ORDER BY addedTimestamp DESC")
    fun getAllStreams(): Flow<List<SavedStream>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: SavedStream)

    @Query("DELETE FROM saved_streams WHERE id = :id")
    suspend fun deleteStream(id: Int)
}
