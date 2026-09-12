package com.example.player

data class EqualizerPreset(
    val name: String,
    val bands: List<Float> // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz from -10f to +10f dB
)

object EqualizerData {
    val presets = listOf(
        EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
        EqualizerPreset("Bass Boost", listOf(6f, 4.5f, 2f, 0f, 0f)),
        EqualizerPreset("Rock", listOf(4.5f, 2.5f, -1f, 3f, 5f)),
        EqualizerPreset("Pop", listOf(-1.5f, 1.5f, 4f, 2f, -1.5f)),
        EqualizerPreset("Classical", listOf(4f, 3f, -1.5f, 3.5f, 4f)),
        EqualizerPreset("Voice / Dialogue", listOf(-2f, 2f, 5f, 3f, -1f)),
        EqualizerPreset("Party", listOf(5f, 3.5f, 0f, 3f, 4.5f))
    )
}
