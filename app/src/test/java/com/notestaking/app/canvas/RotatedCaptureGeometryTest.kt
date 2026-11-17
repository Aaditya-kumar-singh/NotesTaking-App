package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Geometry contract for issue #23: screenshot and selection exporters must use the same page to
 * displayed-content transforms as the canvas; A owns the actual Editor bitmap rendering.
 */
class RotatedCaptureGeometryTest {
    private fun state(turn: Int): CanvasState {
        val page = Page(200.0, 100.0, margins = PageMargins(
            left = 0.05, top = 0.20, right = 0.15, bottom = 0.40,
        ), pdfPage = 2, pdfTurn = 1)
        return CanvasState(Document(mutableListOf(page)), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            rotationDeg = turn
            viewportW = 1000
            viewportH = 800
            relayout()
        }
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, 1e-9)
        assertEquals(expected.top, actual.top, 1e-9)
        assertEquals(expected.w, actual.w, 1e-9)
        assertEquals(expected.h, actual.h, 1e-9)
    }

    @Test fun offCenterInkLandmarkMatchesDisplayedScreenshotForEveryTurn() {
        val expectations = mapOf(
            0 to Pt(30.0, 50.0),
            90 to Pt(110.0, 30.0),
            180 to Pt(210.0, 110.0),
            270 to Pt(50.0, 210.0),
        )
        for ((turn, expected) in expectations) {
            val st = state(turn)
            val pageRect = st.pageRects.single()
            val actual = st.fromPageSpace(0, Pt(20.0, 30.0))
            assertEquals("turn=$turn x", pageRect.left + expected.x, actual.x, 1e-9)
            assertEquals("turn=$turn y", pageRect.top + expected.y, actual.y, 1e-9)
            assertEquals("turn=$turn width", if (turn % 180 == 0) 240.0 else 160.0, pageRect.w, 1e-9)
            assertEquals("turn=$turn height", if (turn % 180 == 0) 160.0 else 240.0, pageRect.h, 1e-9)
            assertEquals(2, st.document.pages.single().pdfPage)
            assertEquals(1, st.document.pages.single().pdfTurn)
        }
    }

    @Test fun selectedItemBoundsRotateInsteadOfOnlyTranslating() {
        val selected = Rect(15.0, 20.0, 60.0, 35.0) // asymmetric image/ink selection
        for (turn in listOf(0, 90, 180, 270)) {
            val st = state(turn)
            val displayed = st.fromPageSpaceRect(0, selected)
            val corners = listOf(
                Pt(selected.left, selected.top), Pt(selected.right, selected.top),
                Pt(selected.left, selected.bottom), Pt(selected.right, selected.bottom),
            ).map { st.fromPageSpace(0, it) }
            assertEquals(corners.minOf { it.x }, displayed.left, 1e-9)
            assertEquals(corners.minOf { it.y }, displayed.top, 1e-9)
            assertEquals(corners.maxOf { it.x }, displayed.right, 1e-9)
            assertEquals(corners.maxOf { it.y }, displayed.bottom, 1e-9)
            assertRect(selected, st.toPageSpaceRect(0, displayed))
            if (turn == 90 || turn == 270) {
                assertEquals(selected.h, displayed.w, 1e-9)
                assertEquals(selected.w, displayed.h, 1e-9)
            }
        }
    }

    @Test fun displayCaptureClipConvertsBackToExactPageSpaceSliceAtEveryTurn() {
        val sourceSlice = Rect(-5.0, 8.0, 75.0, 50.0) // partial margin and PDF background
        for (turn in listOf(0, 90, 180, 270)) {
            val st = state(turn)
            val contentSlice = st.fromPageSpaceRect(0, sourceSlice)
            val localPdfSlice = st.toPageSpaceRect(0, contentSlice)
            assertRect(sourceSlice, localPdfSlice)
            val pageRect = st.pageRects.single()
            assertEquals(true, contentSlice.left >= pageRect.left - 1e-9)
            assertEquals(true, contentSlice.top >= pageRect.top - 1e-9)
            assertEquals(true, contentSlice.right <= pageRect.right + 1e-9)
            assertEquals(true, contentSlice.bottom <= pageRect.bottom + 1e-9)
            assertFalse(st.document.dirty)
        }
    }

    @Test fun zoomedScrolledSelectionAndPdfCaptureUseIdenticalPageCoordinates() {
        val selected = Rect(15.0, 20.0, 60.0, 35.0)
        val pdfSlice = Rect(-5.0, 8.0, 75.0, 50.0)
        for (turn in listOf(0, 90, 180, 270)) {
            val st = state(turn)
            val displaySelection = st.fromPageSpaceRect(0, selected)
            val displayPdfSlice = st.fromPageSpaceRect(0, pdfSlice)
            val pageRect = st.pageRects.single()
            val sourcePage = st.document.pages.single()
            for (factor in listOf(0.25, 1.0, 4.0)) {
                st.zoom = factor
                st.scrollX = 37.0
                st.scrollY = 53.0

                val viewportCorners = listOf(
                    Pt(displaySelection.left, displaySelection.top),
                    Pt(displaySelection.right, displaySelection.bottom),
                ).map(st::contentToViewport)
                val restoredContent = Rect.fromPoints(
                    st.viewportToContent(viewportCorners[0]),
                    st.viewportToContent(viewportCorners[1]),
                )
                assertRect(displaySelection, restoredContent)
                assertRect(selected, st.toPageSpaceRect(0, restoredContent))

                val pdfViewportCorners = listOf(
                    Pt(displayPdfSlice.left, displayPdfSlice.top),
                    Pt(displayPdfSlice.right, displayPdfSlice.bottom),
                ).map(st::contentToViewport)
                val captureContent = Rect.fromPoints(
                    st.viewportToContent(pdfViewportCorners[0]),
                    st.viewportToContent(pdfViewportCorners[1]),
                )
                assertRect(displayPdfSlice, captureContent)
                assertRect(pdfSlice, st.toPageSpaceRect(0, captureContent))
                assertEquals(true, captureContent.left >= pageRect.left - 1e-7)
                assertEquals(true, captureContent.top >= pageRect.top - 1e-7)
                assertEquals(true, captureContent.right <= pageRect.right + 1e-7)
                assertEquals(true, captureContent.bottom <= pageRect.bottom + 1e-7)
                assertEquals(2, sourcePage.pdfPage)
                assertEquals(1, sourcePage.pdfTurn)
                assertFalse(st.document.dirty)
            }
            // A screenshot takes a content-space rectangle; viewport zoom must not alter it.
            assertRect(displaySelection, st.fromPageSpaceRect(0, selected))
            assertRect(displayPdfSlice, st.fromPageSpaceRect(0, pdfSlice))
        }
    }
}
