package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.ImageCrop
import com.notestaking.app.core.model.ImageData
import com.notestaking.app.core.model.ImageItem
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.PagePattern
import com.notestaking.app.core.model.PageStyle
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-068: one page coordinate frame is used by PDF/template overlays, selection and erasing. */
class PageMarginOverlayGeometryTest {
    private fun stroke(y: Double) = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        listOf(Sample(40.0, y, 0.8), Sample(300.0, y, 0.8)),
        straight = true,
    )

    private fun setup(rotation: Int, zoom: Double, vertical: Boolean = true): CanvasState {
        val sourceInk = stroke(120.0)
        val targetInk = stroke(120.0)
        val source = Page(500.0, 700.0, mutableListOf(sourceInk), pdfPage = 8)
        val target = Page(
            500.0, 700.0, mutableListOf(targetInk), pdfPage = 9,
            style = PageStyle(template = PagePattern.GRID.id),
            margins = PageMargins(left = 0.20, top = 0.10, right = 0.05),
        )
        val doc = Document(mutableListOf(source, target), dpi = 72).apply {
            margins = PageMargins(bottom = 0.15, right = 0.10)
            style = PageStyle(template = PagePattern.LINES.id)
        }
        return CanvasState(doc, FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1100
            viewportH = 1400
            rotationDeg = rotation
            this.zoom = zoom
            verticalScroll = vertical
            viewingMode = ViewingMode.SINGLE
            relayout()
            goToPage(1)
            runAsync = {}
        }
    }

    private fun assertPt(expected: Pt, actual: Pt) {
        assertEquals(expected.x, actual.x, 1e-6)
        assertEquals(expected.y, actual.y, 1e-6)
    }

    @Test fun allRotatedPageFootprintsAndTemplateRegionsKeepTheirMargins() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val st = setup(rotation, zoom = 1.25)
            val page = st.document.pages[1]
            val cover = st.footprint(page)
            assertEquals(-100.0, cover.left, 1e-9)
            assertEquals(-70.0, cover.top, 1e-9)
            assertEquals(625.0, cover.w, 1e-9)
            assertEquals(875.0, cover.h, 1e-9)
            val rect = st.pageRects[1]
            assertEquals(st.displayW(page), rect.w, 1e-9)
            assertEquals(st.displayH(page), rect.h, 1e-9)
            assertEquals(if (rotation % 180 == 0) cover.w else cover.h, rect.w, 1e-9)
            assertEquals(if (rotation % 180 == 0) cover.h else cover.w, rect.h, 1e-9)
            assertEquals(PagePattern.GRID.id, st.effectiveTemplate(page))
            assertEquals(PagePattern.LINES.id, st.effectiveTemplate(st.document.pages[0]))
            assertEquals(9, page.pdfPage)

            val requested = ArrayList<Rect>()
            st.paintPageBackground = { p, _, _, region ->
                if (p === page) requested += region
            }
            val raster = st.backgroundFor(page)
            assertEquals(cover, raster!!.cover)
            assertEquals(listOf(cover), requested)
            assertPt(st.fromPageSpace(1, Pt(0.0, 0.0)),
                st.fromPageSpace(1, st.displayToPage(page, st.pageToDisplay(page, Pt(0.0, 0.0)))))
            val footprintDisplay = st.fromPageSpaceRect(1, cover)
            assertEquals(rect.left, footprintDisplay.left, 1e-6)
            assertEquals(rect.top, footprintDisplay.top, 1e-6)
            assertEquals(rect.w, footprintDisplay.w, 1e-6)
            assertEquals(rect.h, footprintDisplay.h, 1e-6)
        }
    }

    @Test fun changingPageMarginsRebuildsPageAndTemplateFootprintWithoutMovingInk() {
        val st = setup(rotation = 90, zoom = 1.0)
        val page = st.document.pages[1]
        val original = (page.items.single() as Stroke).samples.toList()
        st.paintPageBackground = { _, _, _, _ -> }
        val before = st.backgroundFor(page)!!
        val previousCenter = st.fromPageSpace(1, Pt(170.0, 120.0))
        val oldPdfIndex = page.pdfPage

        page.margins = PageMargins(left = 0.05, top = 0.30, right = 0.10, bottom = 0.20)
        st.relayout()
        st.invalidatePageGeometry()
        val after = st.backgroundFor(page)!!
        assertEquals(st.footprint(page), after.cover)
        assertTrue(after.cover != before.cover)
        assertEquals(oldPdfIndex, page.pdfPage)
        assertEquals(PagePattern.GRID.id, st.effectiveTemplate(page))
        assertEquals(original, (page.items.single() as Stroke).samples)
        val newCenter = st.fromPageSpace(1, Pt(170.0, 120.0))
        assertPt(Pt(170.0, 120.0), st.toPageSpace(1, newCenter))
        assertTrue("paper grew/shrank, ink stays at same page coordinates", previousCenter != newCenter)
    }

    @Test fun lassoAndEraseAgreeWithPageLocalInkAcrossRotationMarginsAndZoom() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (zoom in listOf(0.75, 1.0, 2.0)) {
                val st = setup(rotation, zoom, vertical = false)
                val page = st.document.pages[1]
                val ink = page.items.single()
                val other = st.document.pages[0].items.single()
                val history = History()
                val ctrl = InteractionController(st, history, FakeTextMeasurer(), requestRender = {}).apply {
                    setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(eraseMode = EraseMode.STROKE))
                }
                val lasso = listOf(
                    Pt(80.0, 90.0), Pt(190.0, 90.0), Pt(190.0, 150.0), Pt(80.0, 150.0),
                ).map { st.fromPageSpace(1, it) }
                val selected = SelectionMath.lassoMembers(
                    st.document.pages, st.pageRects, lasso,
                ) { index, p -> st.toPageSpace(index, p) }
                assertEquals("rotation=$rotation zoom=$zoom", 1, selected.size)
                assertSame(ink, selected.single().item)
                assertEquals(1, selected.single().pageIndex)

                val hit = st.contentToViewport(st.fromPageSpace(1, Pt(150.0, 120.0)))
                ctrl.beginErase(hit.x, hit.y)
                ctrl.endErase()
                assertTrue(page.items.isEmpty())
                assertSame(other, st.document.pages[0].items.single())
                assertEquals(9, page.pdfPage)
                assertTrue(history.canUndo)
                history.undo()
                assertSame(ink, page.items.single())
                assertFalse(history.canUndo)
                history.redo()
                assertTrue(page.items.isEmpty())
            }
        }
    }

    @Test fun imageCropAndOverlayCenterStayPageLocalAtEveryViewRotation() {
        val fractions = Rect.ltrb(0.25, 0.25, 0.75, 0.75)
        for (rotation in listOf(0, 90, 180, 270)) {
            val st = setup(rotation, zoom = 1.75)
            val picture = ImageItem(
                ImageData(File("not-opened-by-test.png"), 100, 200),
                Rect(100.0, 180.0, 80.0, 120.0),
                orientation = 90,
                angle = Math.PI / 6.0,
            )
            val before = picture.rect
            val sourcePixels = ImageCrop.sourceRect(picture, 100, 200, fractions)
            val clipped = ImageCrop.croppedRect(picture, fractions)
            val expectedCenter = st.fromPageSpace(1, clipped.center)
            val onScreen = st.contentToViewport(expectedCenter)
            val restored = st.toPageSpace(1, st.viewportToContent(onScreen))
            assertPt(clipped.center, restored)
            assertEquals(Rect.ltrb(25.0, 50.0, 75.0, 150.0), sourcePixels)
            assertEquals(before, picture.rect) // a crop preview never modifies the saved picture
            assertEquals(9, st.document.pages[1].pdfPage)
        }
    }
}
