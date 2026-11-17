package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** NP-070: scrolling hundreds of PDF pages must not traverse or rebuild all rows each frame. */
class LongPdfViewportTest {
    private fun state(): CanvasState = CanvasState(
        Document((0 until 241).mapTo(mutableListOf()) { i ->
            Page(595.0 + (i % 3) * 30, 842.0 + (i % 5) * 70, pdfPage = i)
        }),
        FakeSurfaceFactory(),
        Palette.DEFAULT,
    ).apply {
        viewportW = 960
        viewportH = 1400
        relayout()
        establishInitialView()
    }

    /** The old linear oracle: kept separate from the optimized row lookup to catch boundary drift. */
    private fun linearBand(st: CanvasState): Pair<List<Page>, Set<Page>> {
        val pages = st.document.pages
        val rows = st.rowRanges()
        val visible = st.visibleContentRect()
        if (rows.isEmpty()) return emptyList<Page>() to emptySet()

        val first: Int
        val last: Int
        val lo: Int
        val hi: Int
        if (st.verticalScroll) {
            fun top(r: Int) = rows[r].minOf { st.pageRects[it].top }
            fun bottom(r: Int) = rows[r].maxOf { st.pageRects[it].bottom }
            var f = 0
            while (f < rows.size && bottom(f) < visible.top) f++
            var l = rows.lastIndex
            while (l >= 0 && top(l) > visible.bottom) l--
            var h = l + 1
            while (h < rows.lastIndex && top(h + 1) <= visible.bottom + visible.h) h++
            var lower = f - 1
            while (lower > 0 && bottom(lower - 1) >= visible.top - visible.h) lower--
            first = f
            last = l
            lo = lower
            hi = h
        } else {
            first = st.currentRow.coerceIn(0, rows.lastIndex)
            last = first
            lo = first - 1
            hi = first + 1
        }

        val prefetch = ArrayList<Page>()
        for (r in first..last) for (i in rows[r]) prefetch += pages[i]
        for (d in 1..max(hi - last, first - lo)) {
            for (r in intArrayOf(last + d, first - d)) {
                if (r in lo..hi && r in rows.indices) for (i in rows[r]) prefetch += pages[i]
            }
        }
        val keep = HashSet<Page>()
        for (r in max(lo - 1, 0)..min(hi + 1, rows.lastIndex)) for (i in rows[r]) keep.add(pages[i])
        return prefetch to keep
    }

    @Test fun rowListIsReusedUntilThePageCountOrViewingModeChanges() {
        val st = state()
        val single = st.rowRanges()
        assertEquals(241, single.size)
        repeat(25) { assertSame("every frame should reuse the same row grouping", single, st.rowRanges()) }

        st.viewingMode = ViewingMode.DOUBLE
        val double = st.rowRanges()
        assertNotSame(single, double)
        assertEquals(121, double.size)
        assertSame(double, st.rowRanges())

        st.document.pages.add(Page(595.0, 842.0, pdfPage = 241))
        assertEquals(121, st.rowRanges().size)
        assertNotSame("even unchanged row count needs a fresh list when pages grow", double, st.rowRanges())

        st.viewingMode = ViewingMode.COVER
        val cover = st.rowRanges()
        assertEquals(0..0, cover.first())
        assertEquals(122, cover.size)
        assertSame(cover, st.rowRanges())
    }

    @Test fun cachePrefetchAndRetentionMatchLinearOracleAcrossLargePdfAndPageGaps() {
        val st = state()
        for (mode in ViewingMode.values()) {
            for (vertical in listOf(true, false)) {
                st.viewingMode = mode
                st.verticalScroll = vertical
                st.relayout()
                st.zoom = 1.9
                for (pageIndex in listOf(0, 1, 13, 90, 119, 198, 238, 240)) {
                    st.goToPage(pageIndex)
                    for (delta in listOf(0.0, 95.0, -60.0, 370.0)) {
                        st.scrollBy(0.0, delta)
                        val (expectedPrefetch, expectedKeep) = linearBand(st)
                        val actual = st.cacheBand()
                        assertEquals("$mode vertical=$vertical page=$pageIndex delta=$delta prefetch",
                            expectedPrefetch, actual.prefetch)
                        assertEquals("$mode vertical=$vertical page=$pageIndex delta=$delta keep",
                            expectedKeep, actual.keep)
                        assertTrue("keep must cover the visible pages",
                            st.visiblePageRange()?.all { st.document.pages[it] in actual.keep } ?: true)
                    }
                }
            }
        }
    }

