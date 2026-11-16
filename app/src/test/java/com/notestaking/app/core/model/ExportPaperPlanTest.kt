package com.notestaking.app.core.model

import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-076: export-only paper suppression must not change native pages or the selected content. */
class ExportPaperPlanTest {
    private val notePaper = Rgba(224, 235, 246, 255)
    private val pagePaper = Rgba(247, 223, 180, 255)
    private val globalPaper = Rgba(180, 185, 190, 255)

    @Test fun solidPaperResolvesPageThenDocumentThenGlobalWithoutChangingDefaultExport() {
        val page = Page(600.0, 850.0)
        val doc = Document(mutableListOf(page)).apply {
            style = PageStyle(pageColor = notePaper)
        }
        val inherited = page.paperPlanForExport(doc, globalPaperColor = globalPaper)
        assertTrue(inherited.paintBlankPaper)
        assertEquals(notePaper, inherited.paperColor)
        assertEquals(PageTemplates.NONE, inherited.template)
        assertFalse(inherited.paintRuling)
        assertFalse(inherited.linkedSourcePdfPage)

        page.style = PageStyle(pageColor = pagePaper)
        assertEquals(pagePaper, page.paperPlanForExport(doc, globalPaperColor = globalPaper).paperColor)
        page.style = PageStyle()
        doc.style = PageStyle()
        assertEquals(globalPaper, page.paperPlanForExport(doc, globalPaperColor = globalPaper).paperColor)
        assertEquals(null, page.paperPlanForExport(doc).paperColor)
        assertTrue("missing explicit paper color still uses the normal caller fallback",
            page.paperPlanForExport(doc).paintBlankPaper)
    }

    @Test fun linesDotsGridAndExplicitNoneRespectTheStyleInheritanceChain() {
        for (pattern in PagePattern.entries) {
            val page = Page(620.0, 880.0)
            val doc = Document(mutableListOf(page)).apply {
                style = PageStyle(template = pattern.id, patternColor = Rgba(8, 9, 10, 128), spacing = 42.0)
            }
            val inherited = page.paperPlanForExport(doc)
            assertEquals(pattern.id, inherited.template)
            assertEquals(pattern != PagePattern.NONE, inherited.paintRuling)
            assertTrue(inherited.paintBlankPaper)

            page.style = PageStyle(template = PagePattern.NONE.id)
            assertEquals(PageTemplates.NONE, page.paperPlanForExport(doc).template)
            assertFalse(page.paperPlanForExport(doc).paintRuling)

            page.style = PageStyle(template = PagePattern.GRID.id)
            assertEquals(PagePattern.GRID.id, page.paperPlanForExport(doc).template)
            assertTrue(page.paperPlanForExport(doc).paintRuling)

            for (printPaper in listOf(true, false)) {
                val flags = ExportContent(paper = printPaper)
                val plan = page.paperPlanForExport(doc, flags)
                assertEquals(printPaper, plan.paintBlankPaper)
                assertEquals(if (printPaper) PagePattern.GRID.id else PageTemplates.NONE, plan.template)
                assertEquals(printPaper, plan.paintRuling)
                assertEquals(pattern.id, doc.style.template)
                assertEquals(PagePattern.GRID.id, page.style.template)
            }
        }
    }

    @Test fun packagedTemplatesAndPerPageColorsAreAlsoOmittedOnlyFromAnExportPlan() {
        val page = Page(520.0, 700.0, style = PageStyle(template = "custom-paper", pageColor = pagePaper))
        val doc = Document(mutableListOf(page)).apply {
            style = PageStyle(template = PagePattern.LINES.id, pageColor = notePaper)
            templates = mapOf("custom-paper" to "<template/>")
        }
        val originalTemplate = doc.templates
        val on = page.paperPlanForExport(doc)
        val off = page.paperPlanForExport(doc, ExportContent(paper = false), globalPaper)
        assertEquals("custom-paper", on.template)
        assertTrue(on.paintRuling)
        assertEquals(pagePaper, on.paperColor)
        assertFalse(off.paintBlankPaper)
        assertEquals(null, off.paperColor)
        assertEquals(PageTemplates.NONE, off.template)
        assertFalse(off.paintRuling)
        assertEquals(originalTemplate, doc.templates)
        assertEquals("custom-paper", page.style.template)
        assertEquals(PagePattern.LINES.id, doc.style.template)
        assertEquals(pagePaper, page.style.pageColor)
    }

