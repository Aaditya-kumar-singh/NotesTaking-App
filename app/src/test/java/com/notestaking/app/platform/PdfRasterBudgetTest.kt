package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfRasterBudgetTest {
    @Test fun ordinaryPdfPageIsRenderedAtRequestedSize() {
        assertEquals(595 to 842, PdfRasterBudget.pageSize(595, 842))
        assertEquals(1920 to 1080, PdfRasterBudget.pageSize(1920, 1080))
        assertEquals(1 to 1, PdfRasterBudget.pageSize(0, -10))
    }

    @Test fun wideAndTallPagesKeepTheirAspectRatioWhenDownscaled() {
        val (wideW, wideH) = PdfRasterBudget.pageSize(16_000, 2000)
        assertEquals(4096, wideW)
        assertEquals(512, wideH)
        val (tallW, tallH) = PdfRasterBudget.pageSize(2000, 16_000)
        assertEquals(512, tallW)
        assertEquals(4096, tallH)
    }

    @Test fun largeSquarePageFitsBothDimensionAndCombinedPixelBudget() {
        val (w, h) = PdfRasterBudget.pageSize(4096, 4096)
        assertTrue(w > 2500 && h > 2500)
        assertTrue(w.toLong() * h <= PdfRasterBudget.MAX_PIXELS)
        assertEquals(w, h)
        assertTrue(PdfRasterBudget.allowsRegion(w, h))
    }

    @Test fun hugeAndMalformedRequestedDimensionsNeverOverflow() {
        val (w, h) = PdfRasterBudget.pageSize(Int.MAX_VALUE, Int.MAX_VALUE)
        assertTrue(w in 1..PdfRasterBudget.MAX_DIM)
        assertTrue(h in 1..PdfRasterBudget.MAX_DIM)
        assertTrue(w.toLong() * h <= PdfRasterBudget.MAX_PIXELS)
        assertFalse(PdfRasterBudget.allowsRegion(Int.MAX_VALUE, Int.MAX_VALUE))
        assertFalse(PdfRasterBudget.allowsRegion(-1, 200))
    }

    @Test fun viewportTileBudgetKeepsItsGeometryExact() {
        assertTrue(PdfRasterBudget.allowsRegion(3840, 2160))
        assertTrue(PdfRasterBudget.allowsRegion(4096, 2048))
        assertFalse(PdfRasterBudget.allowsRegion(4096, 4096))
        assertFalse(PdfRasterBudget.allowsRegion(0, 1200))
    }
}
