package com.notestaking.app.platform

import com.notestaking.app.core.geometry.Affine
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.pdf.PageFrame
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

class PdfInkAnnotationsTest {
    private fun pen(config: ToolConfig = ToolConfig(baseWidth = 4.0, pressureEnabled = false)): Stroke =
        Stroke(Tool.PEN, config, listOf(Sample(12.0, 22.0, 1.0), Sample(28.0, 34.0, 1.0)))

    @Test fun optInSupportsOnlySimpleOpaqueConstantWidthInk() {
        assertTrue(PdfInkAnnotations.eligible(pen()))
        assertFalse(PdfInkAnnotations.eligible(pen(ToolConfig())))
        assertFalse(PdfInkAnnotations.eligible(pen(ToolConfig(pressureEnabled = false, taperEnabled = true))))
        assertFalse(PdfInkAnnotations.eligible(pen(ToolConfig(pressureEnabled = false, neon = true))))
        assertFalse(PdfInkAnnotations.eligible(pen(ToolConfig(pressureEnabled = false, speedStrength = 0.5))))
        assertFalse(PdfInkAnnotations.eligible(pen(ToolConfig(pressureEnabled = false, directionStrength = 0.5))))
        assertFalse(PdfInkAnnotations.eligible(Stroke(Tool.HIGHLIGHTER, ToolConfig(pressureEnabled = false),
            listOf(Sample(12.0, 22.0, 1.0), Sample(28.0, 34.0, 1.0)))))
        assertFalse(PdfInkAnnotations.eligible(Stroke(Tool.PEN, ToolConfig(pressureEnabled = false),
            listOf(Sample(12.0, 22.0, 1.0)))))
    }

    @Test fun onlyTrailingSimpleInkIsDetachedSoOverlappingObjectsKeepTheirOrder() {
        val initial = pen()
        val highlighter = Stroke(Tool.HIGHLIGHTER, ToolConfig(pressureEnabled = false),
            listOf(Sample(8.0, 8.0, 1.0), Sample(20.0, 20.0, 1.0)))
        val last = pen()
        assertEquals(listOf(last), PdfInkAnnotations.trailing(listOf(initial, highlighter, last)))
        assertTrue(PdfInkAnnotations.trailing(listOf(last, highlighter)).isEmpty())
        assertEquals(listOf(initial, last), PdfInkAnnotations.trailing(listOf(initial, last)))
    }

    @Test fun exportedInkReopensAsIndependentPdfAnnotationWithAppearanceAndCoordinates() {
        val stroke = pen()
        val second = pen()
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle(500f, 600f))
            doc.addPage(page)
            val frame = PageFrame(Affine.IDENTITY, 0.0, 600.0, 1.0)
            PdfInkAnnotations.write(doc, page, listOf(stroke, second), frame)
            assertEquals(2, page.annotations.size)
            val annotation = page.annotations.first() as PDAnnotationMarkup
            assertEquals(PDAnnotationMarkup.SUB_TYPE_INK, annotation.subtype)
            assertEquals(1, annotation.inkList.size)
            assertNotNull(annotation.appearance?.normalAppearance)
            assertTrue(annotation.isPrinted)
            val geometry = stroke.geometry()
            val coordinates = annotation.inkList.single()
            assertEquals(2 * geometry.pointCount, coordinates.size)
            assertEquals(frame.toUser(geometry.cx(0), geometry.cy(0)).x, coordinates[0].toDouble(), 0.001)
            assertEquals(frame.toUser(geometry.cx(0), geometry.cy(0)).y, coordinates[1].toDouble(), 0.001)
            assertTrue(annotation.rectangle.width > 0f)

            val serialized = ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
            PDDocument.load(serialized).use { reopened ->
                val saved = reopened.getPage(0).annotations
                assertEquals(2, saved.size)
                assertTrue(saved.all { it is PDAnnotationMarkup })
                val actual = (saved[0] as PDAnnotationMarkup).inkList.single()
                assertEquals(coordinates.size, actual.size)
                for (i in coordinates.indices) assertEquals(coordinates[i], actual[i], 0.001f)
                assertNotNull(saved[0].appearance?.normalAppearance)
            }
        }
    }

    @Test fun rotatedAndCroppedSourceFrameUsesPdfUserSpaceNotScreenPixels() {
        val stroke = pen()
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle(100f, 200f)).apply { rotation = 90 }
            doc.addPage(page)
            val frame = PageFrame.of(10.0, 15.0, 110.0, 215.0, 90, 0.5)
            PdfInkAnnotations.write(doc, page, listOf(stroke), frame)
            val coordinates = (page.annotations.single() as PDAnnotationMarkup).inkList.single()
            val geometry = stroke.geometry()
            for (i in 0 until geometry.pointCount) {
                val expected = frame.toUser(geometry.cx(i), geometry.cy(i))
                assertEquals(expected.x, coordinates[2 * i].toDouble(), 0.001)
                assertEquals(expected.y, coordinates[2 * i + 1].toDouble(), 0.001)
            }
        }
    }

    @Test fun invalidPdfTransformFailsRatherThanLosingAnUnpaintedStroke() {
        PDDocument().use { doc ->
            val page = PDPage(PDRectangle(300f, 400f))
            doc.addPage(page)
            assertThrows(IOException::class.java) {
                PdfInkAnnotations.write(
                    doc, page, listOf(pen()),
                    PageFrame(Affine.IDENTITY, 0.0, 400.0, Double.POSITIVE_INFINITY),
                )
            }
        }
    }
}
