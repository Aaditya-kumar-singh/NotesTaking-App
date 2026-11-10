package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import com.notestaking.app.settings.ToolbarSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Editor tool controls share the same hit-area metrics in paged and infinite canvases. */
class ToolbarMetricsTest {
    @Test
    fun allButtonSizesMeetAndroidMinimumAndHaveSpacing() {
        for (size in ToolbarSize.entries) {
            val metrics = barMetrics(size)
            assertTrue("$size button must be finger-sized", metrics.button >= 48.dp)
            assertEquals(8.dp, metrics.thickness - metrics.button)
            assertTrue(metrics.circle < metrics.button)
            assertTrue(metrics.swatch < metrics.button)
        }
    }

    @Test
    fun iconAndToolbarDimensionsMatchDesignPresets() {
        assertEquals(20.dp, barMetrics(ToolbarSize.COMPACT).icon)
        assertEquals(24.dp, barMetrics(ToolbarSize.REGULAR).icon)
        assertEquals(26.dp, barMetrics(ToolbarSize.COMFORTABLE).icon)
        assertEquals(56.dp, barMetrics(ToolbarSize.COMPACT).thickness)
        assertEquals(56.dp, barMetrics(ToolbarSize.REGULAR).thickness)
        assertEquals(64.dp, barMetrics(ToolbarSize.COMFORTABLE).thickness)
    }
}
