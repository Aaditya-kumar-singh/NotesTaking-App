package com.notestaking.app.core.infinite

import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-044: selectively remove only highlighter ink without invalidating the infinite index or undo. */
class MarkerEraseSessionTest {
    private fun ink(tool: Tool, y: Double): Stroke = Stroke(
        tool, ToolDefaults.configFor(tool),
        listOf(Sample(0.0, y, 1.0), Sample(200.0, y, 1.0)), straight = true,
    )

    @Test fun markerModeLeavesOverlappingPenFilledShapesAndLockedMarkersIntact() {
        val markerA = ink(Tool.HIGHLIGHTER, 0.0)
        val pen = ink(Tool.PEN, 0.0)
        val shape = ShapeItem(ShapeKind.RECTANGLE, Pt(0.0, -20.0), Pt(200.0, 100.0),
            Rgba(10, 10, 10, 255), 3.0, Rgba(100, 200, 100, 255))
        val fill = shape.fillRgba
        val locked = ink(Tool.HIGHLIGHTER, 0.0).also { it.locked = true }
        val markerB = ink(Tool.HIGHLIGHTER, 60.0)
        val original = listOf<CanvasItem>(markerA, pen, shape, locked, markerB)
        val doc = InfiniteDocument().apply { addAll(original) }
        val session = EraseSession(doc)

        assertTrue(session.erase(50.0, 0.0, 8.0, EraseMode.MARKER) != null)
        assertTrue(session.erase(50.0, 60.0, 8.0, EraseMode.MARKER) != null)
        val after = listOf<CanvasItem>(pen, shape, locked)
        assertEquals(after, doc.items)
        assertEquals(doc.itemCount, doc.index.size)
        assertSame(fill, shape.fillRgba)

        val history = History()
        history.push(session.buildCommand()!!)
        history.undo()
        assertEquals("one undo restores the original z-order", original, doc.items)
        assertFalse(history.canUndo)
        assertEquals(doc.itemCount, doc.index.size)
        history.redo()
        assertEquals(after, doc.items)
        assertEquals(doc.itemCount, doc.index.size)
        assertSame(fill, shape.fillRgba)
    }

    @Test fun markerModeDoesNothingWhenOnlyOrdinaryInkOrShapesAreTouched() {
        val pen = ink(Tool.PEN, 0.0)
        val shape = ShapeItem(ShapeKind.LINE, Pt(0.0, 0.0), Pt(200.0, 0.0),
            Rgba(0, 0, 0, 255), 2.0)
        val doc = InfiniteDocument().apply { addAll(listOf(pen, shape)) }
        val session = EraseSession(doc)
        assertNull(session.erase(50.0, 0.0, 8.0, EraseMode.MARKER))
        assertEquals(listOf<CanvasItem>(pen, shape), doc.items)
        assertNull(session.buildCommand())
    }

    @Test fun persistedModeIdRoundTripPreservesExistingModes() {
        assertEquals(EraseMode.MARKER, EraseMode.fromId(EraseMode.MARKER.id))
        assertEquals(EraseMode.AREA, EraseMode.fromId(EraseMode.AREA.id))
        assertEquals(EraseMode.STROKE, EraseMode.fromId(EraseMode.STROKE.id))
        assertEquals(EraseMode.STROKE, EraseMode.fromId("not-a-mode"))
    }

    @Test fun ordinaryEraserModesStillRemoveOrdinaryInk() {
        for (mode in listOf(EraseMode.STROKE, EraseMode.AREA)) {
            val pen = ink(Tool.PEN, 0.0)
            val doc = InfiniteDocument().apply { add(pen) }
            val session = EraseSession(doc)
            assertTrue("mode=$mode", session.erase(100.0, 0.0, 8.0, mode) != null)
            assertTrue("mode=$mode", doc.items.none { it === pen })
            session.buildCommand()!!.undo()
            assertSame(pen, doc.items.single())
        }
    }
}
