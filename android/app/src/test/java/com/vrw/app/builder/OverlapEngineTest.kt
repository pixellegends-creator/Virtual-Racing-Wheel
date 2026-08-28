package com.vrw.app.builder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlapEngineTest {

    private fun el(id: String, x: Float, y: Float, w: Float = 100f, h: Float = 100f) =
        LayoutElement(id, "test", x, y, w, h)

    @Test
    fun `identical rects overlap`() {
        val a = el("a", 0f, 0f)
        val b = el("b", 0f, 0f)
        assertTrue(OverlapEngine.rectsOverlap(a, b))
    }

    @Test
    fun `far apart rects do not overlap`() {
        val a = el("a", 0f, 0f)
        val b = el("b", 500f, 500f)
        assertFalse(OverlapEngine.rectsOverlap(a, b))
    }

    @Test
    fun `element does not overlap itself by id`() {
        val a = el("a", 0f, 0f)
        val aAgain = el("a", 0f, 0f)
        assertFalse(OverlapEngine.rectsOverlap(a, aAgain))
    }

    @Test
    fun `touching edges do not count as overlap`() {
        val a = el("a", 0f, 0f, 100f, 100f)
        val b = el("b", 100f, 0f, 100f, 100f)
        assertFalse(OverlapEngine.rectsOverlap(a, b))
    }

    @Test
    fun `partial overlap detected`() {
        val a = el("a", 0f, 0f, 100f, 100f)
        val b = el("b", 50f, 50f, 100f, 100f)
        assertTrue(OverlapEngine.rectsOverlap(a, b))
    }

    @Test
    fun `resolveOnRelease keeps moved element when no overlap`() {
        val pre = el("a", 0f, 0f)
        val moved = el("a", 300f, 300f)
        val others = listOf(el("b", 500f, 500f))
        val result = OverlapEngine.resolveOnRelease(moved, pre, others)
        assertEquals(moved, result)
    }

    @Test
    fun `resolveOnRelease reverts to pre-gesture state when overlap occurs`() {
        val pre = el("a", 0f, 0f)
        val moved = el("a", 50f, 50f)
        val others = listOf(el("b", 0f, 0f, 100f, 100f))
        val result = OverlapEngine.resolveOnRelease(moved, pre, others)
        assertEquals(pre, result)
    }

    @Test
    fun `findNonOverlappingPlacement returns preferred spot when free`() {
        val (x, y) = OverlapEngine.findNonOverlappingPlacement(
            preferredX = 10f, preferredY = 10f, width = 50f, height = 50f,
            canvasWidth = 800f, canvasHeight = 600f, existing = emptyList()
        )
        assertEquals(10f, x, 0.001f)
        assertEquals(10f, y, 0.001f)
    }

    @Test
    fun `findNonOverlappingPlacement searches diagonally when preferred spot is taken`() {
        val existing = listOf(el("a", 10f, 10f, 50f, 50f))
        val (x, y) = OverlapEngine.findNonOverlappingPlacement(
            preferredX = 10f, preferredY = 10f, width = 50f, height = 50f,
            canvasWidth = 800f, canvasHeight = 600f, existing = existing
        )
        val candidate = LayoutElement("__probe__", "probe", x, y, 50f, 50f)
        assertFalse(OverlapEngine.overlapsAny(candidate, existing))
    }

    @Test
    fun `findNonOverlappingPlacement falls back gracefully when canvas is full`() {
        // Densely pack the whole tiny canvas so no free spot can be found within the search budget.
        val existing = (0 until 100).map { i ->
            el("e$i", (i % 10) * 10f, (i / 10) * 10f, 10f, 10f)
        }
        val (x, y) = OverlapEngine.findNonOverlappingPlacement(
            preferredX = 5f, preferredY = 5f, width = 50f, height = 50f,
            canvasWidth = 100f, canvasHeight = 100f, existing = existing, maxSteps = 5
        )
        // Should not throw, and should return *some* coordinate (the fallback preferred position).
        assertEquals(5f, x, 0.001f)
        assertEquals(5f, y, 0.001f)
    }
}
