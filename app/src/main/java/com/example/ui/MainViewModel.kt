package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MediaRecord
import com.example.data.model.SavedStream
import com.example.data.model.VideoItem
import com.example.data.repository.MediaRepository
import com.example.player.EqualizerData
import com.example.player.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VlcTab {
    VIDEOS,
    STREAMS,
    PLAYLISTS,
    EQUALIZER
}

enum class SortOption(val title: String) {
    NAME("Name"),
    RECENT("Recently Played"),
    DURATION("Duration"),
    SIZE("Size")
}

data class UiState(
    val selectedTab: VlcTab = VlcTab.VIDEOS,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val sortOption: SortOption = SortOption.RECENT,
    val isGridView: Boolean = true,
    val activePlayingVideo: VideoItem? = null,
    val currentEqualizerPreset: EqualizerPreset = EqualizerData.presets[0],
    val customBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val audioBoostEnabled: Boolean = true,
    val nightModeEnabled: Boolean = false,
    val hardwareAcceleration: Boolean = true
)

class MainViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val videos: StateFlow<List<VideoItem>> = mediaRepository.getAllVideosStream()
        .combine(_uiState) { videoList, state ->
            var filtered = videoList

            // Category filter
            if (state.selectedCategory != "All") {
                filtered = filtered.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
            }

            // Search filter
            if (state.searchQuery.isNotBlank()) {
                filtered = filtered.filter {
                    it.title.contains(state.searchQuery, ignoreCase = true) ||
                    it.category.contains(state.searchQuery, ignoreCase = true)
                }
            }

            // Sort
            when (state.sortOption) {
                SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
                SortOption.RECENT -> filtered.sortedByDescending { it.lastPositionMs }
                SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
                SortOption.SIZE -> filtered.sortedByDescending { it.sizeText }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<MediaRecord>> = mediaRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<MediaRecord>> = mediaRepository.favorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedStreams: StateFlow<List<SavedStream>> = mediaRepository.savedStreams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultStreams = mediaRepository.defaultStreams

    fun selectTab(tab: VlcTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(cat: String) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    fun setSortOption(sort: SortOption) {
        _uiState.update { it.copy(sortOption = sort) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun playVideo(video: VideoItem) {
        _uiState.update { it.copy(activePlayingVideo = video) }
    }

    fun playStream(url: String, title: String = "Network Stream") {
        val streamItem = VideoItem(
            id = "stream_${System.currentTimeMillis()}",
            title = title.ifBlank { url },
            uri = url,
            durationMs = 0L,
            resolution = "Live HLS / Stream",
            isStream = true,
            category = "Network Stream"
        )
        _uiState.update { it.copy(activePlayingVideo = streamItem) }
    }

    fun dismissPlayer() {
        _uiState.update { it.copy(activePlayingVideo = null) }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            mediaRepository.toggleFavorite(video)
        }
    }

    fun addNetworkStream(title: String, url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            mediaRepository.addSavedStream(title, url.trim())
        }
    }

    fun deleteStream(id: Int) {
        viewModelScope.launch {
            mediaRepository.deleteSavedStream(id)
        }
    }

    fun deleteHistory(uri: String) {
        viewModelScope.launch {
            mediaRepository.deleteHistoryItem(uri)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            mediaRepository.clearAllHistory()
        }
    }

    fun onLocalFilePicked(uri: Uri, fileName: String?) {
        val name = fileName ?: uri.lastPathSegment ?: "Local Video"
        val item = VideoItem(
            id = "picked_${System.currentTimeMillis()}",
            title = name,
            uri = uri.toString(),
            durationMs = 0L,
            resolution = "Local Media",
            category = "Local Files"
        )
        playVideo(item)
    }

    // Equalizer
    fun selectEqualizerPreset(preset: EqualizerPreset) {
        _uiState.update {
            it.copy(
                currentEqualizerPreset = preset,
                customBands = preset.bands
            )
        }
    }

    fun updateEqualizerBand(index: Int, gainDb: Float) {
        val current = _uiState.value.customBands.toMutableList()
        if (index in current.indices) {
            current[index] = gainDb
            _uiState.update {
                it.copy(
                    customBands = current,
                    currentEqualizerPreset = EqualizerPreset("Custom", current)
                )
            }
        }
    }

    fun toggleAudioBoost() {
        _uiState.update { it.copy(audioBoostEnabled = !it.audioBoostEnabled) }
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(nightModeEnabled = !it.nightModeEnabled) }
    }

    fun toggleHardwareAcceleration() {
        _uiState.update { it.copy(hardwareAcceleration = !it.hardwareAcceleration) }
    }
}
