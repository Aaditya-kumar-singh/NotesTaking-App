package com.notestaking.app.core.model

import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.pdf.PageFrame
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-079: optional export margins change paper geometry, not stored page coordinates. */
class ExportMarginsTest {
    @Test fun defaultKeepsAsymmetricInheritedMarginsAndExplicitPageZero() {
        val page = Page(400.0, 800.0, margins = PageMargins(left = 0.0, top = 0.5))
        val doc = Document(mutableListOf(page), margins = PageMargins(
            left = 0.2, top = 0.1, right = 0.1, bottom = 0.05,
        ))
        val expected = PageInsets(0.0, 400.0, 40.0, 40.0)
        assertEquals(expected, page.insets(doc))
        assertEquals(expected, page.insetsForExport(doc))
        assertEquals(expected, page.insetsForExport(doc, ExportContent()))
        assertEquals(Rect(0.0, -400.0, 440.0, 1240.0), page.footprintForExport(doc))
        assertEquals(PageMargins(left = 0.0, top = 0.5), page.margins)

        val without = ExportContent(margins = false)
        assertEquals(PageInsets.NONE, page.insetsForExport(doc, without))
        assertEquals(Rect(0.0, 0.0, 400.0, 800.0), page.footprintForExport(doc, without))
        assertEquals(expected, page.insets(doc))
        assertEquals(400.0, page.width, 0.0)
        assertEquals(800.0, page.height, 0.0)
        assertFalse(doc.dirty)
    }

    @Test fun invalidAndOutOfRangeFractionsNeverBecomeInvalidExportBoxes() {
        val page = Page(300.0, 600.0)
        val doc = Document(mutableListOf(page), margins = PageMargins(
            left = Double.NaN,
            top = Double.POSITIVE_INFINITY,
            right = Double.NEGATIVE_INFINITY,
            bottom = -2.5,
        ))
        assertEquals(PageInsets(0.0, 600.0, 0.0, 0.0), page.insetsForExport(doc))
        assertEquals(Rect(0.0, -600.0, 300.0, 1200.0), page.footprintForExport(doc))
        assertEquals(PageInsets.NONE, page.insetsForExport(doc, ExportContent(margins = false)))

        page.margins = PageMargins(left = 0.0, top = Double.NaN, right = 5.0, bottom = 0.25)
        assertEquals(PageInsets(0.0, 0.0, 300.0, 150.0), page.insetsForExport(doc))
        assertEquals(Rect(0.0, 0.0, 600.0, 750.0), page.footprintForExport(doc))
        assertTrue(page.footprintForExport(doc).w.isFinite())
        assertTrue(page.footprintForExport(doc).h.isFinite())
        assertTrue(page.margins.top!!.isNaN())
        assertEquals(-2.5, doc.margins.bottom!!, 0.0)
    }

