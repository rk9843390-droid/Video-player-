package com.example

import com.example.data.model.VideoItem
import com.example.player.AspectRatioMode
import com.example.player.EqualizerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VlcPlayerUnitTest {

    @Test
    fun `formatDuration formats milliseconds correctly`() {
        assertEquals("Live", VideoItem.formatDuration(0L))
        assertEquals("00:45", VideoItem.formatDuration(45000L))
        assertEquals("03:15", VideoItem.formatDuration(195000L))
        assertEquals("1:02:30", VideoItem.formatDuration(3750000L))
    }

    @Test
    fun `aspect ratio modes provide appropriate display names`() {
        assertEquals("Fit Screen", AspectRatioMode.FIT.displayName)
        assertEquals("Fill / Crop", AspectRatioMode.ZOOM.displayName)
        assertEquals("Stretch", AspectRatioMode.STRETCH.displayName)
        assertEquals("16:9", AspectRatioMode.RATIO_16_9.displayName)
        assertEquals("4:3", AspectRatioMode.RATIO_4_3.displayName)
        assertEquals("Original 100%", AspectRatioMode.ORIGINAL.displayName)
    }

    @Test
    fun `equalizer presets contain 5 frequency bands`() {
        assertTrue(EqualizerData.presets.isNotEmpty())
        EqualizerData.presets.forEach { preset ->
            assertEquals(5, preset.bands.size)
        }
    }
}
