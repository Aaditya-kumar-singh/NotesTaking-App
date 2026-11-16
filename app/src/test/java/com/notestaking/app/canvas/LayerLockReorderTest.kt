package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Arrange
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.EraseRule
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-050: locked layers and their z-order must survive edits and history on the correct page. */
class LayerLockReorderTest {
    private fun box(x: Double, color: Int = 25) =
        ShapeItem(ShapeKind.RECTANGLE, Pt(x, 40.0), Pt(x + 45.0, 90.0),
            Rgba(color, 40, 80, 255), 3.0)

    private fun state(vararg pages: Page) = CanvasState(
        Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT,
    ).apply {
        viewportW = 1000
        viewportH = 1200
        relayout()
        runAsync = {}
    }

    @Test fun pagedLayerLockAndUnlockMarkNoteDirtyAndRestoreWithHistory() {
        val a = box(30.0)
        val b = box(35.0)
        val lockedNeighbour = box(32.0).apply { locked = true }
        val page = Page(600.0, 600.0, mutableListOf(a, lockedNeighbour, b), pdfPage = 4)
        val st = state(page)
        val history = History()
        val dirtinessAtCallback = mutableListOf<Boolean>()
        val controller = InteractionController(
            st, history, FakeTextMeasurer(), requestRender = {},
            onContentChanged = { dirtinessAtCallback += st.document.dirty },
        )

        controller.selectPlaced(0, listOf(a, b))
        assertFalse(st.document.dirty)
        controller.lockSelection()
        assertTrue("locking must schedule a save through the dirty flag", st.document.dirty)
        assertEquals(listOf(true), dirtinessAtCallback)
        assertTrue(a.locked && b.locked && lockedNeighbour.locked)
        assertFalse(controller.hasSelection)
        assertFalse(EraseRule.erasable(a))
        assertFalse(EraseRule.erasable(lockedNeighbour))
        assertTrue(SelectionMath.lassoMembers(page.items,
            listOf(Pt(20.0, 20.0), Pt(100.0, 20.0), Pt(100.0, 120.0), Pt(20.0, 120.0))).isEmpty())

        history.undo()
        assertFalse(a.locked || b.locked)
        assertTrue(lockedNeighbour.locked)
        assertEquals(listOf<CanvasItem>(a, lockedNeighbour, b), page.items)
        history.redo()
        assertTrue(a.locked && b.locked)
        st.document.dirty = false // simulate a completed autosave
        controller.unlockItem(a)
        assertTrue("unlock must also schedule a save", st.document.dirty)
        assertEquals(listOf(true, true), dirtinessAtCallback)
        assertFalse(a.locked)
        assertTrue(b.locked && lockedNeighbour.locked)
        history.undo()
        assertTrue(a.locked)
        history.redo()
        assertFalse(a.locked)
        assertEquals(4, page.pdfPage)
    }

    @Test fun noOpUnlockDoesNotDirtyOrInsertHistory() {
        val a = box(20.0)
        val page = Page(600.0, 600.0, mutableListOf(a))
        val st = state(page)
        val history = History()
        val controller = InteractionController(st, history, FakeTextMeasurer(), requestRender = {})
        controller.unlockItem(a)
        assertFalse(st.document.dirty)
        assertFalse(history.canUndo)
        assertSame(a, page.items.single())
    }

    @Test fun lockedLayerKeepsItsIdentityAndPositionThroughPageReorderAndArrangeUndo() {
        val other = Page(600.0, 600.0, mutableListOf(box(220.0)), pdfPage = 0)
        val selected = box(20.0)
        val locked = box(25.0).apply { this.locked = true }
        val top = box(30.0)
        val target = Page(600.0, 600.0, mutableListOf(selected, locked, top), pdfPage = 7)
        val st = state(other, target)
        val history = History()
        val controller = InteractionController(st, history, FakeTextMeasurer(), requestRender = {})
        controller.selectPlaced(1, listOf(selected))
        controller.arrange(Arrange.FRONT)
        assertEquals(listOf<CanvasItem>(locked, top, selected), target.items)
        assertSame(other.items.single(), st.document.pages[0].items.single())
        assertTrue(history.canUndo)
        assertTrue(locked.locked)

        st.document.pages.reverse()
        st.relayout()
        assertSame(target, st.document.pages[0])
        history.undo()
        assertEquals(listOf<CanvasItem>(selected, locked, top), target.items)
        assertFalse(history.canUndo)
        history.redo()
        assertEquals(listOf<CanvasItem>(locked, top, selected), target.items)
        assertTrue(locked.locked)
        assertEquals(7, target.pdfPage)
        assertEquals(0, other.pdfPage)
        assertSame(other, st.document.pages[1])
    }
}
