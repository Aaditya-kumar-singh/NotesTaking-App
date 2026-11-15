package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.AddItem
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** NT-039: all contacts between down and up form one exact undoable erase transaction. */
class EraseSweepTransactionTest {
    private fun line(y: Double): Stroke = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        listOf(Sample(20.0, y, 1.0), Sample(540.0, y, 1.0)),
        straight = true,
    )

    private fun fixture(vararg strokes: Stroke): Triple<CanvasState, InteractionController, History> {
        val items = mutableListOf<CanvasItem>()
        items.addAll(strokes.asList())
        val page = Page(600.0, 600.0, items)
        val state = CanvasState(Document(mutableListOf(page)), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 800
            viewportH = 1000
            relayout()
        }
        val history = History()
        return Triple(state, InteractionController(state, history, FakeTextMeasurer(), requestRender = {}), history)
    }

    private fun at(state: CanvasState, x: Double, y: Double): Pt =
        state.contentToViewport(state.fromPageSpace(0, Pt(x, y)))

    private fun erase(controller: InteractionController, at: Pt) {
        controller.eraseAlong(at.x, at.y)
    }

    @Test fun threeCrossingsOfOneLongStrokeUndoInOneStepAndRedoIdentically() {
        val underneath = line(320.0)
        val ink = line(100.0)
        val above = line(420.0)
        val (state, controller, history) = fixture(underneath, ink, above)
        val page = state.document.pages.single()
        val before = page.items.toList()

        controller.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
            baseWidth = 8.0, eraseMode = EraseMode.AREA,
        ))
        val first = at(state, 100.0, 100.0)
        controller.beginErase(first.x, first.y)
        for ((x, y) in listOf(100.0 to 210.0, 250.0 to 210.0, 250.0 to 100.0,
            250.0 to 210.0, 400.0 to 210.0, 400.0 to 100.0)) {
            erase(controller, at(state, x, y))
        }
        controller.endErase()
        val after = page.items.toList()
        assertEquals("three holes leave four independently editable pieces", 6, after.size)
        assertEquals(4, after.filterIsInstance<Stroke>().count { it !== underneath && it !== above })
        assertEquals(underneath, after.first())
        assertEquals(above, after.last())
        assertTrue(history.canUndo)

        history.undo()
        assertEquals("one Undo restores all crossings and z-order", before, page.items)
        assertFalse("no intermediate hole should be independently undoable", history.canUndo)
        assertTrue(history.canRedo)
        history.redo()
        assertEquals("Redo restores exact fragment identities and positions", after, page.items)
        history.undo()

        val newInk = line(500.0)
        page.items.add(newInk)
        history.push(AddItem(page, newInk))
        assertFalse("a new edit clears the erased sweep's redo branch", history.canRedo)
    }

    @Test fun changingEraserModeMidDragDoesNotLoseUndoHistory() {
        for (start in listOf(EraseMode.AREA, EraseMode.STROKE)) {
            val ink = line(100.0)
            val untouched = line(300.0)
            val (state, controller, history) = fixture(ink, untouched)
            val before = state.document.pages.single().items.toList()
            val first = at(state, 260.0, 100.0)
            val cfg = ToolDefaults.configFor(Tool.ERASER).copy(baseWidth = 8.0, eraseMode = start)
            controller.setToolConfig(Tool.ERASER, cfg)
            controller.beginErase(first.x, first.y)
            controller.setToolConfig(Tool.ERASER, cfg.copy(
                eraseMode = if (start == EraseMode.AREA) EraseMode.STROKE else EraseMode.AREA,
            ))
            controller.endErase()

            val after = state.document.pages.single().items.toList()
            assertNotEquals("the contact should erase ink: $start", before, after)
            assertTrue("the original mode must still commit: $start", history.canUndo)
            history.undo()
            assertEquals("original stroke and layering restored: $start", before, state.document.pages.single().items)
            assertFalse("exactly one undo entry for $start", history.canUndo)
            history.redo()
            assertEquals(after, state.document.pages.single().items)
        }
    }

    @Test fun changingToolDuringAnActiveEraseCommitsOneUndoableGesture() {
        val ink = line(100.0)
        val (state, controller, history) = fixture(ink, line(400.0))
        val before = state.document.pages.single().items.toList()
        controller.setTool(Tool.ERASER)
        controller.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(baseWidth = 10.0))
        val first = at(state, 100.0, 100.0)
        controller.beginErase(first.x, first.y)
        erase(controller, at(state, 240.0, 100.0))
        controller.setTool(Tool.PEN) // abortGesture commits the already-applied erase
        assertTrue(history.canUndo)
        history.undo()
        assertEquals(before, state.document.pages.single().items)
        assertFalse("aborting does not create extra history entries", history.canUndo)
    }
}