    @Test fun blankAndPdfLinkedPagesKeepTheirPageFrameAndRotationAtEveryQuarterTurn() {
        for (source in listOf<Int?>(null, 7)) {
            for (turn in 0..3) {
                val point = Pt(25.0, 75.0)
                val ink = Stroke(Tool.PEN, ToolConfig(), listOf(
                    Sample(point.x, point.y, 0.8), Sample(85.0, 95.0, 0.8),
                ))
                val quad = TextQuad(10f, 12f, 30f, 19f, 0)
                val markup = TextMarkup(
                    "a", MarkupType.HIGHLIGHT, Rgba(255, 20, 30), 0.5,
                    listOf(quad), "selected", "note", 4L, 6L,
                )
                val page = Page(
                    300.0, 500.0, mutableListOf(ink), pdfPage = source,
                    margins = PageMargins(left = 0.2, top = 0.1, bottom = 0.25),
                    markups = listOf(markup),
                )
                val pdf = File("source.pdf")
                val doc = Document(mutableListOf(page), pdfFile = pdf)
                PageTurn.turn(page, doc, turn)
                val savedInsets = page.insets(doc)
                val savedMarkups = page.markups
                val savedKey = page.pdfKey
                val savedInk = ink.samples.toList()
                val frame = PageFrame.of(20.0, 40.0, 320.0, 540.0, 90 * turn, 0.5)
                val before = frame.toUser(point.x, point.y)
                val width = page.width
                val height = page.height

                for (include in listOf(true, false, true, false)) {
                    val flags = ExportContent(margins = include)
                    val box = page.footprintForExport(doc, flags)
                    assertEquals(if (include) savedInsets else PageInsets.NONE,
                        page.insetsForExport(doc, flags))
                    assertEquals(if (include) width + savedInsets.left + savedInsets.right else width,
                        box.w, 1e-9)
                    assertEquals(if (include) height + savedInsets.top + savedInsets.bottom else height,
                        box.h, 1e-9)
                    assertEquals(before, frame.toUser(point.x, point.y))
                    assertEquals(savedKey, page.pdfKey)
                    assertEquals(savedMarkups, page.markups)
                    assertEquals(savedInk, ink.samples)
                    assertSame(pdf, doc.pdfFile)
                    assertEquals(source, page.pdfPage)
                }
                assertEquals(width, page.width, 0.0)
                assertEquals(height, page.height, 0.0)
                assertEquals(savedInsets, page.insets(doc))
                assertFalse(doc.dirty)
            }
        }
    }

    @Test fun everyExportFlagCombinationKeepsContentMetadataAndNoteUnchanged() {
        val stroke = Stroke(Tool.PEN, ToolConfig(), listOf(
            Sample(20.0, 35.0, 0.9), Sample(120.0, 135.0, 0.9),
        ))
        val markup = TextMarkup(
            "m", MarkupType.STRIKEOUT, Rgba(0, 80, 180), 1.0,
            listOf(TextQuad(10f, 10f, 60f, 20f, 1)), "words", null, 10L, 12L,
        )
        val page = Page(400.0, 700.0, mutableListOf(stroke), markups = listOf(markup),
            margins = PageMargins(right = 0.125))
        val blank = Page(400.0, 700.0)
        val doc = Document(mutableListOf(page, blank), margins = PageMargins(left = 0.15))
        doc.bookmarks.add(Bookmark(0, "Start"))
        val originalMargins = page.margins
        val originalDocumentMargins = doc.margins
        val originalItems = page.items.toList()
        val originalMarkups = page.markups
        val originalBookmarks = doc.bookmarks.toList()
        val baseline = page.insets(doc)

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
            val opposite = flags.copy(margins = !flags.margins)
            assertEquals(page.items.forExport(opposite), page.items.forExport(flags))
            assertEquals(doc.metadataForExport(opposite), doc.metadataForExport(flags))
            assertEquals(if (flags.margins) baseline else PageInsets.NONE,
                page.insetsForExport(doc, flags))
            assertEquals(if (flags.margins) 400.0 + baseline.left + baseline.right else 400.0,
                page.footprintForExport(doc, flags).w, 1e-9)
            assertEquals(if (flags.margins) 700.0 + baseline.top + baseline.bottom else 700.0,
                page.footprintForExport(doc, flags).h, 1e-9)
            assertEquals(if (flags.margins) PageInsets(60.0, 0.0, 0.0, 0.0) else PageInsets.NONE,
                blank.insetsForExport(doc, flags))
        }

        assertEquals(originalMargins, page.margins)
        assertEquals(originalDocumentMargins, doc.margins)
        assertEquals(originalItems, page.items)
        assertEquals(originalMarkups, page.markups)
        assertEquals(originalBookmarks, doc.bookmarks)
        assertSame(stroke, page.items.single())
        assertSame(markup, page.markups.single())
        assertEquals(PageInsets(60.0, 0.0, 50.0, 0.0), page.insets(doc))
        assertFalse(doc.dirty)
    }
}
