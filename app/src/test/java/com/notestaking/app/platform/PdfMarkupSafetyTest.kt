package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.AddMarkups
import com.notestaking.app.core.history.MovePages
import com.notestaking.app.core.history.ReshapePages
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageShape
import com.notestaking.app.core.model.PageTurn
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.pdf.MarkupAnnotation
import com.notestaking.app.core.pdf.PageFrame
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfMarkupSafetyTest {
    private val originalQuad = TextQuad(100f, 50f, 300f, 66f, 0)
    private val color = Rgba(255, 180, 20, 255)

    private fun mark(type: MarkupType, id: String) =
        TextMarkup(id, type, color, 0.45, listOf(originalQuad), "Selected $id", "Note $id", 100L, 200L)

    @Test fun pendingTextMustKeepOriginalPagesAfterReorderAndUndo() {
        val doc = Document(mutableListOf(Page(600.0, 400.0), Page(600.0, 400.0, pdfPage = 0),
            Page(600.0, 400.0, pdfPage = 1)), dpi = 72)
        val captured = PdfMarkupTargets.capture(doc, 1, 2)!!
        assertTrue(captured.stillValid(doc))
        val before = doc.pages.toList()
        val move = MovePages(doc, before, MovePages.order(before, listOf(doc.pages[1]), 3))
        move.redo()
        assertFalse(captured.stillValid(doc))
        move.undo()
        assertTrue(captured.stillValid(doc))
    }

    @Test fun pendingTextCannotUseGeometryFromPreviousPdfRotation() {
        val page = Page(600.0, 400.0, pdfPage = 0)
        val doc = Document(mutableListOf(page), dpi = 72)
        val captured = PdfMarkupTargets.capture(doc, 0, 0)!!
        val before = PageShape.of(page)
        PageTurn.turn(page, doc, 1)
        val rotated = PageShape.of(page)
        val edit = ReshapePages(listOf(page), listOf(before), listOf(rotated))
        assertFalse(captured.stillValid(doc))
        edit.undo()
        assertTrue(captured.stillValid(doc))
        edit.redo()
        assertFalse(captured.stillValid(doc))
    }

    @Test fun pendingTextFailsClosedAfterNoteSwitchDeletionOrInvalidSelection() {
        val page = Page(600.0, 400.0, pdfPage = 2)
        val doc = Document(mutableListOf(Page(600.0, 400.0), page), dpi = 72)
        val captured = PdfMarkupTargets.capture(doc, 0, 1)!!
        assertTrue(captured.stillValid(doc))
        assertFalse(captured.stillValid(Document(mutableListOf(doc.pages[0], page), dpi = 72)))
        doc.pages.removeAt(0)
        assertFalse(captured.stillValid(doc))
        assertNull(PdfMarkupTargets.capture(doc, 0, 5))
        assertNull(PdfMarkupTargets.capture(doc, -1, 0))
        assertNull(PdfMarkupTargets.capture(doc, 1, 0))
        assertNull(PdfMarkupTargets.capture(Document.blank(2), 0, 1))
    }

    @Test fun pendingTextCannotApplyToAResizedPdfBackground() {
        val page = Page(600.0, 400.0, pdfPage = 0)
        val doc = Document(mutableListOf(page), dpi = 72)
        val snapshot = PdfMarkupTargets.capture(doc, 0, 0)!!
        assertTrue(snapshot.stillValid(doc))
        page.width = 580.0
        assertFalse(snapshot.stillValid(doc))
        page.width = 600.0
        assertTrue(snapshot.stillValid(doc))
        page.height = 410.0
        assertFalse(snapshot.stillValid(doc))
    }

    @Test fun highlightUnderlineAndStrikeoutRemainEditableAfterRotationUndoAndNativeReopen() {
        val dir = java.nio.file.Files.createTempDirectory("markup-rotate").toFile()
        try {
            val page = Page(600.0, 400.0, pdfPage = 0)
            val doc = Document(mutableListOf(page), dpi = 72)
            val source = java.io.File(dir, "source.pdf")
            PDDocument().use { pdf ->
                pdf.addPage(PDPage(PDRectangle(600f, 400f)))
                pdf.save(source)
            }
            doc.pdfFile = source
            val types = listOf(MarkupType.HIGHLIGHT, MarkupType.UNDERLINE, MarkupType.STRIKEOUT)
            val original = types.map { type -> mark(type, type.id) }
            val add = AddMarkups(page, original)
            add.redo()
            val originalShape = PageShape.of(page)
            PageTurn.turn(page, doc, 1)
            val rotatedShape = PageShape.of(page)
            val change = ReshapePages(listOf(page), listOf(originalShape), listOf(rotatedShape))
            assertEquals(types, page.markups.map { it.type })
            assertEquals(1, page.pdfTurn)
            assertEquals(400.0, page.width, 1e-6)
            assertEquals(listOf(1, 1, 1), page.markups.map { it.quads.single().quarter })
            change.undo()
            assertEquals(0, page.pdfTurn)
            assertEquals(original.map { it.quads }, page.markups.map { it.quads })
            change.redo()
            assertEquals(1, page.pdfTurn)
            assertEquals(types, page.markups.map { it.type })
            assertEquals(original.map { it.id }, page.markups.map { it.id })
            assertEquals(original.map { it.note }, page.markups.map { it.note })

            val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
            val native = ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
            val extracted = java.io.File(dir, "extracted").apply { mkdirs() }
            val restored = codec.read(ByteArrayInputStream(native), pdfDir = extracted)
            assertNotNull(restored.pdfFile)
            assertArrayEquals(source.readBytes(), restored.pdfFile!!.readBytes())
            assertEquals(1, restored.pages.single().pdfTurn)
            assertEquals(types, restored.pages.single().markups.map { it.type })
            assertEquals(page.markups.map { it.quads }, restored.pages.single().markups.map { it.quads })
            assertEquals(original.map { it.note }, restored.pages.single().markups.map { it.note })
            change.undo()
            add.undo()
            assertTrue(page.markups.isEmpty())
            add.redo()
            change.redo()
            assertEquals(1, page.pdfTurn)
            assertEquals(types, page.markups.map { it.type })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun rotatedPdfExportReopensThreeIndependentTextMarkupAnnotationsWithCorrectQuads() {
        val page = Page(600.0, 400.0, pdfPage = 0)
        val doc = Document(mutableListOf(page), dpi = 72)
        page.markups = listOf(
            mark(MarkupType.HIGHLIGHT, "h"),
            mark(MarkupType.UNDERLINE, "u"),
            mark(MarkupType.STRIKEOUT, "s"),
        )
        PageTurn.turn(page, doc, 1)
        PDDocument().use { pdf ->
            val pdfPage = PDPage(PDRectangle(600f, 400f)).apply { rotation = 90 }
            pdf.addPage(pdfPage)
            val frame = PageFrame.of(0.0, 0.0, 600.0, 400.0, 90, 1.0)
            PdfMarkups.write(PdfExportContext(pdf), pdfPage, page.markups, frame)
            assertEquals(3, pdfPage.annotations.size)
            val bytes = ByteArrayOutputStream().also { pdf.save(it) }.toByteArray()
            PDDocument.load(bytes).use { reopened ->
                val marks = reopened.getPage(0).annotations
                val subtypes = listOf(
                    PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT,
                    PDAnnotationTextMarkup.SUB_TYPE_UNDERLINE,
                    PDAnnotationTextMarkup.SUB_TYPE_STRIKEOUT,
                )
                assertEquals(subtypes, marks.map { it.subtype })
                for ((i, annot) in marks.withIndex()) {
                    assertTrue(annot is PDAnnotationTextMarkup)
                    val text = annot as PDAnnotationTextMarkup
                    val selected = page.markups[i]
                    val expected = MarkupAnnotation.of(selected, frame::pointToUser)!!
                    assertEquals(selected.id, text.annotationName)
                    assertEquals(selected.note, text.contents)
                    assertArrayEquals(expected.quadPoints, text.quadPoints, 0.001f)
                    assertTrue(text.rectangle.width > 0f)
                    assertTrue(text.rectangle.height > 0f)
                    assertNotNull(text.appearance?.normalAppearance)
                }
                assertEquals(90, reopened.getPage(0).rotation)
            }
        }
    }
}
