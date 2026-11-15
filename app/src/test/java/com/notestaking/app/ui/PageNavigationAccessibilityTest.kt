package com.notestaking.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Navigation controls must reflect the same page rows that the editor actually steps through. */
class PageNavigationAccessibilityTest {
    @Test
    fun emptyAndSinglePageDocumentsHaveNoAvailablePageStep() {
        for (pageCount in listOf(0, 1)) {
            assertFalse(pageNavigationEnabled(0..0, pageCount, forward = false))
            assertFalse(pageNavigationEnabled(0..0, pageCount, forward = true))
        }
    }

    @Test
    fun doublePageSpreadsDisableOnlyTheUnavailableEdgeAction() {
        assertFalse(pageNavigationEnabled(0..1, 6, forward = false))
        assertTrue(pageNavigationEnabled(0..1, 6, forward = true))
        assertTrue(pageNavigationEnabled(2..3, 6, forward = false))
        assertTrue(pageNavigationEnabled(2..3, 6, forward = true))
        assertTrue(pageNavigationEnabled(4..5, 6, forward = false))
        assertFalse(pageNavigationEnabled(4..5, 6, forward = true))
    }

    @Test
    fun coverModeAndPartialLastSpreadUseRowBoundaries() {
        assertFalse(pageNavigationEnabled(0..0, 5, forward = false))
        assertTrue(pageNavigationEnabled(0..0, 5, forward = true))
        assertTrue(pageNavigationEnabled(1..2, 5, forward = false))
        assertTrue(pageNavigationEnabled(1..2, 5, forward = true))
        assertTrue(pageNavigationEnabled(3..4, 5, forward = false))
        assertFalse(pageNavigationEnabled(3..4, 5, forward = true))
    }
}
