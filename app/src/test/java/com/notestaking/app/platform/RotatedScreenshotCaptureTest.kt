package com.notestaking.app.platform

import com.notestaking.app.canvas.CanvasState
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

/** Regression geometry for Editor's rotation-aware screenshot and selection paths (issue #23). */
class RotatedScreenshotCaptureTest {
    private fun canvas(turn: Int): CanvasState {
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

    @Test fun selectionBoundsAndPdfSliceMatchDisplayAtEveryRotation() {
        val selection = Rect(15.0, 20.0, 60.0, 35.0)
        val sourcePdfSlice = Rect(-5.0, 8.0, 75.0, 50.0)
        val landmarks = mapOf(
            0 to Pt(30.0, 50.0),
            90 to Pt(110.0, 30.0),
            180 to Pt(210.0, 110.0),
            270 to Pt(50.0, 210.0),
        )
        for ((turn, expected) in landmarks) {
            val st = canvas(turn)
            val pr = st.pageRects.single()
            val displayedPoint = st.fromPageSpace(0, Pt(20.0, 30.0))
            assertEquals("turn=$turn x", pr.left + expected.x, displayedPoint.x, 1e-9)
            assertEquals("turn=$turn y", pr.top + expected.y, displayedPoint.y, 1e-9)

            val displayedSelection = st.fromPageSpaceRect(0, selection)
            val recoveredSelection = st.toPageSpaceRect(0, displayedSelection)
            assertEquals(selection.left, recoveredSelection.left, 1e-9)
            assertEquals(selection.top, recoveredSelection.top, 1e-9)
            assertEquals(selection.w, recoveredSelection.w, 1e-9)
            assertEquals(selection.h, recoveredSelection.h, 1e-9)
            if (turn == 90 || turn == 270) {
                assertEquals(selection.h, displayedSelection.w, 1e-9)
                assertEquals(selection.w, displayedSelection.h, 1e-9)
            }

            // Editor receives content-space capture bounds, but PDF painters need page-space slices.
            val requestedCapture = st.fromPageSpaceRect(0, sourcePdfSlice)
            val pdfClip = st.toPageSpaceRect(0, requestedCapture)
            assertEquals(sourcePdfSlice.left, pdfClip.left, 1e-9)
            assertEquals(sourcePdfSlice.top, pdfClip.top, 1e-9)
            assertEquals(sourcePdfSlice.w, pdfClip.w, 1e-9)
            assertEquals(sourcePdfSlice.h, pdfClip.h, 1e-9)
            assertEquals(2, st.document.pages.single().pdfPage)
            assertEquals(1, st.document.pages.single().pdfTurn)
            assertFalse(st.document.dirty)
        }
    }
}
