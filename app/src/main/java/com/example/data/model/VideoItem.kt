package com.example.data.model

data class VideoItem(
    val id: String,
    val title: String,
    val uri: String,
    val durationMs: Long,
    val resolution: String = "1080p",
    val sizeText: String = "",
    val thumbnailUrl: String = "",
    val isStream: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val category: String = "Movies"
) {
    val durationText: String
        get() = formatDuration(durationMs)

    val progressPercent: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "Live"
            val totalSeconds = durationMs / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
