package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.VideoItem
import com.example.player.PlayerViewModel
import com.example.ui.components.VlcTopBar
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StreamsScreen
import com.example.ui.theme.VlcOrange

@Composable
fun VlcApp(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsState()
    val videos by mainViewModel.videos.collectAsState()
    val history by mainViewModel.history.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val savedStreams by mainViewModel.savedStreams.collectAsState()

    // System File Picker for local video playback
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var displayName: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            mainViewModel.onLocalFilePicked(uri, displayName)
        }
    }

    // Active Fullscreen Player or Media Library Navigation
    AnimatedContent(
        targetState = uiState.activePlayingVideo,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "player_screen_transition"
    ) { activeVideo ->
        if (activeVideo != null) {
            PlayerScreen(
                video = activeVideo,
                playerViewModel = playerViewModel,
                onClose = {
                    playerViewModel.saveCurrentPlaybackProgress()
                    mainViewModel.dismissPlayer()
                }
            )
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    VlcTopBar(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { mainViewModel.setSearchQuery(it) },
                        isGridView = uiState.isGridView,
                        onToggleViewMode = { mainViewModel.toggleViewMode() },
                        currentSort = uiState.sortOption,
                        onSortOptionSelected = { mainViewModel.setSortOption(it) },
                        onOpenFilePicker = { filePickerLauncher.launch(arrayOf("video/*")) }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        // 1. Videos Tab
                        NavigationBarItem(
                            selected = uiState.selectedTab == VlcTab.VIDEOS,
                            onClick = { mainViewModel.selectTab(VlcTab.VIDEOS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == VlcTab.VIDEOS) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                    contentDescription = "Videos"
                                )
                            },
                            label = { Text("Videos", fontWeight = if (uiState.selectedTab == VlcTab.VIDEOS) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = VlcOrange
                            ),
                            modifier = Modifier.testTag("nav_tab_videos")
                        )

                        // 2. Streams Tab
                        NavigationBarItem(
                            selected = uiState.selectedTab == VlcTab.STREAMS,
                            onClick = { mainViewModel.selectTab(VlcTab.STREAMS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == VlcTab.STREAMS) Icons.Filled.Podcasts else Icons.Outlined.Podcasts,
                                    contentDescription = "Streams"
                                )
                            },
                            label = { Text("Streams", fontWeight = if (uiState.selectedTab == VlcTab.STREAMS) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = VlcOrange
                            ),
                            modifier = Modifier.testTag("nav_tab_streams")
                        )

                        // 3. Playlists & History Tab
                        NavigationBarItem(
                            selected = uiState.selectedTab == VlcTab.PLAYLISTS,
                            onClick = { mainViewModel.selectTab(VlcTab.PLAYLISTS) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == VlcTab.PLAYLISTS) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "History"
                                )
                            },
                            label = { Text("History", fontWeight = if (uiState.selectedTab == VlcTab.PLAYLISTS) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = VlcOrange
                            ),
                            modifier = Modifier.testTag("nav_tab_history")
                        )

                        // 4. Equalizer & More Tab
                        NavigationBarItem(
                            selected = uiState.selectedTab == VlcTab.EQUALIZER,
                            onClick = { mainViewModel.selectTab(VlcTab.EQUALIZER) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.selectedTab == VlcTab.EQUALIZER) Icons.Filled.Tune else Icons.Outlined.Tune,
                                    contentDescription = "Equalizer"
                                )
                            },
                            label = { Text("Equalizer", fontWeight = if (uiState.selectedTab == VlcTab.EQUALIZER) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = VlcOrange
                            ),
                            modifier = Modifier.testTag("nav_tab_equalizer")
                        )
                    }
                },
                modifier = modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (uiState.selectedTab) {
                        VlcTab.VIDEOS -> {
                            LibraryScreen(
                                videos = videos,
                                isGridView = uiState.isGridView,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = { mainViewModel.setSelectedCategory(it) },
                                onVideoClick = { mainViewModel.playVideo(it) },
                                onToggleFavorite = { mainViewModel.toggleFavorite(it) },
                                onOpenFilePicker = { filePickerLauncher.launch(arrayOf("video/*")) }
                            )
                        }

                        VlcTab.STREAMS -> {
                            StreamsScreen(
                                savedStreams = savedStreams,
                                defaultStreams = mainViewModel.defaultStreams,
                                onPlayStream = { url, title -> mainViewModel.playStream(url, title) },
                                onAddStream = { title, url -> mainViewModel.addNetworkStream(title, url) },
                                onDeleteStream = { mainViewModel.deleteStream(it) }
                            )
                        }

                        VlcTab.PLAYLISTS -> {
                            PlaylistsScreen(
                                history = history,
                                favorites = favorites,
                                onPlayMedia = { uri, title ->
                                    val match = videos.find { it.uri == uri }
                                    if (match != null) {
                                        mainViewModel.playVideo(match)
                                    } else {
                                        mainViewModel.playStream(uri, title)
                                    }
                                },
                                onDeleteHistory = { mainViewModel.deleteHistory(it) },
                                onClearAllHistory = { mainViewModel.clearAllHistory() }
                            )
                        }

                        VlcTab.EQUALIZER -> {
                            SettingsScreen(
                                currentPreset = uiState.currentEqualizerPreset,
                                customBands = uiState.customBands,
                                audioBoostEnabled = uiState.audioBoostEnabled,
                                nightModeEnabled = uiState.nightModeEnabled,
                                hardwareAcceleration = uiState.hardwareAcceleration,
                                onSelectPreset = { mainViewModel.selectEqualizerPreset(it) },
                                onBandChange = { idx, value -> mainViewModel.updateEqualizerBand(idx, value) },
                                onToggleAudioBoost = { mainViewModel.toggleAudioBoost() },
                                onToggleNightMode = { mainViewModel.toggleNightMode() },
                                onToggleHardwareAcceleration = { mainViewModel.toggleHardwareAcceleration() }
                            )
                        }
                    }
                }
            }
        }
    }
}
