package dev.chell.launcher.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridMetricsTest {

    @Test
    fun `fits whole cells into the available area`() {
        val metrics = GridMetrics.fit(
            availableWidth = 1080,
            availableHeight = 1800,
            cellWidth = 270,
            cellHeight = 360,
        )
        assertEquals(4, metrics.columns)
        assertEquals(5, metrics.rows)
        assertEquals(20, metrics.pageSize)
    }

    @Test
    fun `ignores the remainder when cells do not divide evenly`() {
        val metrics = GridMetrics.fit(1000, 1000, 300, 300)
        assertEquals(3, metrics.columns)
        assertEquals(3, metrics.rows)
    }

    @Test
    fun `never returns a zero-sized grid`() {
        // A cell larger than the viewport still yields 1x1 rather than 0x0,
        // which AppDrawer would reject as a non-positive page size.
        val metrics = GridMetrics.fit(100, 100, 500, 500)
        assertEquals(1, metrics.columns)
        assertEquals(1, metrics.rows)
        AppDrawer(listOf(AppInfo("a", "A")), metrics.pageSize)
    }

    @Test
    fun `rejects non-positive cell dimensions`() {
        assertFailsWith<IllegalArgumentException> { GridMetrics.fit(100, 100, 0, 10) }
        assertFailsWith<IllegalArgumentException> { GridMetrics.fit(100, 100, 10, -1) }
    }

    @Test
    fun `rejects non-positive grid dimensions`() {
        assertFailsWith<IllegalArgumentException> { GridMetrics(0, 5) }
        assertFailsWith<IllegalArgumentException> { GridMetrics(4, 0) }
    }
}
