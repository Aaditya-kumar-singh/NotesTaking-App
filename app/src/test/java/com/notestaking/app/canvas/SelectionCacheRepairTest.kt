package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the select-tool flicker fix: a selection edit must repair the page's ink cache
 * **in place** (the eraser's path) rather than dropping it for an async rebuild, and a
 * delete must never flush the (PDF/template) background cache. Both used to blank the whole
 * layer for a frame. We assert it at the cache-map level through [CanvasState.cacheSnapshot]:
 * dropping a cache makes its page count fall to 0; an in-place repair leaves it standing.
 */
class SelectionCacheRepairTest {

    private fun dot(x: Double, y: Double): Stroke =
        Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), mutableListOf(Sample(x, y, 1.0)))

    private fun state(background: Boolean = false): CanvasState {
        val page = Page(200.0, 200.0, mutableListOf(dot(20.0, 20.0), dot(120.0, 120.0)))
        if (background) page.pdfPage = 0 // a real PDF-backed page, so a background cache is built and kept
        val doc = Document(mutableListOf(page))
        return CanvasState(doc, FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 800
            viewportH = 1000
            relayout()
            if (background) paintPageBackground = { _, _, _, _ -> } // a non-null background painter (stands in for a PDF)
        }
    }

    private fun controller(st: CanvasState) =
        InteractionController(st, History(), FakeTextMeasurer(), requestRender = {})

    @Test fun selectingThenDismissingKeepsInkCacheLive() {
        val st = state()
        val ctrl = controller(st)
        st.cacheFor(st.document.pages[0]) // warm the ink cache, as a draw frame would
        assertEquals(1, st.cacheSnapshot().inkPages)

        ctrl.selectAll() // lifts every item out of the cache
        assertEquals(
            "selecting must repair in place, not drop the page's ink cache",
            1, st.cacheSnapshot().inkPages,
        )

        ctrl.escape() // dismiss the selection (clearSelection)
        assertEquals(
            "dismissing must repair in place, not drop the page's ink cache",
            1, st.cacheSnapshot().inkPages,
        )
    }

    @Test fun lassoDeleteRemovesOnlyMembersAndOneUndoRestoresExactLayerOrder() {
        val bottom = dot(15.0, 15.0)
        val first = dot(50.0, 50.0)
        val between = dot(120.0, 120.0)
        val second = dot(55.0, 55.0)
        val locked = dot(52.0, 52.0).apply { this.locked = true }
        val top = dot(180.0, 180.0)
        val original = listOf<CanvasItem>(bottom, first, between, second, locked, top)
        val page = Page(200.0, 200.0, original.toMutableList())
        val st = CanvasState(Document(mutableListOf(page)), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 800
            viewportH = 1000
            relayout()
        }
        val history = History()
        val ctrl = InteractionController(st, history, FakeTextMeasurer(), requestRender = {})
        val polygon = listOf(Pt(40.0, 40.0), Pt(70.0, 40.0), Pt(70.0, 70.0), Pt(40.0, 70.0))
        val members = SelectionMath.lassoMembers(page.items, polygon)
        assertEquals(listOf(first, second), members)
        ctrl.selectPlaced(0, members) // the same selection pathway the lasso uses on pointer-up
        st.cacheFor(page)
        ctrl.deleteSelection()
        val remaining = listOf(bottom, between, locked, top)
        assertEquals(remaining, page.items)
        assertFalse(ctrl.hasSelection)
        assertTrue(history.canUndo)
        history.undo()
        assertEquals("Undo restores the original paint order, not an appended top layer", original, page.items)
        assertFalse("the entire lasso deletion is one command", history.canUndo)
        assertTrue(history.canRedo)
        history.redo()
        assertEquals(remaining, page.items)
    }

    @Test fun deletingSelectionLeavesBackgroundCacheIntact() {
        val st = state(background = true)
        val ctrl = controller(st)
        val page = st.document.pages[0]
        st.cacheFor(page)
        st.backgroundFor(page)
        assertEquals(1, st.cacheSnapshot().inkPages)
        assertEquals(1, st.cacheSnapshot().bgPages)

        ctrl.selectAll()
        ctrl.deleteSelection()

        // The expensive PDF/template background must survive a delete — it used to be flushed by
        // invalidateAllCaches(), which is precisely what made the PDF layer flicker on delete/cut.
        assertEquals("delete must not flush the background/PDF cache", 1, st.cacheSnapshot().bgPages)
        // The ink layer stays cached too (repaired in place), never dropped to bare paper.
        assertEquals(1, st.cacheSnapshot().inkPages)
    }
}
