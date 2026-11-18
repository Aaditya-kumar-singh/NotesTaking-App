package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Workers and main-thread publication are queued separately to reproduce late PDF results. */
class PdfAsyncCompletionTest {
    private class Harness {
        val pages = (0 until 241).mapTo(mutableListOf()) { Page(595.0, 842.0, pdfPage = it) }
        val originalPages = pages.toList()
        val workers = ArrayDeque<() -> Unit>()
        val completions = ArrayDeque<() -> Unit>()
        val paintedPdfPages = mutableListOf<Int>()
        val state = CanvasState(Document(pages), FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 960
            viewportH = 1400
            maxCachePx = 600.0
            paintPageBackground = { page, _, _, _ -> paintedPdfPages.add(page.pdfPage!!) }
            relayout()
            setView(1.0, 0.0, 0.0)
            goToPage(0)
            runAsync = { workers.addLast(it) }
            postToMain = { completions.addLast(it) }
        }

        fun finishWorkers(count: Int = workers.size) {
            repeat(count) { workers.removeFirst().invoke() }
        }

        fun publish(count: Int = completions.size) {
            repeat(count) { completions.removeFirst().invoke() }
        }

        fun finishAll() {
            finishWorkers()
            publish()
        }

        fun assertUnchanged() {
            assertFalse("cache completion must never dirty the saved document", state.document.dirty)
            assertTrue("PDF source pages must retain their identities",
                originalPages.indices.all { i ->
                    state.document.pages[i] === originalPages[i] && originalPages[i].pdfPage == i
                })
            val band = state.cacheBand().keep
            val cached = state.cacheSnapshot()
            assertTrue("cache retention band unexpectedly covers the whole document", band.size < pages.size / 2)
            assertTrue("ink cache must stay inside the current band", cached.inkPages <= band.size)
            assertTrue("PDF background cache must stay inside the current band", cached.bgPages <= band.size)
            assertTrue("live cached bitmap bytes must stay within the page-resolution budget",
                cached.bytes <= 2L * band.size * 601L * 601L * 4L)
        }
    }

    private fun stroke() = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        listOf(Sample(20.0, 30.0, 1.0), Sample(50.0, 60.0, 1.0)),
    )

    @Test fun discardedPageRastersDoNotRestoreEvictedStrokeGeometryOrChangeSelection() {
        val h = Harness()
        val oldStroke = stroke().also { h.pages[0].items.add(it) }
        val selected = stroke().also { h.pages[199].items.add(it) }
        h.state.isLiftedItem = { it === selected }
        h.state.prefetchAndPrune()
        val oldWorkers = h.workers.size
        assertTrue("PDF and ink renders must be queued before navigating", oldWorkers > 0)
        val oldBand = h.state.cacheBand().keep

        h.state.goToPage(199)
        val newBand = h.state.cacheBand().keep
        assertTrue("distant pages must have separate cache bands", oldBand.intersect(newBand).isEmpty())
        h.state.prefetchAndPrune()
        h.finishWorkers(oldWorkers)
        assertTrue("obsolete page's ink worker must build geometry before publication", oldStroke.hasGeometry)
        h.publish(oldWorkers)

        assertFalse("discarding an obsolete render must release its stroke geometry", oldStroke.hasGeometry)
        assertEquals("old ink must never return to the new viewport", 0, h.state.cacheSnapshot().inkPages)
        assertEquals("old PDF backgrounds must never return to the new viewport", 0, h.state.cacheSnapshot().bgPages)
        assertTrue("selection must remain lifted", h.state.isLiftedItem(selected))
        assertSame("selected ink must stay on the correct PDF source page", selected, h.pages[199].items.single())
        h.assertUnchanged()

        h.finishAll()
        assertTrue("new page must be rendered", 199 in h.paintedPdfPages)
        assertNotNull(h.state.cacheForOrSchedule(h.pages[199]))
        assertNotNull(h.state.backgroundForOrSchedule(h.pages[199]))
        assertTrue(h.state.isLiftedItem(selected))
        h.assertUnchanged()
    }

    @Test fun completedOldZoomRastersCannotPublishAfterZoomChanges() {
        val h = Harness()
        h.state.setView(0.25, 0.0, 0.0)
        h.state.goToPage(135)
        h.state.prefetchAndPrune()
        assertTrue(h.workers.isNotEmpty())
        h.finishWorkers()
        assertTrue("old-resolution renders must have pending publication", h.completions.isNotEmpty())

        h.state.setZoomAnchored(Pt(320.0, 320.0), 4.0)
        h.state.goToPage(135)
        val view = h.state.snapshotView()
        h.publish()
        assertEquals(0, h.state.cacheSnapshot().inkPages)
        assertEquals(0, h.state.cacheSnapshot().bgPages)
        h.assertUnchanged()

        h.state.prefetchAndPrune()
        h.finishAll()
        assertNotNull("current-resolution ink should rebuild", h.state.cacheForOrSchedule(h.pages[135]))
        assertNotNull("current-resolution PDF should rebuild", h.state.backgroundForOrSchedule(h.pages[135]))
        assertEquals(4.0, h.state.zoom, 1e-9)
        assertEquals(view.scrollX, h.state.scrollX, 1e-9)
        assertEquals(view.scrollY, h.state.scrollY, 1e-9)
        h.assertUnchanged()
    }

    @Test fun staleSharpPdfViewportIsRejectedAfterSplitPaneResize() {
        val h = Harness()
        h.state.maxCachePx = 100.0
        h.state.setView(4.0, 0.0, 0.0)
        h.state.goToPage(0)
        h.state.requestSharpViewport()
        assertEquals(1, h.workers.size)
        h.finishWorkers()

        h.state.viewportW = 240
        h.state.viewportH = 640
        h.state.relayout()
        h.state.goToPage(0)
        h.publish()
        assertNull("a pre-resize viewport must never be displayed", h.state.sharpViewportBlit())

        h.state.requestSharpViewport()
        h.finishAll()
        val current = h.state.sharpViewportBlit()
        assertNotNull("the resized pane should acquire a fresh sharp viewport", current)
        assertEquals(240, current!!.base.width)
        assertEquals(640, current.base.height)
        assertEquals(240, current.ink.width)
        assertEquals(640, current.ink.height)
        h.assertUnchanged()
    }

    @Test fun delayedSharpPdfPageCannotOverlayPageAfterNavigation() {
        val h = Harness()
        h.state.maxCachePx = 100.0
        h.state.setView(4.0, 0.0, 0.0)
        h.state.goToPage(0)
        h.state.requestSharpViewport()
        h.finishWorkers()
        assertTrue(0 in h.paintedPdfPages)

        h.state.goToPage(199)
        h.publish()
        assertNull("page zero's sharp raster must not overlay page 199", h.state.sharpViewportBlit())

        h.state.requestSharpViewport()
        h.finishAll()
        assertNotNull(h.state.sharpViewportBlit())
        assertEquals(199, h.paintedPdfPages.last())
        assertEquals(199, h.state.currentPageIndex())
        h.assertUnchanged()
    }
}
