package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PlayerViewModel(
    private val context: Context,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(VlcPlayerState())
    val playerState: StateFlow<VlcPlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var controlsDismissJob: Job? = null
    private var hudDismissJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var initialDragSeekPosition: Long = 0L

    init {
        initExoPlayer()
        readInitialAudioAndBrightness()
    }

    private fun initExoPlayer() {
        if (exoPlayer != null) return

        val player = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> _playerState.update { it.copy(isBuffering = true) }
                        Player.STATE_READY -> {
                            _playerState.update {
                                it.copy(
                                    isBuffering = false,
                                    durationMs = duration.coerceAtLeast(0L),
                                    isEnded = false
                                )
                            }
                        }
                        Player.STATE_ENDED -> {
                            _playerState.update { it.copy(isPlaying = false, isEnded = true, isBuffering = false) }
                            saveCurrentPlaybackProgress()
                        }
                        Player.STATE_IDLE -> _playerState.update { it.copy(isBuffering = false) }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.update { it.copy(isPlaying = isPlaying) }
                    if (!isPlaying) {
                        saveCurrentPlaybackProgress()
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val audioTrackNames = mutableListOf<String>()
                    val subtitleTrackNames = mutableListOf("None")

                    tracks.groups.forEach { group ->
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val label = format.label ?: format.language ?: "Track ${i + 1}"
                            if (format.sampleMimeType?.startsWith("audio") == true) {
                                audioTrackNames.add(label)
                            } else if (format.sampleMimeType?.startsWith("text") == true ||
                                format.sampleMimeType?.contains("sub") == true
                            ) {
                                subtitleTrackNames.add(label)
                            }
                        }
                    }

                    _playerState.update {
                        it.copy(
                            audioTracks = if (audioTrackNames.isEmpty()) listOf("Stereo Default") else audioTrackNames,
                            subtitleTracks = if (subtitleTrackNames.size == 1) listOf("None", "Default (Auto)") else subtitleTrackNames
                        )
                    }
                }
            })
        }
        exoPlayer = player
        startProgressTracker()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun readInitialAudioAndBrightness() {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volPct = if (maxVol > 0) ((currentVol.toFloat() / maxVol.toFloat()) * 100).roundToInt() else 50
        _playerState.update { it.copy(volumePercent = volPct) }
    }

    fun loadMedia(uri: String, title: String) {
        if (_playerState.value.uri == uri && exoPlayer?.mediaItemCount ?: 0 > 0) {
            // Already loaded
            return
        }

        val player = exoPlayer ?: return
        _playerState.update {
            it.copy(
                uri = uri,
                title = title,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = 0L,
                showControls = true
            )
        }

        viewModelScope.launch {
            val resumePos = mediaRepository.getResumePosition(uri)
            val mediaItem = MediaItem.fromUri(Uri.parse(uri))
            player.setMediaItem(mediaItem)
            player.prepare()

            if (resumePos > 3000L) {
                player.seekTo(resumePos)
            }
            player.play()
            resetControlsTimer()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val curPos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val bufPos = player.bufferedPosition.coerceAtLeast(0L)

                    _playerState.update {
                        it.copy(
                            currentPositionMs = curPos,
                            durationMs = if (dur > 0) dur else it.durationMs,
                            bufferedPositionMs = bufPos
                        )
                    }
                }
                delay(400)
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (_playerState.value.isEnded) {
                player.seekTo(0)
            }
            player.play()
        }
        resetControlsTimer()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val target = positionMs.coerceIn(0L, _playerState.value.durationMs)
        player.seekTo(target)
        _playerState.update { it.copy(currentPositionMs = target) }
        resetControlsTimer()
        saveCurrentPlaybackProgress()
    }

    fun seekRelative(deltaMs: Long) {
        val player = exoPlayer ?: return
        val current = player.currentPosition
        val target = (current + deltaMs).coerceIn(0L, _playerState.value.durationMs.coerceAtLeast(1000L))
        player.seekTo(target)
        _playerState.update { it.copy(currentPositionMs = target) }
        triggerHud(
            HudType.SEEK_DELTA,
            valStr = if (deltaMs >= 0) "+${deltaMs / 1000}s" else "${deltaMs / 1000}s",
            progress = (target.toFloat() / _playerState.value.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        )
        resetControlsTimer()
    }

    // Horizontal scrub drag gesture
    fun onSeekDragStart() {
        initialDragSeekPosition = exoPlayer?.currentPosition ?: 0L
    }

    fun onSeekDrag(deltaX: Float, screenWidth: Float) {
        val dur = _playerState.value.durationMs.coerceAtLeast(1000L)
        // Sensitivity: full screen width = 90 seconds
        val deltaSeconds = (deltaX / screenWidth) * 90f
        val deltaMs = (deltaSeconds * 1000).toLong()
        val targetPos = (initialDragSeekPosition + deltaMs).coerceIn(0L, dur)

        _playerState.update { it.copy(seekDeltaMs = deltaMs) }
        triggerHud(
            HudType.SEEK_DELTA,
            valStr = "${if (deltaMs >= 0) "+" else ""}${deltaMs / 1000}s (${formatTime(targetPos)})",
            progress = (targetPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        )
    }

    fun onSeekDragEnd() {
        val dur = _playerState.value.durationMs.coerceAtLeast(1000L)
        val targetPos = (initialDragSeekPosition + _playerState.value.seekDeltaMs).coerceIn(0L, dur)
        seekTo(targetPos)
        _playerState.update { it.copy(seekDeltaMs = 0L) }
    }

    // Vertical Brightness Drag (Left screen)
    fun onBrightnessDrag(deltaY: Float, screenHeight: Float, activity: Activity?) {
        val current = _playerState.value.brightnessPercent
        // Upwards swipe increases brightness (negative deltaY)
        val change = (-deltaY / (screenHeight * 0.7f)) * 100f
        val newBrightness = (current + change).roundToInt().coerceIn(0, 100)

        _playerState.update { it.copy(brightnessPercent = newBrightness) }

        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = (newBrightness / 100f).coerceIn(0.01f, 1.0f)
            window.attributes = lp
        }

        triggerHud(HudType.BRIGHTNESS, "$newBrightness%", newBrightness / 100f)
    }

    // Vertical Volume Drag (Right screen)
    fun onVolumeDrag(deltaY: Float, screenHeight: Float) {
        val current = _playerState.value.volumePercent
        val change = (-deltaY / (screenHeight * 0.7f)) * 100f
        // Support up to 200% VLC Audio Boost!
        val newVolume = (current + change).roundToInt().coerceIn(0, 200)

        _playerState.update { it.copy(volumePercent = newVolume) }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (newVolume <= 100) {
            val sysVol = ((newVolume / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sysVol, 0)
            exoPlayer?.volume = 1.0f
        } else {
            // Audio boost mode! Set system volume to max and scale player software volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            val boostRatio = (newVolume / 100f).coerceIn(1.0f, 2.0f)
            exoPlayer?.volume = boostRatio
        }

        triggerHud(
            HudType.VOLUME,
            if (newVolume > 100) "$newVolume% (BOOST)" else "$newVolume%",
            newVolume / 200f
        )
    }

    private fun triggerHud(type: HudType, valStr: String, progress: Float) {
        _playerState.update {
            it.copy(
                hudType = type,
                hudValue = valStr,
                hudProgress = progress
            )
        }
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            delay(1200)
            _playerState.update { it.copy(hudType = HudType.NONE) }
        }
    }

    fun toggleControls() {
        if (_playerState.value.isLocked) {
            // In locked mode, show just the lock toggle button briefly
            _playerState.update { it.copy(showControls = true) }
            resetControlsTimer(2500)
            return
        }

        val newState = !_playerState.value.showControls
        _playerState.update { it.copy(showControls = newState) }
        if (newState) {
            resetControlsTimer()
        } else {
            controlsDismissJob?.cancel()
        }
    }

    fun resetControlsTimer(delayMs: Long = 4000L) {
        controlsDismissJob?.cancel()
        controlsDismissJob = viewModelScope.launch {
            delay(delayMs)
            if (_playerState.value.isPlaying) {
                _playerState.update { it.copy(showControls = false) }
            }
        }
    }

    fun toggleLock() {
        val newLocked = !_playerState.value.isLocked
        _playerState.update { it.copy(isLocked = newLocked, showControls = true) }
        resetControlsTimer(if (newLocked) 2500L else 4000L)
    }

    fun cycleAspectRatio() {
        val current = _playerState.value.aspectRatioMode
        val next = when (current) {
            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.RATIO_16_9
            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_4_3
            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.ORIGINAL
            AspectRatioMode.ORIGINAL -> AspectRatioMode.FIT
        }
        _playerState.update { it.copy(aspectRatioMode = next) }
        triggerHud(HudType.NONE, "", 0f)
        resetControlsTimer()
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun setRepeatMode(mode: Int) {
        val newMode = (mode) % 3
        exoPlayer?.repeatMode = when (newMode) {
            1 -> Player.REPEAT_MODE_ONE
            2 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        _playerState.update { it.copy(repeatMode = newMode) }
    }

    fun selectAudioTrack(index: Int) {
        _playerState.update { it.copy(selectedAudioTrack = index) }
    }

    fun selectSubtitleTrack(index: Int) {
        _playerState.update { it.copy(selectedSubtitleTrack = index) }
    }

    fun adjustAudioDelay(deltaMs: Int) {
        _playerState.update { it.copy(audioDelayMs = it.audioDelayMs + deltaMs) }
    }

    fun adjustSubtitleDelay(deltaMs: Int) {
        _playerState.update { it.copy(subtitleDelayMs = it.subtitleDelayMs + deltaMs) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _playerState.update { it.copy(sleepTimerMinutes = minutes) }
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                exoPlayer?.pause()
                _playerState.update { it.copy(sleepTimerMinutes = null, isPlaying = false) }
            }
        }
    }

    fun saveCurrentPlaybackProgress() {
        val state = _playerState.value
        if (state.uri.isNotBlank()) {
            viewModelScope.launch {
                mediaRepository.savePlaybackPosition(
                    uri = state.uri,
                    title = state.title,
                    positionMs = state.currentPositionMs,
                    durationMs = state.durationMs
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentPlaybackProgress()
        progressJob?.cancel()
        controlsDismissJob?.cancel()
        hudDismissJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0L)
        val s = totalSeconds % 60
        val m = (totalSeconds / 60) % 60
        val h = totalSeconds / 3600
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }
}
