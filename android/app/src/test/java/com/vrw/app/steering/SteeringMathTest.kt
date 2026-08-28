package com.vrw.app.steering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.math.abs

class SteeringMathTest {

    @Test
    fun `centered input with default config produces zero output`() {
        val config = SteeringMath.Config()
        val result = SteeringMath.process(0f, config, previousSmoothed = 0f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `full lock right produces positive one`() {
        val config = SteeringMath.Config(lockDegrees = 180f, deadZone = 0f, sensitivity = 1f, smoothingFactor = 0f)
        val result = SteeringMath.process(90f, config, previousSmoothed = 0f)
        assertEquals(1f, result, 0.01f)
    }

    @Test
    fun `full lock left produces negative one`() {
        val config = SteeringMath.Config(lockDegrees = 180f, deadZone = 0f, sensitivity = 1f, smoothingFactor = 0f)
        val result = SteeringMath.process(-90f, config, previousSmoothed = 0f)
        assertEquals(-1f, result, 0.01f)
    }

    @Test
    fun `zero lock degrees does not throw or produce NaN - regression for divide-by-zero`() {
        val config = SteeringMath.Config(lockDegrees = 0f)
        val result = SteeringMath.process(45f, config, previousSmoothed = 0f)
        assertFalse(result.isNaN())
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `negative lock degrees is also guarded`() {
        val config = SteeringMath.Config(lockDegrees = -10f)
        val result = SteeringMath.process(45f, config, previousSmoothed = 0f)
        assertFalse(result.isNaN())
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun `dead zone suppresses small inputs near center`() {
        val result = SteeringMath.applyDeadZone(0.01f, deadZone = 0.05f)
        assertEquals(0f, result, 0.0001f)
    }

    @Test
    fun `dead zone still reaches full scale at edges`() {
        val result = SteeringMath.applyDeadZone(1f, deadZone = 0.05f)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `invert flips sign`() {
        val config = SteeringMath.Config(lockDegrees = 180f, deadZone = 0f, invert = true, smoothingFactor = 0f)
        val result = SteeringMath.process(90f, config, previousSmoothed = 0f)
        assertEquals(-1f, result, 0.01f)
    }

    @Test
    fun `smoothing blends toward new value without jumping instantly`() {
        val smoothed = SteeringMath.applySmoothing(1f, previous = 0f, factor = 0.5f)
        assertEquals(0.5f, smoothed, 0.001f)
    }

    @Test
    fun `smoothing factor zero passes value through unchanged`() {
        val smoothed = SteeringMath.applySmoothing(0.73f, previous = 0f, factor = 0f)
        assertEquals(0.73f, smoothed, 0.001f)
    }

    @Test
    fun `output is always clamped within bounds for extreme raw angles`() {
        val config = SteeringMath.Config(lockDegrees = 180f, smoothingFactor = 0f)
        val result = SteeringMath.process(9999f, config, previousSmoothed = 0f)
        assertFalse(abs(result) > 1f)
    }
}
