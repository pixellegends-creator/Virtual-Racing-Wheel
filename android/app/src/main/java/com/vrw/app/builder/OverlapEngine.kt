package com.vrw.app.builder

/**
 * Pure layout/overlap logic for the Controller Builder. Kept UI-free and testable, since the
 * Android app originally had zero test coverage in this area despite being one of the most
 * complex interactive pieces of the app.
 */
data class LayoutElement(
    val id: String,
    val type: String, // e.g. "pedal_throttle", "button", "camera_stick"
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

object OverlapEngine {

    fun rectsOverlap(a: LayoutElement, b: LayoutElement): Boolean {
        if (a.id == b.id) return false
        val aRight = a.x + a.width
        val aBottom = a.y + a.height
        val bRight = b.x + b.width
        val bBottom = b.y + b.height

        return a.x < bRight && aRight > b.x && a.y < bBottom && aBottom > b.y
    }

    fun overlapsAny(candidate: LayoutElement, others: List<LayoutElement>): Boolean {
        return others.any { rectsOverlap(candidate, it) }
    }

    /**
     * Used when a drag/resize gesture ends: if the element's new position/size overlaps any
     * other element, the move is reverted and the element's pre-gesture state is returned
     * instead. Returns the element that should actually be committed to the layout.
     */
    fun resolveOnRelease(
        movedElement: LayoutElement,
        preGestureState: LayoutElement,
        allOtherElements: List<LayoutElement>
    ): LayoutElement {
        return if (overlapsAny(movedElement, allOtherElements)) {
            preGestureState
        } else {
            movedElement
        }
    }

    /**
     * Finds a non-overlapping placement for a newly added element by searching diagonally
     * outward from a preferred starting position, in fixed-size steps, until a free spot is
     * found or the canvas is exhausted (in which case the original preferred position is
     * returned as a last-resort fallback rather than failing).
     */
    fun findNonOverlappingPlacement(
        preferredX: Float,
        preferredY: Float,
        width: Float,
        height: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        existing: List<LayoutElement>,
        step: Float = 24f,
        maxSteps: Int = 40
    ): Pair<Float, Float> {
        for (i in 0 until maxSteps) {
            val x = (preferredX + i * step).coerceIn(0f, (canvasWidth - width).coerceAtLeast(0f))
            val y = (preferredY + i * step).coerceIn(0f, (canvasHeight - height).coerceAtLeast(0f))
            val candidate = LayoutElement(id = "__probe__", type = "probe", x = x, y = y, width = width, height = height)
            if (!overlapsAny(candidate, existing)) {
                return x to y
            }
        }
        // Canvas is full / no free spot found within search budget - fall back gracefully.
        return preferredX to preferredY
    }
}
