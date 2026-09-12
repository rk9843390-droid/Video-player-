package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.player.AspectRatioMode
import com.example.player.PlayerViewModel
import com.example.ui.components.VlcHudOverlay
import com.example.ui.theme.HudBackground
import com.example.ui.theme.VlcOrange
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    playerViewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val playerState by playerViewModel.playerState.collectAsState()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Load media on launch
    LaunchedEffect(video.uri) {
        playerViewModel.loadMedia(video.uri, video.title)
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            playerViewModel.saveCurrentPlaybackProgress()
        }
    }

    BackHandler {
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // ExoPlayer Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    keepScreenOn = true
                    player = playerViewModel.getPlayer()
                }
            },
            update = { playerView ->
                playerView.player = playerViewModel.getPlayer()
                playerView.resizeMode = when (playerState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Overlay Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(playerState.isLocked) {
                    if (playerState.isLocked) {
                        detectTapGestures(
                            onTap = { playerViewModel.toggleControls() }
                        )
                    } else {
                        var isDraggingHorizontal = false
                        var isDraggingVertical = false
                        var dragStartX = 0f

                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartX = offset.x
                                isDraggingHorizontal = false
                                isDraggingVertical = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x
                                val dy = dragAmount.y

                                if (!isDraggingHorizontal && !isDraggingVertical) {
                                    if (abs(dx) > abs(dy) && abs(dx) > 12f) {
                                        isDraggingHorizontal = true
                                        playerViewModel.onSeekDragStart()
                                    } else if (abs(dy) > abs(dx) && abs(dy) > 12f) {
                                        isDraggingVertical = true
                                    }
                                }

                                if (isDraggingHorizontal) {
                                    playerViewModel.onSeekDrag(dx, size.width.toFloat())
                                } else if (isDraggingVertical) {
                                    val isLeftHalf = dragStartX < size.width * 0.5f
                                    if (isLeftHalf) {
                                        playerViewModel.onBrightnessDrag(dy, size.height.toFloat(), activity)
                                    } else {
                                        playerViewModel.onVolumeDrag(dy, size.height.toFloat())
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isDraggingHorizontal) {
                                    playerViewModel.onSeekDragEnd()
                                }
                                isDraggingHorizontal = false
                                isDraggingVertical = false
                            },
                            onDragCancel = {
                                isDraggingHorizontal = false
                                isDraggingVertical = false
                            }
                        )
                    }
                }
                .pointerInput(playerState.isLocked) {
                    if (!playerState.isLocked) {
                        detectTapGestures(
                            onTap = { playerViewModel.toggleControls() },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.35f) {
                                    playerViewModel.seekRelative(-10000L)
                                } else if (offset.x > width * 0.65f) {
                                    playerViewModel.seekRelative(10000L)
                                } else {
                                    playerViewModel.togglePlayPause()
                                }
                            }
                        )
                    }
                }
        )

        // HUD Overlay (Brightness, Volume, Seek scrub)
        VlcHudOverlay(playerState = playerState)

        // Floating Lock / Unlock button (Always accessible)
        if (playerState.isLocked && playerState.showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            ) {
                IconButton(
                    onClick = { playerViewModel.toggleLock() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(VlcOrange)
                        .testTag("unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock Controls",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Full Controls Overlay (When unlocked)
        AnimatedVisibility(
            visible = !playerState.isLocked && playerState.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close player",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = video.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Top Player Tools
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Aspect ratio toggle
                        IconButton(onClick = { playerViewModel.cycleAspectRatio() }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect ratio",
                                tint = VlcOrange
                            )
                        }

                        // Audio track selector
                        IconButton(onClick = { showAudioTrackDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio track",
                                tint = Color.White
                            )
                        }

                        // Subtitle selector
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Subtitles",
                                tint = Color.White
                            )
                        }

                        // Sleep timer
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep timer",
                                tint = if (playerState.sleepTimerMinutes != null) VlcOrange else Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Buffering Indicators
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            color = VlcOrange,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            // 10s Rewind
                            IconButton(
                                onClick = { playerViewModel.seekRelative(-10000L) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55000000))
                                    .testTag("rewind_10s_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Big Play/Pause
                            IconButton(
                                onClick = { playerViewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(VlcOrange)
                                    .testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (playerState.isEnded) {
                                        Icons.Default.Replay
                                    } else if (playerState.isPlaying) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            // 10s Forward
                            IconButton(
                                onClick = { playerViewModel.seekRelative(10000L) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55000000))
                                    .testTag("forward_10s_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD000000))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Timeline Slider & Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimeMs(playerState.currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Slider(
                            value = playerState.currentPositionMs.toFloat(),
                            onValueChange = { targetPos ->
                                playerViewModel.seekTo(targetPos.toLong())
                            },
                            valueRange = 0f..(playerState.durationMs.coerceAtLeast(1000L).toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = VlcOrange,
                                activeTrackColor = VlcOrange,
                                inactiveTrackColor = Color(0x44FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("timeline_slider")
                        )

                        Text(
                            text = if (video.isStream && playerState.durationMs <= 0) "LIVE" else formatTimeMs(playerState.durationMs),
                            color = if (video.isStream) VlcOrange else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Bottom Action Tools Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Padlock Button (Screen lock)
                        IconButton(
                            onClick = { playerViewModel.toggleLock() },
                            modifier = Modifier.testTag("lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Screen Controls",
                                tint = Color.White
                            )
                        }

                        // Playback Speed Button
                        TextButton(
                            onClick = { showSpeedDialog = true },
                            modifier = Modifier.testTag("playback_speed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = VlcOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playerState.playbackSpeed}x",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Aspect Ratio Mode Display
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = playerState.aspectRatioMode.displayName,
                                color = VlcOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Repeat Mode
                        IconButton(onClick = { playerViewModel.setRepeatMode(playerState.repeatMode + 1) }) {
                            Icon(
                                imageVector = when (playerState.repeatMode) {
                                    1 -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (playerState.repeatMode > 0) VlcOrange else Color.White
                            )
                        }

                        // Picture in Picture (PiP)
                        IconButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                    try {
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity.enterPictureInPictureMode(params)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.playbackSpeed == speed,
                                onClick = {
                                    playerViewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = VlcOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${speed}x", fontWeight = if (playerState.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Done", color = VlcOrange)
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    val options = listOf(null to "Off", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes")
                    options.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.sleepTimerMinutes == mins,
                                onClick = {
                                    playerViewModel.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = VlcOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = VlcOrange)
                }
            }
        )
    }

    // Audio Track & Sync Dialog
    if (showAudioTrackDialog) {
        AlertDialog(
            onDismissRequest = { showAudioTrackDialog = false },
            title = { Text("Audio Track & Delay") },
            text = {
                Column {
                    Text("Select Track:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    playerState.audioTracks.forEachIndexed { index, trackName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.selectedAudioTrack == index,
                                onClick = { playerViewModel.selectAudioTrack(index) },
                                colors = RadioButtonDefaults.colors(selectedColor = VlcOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(trackName, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Audio Delay (${playerState.audioDelayMs} ms):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { playerViewModel.adjustAudioDelay(-50) }) {
                            Text("-50 ms", color = VlcOrange)
                        }
                        TextButton(onClick = { playerViewModel.adjustAudioDelay(50) }) {
                            Text("+50 ms", color = VlcOrange)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioTrackDialog = false }) {
                    Text("OK", color = VlcOrange)
                }
            }
        )
    }

    // Subtitle Track & Sync Dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Subtitles & Closed Captions") },
            text = {
                Column {
                    Text("Select Subtitles:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    playerState.subtitleTracks.forEachIndexed { index, subName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playerState.selectedSubtitleTrack == index,
                                onClick = { playerViewModel.selectSubtitleTrack(index) },
                                colors = RadioButtonDefaults.colors(selectedColor = VlcOrange)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(subName, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Subtitle Delay (${playerState.subtitleDelayMs} ms):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { playerViewModel.adjustSubtitleDelay(-50) }) {
                            Text("-50 ms", color = VlcOrange)
                        }
                        TextButton(onClick = { playerViewModel.adjustSubtitleDelay(50) }) {
                            Text("+50 ms", color = VlcOrange)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("OK", color = VlcOrange)
                }
            }
        )
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val s = totalSeconds % 60
    val m = (totalSeconds / 60) % 60
    val h = totalSeconds / 3600
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
