package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import com.notestaking.app.settings.ToolbarLook
import com.notestaking.app.settings.ToolbarSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Editor icon buttons and clickable labels share the same hit-area metrics. */
class ToolbarMetricsTest {
    @Test
    fun iconAndTextActionTargetsMeetMinimumInBothToolbarOrientations() {
        for (size in ToolbarSize.entries) {
            for (vertical in listOf(false, true)) {
                val metrics = barMetrics(size).copy(vertical = vertical)
                assertTrue("$size vertical=$vertical button and text actions need 48dp", metrics.button >= 48.dp)
                assertEquals(8.dp, metrics.thickness - metrics.button)
                assertTrue(metrics.circle < metrics.button)
                assertTrue(metrics.swatch < metrics.button)
            }
        }
    }

    @Test
    fun floatingQuickActionsScrollWhileDockedQuickActionsRemainPinned() {
        assertTrue(toolbarScrollsTrailing(floating = true))
        assertFalse(toolbarScrollsTrailing(floating = false))
    }

    @Test
    fun collapsedFloatingToolbarHidesFavoritesWhenEvenOneWouldOverflow() {
        for (size in ToolbarSize.entries) {
            val buttonDp = barMetrics(size).button.value.toInt()
            val narrowCount = ToolbarLook.visibleFavoriteCount(180, 180, buttonDp)
            assertEquals(1, narrowCount)
            assertFalse("$size: expand must remain reachable in a 180dp pane",
                floatingFavoritesFit(180, narrowCount, buttonDp))

            val normalCount = ToolbarLook.visibleFavoriteCount(320, 320, buttonDp)
            assertTrue("$size: keep favorites when they fit",
                floatingFavoritesFit(320, normalCount, buttonDp))
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
