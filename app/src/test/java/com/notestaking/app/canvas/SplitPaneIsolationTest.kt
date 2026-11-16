package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-067: each split pane owns its own document, viewport, selection, cache and undo stack. */
class SplitPaneIsolationTest {
    private fun line(y: Double) = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        listOf(Sample(30.0, y, 0.7), Sample(460.0, y, 0.7)),
        straight = true,
    )

    private fun state(
        pages: List<Page>,
        width: Int,
        height: Int,
        paginated: Boolean = false,
    ) = CanvasState(Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT).apply {
        viewportW = width
        viewportH = height
        verticalScroll = !paginated
        viewingMode = ViewingMode.SINGLE
        relayout()
        zoom = 1.0
        goToPage(0)
        runAsync = {}
    }

    private fun hit(st: CanvasState, pageIndex: Int, y: Double): Pt =
        st.contentToViewport(st.fromPageSpace(pageIndex, Pt(120.0, y)))

    private fun strokeEraser(state: CanvasState, history: History, notify: () -> Unit = {}) =
        InteractionController(state, history, FakeTextMeasurer(), requestRender = {}, onContentChanged = notify).apply {
            setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(eraseMode = EraseMode.STROKE))
        }

    @Test fun independentPenPaneEraseSelectionAndUndoNeverTouchSiblingHistory() {
        val leftInk = line(110.0)
        val rightInk = line(230.0)
        val leftPage = Page(600.0, 750.0, mutableListOf(leftInk))
        val rightPage = Page(600.0, 750.0, mutableListOf(rightInk))
        val left = state(listOf(leftPage), 360, 850)
        val right = state(listOf(rightPage), 240, 850)
        val leftHistory = History()
        val rightHistory = History()
        var leftNotified = 0
        var rightNotified = 0
        val leftCtrl = strokeEraser(left, leftHistory) { leftNotified++ }
        val rightCtrl = strokeEraser(right, rightHistory) { rightNotified++ }
        rightCtrl.selectPlaced(0, listOf(rightInk))
        assertTrue(rightCtrl.hasSelection)
        val leftPoint = hit(left, 0, 110.0)
        leftCtrl.beginErase(leftPoint.x, leftPoint.y)
        leftCtrl.endErase()

        assertTrue("primary pane erase committed", leftPage.items.isEmpty())
        assertSame("secondary ink remains", rightInk, rightPage.items.single())
        assertTrue("secondary selection remains", rightCtrl.hasSelection)
        assertTrue(left.document.dirty)
        assertFalse(right.document.dirty)
        assertTrue(leftNotified > 0)
        assertEquals(0, rightNotified)
        assertTrue(leftHistory.canUndo)
        assertFalse(rightHistory.canUndo)

        leftHistory.undo()
        assertSame(leftInk, leftPage.items.single())
        assertTrue(leftHistory.canRedo)
        assertFalse(rightHistory.canRedo)

        rightCtrl.clearSelection()
        val rightPoint = hit(right, 0, 230.0)
        rightCtrl.beginErase(rightPoint.x, rightPoint.y)
        rightCtrl.endErase()
        assertTrue(rightPage.items.isEmpty())
        assertTrue(rightNotified > 0)
        assertTrue(rightHistory.canUndo)
        assertTrue(leftHistory.canRedo)
        rightHistory.undo()
        assertSame(rightInk, rightPage.items.single())
        leftHistory.redo()
        assertTrue(leftPage.items.isEmpty())
        assertSame(rightInk, rightPage.items.single())
        rightHistory.redo()
        assertTrue(rightPage.items.isEmpty())
    }

    @Test fun overlappingGesturesInTwoPanesCommitToTheirOwnHistoryOnly() {
        val leftInk = line(135.0)
        val rightInk = line(245.0)
        val leftPage = Page(600.0, 800.0, mutableListOf(leftInk))
        val rightPage = Page(600.0, 800.0, mutableListOf(rightInk))
        val left = state(listOf(leftPage), 320, 760)
        val right = state(listOf(rightPage), 320, 760)
        val leftHistory = History()
        val rightHistory = History()
        val leftCtrl = strokeEraser(left, leftHistory)
        val rightCtrl = strokeEraser(right, rightHistory)

        val leftPoint = hit(left, 0, 135.0)
        val rightPoint = hit(right, 0, 245.0)
        leftCtrl.beginErase(leftPoint.x, leftPoint.y)
        rightCtrl.beginErase(rightPoint.x, rightPoint.y)
        rightCtrl.endErase()
        assertTrue(rightPage.items.isEmpty())
        assertTrue(leftPage.items.isEmpty())
        assertFalse("primary gesture has not committed yet", leftHistory.canUndo)
        assertTrue(rightHistory.canUndo)
        leftCtrl.endErase()
        assertTrue(leftHistory.canUndo)

        rightHistory.undo()
        assertSame(rightInk, rightPage.items.single())
        assertTrue(leftPage.items.isEmpty())
        leftHistory.undo()
        assertSame(leftInk, leftPage.items.single())
        assertSame(rightInk, rightPage.items.single())
        leftHistory.redo()
        assertTrue(leftPage.items.isEmpty())
        assertSame(rightInk, rightPage.items.single())
    }

    @Test fun resizingOnePaneAndNavigatingPagesPreservesTheOtherPanesView() {
        for (paginated in listOf(false, true)) {
            val first = state(List(4) { Page(600.0, 800.0) }, 600, 800, paginated)
            val second = state(List(3) { Page(700.0, 900.0) }, 600, 800, paginated)
            first.fitWidth()
            second.setView(1.7, 0.0, 480.0)
            second.goToPage(2)
            val original = second.snapshotView()
            val otherIndex = second.currentPageIndex()
            val otherFit = second.fitWidthActive

            for ((w, h) in listOf(180 to 850, 240 to 850, 320 to 680, 600 to 480, 1000 to 180)) {
                first.viewportW = w
                first.viewportH = h
                first.relayout()
                first.reflowFitWidthForResize()
                first.goToPage(3)
                if (paginated) assertEquals(3, first.currentPageIndex())
                else assertTrue(first.currentPageIndex() in 0..3)
                assertTrue("pane width=$w and height=$h must keep a finite zoom", first.zoom.isFinite())
                assertTrue(first.zoom in first.minZoom..first.maxZoom)
                assertTrue(first.scrollX.isFinite() && first.scrollY.isFinite())
                assertEquals("sibling zoom is unchanged", original.zoom, second.zoom, 0.0)
                assertEquals("sibling scroll X unchanged", original.scrollX, second.scrollX, 0.0)
                assertEquals("sibling scroll Y unchanged", original.scrollY, second.scrollY, 0.0)
                assertEquals(otherIndex, second.currentPageIndex())
                assertEquals(otherFit, second.fitWidthActive)
            }
        }
    }

    @Test fun splitPaneSnapshotsRestoreIndependentlyWithoutLeakingViewOrCanvasCaches() {
        val leftPage = Page(600.0, 700.0, mutableListOf(line(100.0)))
        val rightPage = Page(600.0, 700.0, mutableListOf(line(200.0)))
        val left = state(listOf(leftPage, Page(600.0, 700.0)), 320, 850)
        val right = state(listOf(rightPage, Page(600.0, 700.0)), 240, 850)
        left.setView(1.5, 120.0, 280.0)
        right.setView(2.1, 220.0, 650.0)
        val leftSnapshot = left.snapshotView()
        val rightSnapshot = right.snapshotView()
        assertTrue(leftSnapshot.zoom != rightSnapshot.zoom)
        val leftCache = left.cacheFor(leftPage)
        val rightCache = right.cacheFor(rightPage)
        assertNotSame(leftCache, rightCache)

        left.invalidatePage(leftPage)
        assertSame("one pane invalidation must not evict sibling cache",
            rightCache, right.cacheFor(rightPage))
        left.cacheFor(leftPage)
        val recoveredLeft = state(left.document.pages, 320, 850)
        val recoveredRight = state(right.document.pages, 240, 850)
        recoveredLeft.setView(leftSnapshot.zoom, leftSnapshot.scrollX, leftSnapshot.scrollY)
        recoveredRight.setView(rightSnapshot.zoom, rightSnapshot.scrollX, rightSnapshot.scrollY)

        assertEquals(leftSnapshot.zoom, recoveredLeft.zoom, 1e-9)
        assertEquals(rightSnapshot.zoom, recoveredRight.zoom, 1e-9)
        assertEquals(leftSnapshot.scrollX, recoveredLeft.scrollX, 1e-6)
        assertEquals(rightSnapshot.scrollX, recoveredRight.scrollX, 1e-6)
        assertEquals(leftSnapshot.scrollY, recoveredLeft.scrollY, 1e-6)
        assertEquals(rightSnapshot.scrollY, recoveredRight.scrollY, 1e-6)
        assertSame(leftPage, recoveredLeft.document.pages.first())
        assertSame(rightPage, recoveredRight.document.pages.first())
        assertNotSame(recoveredLeft.document, recoveredRight.document)
    }
}
