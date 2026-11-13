package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The keyboard shortcut moves below the tooltip label for large text or narrow windows. */
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
        assertFalse(tooltipStackShortcut(1f, 320))
        assertFalse(tooltipStackShortcut(1.29f, 360))
    }

    @Test
    fun shortcutStacksAtLargeTextScale() {
        assertTrue(tooltipStackShortcut(1.3f, 320))
        assertTrue(tooltipStackShortcut(2f, 360))
    }

    @Test
    fun narrowWindowsStackShortcutsEvenAtNormalFontSize() {
        assertFalse(tooltipStackShortcut(1f, 312))
        assertTrue(tooltipStackShortcut(1f, 311))
        assertTrue(tooltipStackShortcut(1f, 240))
        assertTrue(tooltipStackShortcut(1.29f, 180))
    }
}
