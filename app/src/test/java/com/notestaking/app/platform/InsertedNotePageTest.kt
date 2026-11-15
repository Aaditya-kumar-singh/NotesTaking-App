package com.notestaking.app.platform

import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Orientation
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageSize
import com.notestaking.app.core.model.PageSizeChoice
import com.notestaking.app.core.model.PageStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class InsertedNotePageTest {
    @Test fun insertingAfterPdfMakesAnIndependentBlankSheetWithSourceDimensions() {
        val source = Page(900.0, 1300.0, pdfPage = 6, style = PageStyle(template = "grid"))
        val inserted = InsertedNotePage.after(Document(dpi = 150), source)
        assertEquals(900.0, inserted.width, 0.0)
        assertEquals(1300.0, inserted.height, 0.0)
        assertNull(inserted.pdfPage)
        assertEquals(PageStyle(), inserted.style)
        assertFalse(inserted.cover)
        assertEquals(6, source.pdfPage)
        assertEquals("grid", source.style.template)
    }

    @Test fun nextSheetKeepsTemplatePageSettingsButNoContentOrPdfSource() {
        val note = Document(dpi = 150)
        val template = Page(800.0, 1100.0, style = PageStyle(template = "dots", spacing = 44.0))
        val inserted = InsertedNotePage.after(note, template)
        assertSame(template.style, inserted.style)
        assertEquals("dots", inserted.style.template)
        assertEquals(44.0, inserted.style.spacing!!, 0.0)
        assertNull(inserted.pdfPage)
        assertEquals(0, inserted.items.size)
    }

    @Test fun coverDoesNotLeakItsOwnTemplateIntoOrdinaryNotePages() {
        val cover = Page(900.0, 1200.0, style = PageStyle(template = "grid")).apply { this.cover = true }
        val sheet = InsertedNotePage.after(Document(), cover)
        assertEquals(PageStyle(), sheet.style)
        assertFalse(sheet.cover)
        assertNull(sheet.pdfPage)
    }

    @Test fun explicitNewPageSizeOverridesAdjacentPdfOrTemplateSize() {
        val note = Document(dpi = 150)
        note.newPageSize = PageSizeChoice(PageSize.A5, Orientation.LANDSCAPE)
        val afterPdf = InsertedNotePage.after(note, Page(900.0, 1200.0, pdfPage = 0))
        val (width, height) = PageSize.A5.pixels(Orientation.LANDSCAPE, note.dpi)
        assertEquals(width, afterPdf.width, 1e-9)
        assertEquals(height, afterPdf.height, 1e-9)
    }
}
