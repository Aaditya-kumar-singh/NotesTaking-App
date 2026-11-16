package com.notestaking.app.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The keyboard shortcut moves below the tooltip label for large text or narrow windows/panes. */
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

    @Test
    fun narrowPanesUseTheirOwnWidthsInsideWideWindows() {
        for (window in listOf(800, 840)) {
            assertEquals(148.dp, tooltipMaxWidth(window.dp, 180.dp))
            assertEquals(208.dp, tooltipMaxWidth(window.dp, 240.dp))
            assertEquals(280.dp, tooltipMaxWidth(window.dp, 320.dp))
            assertEquals(280.dp, tooltipMaxWidth(window.dp))
            assertEquals(208.dp, tooltipMaxWidth(240.dp, 800.dp))
        }
    }

    @Test
    fun shortcutsStackAtPaneWidthAndLargeFontScale() {
        for (window in listOf(800, 840)) {
            assertTrue(tooltipStackShortcut(1f, window, 180.dp))
            assertTrue(tooltipStackShortcut(1f, window, 240.dp))
            assertFalse(tooltipStackShortcut(1f, window, 320.dp))
            for (scale in listOf(1.3f, 2f)) {
                for (paneWidth in listOf(180.dp, 240.dp, 320.dp)) {
                    assertTrue(tooltipStackShortcut(scale, window, paneWidth))
                }
            }
        }
    }

    private fun assertPopupInside(pos: IntOffset, popup: IntSize, pane: IntRect) {
        assertTrue("popup x outside pane $pane: $pos", pos.x >= pane.left)
        assertTrue("popup right outside pane $pane: $pos", pos.x + popup.width <= pane.right)
        assertTrue("popup y outside pane $pane: $pos", pos.y >= pane.top)
        assertTrue("popup bottom outside pane $pane: $pos", pos.y + popup.height <= pane.bottom)
    }

    @Test
    fun tooltipPlacementStaysInEachSplitPaneRatherThanOnlyTheWindow() {
        val window = IntSize(840, 600)
        for (width in listOf(180, 240, 320)) {
            val popup = IntSize(tooltipMaxWidth(800.dp, width.dp).value.toInt() + 16, 72)
            val panes = listOf(IntRect(0, 0, width, 600), IntRect(840 - width, 0, 840, 600))
            for (pane in panes) {
                for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
                    val anchor = if (pane.left == 0) {
                        IntRect(pane.right - 48, 100, pane.right, 148)
                    } else {
                        IntRect(pane.left, 100, pane.left + 48, 148)
                    }
                    val placement = TipPlacement(TipSide.BELOW, gap = -2, pane = pane)
                    val result = placement.calculatePosition(anchor, window, direction, popup)
                    assertPopupInside(result, popup, pane)
                }
            }
        }
    }

    @Test
    fun horizontalAndVerticalDockingFlipAgainstPaneNotWindow() {
        val window = IntSize(840, 600)
        val narrowTop = IntRect(0, 0, 840, 180)
        val below = TipPlacement(TipSide.BELOW, -2, narrowTop)
        val fromBottomEdge = below.calculatePosition(
            IntRect(330, 132, 378, 172), window, LayoutDirection.Ltr, IntSize(180, 64),
        )
        assertEquals(TipSide.ABOVE, below.placed)
        assertPopupInside(fromBottomEdge, IntSize(180, 64), narrowTop)

        val narrowBottom = IntRect(0, 340, 840, 600)
        val above = TipPlacement(TipSide.ABOVE, -2, narrowBottom)
        val fromTopEdge = above.calculatePosition(
            IntRect(330, 344, 378, 392), window, LayoutDirection.Ltr, IntSize(180, 80),
        )
        assertEquals(TipSide.BELOW, above.placed)
        assertPopupInside(fromTopEdge, IntSize(180, 80), narrowBottom)

        val floatingPane = IntRect(300, 80, 540, 480)
        val right = TipPlacement(TipSide.RIGHT, -2, floatingPane)
        val rightResult = right.calculatePosition(
            IntRect(495, 200, 527, 248), window, LayoutDirection.Ltr, IntSize(180, 60),
        )
        assertEquals(TipSide.LEFT, right.placed)
        assertPopupInside(rightResult, IntSize(180, 60), floatingPane)

        val left = TipPlacement(TipSide.LEFT, -2, floatingPane)
        val leftResult = left.calculatePosition(
            IntRect(313, 200, 345, 248), window, LayoutDirection.Ltr, IntSize(180, 60),
        )
        assertEquals(TipSide.RIGHT, left.placed)
        assertPopupInside(leftResult, IntSize(180, 60), floatingPane)
    }

    @Test
    fun impossibleTinyPaneFallsBackToWindowWithoutOverflow() {
        val window = IntSize(840, 600)
        val pane = IntRect(200, 100, 220, 112)
        val popup = IntSize(180, 70)
        val tip = TipPlacement(TipSide.BELOW, -2, pane)
        val result = tip.calculatePosition(
            IntRect(200, 100, 220, 112), window, LayoutDirection.Ltr, popup,
        )
        assertTrue(result.x >= 0 && result.x + popup.width <= window.width)
        assertTrue(result.y >= 0 && result.y + popup.height <= window.height)
    }

    @Test
    fun noPanePreservesWindowClamping() {
        val window = IntSize(800, 600)
        val popup = IntSize(200, 72)
        val tip = TipPlacement(TipSide.RIGHT, -2)
        val pos = tip.calculatePosition(
            IntRect(760, 300, 792, 348), window, LayoutDirection.Ltr, popup,
        )
        assertEquals(TipSide.LEFT, tip.placed)
        assertTrue(pos.x >= 0 && pos.x + popup.width <= window.width)
    }
}