    @Test fun widelySeparatedPdfNavigationKeepsOnlyNearbyCachesAcrossZoomAndPaneWidths() {
        val st = state()
        val pages = st.document.pages
        val original = pages.toList()
        val visit = listOf(0, 199, 1, 198, 0, 199, 1, 198)
        val tracked = visit.distinct()
        val marks = tracked.associateWith { i ->
            Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), listOf(
                Sample(20.0, 30.0, 1.0), Sample(40.0, 50.0, 1.0),
            )).also { pages[i].items.add(it) }
        }
        st.maxCachePx = 600.0
        st.paintPageBackground = { _, _, _, _ -> }

        for (width in listOf(180, 240, 320)) {
            st.viewportW = width
            st.viewportH = 640
            st.relayout()
            for (zoom in listOf(0.25, 1.0, 4.0)) {
                st.setView(zoom, 0.0, 0.0)
                for ((step, index) in visit.withIndex()) {
                    st.goToPage(index)
                    val target = pages[index]
                    val before = st.snapshotView()
                    st.prefetchAndPrune()
                    val band = st.cacheBand()
                    val cache = st.cacheSnapshot()
                    val label = "width=$width zoom=$zoom step=$step page=$index"

                    assertTrue("$label: selected page left retention band", target in band.keep)
                    assertTrue("$label: no source page can disappear", original.indices.all {
                        pages[it] === original[it] && pages[it].pdfPage == it
                    })
                    assertTrue("$label: ink caches exceeded nearby pages", cache.inkPages <= band.keep.size)
                    assertTrue("$label: PDF caches exceeded nearby pages", cache.bgPages <= band.keep.size)
                    assertTrue("$label: caches grew with all 241 PDF pages", band.keep.size < pages.size / 2)
                    assertTrue("$label: cached bitmap surfaces exceeded configured resolution cap",
                        cache.bytes <= 2L * band.keep.size * 601L * 601L * 4L)
                    assertTrue("$label: visited page raster not ready", st.cacheForOrSchedule(target) != null)
                    assertTrue("$label: visited PDF raster not ready", st.backgroundForOrSchedule(target) != null)
                    assertEquals("$label: zoom changed during cache maintenance", before.zoom, st.zoom, 1e-9)
                    assertEquals("$label: x scroll changed during cache maintenance", before.scrollX, st.scrollX, 1e-9)
                    assertEquals("$label: y scroll changed during cache maintenance", before.scrollY, st.scrollY, 1e-9)
                    assertTrue("$label: selected page ink geometry missing", marks.getValue(index).hasGeometry)
                    for (other in tracked.filter { pages[it] !in band.keep }) {
                        assertTrue("$label: distant stroke geometry retained",
                            !marks.getValue(other).hasGeometry)
                    }
                    assertTrue("$label: navigation dirtied original note", !st.document.dirty)
                }
            }
        }
    }

    @Test fun repeatedViewportFramesKeepCommittedInkWithoutRerasterizingThePdf() {
        val st = state()
        val page = st.document.pages[150]
        st.goToPage(150)
        val pdfRenders = HashMap<Page, Int>()
        st.paintPageBackground = { p, _, _, _ -> pdfRenders[p] = (pdfRenders[p] ?: 0) + 1 }
        st.prefetchAndPrune()
        val inkRaster = st.cacheForOrSchedule(page)?.surface
        assertNotNull(inkRaster)

        val committed = Stroke(
            Tool.PEN, ToolDefaults.configFor(Tool.PEN), listOf(Sample(150.0, 150.0, 1.0)),
        )
        page.items.add(committed)
        st.appendToCache(page, committed)
        repeat(20) {
            st.scrollBy(0.0, 6.0)
            st.prefetchAndPrune()
            assertTrue("page stays in the visible/prefetch band", page in st.cacheBand().keep)
            assertSame("pen-up must not force a new whole-page PDF or ink raster",
                inkRaster, st.cacheForOrSchedule(page)?.surface)
        }
        assertEquals("PDF background should be rasterized just once", 1, pdfRenders[page] ?: 0)
        assertTrue("the committed stroke remains editable", page.items.any { it === committed })
    }
}
