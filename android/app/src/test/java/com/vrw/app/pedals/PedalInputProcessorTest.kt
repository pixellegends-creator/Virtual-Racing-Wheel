package com.vrw.app.pedals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PedalInputProcessorTest {

    @Test
    fun `raw at min produces zero`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0f, maxRaw = 1f, deadZone = 0f)
        assertEquals(0f, PedalInputProcessor.normalize(0f, cal), 0.001f)
    }

    @Test
    fun `raw at max produces one`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0f, maxRaw = 1f, deadZone = 0f)
        assertEquals(1f, PedalInputProcessor.normalize(1f, cal), 0.001f)
    }

    @Test
    fun `dead zone suppresses light presses`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0f, maxRaw = 1f, deadZone = 0.1f)
        assertEquals(0f, PedalInputProcessor.normalize(0.05f, cal), 0.001f)
    }

    @Test
    fun `zero calibration range does not throw or produce NaN`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0.5f, maxRaw = 0.5f, deadZone = 0.03f)
        val result = PedalInputProcessor.normalize(0.5f, cal)
        assertFalse(result.isNaN())
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `output past max raw is clamped to one`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0f, maxRaw = 1f, deadZone = 0f)
        assertEquals(1f, PedalInputProcessor.normalize(1.5f, cal), 0.001f)
    }

    @Test
    fun `output below min raw is clamped to zero`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0.2f, maxRaw = 1f, deadZone = 0f)
        assertEquals(0f, PedalInputProcessor.normalize(-1f, cal), 0.001f)
    }

    @Test
    fun `process combines all three pedals independently`() {
        val cal = PedalInputProcessor.PedalCalibration(minRaw = 0f, maxRaw = 1f, deadZone = 0f)
        val state = PedalInputProcessor.process(0.5f, 0.25f, 1f, cal, cal, cal)
        assertEquals(0.5f, state.throttle, 0.001f)
        assertEquals(0.25f, state.brake, 0.001f)
        assertEquals(1f, state.clutch, 0.001f)
    }

    @Test
    fun `significant change detects meaningful throttle delta`() {
        val a = PedalInputProcessor.PedalState(throttle = 0.1f)
        val b = PedalInputProcessor.PedalState(throttle = 0.2f)
        assertTrue(PedalInputProcessor.hasSignificantChange(a, b))
    }

    @Test
    fun `significant change ignores noise below threshold`() {
        val a = PedalInputProcessor.PedalState(throttle = 0.100f)
        val b = PedalInputProcessor.PedalState(throttle = 0.101f)
        assertFalse(PedalInputProcessor.hasSignificantChange(a, b, threshold = 0.005f))
    }
}
