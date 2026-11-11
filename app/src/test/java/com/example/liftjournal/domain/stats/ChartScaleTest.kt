package com.example.liftjournal.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScaleTest {
    @Test
    fun `nice step rounds up to 1, 2, 2_5 or 5 times a power of ten`() {
        assertEquals(1.0, niceStep(0.0), 0.0)
        assertEquals(2.5, niceStep(2.1), 0.0)
        assertEquals(5000.0, niceStep(4100.0), 0.0)
        assertEquals(100.0, niceStep(100.0), 0.0)
    }

    @Test
    fun `linear scale is zero based and covers the maximum`() {
        val scale = linearScale(listOf(0.0, 41_230.0, 12_000.0))
        assertEquals(0.0, scale.low, 0.0)
        assertEquals(20_000.0, scale.step, 0.0)
        assertEquals(60_000.0, scale.high, 0.0)
    }

    @Test
    fun `zoomed scale starts a step below the lightest normal week`() {
        val tops = listOf(100.0, 105.0, 110.0, 112.5, 0.0)
        val scale = zoomedScale(tops, zoomHeightPx = 280f)
        assertTrue(scale.isZoomed)
        assertEquals(2.5, scale.step, 0.0)
        assertEquals(97.5, scale.low, 0.0)
        assertEquals(112.5, scale.high, 0.0)
    }

    @Test
    fun `load-off weeks below 80 percent of the median do not stretch the zoom`() {
        val tops = listOf(100.0, 105.0, 110.0, 60.0)
        val scale = zoomedScale(tops, zoomHeightPx = 280f)
        assertTrue(scale.low > 60.0)
    }

    @Test
    fun `short charts use a larger step`() {
        val tops = listOf(100.0, 120.0, 140.0)
        val scale = zoomedScale(tops, zoomHeightPx = 60f)
        assertTrue(scale.gridlines().size <= 5)
        assertTrue(scale.step > 2.5)
    }

    @Test
    fun `light weights near zero fall back to an unzoomed axis`() {
        val scale = zoomedScale(listOf(2.0, 2.5), zoomHeightPx = 280f)
        assertFalse(scale.isZoomed)
    }

    @Test
    fun `goal scale has headroom above the larger value`() {
        val scale = goalScale(currentKg = 145.0, targetKg = 160.0)
        assertEquals(50.0, scale.step, 0.0)
        assertEquals(200.0, scale.high, 0.0)
    }
}
