package com.notestaking.app.format

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeRenderer
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.text.FlowLayout
import com.notestaking.app.core.text.FlowPainter
import com.notestaking.app.core.text.PageBox
import com.notestaking.app.core.text.Paragraph
import com.notestaking.app.core.text.Run
import com.notestaking.app.platform.PdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** NT-054: distinguish archived flow from text actually placed/painted after a cold load. */
class NativeFlowVisibilityTest {
    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
    private fun savedNote(blankParagraphs: Int, marker: String): ByteArray {
        val doc = Document(dpi = 150).apply {
            pages.add(Page(1240.0, 1754.0))
            repeat(blankParagraphs) { flow.paragraphs.add(Paragraph()) }
            flow.paragraphs.add(Paragraph(mutableListOf(Run(marker))))
        }
        return ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
    }

    private fun assertReopenedTextPaints(blankParagraphs: Int, marker: String) {
        val archive = savedNote(blankParagraphs, marker)
        // Cold-reopen twice from the SAME immutable bytes, rather than from a live editor object.
        repeat(2) {
            val doc = codec.read(ByteArrayInputStream(archive))
            assertEquals(1, doc.pages.size)
            assertEquals(blankParagraphs + 1, doc.flow.paragraphs.size)
            assertEquals(marker, doc.flow.paragraphs[blankParagraphs].plainText())
            val frame = FlowLayout(FakeTextMeasurer()).layout(
                doc.flow, doc.pages.map { PageBox(it.width, it.height) }, doc.dpi
            )
            val placed = frame.pages[0].lines.single { line ->
                line.paraIndex == blankParagraphs && line.segs.any { seg -> seg.text == marker }
            }
            assertEquals(blankParagraphs, placed.paraIndex)
            assertEquals(0, frame.extraPagesNeeded)
            val screen = FakeRenderer()
            FlowPainter.paintPage(screen, frame, 0, Rect(0.0, 0.0, 1240.0, 1754.0))
            assertTrue(screen.ops.any { it.startsWith("drawTextRun:$marker@") })

            // Ordinary PDF uses this same page-indexed FlowExport hook, with a different renderer.
            val export = PdfExporter.FlowExport(doc.flow, frame) { page ->
                doc.pages.indexOfFirst { it === page }.takeIf { idx -> idx >= 0 }
            }
            val pdfFlow = FakeRenderer()
            export.paint(doc.pages.single(), pdfFlow, Rect(0.0, 0.0, 1240.0, 1754.0))
            assertTrue(pdfFlow.ops.any { it.startsWith("drawTextRun:$marker@") })
            assertFalse(doc.flow.isEmpty)
        }
    }

    @Test fun topPageTextWithTwoEmptyParagraphsSurvivesNativeColdReopenAndPaint() {
        assertReopenedTextPaints(2, "QA_NT054_IME_CONFIRMED_20261117")
    }

    @Test fun lowerPageTextWith48EmptyParagraphsSurvivesNativeColdReopenAndPaint() {
        assertReopenedTextPaints(48, "QA_NT054_TEXT_20261117")
    }
}
