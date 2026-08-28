package com.vrw.app.pedals

import kotlin.math.abs

/**
 * Normalizes raw pedal sensor input (e.g. touch-slider position or accelerometer-derived value)
 * into a 0f..1f range per pedal, applying per-pedal calibration and dead-zone.
 */
object PedalInputProcessor {

    data class PedalCalibration(
        val minRaw: Float = 0f,
        val maxRaw: Float = 1f,
        val deadZone: Float = 0.03f
    )

    data class PedalState(
        val throttle: Float = 0f,
        val brake: Float = 0f,
        val clutch: Float = 0f
    )

    fun normalize(raw: Float, calibration: PedalCalibration): Float {
        val range = calibration.maxRaw - calibration.minRaw
        if (range <= 0f) return 0f

        val t = ((raw - calibration.minRaw) / range).coerceIn(0f, 1f)
        val dz = calibration.deadZone.coerceIn(0f, 0.99f)

        if (t <= dz) return 0f
        return ((t - dz) / (1f - dz)).coerceIn(0f, 1f)
    }

    fun process(
        rawThrottle: Float,
        rawBrake: Float,
        rawClutch: Float,
        throttleCal: PedalCalibration,
        brakeCal: PedalCalibration,
        clutchCal: PedalCalibration
    ): PedalState {
        return PedalState(
            throttle = normalize(rawThrottle, throttleCal),
            brake = normalize(rawBrake, brakeCal),
            clutch = normalize(rawClutch, clutchCal)
        )
    }

    /** True if two pedal states differ enough to be worth sending over the network. */
    fun hasSignificantChange(a: PedalState, b: PedalState, threshold: Float = 0.005f): Boolean {
        return abs(a.throttle - b.throttle) > threshold ||
            abs(a.brake - b.brake) > threshold ||
            abs(a.clutch - b.clutch) > threshold
    }
}
