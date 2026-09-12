package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.db.MediaDao
import com.example.data.model.MediaRecord
import com.example.data.model.SavedStream
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    // Built-in curated open-source showcase videos
    private val showcaseVideos = listOf(
        VideoItem(
            id = "showcase_1",
            title = "Big Buck Bunny",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            durationMs = 596000L,
            resolution = "1080p FHD",
            sizeText = "158 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=600&auto=format&fit=crop&q=80",
            category = "Animation"
        ),
        VideoItem(
            id = "showcase_2",
            title = "Sintel - The Quest",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            durationMs = 888000L,
            resolution = "4K UHD",
            sizeText = "240 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
            category = "Fantasy"
        ),
        VideoItem(
            id = "showcase_3",
            title = "Tears of Steel (Sci-Fi)",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            durationMs = 734000L,
            resolution = "1080p 60fps",
            sizeText = "192 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            category = "Sci-Fi"
        ),
        VideoItem(
            id = "showcase_4",
            title = "Elephants Dream",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            durationMs = 653000L,
            resolution = "1080p",
            sizeText = "142 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            category = "CGI Surreal"
        ),
        VideoItem(
            id = "showcase_5",
            title = "For Bigger Blazes",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            durationMs = 15000L,
            resolution = "4K Ultra",
            sizeText = "35 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=600&auto=format&fit=crop&q=80",
            category = "Demo"
        ),
        VideoItem(
            id = "showcase_6",
            title = "We Are Going On Bullrun",
            uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            durationMs = 47000L,
            resolution = "720p",
            sizeText = "68 MB",
            thumbnailUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=600&auto=format&fit=crop&q=80",
            category = "Action"
        )
    )

    // Curated Test Live & HLS Network Streams
    val defaultStreams = listOf(
        SavedStream(
            id = -1,
            title = "Big Buck Bunny (HLS Multi-Bitrate)",
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            category = "HLS Stream"
        ),
        SavedStream(
            id = -2,
            title = "Tears of Steel (Adaptive HLS Stream)",
            url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            category = "HLS Live"
        ),
        SavedStream(
            id = -3,
            title = "Apple BipBop 16x9 (Test Stream)",
            url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            category = "HLS Test"
        )
    )

    val allHistory: Flow<List<MediaRecord>> = mediaDao.getAllHistory()
    val favorites: Flow<List<MediaRecord>> = mediaDao.getFavorites()
    val savedStreams: Flow<List<SavedStream>> = mediaDao.getAllStreams()

    // Combines showcase + local media + Room history
    fun getAllVideosStream(): Flow<List<VideoItem>> {
        return mediaDao.getAllHistory().combine(mediaDao.getFavorites()) { history, favs ->
            val favUris = favs.map { it.uri }.toSet()
            val historyMap = history.associateBy { it.uri }

            val localVideos = queryLocalVideos()
            val combinedList = (showcaseVideos + localVideos).distinctBy { it.uri }.map { item ->
                val record = historyMap[item.uri]
                item.copy(
                    isFavorite = favUris.contains(item.uri),
                    lastPositionMs = record?.lastPositionMs ?: 0L,
                    durationMs = if (record != null && record.durationMs > 0) record.durationMs else item.durationMs
                )
            }
            combinedList
        }.flowOn(Dispatchers.IO)
    }

    private fun queryLocalVideos(): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        try {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val title = it.getString(titleCol) ?: "Video $id"
                    val duration = it.getLong(durCol)
                    val sizeBytes = it.getLong(sizeCol)
                    val width = if (widthCol >= 0) it.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) it.getInt(heightCol) else 0
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else "HD"

                    result.add(
                        VideoItem(
                            id = "local_$id",
                            title = title,
                            uri = contentUri.toString(),
                            durationMs = duration,
                            resolution = resolution,
                            sizeText = formatFileSize(sizeBytes),
                            thumbnailUrl = contentUri.toString(),
                            isStream = false,
                            category = "Local Files"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or security exception fallback
        }
        return result
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    suspend fun savePlaybackPosition(uri: String, title: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getRecord(uri)
        if (existing == null) {
            mediaDao.upsertRecord(
                MediaRecord(
                    uri = uri,
                    title = title,
                    durationMs = durationMs,
                    lastPositionMs = positionMs,
                    lastPlayedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            mediaDao.updatePosition(
                uri = uri,
                positionMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else existing.durationMs,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun toggleFavorite(video: VideoItem) = withContext(Dispatchers.IO) {
        val newFav = !video.isFavorite
        val existing = mediaDao.getRecord(video.uri)
        if (existing == null) {
            mediaDao.upsertRecord(
                MediaRecord(
                    uri = video.uri,
                    title = video.title,
                    durationMs = video.durationMs,
                    isFavorite = newFav,
                    thumbnailUrl = video.thumbnailUrl,
                    resolution = video.resolution
                )
            )
        } else {
            mediaDao.setFavorite(video.uri, newFav)
        }
    }

    suspend fun deleteHistoryItem(uri: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteHistory(uri)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        mediaDao.clearAllHistory()
    }

    suspend fun addSavedStream(title: String, url: String) = withContext(Dispatchers.IO) {
        mediaDao.insertStream(SavedStream(title = title.ifBlank { url }, url = url))
    }

    suspend fun deleteSavedStream(id: Int) = withContext(Dispatchers.IO) {
        mediaDao.deleteStream(id)
    }

    suspend fun getResumePosition(uri: String): Long = withContext(Dispatchers.IO) {
        mediaDao.getRecord(uri)?.lastPositionMs ?: 0L
    }
}
