package com.example.player

enum class AspectRatioMode(val displayName: String) {
    FIT("Fit Screen"),
    ZOOM("Fill / Crop"),
    STRETCH("Stretch"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3"),
    ORIGINAL("Original 100%")
}

enum class HudType {
    NONE,
    VOLUME,
    BRIGHTNESS,
    SEEK_DELTA
}
