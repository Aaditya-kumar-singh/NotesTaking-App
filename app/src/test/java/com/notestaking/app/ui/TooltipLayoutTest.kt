package com.notestaking.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The keyboard shortcut moves below the tooltip label at large accessibility text sizes. */
class TooltipLayoutTest {
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
