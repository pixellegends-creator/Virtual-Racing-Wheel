package com.vrw.app.steering

import kotlin.math.abs
import kotlin.math.sign

/**
 * Pure, testable steering pipeline: raw sensor angle -> normalized output in [-1, 1].
 *
 * Pipeline order: calibrate (center offset) -> clamp to lock -> dead-zone -> sensitivity curve
 * -> invert -> smoothing.
 *
 * Note: lockDegrees == 0 previously caused a divide-by-zero (NaN propagated through the whole
 * pipeline). That's guarded here explicitly - a zero lock is treated as "no steering range" and
 * always returns 0.
 */
object SteeringMath {

    data class Config(
        val centerOffsetDegrees: Float = 0f,
        val lockDegrees: Float = 180f, // total wheel lock, e.g. 900 for a full 900-degree wheel
        val deadZone: Float = 0.02f,   // normalized, applied post-clamp
        val sensitivity: Float = 1.0f, // >1 = more aggressive off-center response
        val invert: Boolean = false,
        val smoothingFactor: Float = 0.15f // 0 = no smoothing, closer to 1 = heavier smoothing
    )

    /**
     * @param rawAngleDegrees current raw sensor reading
     * @param config current calibration/tuning config
     * @param previousSmoothed last smoothed output, for the low-pass filter (pass 0f on first call)
     * @return normalized steering value in [-1, 1]
     */
    fun process(rawAngleDegrees: Float, config: Config, previousSmoothed: Float): Float {
        if (config.lockDegrees <= 0f) {
            // Guard: no valid lock range means no meaningful steering output.
            return 0f
        }

        val centered = rawAngleDegrees - config.centerOffsetDegrees
        val halfLock = config.lockDegrees / 2f
        val clamped = centered.coerceIn(-halfLock, halfLock)
        var normalized = clamped / halfLock // safe now: halfLock > 0 guaranteed above

        normalized = applyDeadZone(normalized, config.deadZone)
        normalized = applySensitivity(normalized, config.sensitivity)

        if (config.invert) {
            normalized = -normalized
        }

        return applySmoothing(normalized, previousSmoothed, config.smoothingFactor)
    }

    fun applyDeadZone(value: Float, deadZone: Float): Float {
        val dz = deadZone.coerceIn(0f, 0.99f)
        if (abs(value) <= dz) return 0f
        val s = sign(value)
        // Rescale so output still reaches +/-1 at the edges instead of clipping range.
        return s * ((abs(value) - dz) / (1f - dz))
    }

    fun applySensitivity(value: Float, sensitivity: Float): Float {
        val s = sensitivity.coerceAtLeast(0f)
        val sign = sign(value)
        val magnitude = abs(value).coerceIn(0f, 1f)
        // Power curve: sensitivity > 1 sharpens response near center, < 1 softens it.
        val curved = Math.pow(magnitude.toDouble(), (1.0 / s.coerceAtLeast(0.0001f))).toFloat()
        return (sign * curved).coerceIn(-1f, 1f)
    }

    fun applySmoothing(value: Float, previous: Float, factor: Float): Float {
        val f = factor.coerceIn(0f, 0.999f)
        return previous * f + value * (1f - f)
    }
}
