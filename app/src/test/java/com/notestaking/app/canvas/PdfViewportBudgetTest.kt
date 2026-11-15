package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-056: PDF source pages are rasterized on demand, not retained as 200 full-page bitmaps. */
class PdfViewportBudgetTest {
    private fun pages(count: Int): Document = Document(
        (0 until count).mapTo(mutableListOf()) { Page(595.0, 842.0, pdfPage = it) },
    )

    private fun state(doc: Document, factory: FakeSurfaceFactory) =
        CanvasState(doc, factory, Palette.DEFAULT).apply {
            viewportW = 960
            viewportH = 1440
            maxCachePx = 1200.0
            paintPageBackground = { _, _, _, _ -> }
            relayout()
            establishInitialView()
        }

    @Test fun latePdfRendersCannotRepopulatePagesPrunedAfterJumpingAcrossTwoHundredPages() {
        val doc = pages(200)
        val factory = FakeSurfaceFactory()
        val queued = ArrayDeque<() -> Unit>()
        val st = state(doc, factory).apply {
            runAsync = { queued.addLast(it) }
        }

        st.goToPage(1)
        val oldBand = st.cacheBand().keep
        st.prefetchAndPrune()
        assertTrue("the starting PDF pages were queued", queued.isNotEmpty())
        val oldJobs = queued.size

        st.goToPage(190)
        val currentBand = st.cacheBand().keep
        assertTrue("far apart page bands must not overlap", oldBand.intersect(currentBand).isEmpty())
        st.prefetchAndPrune()

        // The queued old jobs finish late, after their pages have left the keep band.
        // Their completed rasters must not re-enter the live caches.
        repeat(oldJobs) { queued.removeFirst().invoke() }
        assertEquals("obsolete background renders must not return after the jump", 0, st.cacheSnapshot().bgPages)
        assertEquals("obsolete ink renders must not return after the jump", 0, st.cacheSnapshot().inkPages)
        while (queued.isNotEmpty()) queued.removeFirst().invoke()
        val cached = st.cacheSnapshot()
        assertTrue("a 200-page document must retain only nearby PDF backgrounds",
            cached.bgPages <= currentBand.size)
        assertTrue("ink cache must obey the same scrolling budget",
            cached.inkPages <= currentBand.size)
        assertTrue("the final viewport has cached PDF backgrounds", cached.bgPages > 0)
        assertTrue("the final viewport has cached ink", cached.inkPages > 0)
    }

    @Test fun deepZoomRedrawsOnlyVisiblePdfSliceAtScreenResolution() {
        val doc = pages(200)
        val factory = FakeSurfaceFactory()
        val sourceCalls = mutableListOf<Triple<Page, Double, Rect>>()
        val st = state(doc, factory).apply {
            maxCachePx = 600.0
            paintPageBackground = { page, _, resolution, region ->
                sourceCalls.add(Triple(page, resolution, region))
            }
            zoom = 4.0
            goToPage(135)
        }
        val page = doc.pages[135]
        assertTrue("the page cache must be capped at deep zoom", st.isPastResolutionCap())

        // A soft background is bounded by the page-cache cap.
        val cached = st.backgroundFor(page)!!
        assertTrue(cached.surface.width <= 601)
        assertTrue(cached.surface.height <= 601)
        sourceCalls.clear()

        st.requestSharpViewport()
        val sharp = st.sharpViewportBlit()
        assertNotNull("the PDF has a sharp on-demand viewport layer", sharp)
        assertEquals(st.viewportW, sharp!!.base.width)
        assertEquals(st.viewportH, sharp.base.height)
        assertEquals(st.viewportW, sharp.ink.width)
        assertEquals(st.viewportH, sharp.ink.height)
        assertFalse("a deep zoom must re-render from the source rather than stretching the cache", sourceCalls.isEmpty())
        assertTrue("the PDF was re-rendered at the actual view resolution",
            sourceCalls.all { it.second == st.zoom * st.renderScale })
        assertTrue("only visible page slices should be painted, never entire high-zoom PDF pages",
            sourceCalls.all { (sourcePage, _, region) ->
                region.w > 0 && region.h > 0 &&
                    region.w < sourcePage.width && region.h < sourcePage.height
            })
    }
}
