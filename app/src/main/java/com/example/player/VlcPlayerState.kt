package com.example.player

data class VlcPlayerState(
    val uri: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volumePercent: Int = 80, // 0 to 100, or up to 200 with audio boost
    val brightnessPercent: Int = 50, // 0 to 100
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isLocked: Boolean = false,
    val showControls: Boolean = true,
    val hudType: HudType = HudType.NONE,
    val hudValue: String = "",
    val hudProgress: Float = 0f,
    val seekDeltaMs: Long = 0L,
    val sleepTimerMinutes: Int? = null,
    val audioDelayMs: Int = 0,
    val subtitleDelayMs: Int = 0,
    val audioTracks: List<String> = emptyList(),
    val selectedAudioTrack: Int = 0,
    val subtitleTracks: List<String> = listOf("None", "Default (Auto)"),
    val selectedSubtitleTrack: Int = 1,
    val repeatMode: Int = 0 // 0: None, 1: Repeat One, 2: Repeat All
)
