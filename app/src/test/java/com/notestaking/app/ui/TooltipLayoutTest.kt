package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The keyboard shortcut moves below the tooltip label at large accessibility text sizes. */
class TooltipLayoutTest {
    @Test
    fun tooltipWidthLeavesSpaceForWindowEdges() {
        assertEquals(280.dp, tooltipMaxWidth(320.dp))
        assertEquals(280.dp, tooltipMaxWidth(360.dp))
        assertEquals(208.dp, tooltipMaxWidth(240.dp))
        assertEquals(148.dp, tooltipMaxWidth(180.dp))
    }

    @Test
    fun tinyWindowsStillProduceAPositiveWidth() {
        assertEquals(1.dp, tooltipMaxWidth(24.dp))
    }

    @Test
    fun shortcutRemainsInlineAtNormalTextScale() {
        assertFalse(tooltipStackShortcut(1f))
        assertFalse(tooltipStackShortcut(1.29f))
    }

    @Test
    fun shortcutStacksAtLargeTextScale() {
        assertTrue(tooltipStackShortcut(1.3f))
        assertTrue(tooltipStackShortcut(2f))
    }
}
