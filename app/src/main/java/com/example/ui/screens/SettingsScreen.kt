package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.EqualizerData
import com.example.player.EqualizerPreset
import com.example.ui.theme.VlcOrange

@Composable
fun SettingsScreen(
    currentPreset: EqualizerPreset,
    customBands: List<Float>,
    audioBoostEnabled: Boolean,
    nightModeEnabled: Boolean,
    hardwareAcceleration: Boolean,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandChange: (index: Int, value: Float) -> Unit,
    onToggleAudioBoost: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleHardwareAcceleration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Equalizer Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("equalizer_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VlcOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = VlcOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Audio Equalizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqualizerData.presets.forEach { preset ->
                            val isSelected = currentPreset.name == preset.name
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = VlcOrange,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5-band slider row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        customBands.forEachIndexed { index, gain ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format("%+.1fdB", gain),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (gain != 0f) VlcOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Slider(
                                    value = gain,
                                    onValueChange = { onBandChange(index, it) },
                                    valueRange = -10f..10f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = VlcOrange,
                                        activeTrackColor = VlcOrange
                                    ),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = bandLabels.getOrElse(index) { "Band $index" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio & Playback Options
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Playback Enhancements",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audio Boost Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = VlcOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("200% Audio Boost", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Amplify maximum volume up to 200% beyond hardware limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = audioBoostEnabled,
                            onCheckedChange = { onToggleAudioBoost() },
                            colors = SwitchDefaults.colors(checkedThumbColor = VlcOrange, checkedTrackColor = VlcOrange.copy(alpha = 0.5f))
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Night Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Nightlight, contentDescription = null, tint = VlcOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Night Mode (Dynamic Range Compression)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Reduces loud explosions while enhancing quiet dialogue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = nightModeEnabled,
                            onCheckedChange = { onToggleNightMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = VlcOrange, checkedTrackColor = VlcOrange.copy(alpha = 0.5f))
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Hardware acceleration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = VlcOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Hardware Acceleration", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Use device GPU and MediaCodec decoders for 4K/60fps playback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = hardwareAcceleration,
                            onCheckedChange = { onToggleHardwareAcceleration() },
                            colors = SwitchDefaults.colors(checkedThumbColor = VlcOrange, checkedTrackColor = VlcOrange.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // VLC Gestures Reference Guide
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = VlcOrange)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VLC Gesture Controls",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val gestures = listOf(
                        "Left screen swipe up/down" to "Adjust brightness with on-screen HUD",
                        "Right screen swipe up/down" to "Adjust volume up to 200% audio boost",
                        "Horizontal swipe across screen" to "Fast seek forward or rewind with delta badge",
                        "Double tap left / right" to "Quick jump 10 seconds back / forward",
                        "Single tap screen" to "Toggle playback controls overlay",
                        "Pinch / Aspect button" to "Cycle Fit Screen, Fill Crop, Stretch, 16:9, 4:3",
                        "Padlock button" to "Lock touch controls to prevent accidental taps"
                    )

                    gestures.forEach { (gesture, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = gesture,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = VlcOrange,
                                modifier = Modifier.weight(0.5f)
                            )
                            Text(
                                text = desc,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }
                }
            }
        }

        // About VLC Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = VlcOrange)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "About VLC Video Player",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "VLC Player for Android with Jetpack Compose & AndroidX Media3 ExoPlayer engine. Supports local files, network streams (HLS, DASH, HTTP), audio boost, and hardware decoders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
