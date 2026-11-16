package com.notestaking.app.core.model

import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.pdf.TextQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JpegExportPlanTest {
    private fun doc(vararg pages: Page) = Document(pages.toMutableList())

    private fun rejects(block: () -> Unit) {
        assertTrue(
            "invalid export request must fail before writing any files",
            runCatching(block).exceptionOrNull() is IllegalArgumentException,
        )
    }

    @Test fun onePageUsesOriginalFootprintAndDeterministicJpegName() {
        val page = Page(400.0, 800.0, margins = PageMargins(left = 0.1, bottom = 0.125))
        val note = doc(page).also { it.displayName = "Study Notes.xnote" }
        val result = JpegExportPlanner.plan(note)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals(JpegExportPlanner.DEFAULT_QUALITY, result.quality)
        assertEquals(1, result.pages.size)
        val item = result.pages.single()
        assertEquals("Study Notes-p0001-o0001.jpg", item.filename)
        assertEquals(0, item.sourceIndex)
        assertEquals(0, item.outputIndex)
        assertEquals(page.uid, item.pageUid)
        assertEquals(Rect(-40.0, 0.0, 440.0, 900.0), item.footprint)
        assertEquals(440, item.widthPx)
        assertEquals(900, item.heightPx)
        assertEquals(ExportContent(), result.content)
        assertTrue(item.paper.paintBlankPaper)
        assertEquals(page.insets(note), page.insetsForExport(note))
        assertFalse(note.dirty)
    }

    @Test fun reorderedAndRepeatedPagesRetainSourceIdentityAndNeverCollideOnFilenames() {
        val a = Page(400.0, 700.0, pdfPage = 0)
        val b = Page(350.0, 550.0)
        val c = Page(700.0, 300.0, pdfPage = 7, pdfTurn = 3)
        val note = doc(a, b, c)
        val chosen = listOf(2, 0, 2, 1, 0)
        val first = JpegExportPlanner.plan(note, chosen, stem = "Lecture/Unit?:A")
        val second = JpegExportPlanner.plan(note, chosen, stem = "Lecture/Unit?:A")
        assertEquals(first, second)
        assertEquals(chosen, first.pages.map { it.sourceIndex })
        assertEquals(listOf(c.uid, a.uid, c.uid, b.uid, a.uid), first.pages.map { it.pageUid })
        assertEquals(listOf(0, 1, 2, 3, 4), first.pages.map { it.outputIndex })
        assertEquals(listOf(c.pdfKey, a.pdfKey, c.pdfKey, b.pdfKey, a.pdfKey),
            first.pages.map { it.pdfKey })
        assertEquals(5, first.pages.map { it.filename }.toSet().size)
        assertTrue(first.pages.all { it.filename.startsWith("LectureUnitA-p") && it.filename.endsWith(".jpg") })
        assertEquals(listOf(a, b, c), note.pages)
        assertFalse(note.dirty)

        // Same Page object in two input positions: both outputs still need distinct filenames.
        val alias = doc(a, a)
        val copies = JpegExportPlanner.plan(alias, listOf(1, 0, 1), stem = "same")
        assertEquals(listOf(a.uid, a.uid, a.uid), copies.pages.map { it.pageUid })
        assertEquals(3, copies.pages.map { it.filename }.distinct().size)
        assertEquals(listOf(1, 0, 1), copies.pages.map { it.sourceIndex })
    }

    @Test fun boundedRasterDimensionsPreserveAspectAndAvoidLargeArgbAllocations() {
        val large = doc(Page(9000.0, 10000.0))
        val item = JpegExportPlanner.plan(large, scale = 8.0).pages.single()
        assertTrue(item.widthPx in 1..JpegExportPlanner.MAX_SIDE_PX)
        assertTrue(item.heightPx in 1..JpegExportPlanner.MAX_SIDE_PX)
        assertTrue(item.widthPx.toLong() * item.heightPx <= JpegExportPlanner.MAX_PIXELS)
        assertTrue(kotlin.math.abs(item.widthPx.toDouble() / item.heightPx - 0.9) < 0.001)
        val small = JpegExportPlanner.plan(doc(Page(400.0, 600.0)), scale = 0.5).pages.single()
        assertEquals(200, small.widthPx)
        assertEquals(300, small.heightPx)
        assertEquals(1, JpegExportPlanner.plan(doc(Page(0.25, 0.5))).pages.single().widthPx)
        assertEquals(1, JpegExportPlanner.plan(doc(Page(0.25, 0.5))).pages.single().heightPx)
    }

    @Test fun allContentFlagsAndMarginsAreExportOnlyForRotatedSourcePages() {
        val note = doc(Page(
            300.0, 500.0, pdfPage = 9, pdfTurn = 1,
            style = PageStyle(pageColor = Rgba(30, 50, 70), template = PagePattern.DOTS.id),
            margins = PageMargins(top = 0.2, right = 0.1),
        ))
        val page = note.pages.single()
        val ink = Stroke(com.notestaking.app.core.tools.Tool.PEN,
            com.notestaking.app.core.tools.ToolConfig(),
            listOf(com.notestaking.app.core.stroke.Sample(10.0, 20.0, 0.8)),
        )
        page.items.add(ink)
        page.markups = listOf(TextMarkup(
            "markup", MarkupType.UNDERLINE, Rgba(90, 90, 30), 0.7,
            listOf(TextQuad(10f, 20f, 30f, 30f, 1)), "text", "note", 1L, 2L,
        ))
        note.bookmarks.add(Bookmark(0, "source"))
        val originalItems = page.items.toList()
        val originalMarkup = page.markups.single()
        val originalMargins = page.margins
        val originalStyle = page.style
        val originalPdfKey = page.pdfKey

        for (mask in 0 until 128) {
            val flags = ExportContent(
                strokes = mask and 1 != 0,
                images = mask and 2 != 0,
                text = mask and 4 != 0,
                paper = mask and 8 != 0,
                bookmarks = mask and 16 != 0,
                markups = mask and 32 != 0,
                margins = mask and 64 != 0,
            )
            val entry = JpegExportPlanner.plan(note, content = flags).pages.single()
            assertEquals(if (flags.margins) Rect(0.0, -100.0, 330.0, 600.0)
                else Rect(0.0, 0.0, 300.0, 500.0), entry.footprint)
            assertEquals(flags.paper, entry.paper.paintBlankPaper)
            assertEquals(if (flags.paper) PagePattern.DOTS.id else PageTemplates.NONE, entry.paper.template)
            assertEquals(originalPdfKey, entry.pdfKey)
            assertEquals(page.uid, entry.pageUid)
        }
        assertEquals(originalItems, page.items)
        assertSame(ink, page.items.single())
        assertSame(originalMarkup, page.markups.single())
        assertEquals(originalMargins, page.margins)
        assertEquals(originalStyle, page.style)
        assertEquals(listOf(Bookmark(0, "source")), note.bookmarks)
        assertFalse(note.dirty)
    }

    @Test fun maximumAllowedBatchAndStaleSelectionAreExplicit() {
        val first = Page(100.0, 150.0)
        val second = Page(100.0, 150.0)
        val note = doc(first, second)
        val selection = List(JpegExportPlanner.MAX_PAGES) { if (it % 2 == 0) 1 else 0 }
        val plan = JpegExportPlanner.plan(note, selection, stem = "batch", quality = 100)
        assertEquals(JpegExportPlanner.MAX_PAGES, plan.pages.size)
        assertEquals(100, plan.quality)
        assertEquals(plan.pages.size, plan.pages.map { it.filename }.toSet().size)
        assertEquals(second.uid, plan.pages.first().pageUid)
        assertEquals(first.uid, plan.pages[1].pageUid)
        assertTrue(plan.pages.last().filename.endsWith("-o0500.jpg"))
        // The renderer can reject an old plan by checking the stored pageUid against the live page.
        note.pages.reverse()
        assertTrue(note.pages[plan.pages.first().sourceIndex].uid != plan.pages.first().pageUid)
        assertFalse(note.dirty)
    }

    @Test fun invalidPagesQualityScaleAndDimensionsFailWithoutAnyPartialPlan() {
        val note = doc(Page(400.0, 500.0), Page(600.0, 700.0))
        rejects { JpegExportPlanner.plan(note, emptyList()) }
        rejects { JpegExportPlanner.plan(doc()) }
        rejects { JpegExportPlanner.plan(note, listOf(-1, 0)) }
        rejects { JpegExportPlanner.plan(note, listOf(0, note.pages.size)) }
        rejects { JpegExportPlanner.plan(note, listOf(0, Int.MAX_VALUE)) }
        rejects { JpegExportPlanner.plan(note, List(JpegExportPlanner.MAX_PAGES + 1) { 0 }) }
        for (quality in listOf(-1, 0, 101, 9999)) rejects {
            JpegExportPlanner.plan(note, quality = quality)
        }
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) rejects {
            JpegExportPlanner.plan(note, scale = scale)
        }
        for ((w, h) in listOf(
            0.0 to 300.0, 300.0 to 0.0, -100.0 to 300.0,
            Double.NaN to 100.0, 100.0 to Double.POSITIVE_INFINITY,
            1.0e240 to 1.0e240,
        )) rejects { JpegExportPlanner.plan(doc(Page(w, h))) }
        assertFalse(note.dirty)
        assertEquals(2, note.pages.size)
    }

    @Test fun filenameSanitizationIsBoundedAndUniqueWithLongOrEmptyNoteTitles() {
        val note = doc(Page(10.0, 20.0))
        val blank = JpegExportPlanner.plan(note, stem = " / . / \\ ").pages.single().filename
        assertTrue(blank.startsWith("untitled-p"))
        val huge = JpegExportPlanner.plan(note, indices = listOf(0, 0, 0), stem = "A".repeat(1000))
        assertEquals(3, huge.pages.map { it.filename }.toSet().size)
        assertTrue(huge.pages.all { it.filename.length <= 72 })
        assertTrue(huge.pages.all { '/' !in it.filename && '\\' !in it.filename })
        assertFalse(note.dirty)
    }
}
