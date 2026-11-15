package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.AddPage
import com.notestaking.app.core.history.CompositeCommand
import com.notestaking.app.core.history.DeletePage
import com.notestaking.app.core.history.MovePages
import com.notestaking.app.core.history.ReshapePages
import com.notestaking.app.core.model.Bookmark
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageShape
import com.notestaking.app.core.model.PageTurn
import com.notestaking.app.core.model.PdfKey
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.model.deepCopy
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files

/** Production page commands used by Editor; Android UI/renderer and hardware remain CI/device-only. */
class PageOperationRegressionTest {
    private fun stroke() = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        mutableListOf(Sample(10.0, 20.0, 0.8), Sample(30.0, 60.0, 0.9)),
    )

    private fun markup() = TextMarkup(
        "annotated-text", MarkupType.HIGHLIGHT, Rgba(255, 0, 0, 255), 0.7,
        listOf(TextQuad(10f, 20f, 50f, 30f, 0)),
        "selected text", "remember this", 100L, 200L,
    )

    @Test fun copyingCoverAndAnnotatedPdfPagesProducesIndependentUndoablePages() {
        val cover = Page(200.0, 100.0).also { it.cover = true }
        val pdf = Page(200.0, 100.0, pdfPage = 0)
        val originalInk = stroke()
        pdf.items.add(originalInk)
        pdf.markups = listOf(markup())
        val doc = Document(mutableListOf(cover, pdf), dpi = 72)
        doc.bookmarks.add(Bookmark(1, "source page"))

        val clones = listOf(cover, pdf).map { it.deepCopy(FakeTextMeasurer()) }
        val command = CompositeCommand(clones.mapIndexed { i, cloned ->
            AddPage(doc, cloned, 2 + i).also { it.redo() }
        })
        assertEquals(4, doc.pages.size)
        assertSame(cover, doc.pages[0])
        assertTrue(cover.cover)
        assertFalse(clones[0].cover) // A copied cover becomes an ordinary page.
        assertFalse(clones[1].cover)
        assertNotSame(pdf, clones[1])
        assertEquals(pdf.pdfKey, clones[1].pdfKey)
        assertEquals(pdf.markups.single().quads, clones[1].markups.single().quads)
        assertEquals(1, doc.bookmarks.single().page)

        val cloneInk = clones[1].items.single() as Stroke
        assertNotSame(originalInk, cloneInk)
        cloneInk.setSamples(listOf(Sample(90.0, 91.0, 0.8), Sample(92.0, 93.0, 0.9)))
        assertEquals(90.0, cloneInk.sampleAt(0).x, 1e-6)
        assertEquals(10.0, originalInk.sampleAt(0).x, 1e-6)

        command.undo()
        assertEquals(listOf(cover, pdf), doc.pages)
        assertEquals(1, doc.bookmarks.single().page)
        command.redo()
        assertEquals(listOf(cover, pdf, clones[0], clones[1]), doc.pages)
        assertEquals(1, doc.bookmarks.single().page)
    }

    @Test fun movingMultiplePagesNeverMovesCoverAndUndoRestoresBookmarksAndPdfIdentity() {
        val cover = Page(100.0, 200.0).apply { this.cover = true }
        val pdf = Page(100.0, 200.0, pdfPage = 2, pdfTurn = 3)
        val normal = Page(100.0, 200.0)
        val last = Page(100.0, 200.0)
        val doc = Document(mutableListOf(cover, pdf, normal, last))
        doc.bookmarks.add(Bookmark(1, "pdf"))
        doc.bookmarks.add(Bookmark(3, "last"))
        val before = doc.pages.toList()
        val after = MovePages.order(before, listOf(last, pdf), 1)
        val move = MovePages(doc, before, after)
        move.redo()
        assertEquals(listOf(cover, pdf, last, normal), doc.pages)
        assertSame(cover, doc.pages.first())
        assertEquals(listOf(1, 2), doc.bookmarks.map { it.page })
        assertEquals(PdfKey.of(2, 3), pdf.pdfKey)
        move.undo()
        assertEquals(before, doc.pages)
        assertEquals(listOf(1, 3), doc.bookmarks.map { it.page })
        move.redo()
        assertEquals(listOf(cover, pdf, last, normal), doc.pages)
    }

    @Test fun rotatingAnnotatedPdfPageIsOneUndoableShapeChangeWithStableSourcePointer() {
        val doc = Document(dpi = 72)
        val pdf = Page(200.0, 100.0, pdfPage = 0)
        val originalInk = stroke()
        pdf.items.add(originalInk)
        pdf.markups = listOf(markup())
        doc.pages.add(pdf)
        val before = PageShape.of(pdf)
        PageTurn.turn(pdf, doc, 1)
        val command = ReshapePages(listOf(pdf), listOf(before), listOf(PageShape.of(pdf)))
        assertEquals(100.0, pdf.width, 1e-6)
        assertEquals(200.0, pdf.height, 1e-6)
        assertEquals(PdfKey.of(0, 1), pdf.pdfKey)
        assertEquals(TextQuad(70f, 10f, 80f, 50f, 1), pdf.markups.single().quads.single())
        assertEquals(80.0, originalInk.sampleAt(0).x, 1e-6)
        command.undo()
        assertEquals(PdfKey.of(0, 0), pdf.pdfKey)
        assertEquals(TextQuad(10f, 20f, 50f, 30f, 0), pdf.markups.single().quads.single())
        assertEquals(10.0, originalInk.sampleAt(0).x, 1e-6)
        command.redo()
        assertEquals(PdfKey.of(0, 1), pdf.pdfKey)
        assertEquals(80.0, originalInk.sampleAt(0).x, 1e-6)
    }

    @Test fun deletingCoverAndAnotherPageKeepsOneEditablePageAndUndoRestoresMarks() {
        val cover = Page(100.0, 200.0).apply { this.cover = true }
        val kept = Page(100.0, 200.0, pdfPage = 0).also { it.markups = listOf(markup()) }
        val removed = Page(100.0, 200.0)
        val doc = Document(mutableListOf(cover, kept, removed))
        doc.bookmarks.addAll(listOf(Bookmark(1, "keep"), Bookmark(2, "remove")))
        val before = doc.pages.toList()
        val command = CompositeCommand(listOf(2, 0).map { at ->
            DeletePage(doc, before[at], at).also { it.redo() }
        })
        assertEquals(listOf(kept), doc.pages)
        assertEquals(listOf(Bookmark(0, "keep")), doc.bookmarks)
        assertEquals(0, kept.pdfPage)
        assertEquals("annotated-text", kept.markups.single().id)
        command.undo()
        assertEquals(before, doc.pages)
        assertTrue(doc.pages.first().cover)
        assertEquals(listOf(Bookmark(1, "keep"), Bookmark(2, "remove")), doc.bookmarks)
        command.redo()
        assertEquals(listOf(kept), doc.pages)
    }

    @Test fun duplicatedPdfPageRotationAndAnnotationsSurviveNativeReopenWithOriginalSource() {
        val dir = Files.createTempDirectory("page-operations-roundtrip").toFile()
        try {
            val original = java.io.File(dir, "source.pdf")
            PDDocument().use { pdf ->
                pdf.addPage(PDPage(PDRectangle(200f, 100f)))
                pdf.save(original)
            }
            val sourceBytes = original.readBytes()
            val doc = Document(dpi = 72, pdfFile = original)
            val cover = Page(200.0, 100.0).apply { this.cover = true }
            val source = Page(200.0, 100.0, pdfPage = 0)
            source.items.add(stroke())
            source.markups = listOf(markup())
            doc.pages.addAll(listOf(cover, source))
            val duplicate = source.deepCopy(FakeTextMeasurer())
            AddPage(doc, duplicate, 2).redo()
            PageTurn.turn(duplicate, doc, 1)
            val file = ByteArrayOutputStream().also {
                DocumentCodec(FakeImageCodec(), FakeTextMeasurer()).write(doc, it)
            }.toByteArray()
            val restored = DocumentCodec(FakeImageCodec(), FakeTextMeasurer()).read(
                ByteArrayInputStream(file), pdfDir = dir,
            )
            assertEquals(3, restored.pages.size)
            assertTrue(restored.pages[0].cover)
            assertFalse(restored.pages[2].cover)
            assertEquals(listOf(null, 0, 0), restored.pages.map { it.pdfPage })
            assertEquals(listOf(0, 0, 1), restored.pages.map { it.pdfTurn })
            assertEquals(listOf(200.0, 200.0, 100.0), restored.pages.map { it.width })
            assertEquals("annotated-text", restored.pages[2].markups.single().id)
            assertEquals(TextQuad(70f, 10f, 80f, 50f, 1), restored.pages[2].markups.single().quads.single())
            assertEquals(80.0, (restored.pages[2].items.single() as Stroke).sampleAt(0).x, 1e-6)
            assertEquals(10.0, (restored.pages[1].items.single() as Stroke).sampleAt(0).x, 1e-6)
            assertArrayEquals(sourceBytes, restored.pdfFile!!.readBytes())
            assertArrayEquals(sourceBytes, original.readBytes())
        } finally {
            dir.deleteRecursively()
        }
    }
}
