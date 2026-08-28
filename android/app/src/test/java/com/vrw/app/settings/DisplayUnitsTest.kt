package com.vrw.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayUnitsTest {

    @Test
    fun `100 kmh converts to correct mph`() {
        assertEquals(62.1371f, DisplayUnits.kmhToMph(100f), 0.01f)
    }

    @Test
    fun `round trip kmh to mph and back is stable`() {
        val original = 137.4f
        val roundTripped = DisplayUnits.mphToKmh(DisplayUnits.kmhToMph(original))
        assertEquals(original, roundTripped, 0.01f)
    }

    @Test
    fun `convert with KMH target is identity`() {
        assertEquals(80f, DisplayUnits.convert(80f, DisplayUnits.SpeedUnit.KMH), 0.001f)
    }
}

class LayoutMirroringTest {

    @Test
    fun `element at left edge mirrors to right edge`() {
        val mirrored = LayoutMirroring.mirrorX(elementX = 0f, elementWidth = 100f, canvasWidth = 800f)
        assertEquals(700f, mirrored, 0.001f)
    }

    @Test
    fun `centered element mirrors onto itself`() {
        val mirrored = LayoutMirroring.mirrorX(elementX = 350f, elementWidth = 100f, canvasWidth = 800f)
        assertEquals(350f, mirrored, 0.001f)
    }

    @Test
    fun `mirroring twice returns original position`() {
        assertTrue(LayoutMirroring.isInvolution(elementX = 120f, elementWidth = 60f, canvasWidth = 800f))
    }
}