    @Test fun sourcePdfArtIsNeverMistakenForRemovablePaperOnRotatedMarginedPages() {
        for (printPaper in listOf(false, true)) {
            val page = Page(
                600.0, 900.0, pdfPage = 4, pdfTurn = 3,
                style = PageStyle(pageColor = pagePaper, template = PagePattern.DOTS.id),
                margins = PageMargins(left = 0.15, top = 0.08),
            )
            val source = File("original-source.pdf")
            val doc = Document(mutableListOf(page), pdfFile = source)
            val before = page.insets(doc)
            val pdfKey = page.pdfKey
            val plan = page.paperPlanForExport(doc, ExportContent(paper = printPaper), globalPaper)
            assertTrue("a linked source-PDF page remains linked regardless of paper option",
                plan.linkedSourcePdfPage)
            assertEquals(printPaper, plan.paintBlankPaper)
            assertEquals(if (printPaper) pagePaper else null, plan.paperColor)
            assertEquals(if (printPaper) PagePattern.DOTS.id else PageTemplates.NONE, plan.template)
            assertEquals(pdfKey, page.pdfKey)
            assertSame(source, doc.pdfFile)
            assertEquals(before, page.insets(doc))
            assertEquals(600.0, page.width, 0.0)
            assertEquals(900.0, page.height, 0.0)
            assertEquals(3, page.pdfTurn)
        }
    }

    @Test fun emptyPageAndEveryInkImageTextPaperCombinationKeepGeometryAndNoteUnchanged() {
        val ink = Stroke(Tool.PEN, ToolConfig(), listOf(
            Sample(20.0, 20.0, 0.8), Sample(80.0, 100.0, 0.8),
        ))
        val picture = ImageItem(ImageData(File("source.png"), 80, 60), Rect(40.0, 50.0, 100.0, 70.0))
        val typed = TextItem(Pt(70.0, 150.0), text = "draft", measurer = FakeTextMeasurer())
        val page = Page(500.0, 750.0, mutableListOf(ink, picture, typed),
            style = PageStyle(template = PagePattern.GRID.id), margins = PageMargins(right = 0.20))
        val doc = Document(mutableListOf(page, Page(500.0, 750.0))).apply {
            style = PageStyle(pageColor = notePaper)
        }
        val originalItems = page.items.toList()
        val originalSamples = ink.samples.toList()
        val originalImage = picture.image
        val originalText = typed.text
        val originalInsets = page.insets(doc)
        val style = page.style
        val documentStyle = doc.style
        for (mask in 0 until 16) {
            val flags = ExportContent(
                strokes = mask and 1 != 0,
                images = mask and 2 != 0,
                text = mask and 4 != 0,
                paper = mask and 8 != 0,
            )
            val plan = page.paperPlanForExport(doc, flags)
            val items = page.items.forExport(flags)
            assertEquals(mask and 8 != 0, plan.paintBlankPaper)
            assertEquals(if (flags.paper) PagePattern.GRID.id else PageTemplates.NONE, plan.template)
            assertEquals(3, page.items.size)
            assertTrue("filtered items are original objects in z-order", items.zipWithNext().all { (a, b) ->
                originalItems.indexOfFirst { it === a } < originalItems.indexOfFirst { it === b }
            })
            assertEquals(flags.strokes, items.any { it === ink })
            assertEquals(flags.images, items.any { it === picture })
            assertEquals(flags.text, items.any { it === typed })
            val blank = doc.pages.last().paperPlanForExport(doc, flags)
            assertEquals(flags.paper, blank.paintBlankPaper)
            assertEquals(notePaper.takeIf { flags.paper }, blank.paperColor)
            assertEquals(PageTemplates.NONE, blank.template)
        }
        assertEquals(originalItems, page.items)
        assertEquals(originalSamples, ink.samples)
        assertSame(originalImage, picture.image)
        assertEquals(originalText, typed.text)
        assertEquals(originalInsets, page.insets(doc))
        assertEquals(style, page.style)
        assertEquals(documentStyle, doc.style)
        assertFalse(doc.dirty)
    }
}
