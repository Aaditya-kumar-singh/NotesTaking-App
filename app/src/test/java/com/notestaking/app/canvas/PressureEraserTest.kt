package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.Stroke
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

/** NP-043: only valid opt-in stylus pressure changes eraser reach and cursor size. */
class PressureEraserTest {
    private fun line(y: Double, tool: Tool = Tool.PEN): Stroke = Stroke(
        tool, ToolDefaults.configFor(tool),
        listOf(Sample(20.0, y, 0.8), Sample(540.0, y, 0.8)), straight = true,
    )

    private fun fixture(
        pages: List<Page>,
        pressure: Boolean = true,
        mode: EraseMode = EraseMode.STROKE,
        zoom: Double = 1.0,
        rotation: Int = 0,
        scale: Boolean = true,
    ): Triple<CanvasState, InteractionController, History> {
        val state = CanvasState(Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1000
            viewportH = 1300
            verticalScroll = false
            viewingMode = ViewingMode.SINGLE
            rotationDeg = rotation
            this.zoom = zoom
            relayout()
            goToPage(pages.lastIndex)
            runAsync = {} // keep PDF/Android background work outside this source-only regression
        }
        val history = History()
        val controller = InteractionController(state, history, FakeTextMeasurer(), requestRender = {}).apply {
            setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
                baseWidth = 16.0, pressureEnabled = pressure, pressureMinFactor = 0.25,
                eraseMode = mode, scale = scale,
            ))
        }
        return Triple(state, controller, history)
    }

    private fun screen(state: CanvasState, page: Int, y: Double): Pt =
        state.contentToViewport(state.fromPageSpace(page, Pt(120.0, y)))

    @Test fun lightPressureTouchesNearbyInkButNotADistantStrokeWhileHeavyPressureReachesBoth() {
        val near = line(100.0)
        val farther = line(114.0)
        val page = Page(600.0, 600.0, mutableListOf(near, farther))
        val (state, controller, history) = fixture(listOf(page))
        val contact = screen(state, 0, 100.0)

        assertEquals(16.0, controller.eraserRadius(), 1e-9)
        controller.beginErase(contact.x, contact.y, stylusPressure = 0.2)
        assertEquals(6.4, controller.eraserRadius(), 1e-9)
        assertEquals(6.4, controller.eraserCursorRadiusPx(), 1e-9)
        controller.endErase()
        assertSame(farther, page.items.single())
        history.undo()
        assertEquals(listOf<CanvasItem>(near, farther), page.items)
        assertFalse(history.canUndo)

        controller.beginErase(contact.x, contact.y, stylusPressure = 1.0)
        assertEquals(16.0, controller.eraserRadius(), 1e-9)
        controller.endErase()
        assertTrue(page.items.isEmpty())
        history.undo()
        assertEquals(listOf<CanvasItem>(near, farther), page.items)
        history.redo()
        assertTrue(page.items.isEmpty())
    }

    @Test fun stationaryStylusPressureChangeUpdatesTheActualEraseReachInOneUndoStep() {
        val near = line(100.0)
        val farther = line(114.0)
        val page = Page(600.0, 600.0, mutableListOf(near, farther))
        val (state, controller, history) = fixture(listOf(page))
        val contact = screen(state, 0, 100.0)
        controller.beginErase(contact.x, contact.y, stylusPressure = 0.2)
        assertSame(farther, page.items.single())
        controller.eraseAlong(contact.x, contact.y, stylusPressure = 1.0)
        assertEquals(16.0, controller.eraserCursorRadiusPx(), 1e-9)
        assertTrue("pressure changed without moving; the larger hit must still take the second line",
            page.items.isEmpty())
        controller.endErase()
        history.undo()
        assertEquals(listOf<CanvasItem>(near, farther), page.items)
        assertFalse("the pressure change stays in the same undo transaction", history.canUndo)
        history.redo()
        assertTrue(page.items.isEmpty())
    }

    @Test fun pressureDisabledOrAbsentPreservesTheSetSizeAtAnyZoom() {
        for (enabled in listOf(false, true)) for (scale in listOf(false, true)) {
            for (zoom in listOf(0.5, 1.0, 2.0)) {
                val target = line(100.0)
                val (state, controller, history) = fixture(
                    listOf(Page(600.0, 600.0, mutableListOf(target))),
                    pressure = enabled, zoom = zoom, scale = scale,
                )
                val contact = screen(state, 0, 100.0)
                val base = if (scale) 16.0 else 16.0 / zoom
                // A mouse or finger never passes stylus pressure; an unavailable stylus sample is also null.
                controller.beginErase(contact.x, contact.y)
                assertEquals(base, controller.eraserRadius(), 1e-9)
                assertEquals(base * zoom, controller.eraserCursorRadiusPx(), 1e-9)
                controller.eraseAlong(contact.x + 1.0, contact.y, stylusPressure = Double.NaN)
                assertEquals(base, controller.eraserRadius(), 1e-9)
                controller.endErase()
                assertTrue(history.canUndo)
                history.undo()
                assertSame(target, state.document.pages.single().items.single())
            }
        }
    }

    @Test fun pressureEnabledDuringAGestureDoesNotChangeSelectedEraserTypeOrUndo() {
        val marker = line(100.0, Tool.HIGHLIGHTER)
        val pen = line(130.0)
        val page = Page(600.0, 600.0, mutableListOf(marker, pen))
        val (state, controller, history) = fixture(listOf(page), mode = EraseMode.MARKER)
        val first = screen(state, 0, 100.0)
        val second = screen(state, 0, 130.0)
        controller.beginErase(first.x, first.y, stylusPressure = 0.25)
        controller.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
            baseWidth = 16.0, pressureEnabled = false, eraseMode = EraseMode.STROKE,
        ))
        controller.eraseAlong(second.x, second.y, stylusPressure = 1.0)
        assertEquals(16.0, controller.eraserRadius(), 1e-9)
        controller.endErase()
        assertSame(pen, page.items.single())
        history.undo()
        assertEquals(listOf<CanvasItem>(marker, pen), page.items)
        assertFalse(history.canUndo)
    }

    @Test fun pressureRadiusStaysPageLocalThroughRotationsZoomAndPageMargins() {
        for (turn in listOf(0, 90, 180, 270)) for (zoom in listOf(0.5, 1.0, 2.0)) {
            val neighbour = line(100.0)
            val near = line(100.0)
            val far = line(114.0)
            val pages = listOf(
                Page(600.0, 600.0, mutableListOf(neighbour)),
                Page(600.0, 600.0, mutableListOf(near, far)).apply {
                    margins = PageMargins(left = 0.1, right = 0.1, top = 0.05)
                },
            )
            val (state, controller, history) = fixture(pages, zoom = zoom, rotation = turn)
            val pt = screen(state, 1, 100.0)
            controller.beginErase(pt.x, pt.y, stylusPressure = 0.2)
            assertEquals("turn=$turn zoom=$zoom", 6.4 * zoom, controller.eraserCursorRadiusPx(), 1e-6)
            controller.endErase()
            assertSame(far, pages[1].items.single())
            assertSame(neighbour, pages[0].items.single())
            history.undo()
            assertEquals(listOf<CanvasItem>(near, far), pages[1].items)
            assertSame(neighbour, pages[0].items.single())
            history.redo()
            assertSame(far, pages[1].items.single())
        }
    }
}
