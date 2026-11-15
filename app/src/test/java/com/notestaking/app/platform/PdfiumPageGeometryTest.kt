package com.notestaking.app.platform

import com.notestaking.app.core.pdf.PdfPageGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfiumPageGeometryTest {
    @Test fun croppedAndRotatedPageMapsStayInvertibleAtAllZooms() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val expected = PdfPageGeometry.of(23.0, 41.0, 623.0, 841.0, rotation)
            val values = doubleArrayOf(expected.a, expected.b, expected.c, expected.d, expected.e, expected.f)
            val geometry = checkedNativePageGeometry(values) ?: error("Valid PDF page map was rejected")
            for ((x, y) in listOf(23.0 to 41.0, 100.0 to 720.0, 620.0 to 839.0)) {
                val display = geometry.toDisplay(x, y)
                for (zoom in listOf(0.25, 0.5, 1.0, 4.0)) {
                    val restored = geometry.toUser(display.x * zoom / zoom, display.y * zoom / zoom)
                    assertEquals(x, restored.x, 1e-8)
                    assertEquals(y, restored.y, 1e-8)
                }
            }
        }
    }

    @Test fun malformedAndSingularNativeMapsNeverReachAnnotationGeometry() {
        assertNull(checkedNativePageGeometry(null))
        assertNull(checkedNativePageGeometry(DoubleArray(5)))
        assertNull(checkedNativePageGeometry(DoubleArray(7)))
        assertNull(checkedNativePageGeometry(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0)))
        assertNull(checkedNativePageGeometry(doubleArrayOf(1e-12, 0.0, 0.0, 1.0, 0.0, 0.0)))
        assertNull(checkedNativePageGeometry(doubleArrayOf(1.0, 0.0, 0.0, 1.0, Double.NaN, 0.0)))
        assertNull(checkedNativePageGeometry(doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, Double.POSITIVE_INFINITY)))
    }
}
