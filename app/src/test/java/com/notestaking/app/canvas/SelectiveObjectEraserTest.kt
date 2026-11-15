package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.ConnectorEnd
import com.notestaking.app.core.model.ConnectorItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.Routing
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.model.TapeItem
import com.notestaking.app.core.model.TapeMode
import com.notestaking.app.core.model.TapeShape
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-045: paged object eraser filters cannot erase non-target objects or PDF source. */
class SelectiveObjectEraserTest {
    private val ink = Rgba(20, 40, 80, 255)

    private fun line(tool: Tool, y: Double = 100.0) = Stroke(
        tool, ToolDefaults.configFor(tool),
        listOf(Sample(20.0, y, 1.0), Sample(220.0, y, 1.0)), straight = true,
    )

    private fun tape(y: Double = 100.0) = TapeItem(
        TapeShape(TapeMode.STRAIGHT, listOf(Pt(20.0, y), Pt(220.0, y)), 20.0), ink,
    )

    private fun box() = ShapeItem(
        ShapeKind.RECTANGLE, Pt(20.0, 20.0), Pt(220.0, 200.0), ink, 3.0,
        Rgba(170, 200, 140, 255),
    )

    private fun connector() = ConnectorItem(
        Routing.STRAIGHT, ConnectorEnd(Pt(20.0, 100.0)),
        ConnectorEnd(Pt(220.0, 100.0)), ink, 3.0,
    )

    private fun setup(
        mode: EraseMode,
        pages: List<Page>,
        rotation: Int = 0,
        zoom: Double = 1.0,
    ): Triple<CanvasState, InteractionController, History> {
        val state = CanvasState(Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1000
            viewportH = 1200
            viewingMode = ViewingMode.SINGLE
            verticalScroll = false
            rotationDeg = rotation
            this.zoom = zoom
            relayout()
            goToPage(pages.lastIndex)
            runAsync = {} // no Android/PDF raster work in a source-only unit test
        }
        val history = History()
        val controller = InteractionController(state, history, FakeTextMeasurer(), requestRender = {}).apply {
            setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
                eraseMode = mode, baseWidth = 8.0, eraseMarkups = true,
            ))
        }
        return Triple(state, controller, history)
    }

    private fun hit(state: CanvasState, page: Int, y: Double = 100.0): Pt =
        state.contentToViewport(state.fromPageSpace(page, Pt(120.0, y)))

    @Test fun targetModesPreserveMixedLayersPdfHighlightsAndOneUndoStep() {
        for (mode in listOf(EraseMode.TAPE, EraseMode.SHAPE, EraseMode.CONNECTOR)) {
            val pen = line(Tool.PEN)
            val marker = line(Tool.HIGHLIGHTER)
            val selectedTape = tape()
            val selectedShape = box()
            val selectedConnector = connector()
            val lockedTape = tape().also { it.locked = true }
            val lockedShape = box().also { it.locked = true }
            val lockedConnector = connector().also { it.locked = true }
            val initial = listOf<CanvasItem>(
                pen, selectedTape, marker, selectedShape, selectedConnector,
                lockedTape, lockedShape, lockedConnector,
            )
            val annotation = TextMarkup("highlight", MarkupType.HIGHLIGHT, ink, 0.5,
                listOf(TextQuad(40f, 40f, 150f, 120f, 0)), "PDF", null, 1L, 1L)
            val page = Page(600.0, 600.0, initial.toMutableList(), pdfPage = 0).apply {
                markups = listOf(annotation)
            }
            val (state, controller, history) = setup(mode, listOf(page))
            val point = hit(state, 0)
            controller.beginErase(point.x, point.y)
            controller.endErase()

            val targeted = when (mode) {
                EraseMode.TAPE -> selectedTape
                EraseMode.SHAPE -> selectedShape
                EraseMode.CONNECTOR -> selectedConnector
                else -> error("unrecognized test mode: $mode")
            }
            val remaining = initial.filterNot { it === targeted }
            assertEquals("mode=$mode", remaining, page.items)
            assertEquals("PDF highlight must remain in an object-only mode", listOf(annotation), page.markups)
            assertEquals("source PDF page must remain", 0, page.pdfPage)
            assertTrue("mode=$mode", history.canUndo)

            history.undo()
            assertEquals(initial, page.items)
            assertFalse("one touch is one Undo step: $mode", history.canUndo)
            history.redo()
            assertEquals(remaining, page.items)
            assertEquals(listOf(annotation), page.markups)
        }
    }

    @Test fun tapeErasesOnlyOnSelectedPageAcrossRotationZoomAndMargins() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (zoom in listOf(0.5, 1.0, 3.0)) {
                val neighbourTape = tape()
                val targetTape = tape()
                val pen = line(Tool.PEN)
                val marker = line(Tool.HIGHLIGHTER)
                val first = Page(600.0, 600.0, mutableListOf(neighbourTape))
                val second = Page(600.0, 600.0, mutableListOf(targetTape, pen, marker)).apply {
                    margins = PageMargins(left = 0.10, top = 0.05, right = 0.05)
                }
                val (state, controller, history) = setup(EraseMode.TAPE, listOf(first, second), rotation, zoom)
                val contact = hit(state, 1)
                controller.beginErase(contact.x, contact.y)
                controller.endErase()
                assertEquals("rot=$rotation zoom=$zoom", listOf<CanvasItem>(pen, marker), second.items)
                assertSame(neighbourTape, first.items.single())
                history.undo()
                assertEquals(listOf<CanvasItem>(targetTape, pen, marker), second.items)
                history.redo()
                assertEquals(listOf<CanvasItem>(pen, marker), second.items)
            }
        }
    }

    @Test fun changingSelectiveTypeMidGestureDoesNotEraseOtherTypes() {
        val targetTape = tape(100.0)
        val secondTape = tape(160.0)
        val pen = line(Tool.PEN, 160.0)
        val page = Page(600.0, 600.0, mutableListOf(targetTape, pen, secondTape))
        val (state, controller, history) = setup(EraseMode.TAPE, listOf(page))
        val down = hit(state, 0, 100.0)
        val moving = hit(state, 0, 160.0)
        controller.beginErase(down.x, down.y)
        controller.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
            eraseMode = EraseMode.STROKE, baseWidth = 8.0,
        ))
        controller.eraseAlong(moving.x, moving.y)
        controller.endErase()
        assertEquals(listOf<CanvasItem>(pen), page.items)
        history.undo()
        assertEquals(listOf<CanvasItem>(targetTape, pen, secondTape), page.items)
        assertFalse(history.canUndo)
    }
}
