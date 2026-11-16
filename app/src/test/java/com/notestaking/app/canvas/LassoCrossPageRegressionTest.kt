package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.history.ResizeItem
import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.ShapeHandle
import com.notestaking.app.core.model.ShapeItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-051: page-local lasso membership, selection restyle, and independent cross-page copies. */
class LassoCrossPageRegressionTest {
    private val ink = Rgba(22, 44, 66, 255)

    private fun dot(x: Double, y: Double) =
        Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), listOf(Sample(x, y, 0.8)))

    private fun box(x: Double, y: Double) = ShapeItem(
        ShapeKind.RECTANGLE, Pt(x, y), Pt(x + 28.0, y + 24.0), ink, 3.0,
    )

    private fun state(pages: List<Page>, rotation: Int = 0, zoom: Double = 1.0): CanvasState =
        CanvasState(Document(pages.toMutableList()), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1100
            viewportH = 1400
            verticalScroll = true
            viewingMode = ViewingMode.SINGLE
            rotationDeg = rotation
            this.zoom = zoom
            relayout()
            runAsync = {} // no Android background work in a source-only unit test
        }

    private fun controller(state: CanvasState, history: History) =
        InteractionController(state, history, FakeTextMeasurer(), requestRender = {})

    private fun ring(cx: Double, cy: Double) = listOf(
        Pt(cx - 12.0, cy - 12.0),
        Pt(cx + 12.0, cy - 12.0),
        Pt(cx + 12.0, cy + 12.0),
        Pt(cx - 12.0, cy + 12.0),
    )

    @Test fun pageSpaceLassoSurvivesViewRotationMarginsAndPageReordering() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (zoom in listOf(0.5, 1.0, 2.0)) {
                val other = dot(132.0, 145.0)
                val wanted = dot(132.0, 145.0)
                val locked = dot(132.0, 145.0).apply { this.locked = true }
                val first = Page(600.0, 600.0, mutableListOf(other), pdfPage = 0)
                val second = Page(600.0, 600.0, mutableListOf(wanted, locked), pdfPage = 1).apply {
                    margins = PageMargins(left = 0.15, right = 0.1, top = 0.05)
                }
                val st = state(listOf(first, second), rotation, zoom)
                fun pick(page: Int): List<Selected> {
                    val path = ring(132.0, 145.0).map { st.fromPageSpace(page, it) }
                    return SelectionMath.lassoMembers(st.document.pages, st.pageRects, path) { i, p ->
                        st.toPageSpace(i, p)
                    }
                }
                assertEquals("rotation=$rotation zoom=$zoom", listOf(Selected(1, wanted)), pick(1))
                // A relayout after reordering must not reuse the old page-space offsets.
                st.document.pages.reverse()
                st.relayout()
                assertEquals("reordered rotation=$rotation zoom=$zoom",
                    listOf(Selected(0, wanted)), pick(0))
                assertSame(locked, st.document.pages[0].items.last())
                assertEquals(1, st.document.pages[0].pdfPage)
                assertEquals(0, st.document.pages[1].pdfPage)
            }
        }
    }

    @Test fun restylingLassoMembersPreviewsWithoutPrematureHistoryAndUndoesOnce() {
        val first = box(50.0, 70.0)
        val second = dot(90.0, 110.0)
        val locked = box(120.0, 70.0).apply { this.locked = true }
        val page = Page(600.0, 600.0, mutableListOf(first, second, locked))
        val st = state(listOf(page))
        val history = History()
        val ctrl = controller(st, history)
        ctrl.selectPlaced(0, listOf(first, second))
        val oldColor = first.strokeRgba
        val oldPenColor = second.config.rgba
        val color = Rgba(180, 20, 70, 255)

        ctrl.restyleSelection(color, null, preview = true)
        assertEquals(color, first.strokeRgba)
        assertEquals(color, second.config.rgba)
        assertFalse("preview is not an undo transaction", history.canUndo)
        ctrl.restyleSelection(null, null, preview = false)
        assertTrue(history.canUndo)
        history.undo()
        assertEquals(oldColor, first.strokeRgba)
        assertEquals(oldPenColor, second.config.rgba)
        assertFalse("two selected objects restyle in one step", history.canUndo)
        assertEquals(ink, locked.strokeRgba)
        history.redo()
        assertEquals(color, first.strokeRgba)
        assertEquals(color, second.config.rgba)
        assertSame(locked, page.items.last())
    }

    @Test fun resizeGeometryUsesPageLocalCoordinatesAndOneUndoAcrossRotations() {
        for (turn in listOf(0, 90, 180, 270)) {
            val selected = box(60.0, 75.0)
            val untouched = box(310.0, 320.0).apply { this.locked = true }
            val neighbour = box(60.0, 75.0)
            val first = Page(600.0, 600.0, mutableListOf(neighbour), pdfPage = 0)
            val target = Page(600.0, 600.0, mutableListOf(selected, untouched), pdfPage = 1).apply {
                margins = PageMargins(left = 0.2, top = 0.1)
            }
            val st = state(listOf(first, target), rotation = turn)
            val original = selected.geometry()
            val newPointer = st.fromPageSpace(1, Pt(220.0, 235.0))
            val pagePoint = st.toPageSpace(1, newPointer)
            val (start, end) = ResizeMath.resizeClosedShape(
                selected.start, selected.end, HandleId.BR, pagePoint,
            )
            selected.setGeometry(ShapeHandle(start, end))
            val history = History()
            history.push(ResizeItem(selected, original, selected.geometry()))
            assertEquals(Pt(60.0, 75.0), selected.start)
            assertEquals(Pt(220.0, 235.0), selected.end)
            assertSame(untouched, target.items.last())
            assertSame(neighbour, first.items.single())
            history.undo()
            assertEquals(original, selected.geometry())
            assertFalse(history.canUndo)
            history.redo()
            assertEquals(Pt(220.0, 235.0), selected.end)
            assertEquals(0, first.pdfPage)
            assertEquals(1, target.pdfPage)
        }
    }

    @Test fun copyingSelectedObjectsToAnotherPageKeepsOriginalsAndUndoRedoClones() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val sourceA = box(60.0, 75.0)
            val sourceB = box(115.0, 130.0)
            val locked = box(85.0, 110.0).apply { this.locked = true }
            val targetBottom = dot(300.0, 320.0)
            val first = Page(600.0, 600.0, mutableListOf(sourceA, locked, sourceB), pdfPage = 0)
            val target = Page(600.0, 600.0, mutableListOf(targetBottom), pdfPage = 1).apply {
                margins = PageMargins(top = 0.10, left = 0.10)
            }
            val st = state(listOf(first, target), rotation, zoom = 1.5)
            val history = History()
            val ctrl = controller(st, history)
            try {
                ctrl.selectPlaced(0, listOf(sourceA, sourceB)) // source lasso members, not locked ink
                ctrl.copySelection()
                assertFalse("copying must not dirty the note", st.document.dirty)
                assertEquals(listOf<CanvasItem>(sourceA, locked, sourceB), first.items)
                val spot = st.fromPageSpace(1, Pt(220.0, 240.0))
                ctrl.pasteItemsAt(spot)
                val pasted = target.items.drop(1)
                assertEquals(2, pasted.size)
                assertNotSame(sourceA, pasted[0])
                assertNotSame(sourceB, pasted[1])
                assertSame(targetBottom, target.items.first())
                assertEquals(listOf<CanvasItem>(sourceA, locked, sourceB), first.items)
                assertEquals(0, first.pdfPage)
                assertEquals(1, target.pdfPage)
                assertTrue(history.canUndo)
                history.undo()
                assertEquals(listOf<CanvasItem>(targetBottom), target.items)
                assertEquals(listOf<CanvasItem>(sourceA, locked, sourceB), first.items)
                assertFalse(history.canUndo)
                history.redo()
                assertEquals(3, target.items.size)
                assertSame(pasted[0], target.items[1])
                assertSame(pasted[1], target.items[2])
            } finally {
                ItemClipboard.clip = null
            }
        }
    }

    @Test fun copyingAcrossSourcePagesPreservesRelativeDisplaySpacing() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val firstItem = box(30.0, 40.0)
            val secondItem = box(60.0, 70.0)
            val left = Page(600.0, 600.0, mutableListOf(firstItem), pdfPage = 0)
            val right = Page(600.0, 600.0, mutableListOf(secondItem), pdfPage = 1).apply {
                margins = PageMargins(left = 0.2, top = 0.05)
            }
            val destination = Page(600.0, 600.0, pdfPage = 2)
            val st = state(listOf(left, right, destination), rotation, zoom = 1.25)
            val history = History()
            val ctrl = controller(st, history)
            try {
                val p0 = st.fromPageSpace(0, firstItem.bounds().center)
                val p1 = st.fromPageSpace(1, secondItem.bounds().center)
                val expected = st.vectorToPageSpace(p1 - p0)
                ctrl.selectAll()
                ctrl.copySelection()
                val clip = ItemClipboard.clip!!
                assertEquals(2, clip.items.size)
                val normalized = clip.items[1].bounds().center - clip.items[0].bounds().center
                assertEquals("rotation=$rotation delta x", expected.x, normalized.x, 1e-6)
                assertEquals("rotation=$rotation delta y", expected.y, normalized.y, 1e-6)
                // Pasting a multi-page selection must create new independent objects on one page.
                ctrl.pasteItemsAt(st.fromPageSpace(2, Pt(100.0, 130.0)))
                assertEquals(2, destination.items.size)
                assertNotSame(firstItem, destination.items[0])
                assertNotSame(secondItem, destination.items[1])
                assertEquals(expected.x,
                    destination.items[1].bounds().center.x - destination.items[0].bounds().center.x, 1e-6)
                assertEquals(expected.y,
                    destination.items[1].bounds().center.y - destination.items[0].bounds().center.y, 1e-6)
                assertSame(firstItem, left.items.single())
                assertSame(secondItem, right.items.single())
                history.undo()
                assertTrue(destination.items.isEmpty())
                assertSame(firstItem, left.items.single())
                assertSame(secondItem, right.items.single())
                history.redo()
                assertEquals(2, destination.items.size)
            } finally {
                ItemClipboard.clip = null
            }
        }
    }
}
