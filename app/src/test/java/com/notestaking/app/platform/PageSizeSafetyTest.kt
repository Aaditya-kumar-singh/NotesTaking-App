package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Orientation
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageSize
import com.notestaking.app.core.model.PageSizeChoice
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files

class PageSizeSafetyTest {
    private val dpi = PageSize.DEFAULT_DPI

    @Test fun exactA4AndNamedLandscapeRetainRecognizablePaperChoices() {
        assertEquals(
            PageSizeChoice(PageSize.A4, Orientation.PORTRAIT),
            PageSizeSafety.forNewNote(210.0, 297.0, dpi),
        )
        assertEquals(
            PageSizeChoice(PageSize.A4, Orientation.LANDSCAPE),
            PageSizeSafety.forNewNote(297.0, 210.0, dpi),
        )
        assertEquals(
            PageSizeChoice(PageSize.A5, Orientation.LANDSCAPE),
            PageSizeSafety.forNewNote(210.0, 148.0, dpi),
        )
    }

    @Test fun nearA4ButExactCustomMillimetresAreNeverRoundedToA4OnSubsequentPages() {
        val selected = PageSizeSafety.forNewNote(297.2, 210.1, dpi)
        assertNotNull(selected)
        assertEquals(PageSize.CUSTOM, selected!!.size)
        assertEquals(Orientation.LANDSCAPE, selected.orientation)
        assertEquals(297.2, selected.customWidthMm, 1e-9)
        assertEquals(210.1, selected.customHeightMm, 1e-9)
        val (w, h) = selected.pixels(dpi)
        assertEquals(297.2, PageSize.pxToMm(w, dpi), 1e-9)
        assertEquals(210.1, PageSize.pxToMm(h, dpi), 1e-9)
    }

    @Test fun invalidOrUnboundedPaperDimensionsCannotReachAnEditableDocument() {
        for ((width, height) in listOf(
            Double.NaN to 210.0,
            210.0 to Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY to 297.0,
            0.0 to 297.0,
            9.9 to 297.0,
            210.0 to 2000.1,
            -100.0 to 100.0,
        )) {
            assertNull(PageSizeSafety.forNewNote(width, height, dpi))
            assertFalse(PageSizeSafety.valid(PageSizeChoice(
                PageSize.CUSTOM, customWidthMm = width, customHeightMm = height,
            )))
        }
        assertNull(PageSizeSafety.forNewNote(210.0, 297.0, 0))
        assertTrue(PageSizeSafety.validMm(10.0, 2000.0))
        assertNotNull(PageSizeSafety.forNewNote(10.0, 2000.0, dpi))
        assertTrue(PageSizeSafety.valid(PageSizeChoice(PageSize.A4, Orientation.LANDSCAPE)))
    }

    @Test fun customLandscapeSizePersistsThroughNativeReopenAndBlankInsertionAfterPdfPage() {
        val dir = Files.createTempDirectory("custom-mm-mixed-pdf").toFile()
        try {
            val original = java.io.File(dir, "picked.pdf")
            PDDocument().use { pdf ->
                pdf.addPage(PDPage(PDRectangle(612f, 792f)))
                pdf.save(original)
            }
            val wantedWidthMm = 254.5
            val wantedHeightMm = 127.25
            val choice = PageSizeSafety.forNewNote(wantedWidthMm, wantedHeightMm, dpi)!!
            val (width, height) = choice.pixels(dpi)
            val note = Document.blankPixels(1, width, height, dpi)
            note.newPageSize = choice
            note.pdfFile = original
            val pdfPage = Page(612.0 / 72 * dpi, 792.0 / 72 * dpi, pdfPage = 0)
            note.pages.add(pdfPage)
            note.pages.add(InsertedNotePage.after(note, pdfPage))

            val native = ByteArrayOutputStream().also { DocumentCodec(FakeImageCodec(), FakeTextMeasurer()).write(note, it) }.toByteArray()
            val extracted = java.io.File(dir, "reopen").apply { mkdirs() }
            val reopened = DocumentCodec(FakeImageCodec(), FakeTextMeasurer()).read(
                ByteArrayInputStream(native), pdfDir = extracted,
            )
            assertEquals(3, reopened.pages.size)
            assertEquals(choice, reopened.newPageSize)
            assertEquals(listOf(null, 0, null), reopened.pages.map { it.pdfPage })
            for (i in listOf(0, 2)) {
                val page = reopened.pages[i]
                assertEquals(wantedWidthMm, PageSize.pxToMm(page.width, dpi), 1e-6)
                assertEquals(wantedHeightMm, PageSize.pxToMm(page.height, dpi), 1e-6)
                assertEquals(wantedWidthMm / 25.4 * 72.0, page.width / dpi * 72.0, 1e-5)
                assertEquals(wantedHeightMm / 25.4 * 72.0, page.height / dpi * 72.0, 1e-5)
            }
            assertEquals(612.0, reopened.pages[1].width / dpi * 72.0, 1e-6)
            assertEquals(792.0, reopened.pages[1].height / dpi * 72.0, 1e-6)
            val embedded = reopened.pdfFile ?: throw AssertionError("Mixed note lost embedded PDF")
            PDDocument.load(embedded).use { pdf ->
                assertEquals(1, pdf.numberOfPages)
                assertEquals(612.0, pdf.getPage(0).mediaBox.width.toDouble(), 1e-6)
            }
            val afterReopen = InsertedNotePage.after(reopened, reopened.pages[1])
            assertEquals(width, afterReopen.width, 1e-8)
            assertEquals(height, afterReopen.height, 1e-8)
        } finally {
            dir.deleteRecursively()
        }
    }
}
