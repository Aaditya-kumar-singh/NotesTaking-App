package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.DeletePage
import com.notestaking.app.core.history.MovePages
import com.notestaking.app.core.model.Bookmark
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files

class PageNavigationTest {
    @Test fun outlineTargetsExactSourcePdfPagesEvenWhenNoteHasNativePagesAndReorderedPdfPages() {
        val plain = Page(200.0, 300.0)
        val sourceFive = Page(200.0, 300.0, pdfPage = 5)
        val sourceZero = Page(200.0, 300.0, pdfPage = 0)
        val sourceThree = Page(200.0, 300.0, pdfPage = 3)
        val pages = listOf(plain, sourceFive, sourceZero, sourceThree)
        assertEquals(1, PageNavigation.outlineTarget(pages, 5))
        assertEquals(3, PageNavigation.outlineTarget(pages, 3))
        assertEquals(2, PageNavigation.outlineTarget(pages, 0))
        assertEquals(2, PageNavigation.outlineTarget(pages, 1))
        assertEquals(2, PageNavigation.outlineTarget(pages, 2))
        assertEquals(3, PageNavigation.outlineTarget(pages, 4))
        assertNull(PageNavigation.outlineTarget(pages, -1))
        assertNull(PageNavigation.outlineTarget(listOf(plain), 5))
    }

    @Test fun sparseOutlineChoosesClosestPrecedingSourceNumberNotLastNotePosition() {
        val pdfTwo = Page(100.0, 200.0, pdfPage = 2)
        val pdfZero = Page(100.0, 200.0, pdfPage = 0)
        val pdfFive = Page(100.0, 200.0, pdfPage = 5)
        val blank = Page(100.0, 200.0)
        val pages = listOf(pdfTwo, blank, pdfZero, pdfFive)
        assertEquals(0, PageNavigation.outlineTarget(pages, 3))
        assertEquals(0, PageNavigation.outlineTarget(pages, 4))
        assertEquals(2, PageNavigation.outlineTarget(pages, 1))
        assertEquals(3, PageNavigation.outlineTarget(pages, 10))
        assertEquals(3, PageNavigation.outlineTarget(pages, 5))
        assertNull(PageNavigation.outlineTarget(listOf(pdfFive, blank), 4))
        val duplicate = listOf(pdfTwo, pdfTwo, pdfZero)
        assertEquals(0, PageNavigation.outlineTarget(duplicate, 2))
    }

    @Test fun bookmarkTargetsFollowPageMoveAndRejectDeletedForeignOrOutOfBoundsRows() {
        val doc = Document.blank(count = 4)
        val targetPage = doc.pages[1]
        val mark = Bookmark(1, "Chapter")
        doc.bookmarks.add(mark)
        assertEquals(1, PageNavigation.bookmarkTarget(doc, mark))
        assertNull(PageNavigation.bookmarkTarget(doc, Bookmark(1, "Chapter")))
        assertNull(PageNavigation.bookmarkTarget(Document.blank(count = 4), mark))

        val original = doc.pages.toList()
        val move = MovePages(doc, original, MovePages.order(original, listOf(targetPage), 4))
        move.redo()
        assertEquals(3, PageNavigation.bookmarkTarget(doc, mark))
        assertSame(targetPage, doc.pages[3])
        move.undo()
        assertEquals(1, PageNavigation.bookmarkTarget(doc, mark))

        val delete = DeletePage(doc, targetPage, 1)
        delete.redo()
        assertNull(PageNavigation.bookmarkTarget(doc, mark))
        delete.undo()
        assertEquals(1, PageNavigation.bookmarkTarget(doc, mark))
        mark.page = doc.pages.size
        assertNull(PageNavigation.bookmarkTarget(doc, mark))
        mark.page = -1
        assertNull(PageNavigation.bookmarkTarget(doc, mark))
    }

    @Test fun mixedPdfSourceIndicesAndNoteBookmarksPersistAcrossNativeSaveAndReopen() {
        val temp = Files.createTempDirectory("outline-bookmark-mixed").toFile()
        try {
            val originalPdf = java.io.File(temp, "source.pdf")
            PDDocument().use { pdf ->
                repeat(6) { pdf.addPage(PDPage(PDRectangle(200f, 300f))) }
                pdf.save(originalPdf)
            }
            val bytes = originalPdf.readBytes()
            val doc = Document.blank(count = 5, dpi = 72)
            doc.pdfFile = originalPdf
            doc.pages[1].pdfPage = 5
            doc.pages[3].pdfPage = 2
            doc.pages[4].pdfPage = 0
            doc.bookmarks.addAll(listOf(
                Bookmark(1, "Chapter 6"),
                Bookmark(3, "Chapter 3"),
                Bookmark(4, "Chapter 1"),
            ))
            val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
            val saved = ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
            val extracted = java.io.File(temp, "extracted").also { it.mkdirs() }
            val reopened = codec.read(ByteArrayInputStream(saved), pdfDir = extracted)
            assertEquals(listOf(null, 5, null, 2, 0), reopened.pages.map { it.pdfPage })
            assertEquals(
                listOf("Chapter 6" to 1, "Chapter 3" to 3, "Chapter 1" to 4),
                reopened.bookmarks.map { it.label to it.page },
            )
            assertEquals(1, PageNavigation.outlineTarget(reopened.pages, 5))
            assertEquals(3, PageNavigation.outlineTarget(reopened.pages, 3))
            assertEquals(4, PageNavigation.outlineTarget(reopened.pages, 1))
            assertEquals(3, PageNavigation.outlineTarget(reopened.pages, 2))
            for (bookmark in reopened.bookmarks) {
                assertEquals(bookmark.page, PageNavigation.bookmarkTarget(reopened, bookmark))
            }
            assertArrayEquals(bytes, originalPdf.readBytes())
            val embedded = reopened.pdfFile ?: throw AssertionError("Source PDF missing after reopening")
            assertArrayEquals(bytes, embedded.readBytes())
            assertTrue(saved.isNotEmpty())
        } finally {
            temp.deleteRecursively()
        }
    }
}
