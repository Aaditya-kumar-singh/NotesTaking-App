package com.notestaking.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PdfWatermarkPlanTest {
    private fun note(vararg pages: Page) = Document(pages.toMutableList())
    private fun invalid(block: () -> Unit) {
        assertTrue(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun textIsTrimmedAndNeverWrittenToTheOriginalNote() {
        val doc = note(Page(400.0, 600.0))
        val before = doc.pages.toList()
        val plan = PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text("  Draft (v2) \\ /  "))
        assertEquals(PdfWatermark.Text("Draft (v2) \\ /"), plan.single().content)
        assertEquals(before, doc.pages)
        assertFalse(doc.dirty)
    }

    @Test fun rejectsEmptyOversizedControlAndUnpairedSurrogateText() {
        val doc = note(Page(400.0, 600.0))
        for (text in listOf("", "   ", "a".repeat(121), "line\nfeed", "x\u0000y", "\u202Ertl", "\u2066isolate", "\uD800", "\uDC00")) {
            invalid { PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text(text)) }
        }
        assertEquals("Résumé 😀", (PdfWatermarkPlanner.plan(doc,
            watermark = PdfWatermark.Text("Résumé 😀")).single().content as PdfWatermark.Text).value)
    }

    @Test fun reorderingAndDuplicatedOutputPagesKeepStableIdentity() {
        val a = Page(300.0, 500.0, pdfPage = 2, pdfTurn = 1)
        val b = Page(600.0, 300.0)
        val doc = note(a, b)
        val order = listOf(1, 0, 1)
        val first = PdfWatermarkPlanner.plan(doc, order, PdfWatermark.Text("COPY"))
        assertEquals(first, PdfWatermarkPlanner.plan(doc, order, PdfWatermark.Text("COPY")))
        assertEquals(order, first.map { it.sourceIndex })
        assertEquals(listOf(0, 1, 2), first.map { it.outputIndex })
        assertEquals(listOf(b.uid, a.uid, b.uid), first.map { it.pageUid })
        assertEquals(1, a.pdfTurn)
        assertFalse(doc.dirty)
    }

    @Test fun positioningAndRotationFitWithinPageIncludingMargins() {
        val doc = note(Page(420.0, 600.0, margins = PageMargins(left = 0.1, top = 0.2)))
        for (anchor in WatermarkAnchor.values()) for (rotation in listOf(-180.0, -90.0, -30.0, 0.0, 45.0, 90.0, 180.0)) {
            val p = PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text("CONFIDENTIAL"),
                anchor = anchor, rotationDegrees = rotation).single()
            val radians = Math.toRadians(rotation)
            val bw = abs(cos(radians)) * p.width + abs(sin(radians)) * p.height
            val bh = abs(sin(radians)) * p.width + abs(cos(radians)) * p.height
            assertTrue(p.centerX - bw / 2 >= 0.05 * 462.0 - 0.00001)
            assertTrue(p.centerX + bw / 2 <= 0.95 * 462.0 + 0.00001)
            assertTrue(p.centerY - bh / 2 >= 0.05 * 720.0 - 0.00001)
            assertTrue(p.centerY + bh / 2 <= 0.95 * 720.0 + 0.00001)
            if (anchor == WatermarkAnchor.CENTER) {
                assertEquals(231.0, p.centerX, 1e-6)
                assertEquals(360.0, p.centerY, 1e-6)
            }
        }
        val withoutMargins = PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text("D"),
            content = ExportContent(margins = false)).single()
        assertEquals(210.0, withoutMargins.centerX, 1e-6)
        assertEquals(300.0, withoutMargins.centerY, 1e-6)
    }

    @Test fun imageUsesOnlyOpaqueAssetKeyAndPreservesAspect() {
        val doc = note(Page(500.0, 250.0))
        val watermark = PdfWatermark.Image("attachedImage_123", 2.0)
        val p = PdfWatermarkPlanner.plan(doc, watermark = watermark, rotationDegrees = 90.0,
            opacity = 0.4).single()
        assertEquals(2.0, p.width / p.height, 1e-9)
        assertEquals(90.0, p.rotationDegrees, 0.0)
        assertEquals(0.4, p.opacity, 0.0)
        assertEquals(watermark, p.content)
        for (key in listOf("../a", "file:///secret", "https://image", "C:\\temp", "", "x".repeat(65))) {
            invalid { PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Image(key, 1.0)) }
        }
        for (ratio in listOf(-1.0, 0.0, 0.01, 21.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            invalid { PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Image("owned", ratio)) }
        }
    }

    @Test fun rejectsBadSelectionOpacityRotationAndGeometry() {
        val doc = note(Page(300.0, 500.0))
        invalid { PdfWatermarkPlanner.plan(doc, emptyList(), PdfWatermark.Text("A")) }
        invalid { PdfWatermarkPlanner.plan(doc, listOf(1), PdfWatermark.Text("A")) }
        invalid { PdfWatermarkPlanner.plan(doc, List(501) { 0 }, PdfWatermark.Text("A")) }
        assertEquals(500, PdfWatermarkPlanner.plan(doc, List(500) { 0 }, PdfWatermark.Text("A")).size)
        for (alpha in listOf(-1.0, 0.0, 1.01, Double.NaN, Double.POSITIVE_INFINITY)) {
            invalid { PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text("A"), opacity = alpha) }
        }
        for (angle in listOf(-181.0, 181.0, Double.NaN, Double.NEGATIVE_INFINITY)) {
            invalid { PdfWatermarkPlanner.plan(doc, watermark = PdfWatermark.Text("A"), rotationDegrees = angle) }
        }
        for (page in listOf(Page(0.0, 500.0), Page(Double.NaN, 500.0),
            Page(Double.POSITIVE_INFINITY, 500.0), Page(1e308, 1e308))) {
            invalid { PdfWatermarkPlanner.plan(note(page), watermark = PdfWatermark.Text("A")) }
        }
    }
}
