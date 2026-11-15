package com.notestaking.app.core.infinite

import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.ConnectorEnd
import com.notestaking.app.core.model.ConnectorItem
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.Routing
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.model.TapeItem
import com.notestaking.app.core.model.TapeMode
import com.notestaking.app.core.model.TapeShape
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

/** NP-045: each explicit object eraser leaves every other kind on the infinite canvas untouched. */
class SelectiveObjectEraseSessionTest {
    private val ink = Rgba(20, 40, 80, 255)

    private fun line(tool: Tool = Tool.PEN, y: Double = 100.0) = Stroke(
        tool, ToolDefaults.configFor(tool),
        listOf(Sample(20.0, y, 1.0), Sample(220.0, y, 1.0)), straight = true,
    )

    private fun tape(y: Double) = TapeItem(
        TapeShape(TapeMode.STRAIGHT, listOf(Pt(20.0, y), Pt(220.0, y)), 20.0), ink,
    )

    private fun shape() = ShapeItem(
        ShapeKind.RECTANGLE, Pt(20.0, 20.0), Pt(220.0, 200.0), ink, 3.0,
        Rgba(170, 200, 140, 255),
    )

    private fun connector() = ConnectorItem(
        Routing.STRAIGHT, ConnectorEnd(Pt(20.0, 100.0)),
        ConnectorEnd(Pt(220.0, 100.0)), ink, 3.0,
    )

    private fun doc(vararg items: CanvasItem) = InfiniteDocument().apply { addAll(items.toList()) }

    @Test fun onlyTapeObjectsAreRemovedAndOneUndoRestoresExactOriginalOrder() {
        val first = tape(100.0)
        val pen = line()
        val marker = line(Tool.HIGHLIGHTER)
        val filled = shape()
        val connector = connector()
        val locked = tape(100.0).also { it.locked = true }
        val second = tape(160.0)
        val original = listOf<CanvasItem>(first, pen, marker, filled, connector, locked, second)
        val document = doc(*original.toTypedArray())
        val session = EraseSession(document)
        val originalFill = filled.fillRgba

        assertTrue(session.erase(120.0, 100.0, 8.0, EraseMode.TAPE) != null)
        assertTrue(session.erase(120.0, 160.0, 8.0, EraseMode.TAPE) != null)
        val remaining = listOf<CanvasItem>(pen, marker, filled, connector, locked)
        assertEquals(remaining, document.items)
        assertSame(originalFill, filled.fillRgba)
        assertEquals(document.itemCount, document.index.size)

        val history = History()
        history.push(session.buildCommand()!!)
        history.undo()
        assertEquals(original, document.items)
        assertFalse("one gesture is exactly one Undo step", history.canUndo)
        history.redo()
        assertEquals(remaining, document.items)
        assertSame(originalFill, filled.fillRgba)
        assertEquals(document.itemCount, document.index.size)
    }

    @Test fun shapeOnlyModeRemovesFilledShapeOnInteriorContactButNotOtherObjects() {
        val pen = line()
        val marker = line(Tool.HIGHLIGHTER)
        val tape = tape(100.0)
        val box = shape()
        val lockedBox = shape().also { it.locked = true }
        val connector = connector()
        val document = doc(pen, marker, tape, box, lockedBox, connector)
        val session = EraseSession(document)
        assertTrue("filled shape is an object, not a fill-only edit",
            session.erase(120.0, 125.0, 8.0, EraseMode.SHAPE) != null)
        assertEquals(listOf<CanvasItem>(pen, marker, tape, lockedBox, connector), document.items)
        val command = session.buildCommand()!!
        command.undo()
        assertEquals(listOf<CanvasItem>(pen, marker, tape, box, lockedBox, connector), document.items)
        assertTrue(box.fillRgba != null)
        command.redo()
        assertTrue(document.items.none { it === box })
        assertSame(lockedBox, document.items[3])
    }

    @Test fun connectorOnlyModeLeavesTapePenMarkerAndShapeUntouched() {
        val pen = line()
        val marker = line(Tool.HIGHLIGHTER)
        val tape = tape(100.0)
        val shape = shape()
        val connector = connector()
        val locked = this.connector().also { it.locked = true }
        val document = doc(pen, marker, tape, shape, connector, locked)
        val session = EraseSession(document)
        assertTrue(session.erase(120.0, 100.0, 8.0, EraseMode.CONNECTOR) != null)
        assertEquals(listOf<CanvasItem>(pen, marker, tape, shape, locked), document.items)
        session.buildCommand()!!.run {
            undo()
            assertEquals(listOf<CanvasItem>(pen, marker, tape, shape, connector, locked), document.items)
            redo()
            assertEquals(listOf<CanvasItem>(pen, marker, tape, shape, locked), document.items)
        }
    }

    @Test fun aSelectiveObjectEraserIsNoOpIfOnlyNonTargetsAreUnderIt() {
        for (mode in listOf(EraseMode.TAPE, EraseMode.SHAPE, EraseMode.CONNECTOR)) {
            val pen = line()
            val marker = line(Tool.HIGHLIGHTER)
            val document = doc(pen, marker)
            val session = EraseSession(document)
            assertNull("mode=$mode", session.erase(120.0, 100.0, 8.0, mode))
            assertNull(session.buildCommand())
            assertEquals(listOf<CanvasItem>(pen, marker), document.items)
        }
    }

    @Test fun aMidStrokeModeChangeCannotStartErasingPenInk() {
        val first = tape(100.0)
        val second = tape(160.0)
        val pen = line(y = 160.0)
        val document = doc(first, pen, second)
        val session = EraseSession(document)
        assertTrue(session.erase(120.0, 100.0, 8.0, EraseMode.TAPE) != null)
        assertTrue(session.erase(120.0, 160.0, 8.0, EraseMode.STROKE) != null)
        assertEquals(listOf<CanvasItem>(pen), document.items)
        session.buildCommand()!!.run {
            undo()
            assertEquals(listOf<CanvasItem>(first, pen, second), document.items)
            redo()
            assertEquals(listOf<CanvasItem>(pen), document.items)
        }
    }

    @Test fun selectiveIdsAreExplicitAndLegacyModesRemainStable() {
        for (mode in EraseMode.entries) assertEquals(mode, EraseMode.fromId(mode.id))
        assertEquals(EraseMode.STROKE, EraseMode.fromId("unrecognized"))
        assertEquals(EraseMode.MARKER, EraseMode.fromId("marker"))
    }
}
