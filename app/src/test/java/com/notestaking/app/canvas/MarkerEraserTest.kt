package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.model.Stroke
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

/** NP-044: marker-only mode must never damage ordinary ink, PDF source or shapes. */
class MarkerEraserTest {
    private fun line(tool: Tool, y: Double): Stroke = Stroke(
        tool, ToolDefaults.configFor(tool),
        listOf(Sample(20.0, y, 0.8), Sample(540.0, y, 0.8)), straight = true,
    )

    private fun box(): ShapeItem = ShapeItem(
        ShapeKind.RECTANGLE, Pt(30.0, 30.0), Pt(510.0, 310.0),
        Rgba(20, 20, 20, 255), 4.0, Rgba(210, 210, 210, 255),
    )

    private fun state(vararg pages: Page): CanvasState =
        CanvasState(Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1000
            viewportH = 1200
            relayout()
            runAsync = {} // avoid background tasks in a source-only transaction test
        }

    private fun at(st: CanvasState, page: Int, x: Double, y: Double): Pt =
        st.contentToViewport(st.fromPageSpace(page, Pt(x, y)))

    private fun controller(st: CanvasState, history: History): InteractionController =
        InteractionController(st, history, FakeTextMeasurer(), requestRender = {}).apply {
            setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
                eraseMode = EraseMode.MARKER, baseWidth = 8.0,
            ))
        }

    @Test fun markerOnlySweepKeepsPenFilledShapesAndLockedMarkerAndUndoRestoresOrder() {
        val highlighterA = line(Tool.HIGHLIGHTER, 100.0)
        val pen = line(Tool.PEN, 100.0)
        val filled = box()
        val locked = line(Tool.HIGHLIGHTER, 100.0).also { it.locked = true }
        val highlighterB = line(Tool.HIGHLIGHTER, 170.0)
        val page = Page(600.0, 600.0, mutableListOf(highlighterA, pen, filled, locked, highlighterB))
        val adjacent = line(Tool.HIGHLIGHTER, 100.0)
        val other = Page(600.0, 600.0, mutableListOf(adjacent))
        val st = state(page, other)
        val history = History()
        val c = controller(st, history)
        val original = page.items.toList()
        val originalFill = filled.fillRgba

        val first = at(st, 0, 120.0, 100.0)
        val second = at(st, 0, 120.0, 170.0)
        c.beginErase(first.x, first.y)
        c.eraseAlong(second.x, second.y)
        c.endErase()

        val after = page.items.toList()
        assertEquals(listOf<CanvasItem>(pen, filled, locked), after)
        assertSame(originalFill, filled.fillRgba)
        assertSame(adjacent, other.items.single())
        assertTrue(history.canUndo)
        history.undo()
        assertEquals(original, page.items)
        assertFalse("one marker drag creates one history entry", history.canUndo)
        assertSame(adjacent, other.items.single())
        history.redo()
        assertEquals(after, page.items)
        assertSame(originalFill, filled.fillRgba)
    }

    @Test fun markerOnlyRemovesPdfHighlightsButNotUnderlineOrOriginalPdf() {
        val quad = listOf(TextQuad(10f, 20f, 70f, 45f, 0))
        fun markup(id: String, type: MarkupType): TextMarkup =
            TextMarkup(id, type, Rgba(200, 120, 0, 255), 0.6, quad, "PDF text", null, 0L, 0L)
        val highlight = markup("highlight", MarkupType.HIGHLIGHT)
        val underline = markup("underline", MarkupType.UNDERLINE)
        val strike = markup("strike", MarkupType.STRIKEOUT)
        val page = Page(600.0, 600.0, pdfPage = 0).apply {
            markups = listOf(highlight, underline, strike)
        }
        val st = state(page)
        val history = History()
        val c = controller(st, history)
        val pxPerPoint = st.document.dpi / 72.0
        val contact = at(st, 0, 36 * pxPerPoint, 30 * pxPerPoint)
        c.beginErase(contact.x, contact.y)
        c.endErase()

        assertEquals(listOf(underline, strike), page.markups)
        assertEquals("PDF source remains linked", 0, page.pdfPage)
        history.undo()
        assertEquals(listOf(highlight, underline, strike), page.markups)
        history.redo()
        assertEquals(listOf(underline, strike), page.markups)
    }

    @Test fun rotationAndZoomStillEraseOnlyTheSelectedPagesMarker() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (zoom in listOf(0.75, 1.0, 2.0)) {
                val neighbourMarker = line(Tool.HIGHLIGHTER, 120.0)
                val targetMarker = line(Tool.HIGHLIGHTER, 120.0)
                val pen = line(Tool.PEN, 120.0)
                val pages = arrayOf(
                    Page(600.0, 600.0, mutableListOf(neighbourMarker)),
                    Page(600.0, 600.0, mutableListOf(targetMarker, pen)),
                )
                val st = state(*pages).apply {
                    rotationDeg = rotation
                    this.zoom = zoom
                    viewingMode = ViewingMode.SINGLE
                    verticalScroll = rotation == 0 || rotation == 180
                    relayout()
                    goToPage(1)
                }
                val history = History()
                val c = controller(st, history)
                val pt = at(st, 1, 120.0, 120.0)
                c.beginErase(pt.x, pt.y)
                c.endErase()

                assertEquals("rotation=$rotation zoom=$zoom", listOf<CanvasItem>(pen), pages[1].items)
                assertSame("another page's marker is intact", neighbourMarker, pages[0].items.single())
                history.undo()
                assertEquals(listOf<CanvasItem>(targetMarker, pen), pages[1].items)
                history.redo()
                assertEquals(listOf<CanvasItem>(pen), pages[1].items)
            }
        }
    }

    @Test fun changingMarkerModeMidSweepCannotEraseOrdinaryInk() {
        val marker = line(Tool.HIGHLIGHTER, 100.0)
        val pen = line(Tool.PEN, 170.0)
        val st = state(Page(600.0, 600.0, mutableListOf(marker, pen)))
        val history = History()
        val c = controller(st, history)
        val first = at(st, 0, 120.0, 100.0)
        val second = at(st, 0, 120.0, 170.0)
        c.beginErase(first.x, first.y)
        c.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
            eraseMode = EraseMode.STROKE, baseWidth = 8.0,
        ))
        c.eraseAlong(second.x, second.y)
        c.endErase()
        assertEquals(listOf<CanvasItem>(pen), st.document.pages[0].items)
        history.undo()
        assertEquals(listOf<CanvasItem>(marker, pen), st.document.pages[0].items)
    }
}
