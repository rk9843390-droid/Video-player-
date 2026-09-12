package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_history")
data class MediaRecord(
    @PrimaryKey
    val uri: String,
    val title: String,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val thumbnailUrl: String? = null,
    val resolution: String? = null,
    val isStream: Boolean = false
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
