package com.vrw.app.settings

/**
 * Small pure-math helpers for two Settings features:
 *  - unit conversion for telemetry display (km/h <-> mph)
 *  - left-handed mode mirroring, which flips element X positions at render time so any
 *    Controller Builder layout works without needing to be rebuilt for left-handed use.
 */
object DisplayUnits {
    enum class SpeedUnit { KMH, MPH }

    fun kmhToMph(kmh: Float): Float = kmh * 0.621371f
    fun mphToKmh(mph: Float): Float = mph / 0.621371f

    fun convert(speedKmh: Float, target: SpeedUnit): Float = when (target) {
        SpeedUnit.KMH -> speedKmh
        SpeedUnit.MPH -> kmhToMph(speedKmh)
    }
}

object LayoutMirroring {

    /**
     * Mirrors an element's horizontal position and width within a canvas of the given width,
     * so a layout built right-handed renders correctly left-handed (and vice versa - mirroring
     * twice is a no-op).
     */
    fun mirrorX(elementX: Float, elementWidth: Float, canvasWidth: Float): Float {
        return canvasWidth - elementX - elementWidth
    }

    /** Mirroring is its own inverse: applying it twice returns the original position. */
    fun isInvolution(elementX: Float, elementWidth: Float, canvasWidth: Float): Boolean {
        val once = mirrorX(elementX, elementWidth, canvasWidth)
        val twice = mirrorX(once, elementWidth, canvasWidth)
        return kotlin.math.abs(twice - elementX) < 0.0001f
    }
}
