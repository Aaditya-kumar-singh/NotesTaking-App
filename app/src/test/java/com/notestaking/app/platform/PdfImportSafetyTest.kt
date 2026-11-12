package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files

class PdfImportSafetyTest {
    private fun withDirectory(test: (java.io.File) -> Unit) {
        val dir = Files.createTempDirectory("notes-pdf-stage").toFile()
        try { test(dir) } finally { dir.deleteRecursively() }
    }

    private fun pdf(bytes: Int): ByteArray = ByteArray(bytes) { 'x'.code.toByte() }.apply {
        "%PDF-1.7\n".toByteArray(Charsets.US_ASCII).copyInto(this)
    }

    @Test fun validPdfIsStagedWithoutLoadingAllBytes() = withDirectory { dir ->
        val original = pdf(4096)
        val staged = PdfImportSafety.stage(ByteArrayInputStream(original), dir, maxBytes = 4096)
        assertTrue(staged?.isFile == true)
        assertArrayEquals(original, staged!!.readBytes())
        staged.delete()
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun rejectsTooLargePdfAndDeletesPartialTemp() = withDirectory { dir ->
        assertNull(PdfImportSafety.stage(ByteArrayInputStream(pdf(4097)), dir, maxBytes = 4096))
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun rejectsMissingSignatureAndEmptyFileWithoutResidue() = withDirectory { dir ->
        assertNull(PdfImportSafety.stage(ByteArrayInputStream("Not PDF".toByteArray()), dir))
        assertNull(PdfImportSafety.stage(ByteArrayInputStream(byteArrayOf()), dir))
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun recognizesPdfSignatureAfterShortPreamble() = withDirectory { dir ->
        val data = (ByteArray(30) { 32 } + pdf(512))
        val staged = PdfImportSafety.stage(ByteArrayInputStream(data), dir)
        assertTrue(staged != null)
        assertEquals(data.size.toLong(), staged!!.length())
        staged.delete()
    }

    @Test fun cancellationBeforeProviderReadNeverConsumesSource() = withDirectory { dir ->
        val input = object : ByteArrayInputStream(pdf(4096)) {
            var closed = false
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                throw AssertionError("Cancelled import must not read the provider")
            }
            override fun close() {
                closed = true
                super.close()
            }
        }
        assertNull(PdfImportSafety.stage(input, dir, isCancelled = { true }))
        assertTrue(input.closed)
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun cancellationDoesNotPublishPartialStagedFile() = withDirectory { dir ->
        var cancel = false
        val input = object : ByteArrayInputStream(pdf(8192)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = super.read(b, off, len.coerceAtMost(1024))
                cancel = n > 0
                return n
            }
        }
        assertNull(PdfImportSafety.stage(input, dir, isCancelled = { cancel }))
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun failedProviderReadCleansUp() = withDirectory { dir ->
        val input = object : ByteArrayInputStream(pdf(8192)) {
            var reads = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (++reads == 2) throw IOException("provider disconnected")
                return super.read(b, off, len.coerceAtMost(100))
            }
        }
        assertNull(PdfImportSafety.stage(input, dir))
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun stagingFailuresReportCorrectReasonsAndCleanUp() = withDirectory { dir ->
        val reasons = mutableListOf<PdfImportSafety.StageFailure>()
        fun record(failure: PdfImportSafety.StageFailure) { reasons += failure }

        assertNull(PdfImportSafety.stage(
            ByteArrayInputStream("not a PDF".toByteArray()), dir, onFailure = ::record,
        ))
        assertNull(PdfImportSafety.stage(
            ByteArrayInputStream(pdf(4097)), dir, maxBytes = 4096, onFailure = ::record,
        ))
        val broken = object : ByteArrayInputStream(pdf(4096)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("Provider disconnected")
        }
        assertNull(PdfImportSafety.stage(broken, dir, onFailure = ::record))
        assertNull(PdfImportSafety.stage(
            ByteArrayInputStream(pdf(4096)), dir, isCancelled = { true }, onFailure = ::record,
        ))
        assertEquals(listOf(
            PdfImportSafety.StageFailure.INVALID_PDF,
            PdfImportSafety.StageFailure.TOO_LARGE,
            PdfImportSafety.StageFailure.UNREADABLE,
            PdfImportSafety.StageFailure.CANCELLED,
        ), reasons)
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun successfulStagingReportsNoFailure() = withDirectory { dir ->
        val failures = mutableListOf<PdfImportSafety.StageFailure>()
        val staged = PdfImportSafety.stage(
            ByteArrayInputStream(pdf(4096)), dir, onFailure = { failures += it },
        )
        assertTrue(staged != null)
        assertTrue(failures.isEmpty())
        staged?.delete()
    }

    @Test fun missingOrIncompletePdfPageSizesNeverCreateBlankReplacementNotes() {
        assertNull(PdfImportSafety.completePageSizes(2, null))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf()))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f)))
        assertNull(PdfImportSafety.completePageSizes(2, FloatArray(6)))
        assertNull(PdfImportSafety.completePageSizes(0, FloatArray(0)))
        assertNull(PdfImportSafety.completePageSizes(5_001, FloatArray(10_002)))
        val complete = floatArrayOf(595f, 842f, 612f, 792f)
        assertArrayEquals(complete, PdfImportSafety.completePageSizes(2, complete)!!, 0f)
    }

    @Test fun fullPdfImportRejectsInvalidPageGeometryWithoutBlankPageFallback() {
        val valid = floatArrayOf(595f, 842f, 612f, 792f)
        assertArrayEquals(valid, PdfImportSafety.completePageSizes(2, valid)!!, 0f)
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 0f, 792f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, -1f, 792f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, Float.NaN, 792f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 612f, Float.POSITIVE_INFINITY)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 14_401f, 792f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(0f, 842f, 612f, 792f)))
    }

    @Test fun insertingPdfPagesRequiresCompleteValidGeometry() {
        val valid = floatArrayOf(595f, 842f, 612f, 792f)
        assertArrayEquals(valid, PdfImportSafety.completePageSizes(2, valid)!!, 0f)
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 612f, 792f, 1f)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 612f, Float.NaN)))
        assertNull(PdfImportSafety.completePageSizes(2, floatArrayOf(595f, 842f, 0f, 792f)))
    }

    @Test fun pdfPageSizesAreReadInBoundedExactSlices() {
        val requests = mutableListOf<Pair<Int, Int>>()
        val sizes = PdfImportSafety.readPageSizes(5, 2) { from, count ->
            requests += from to count
            FloatArray(2 * count) { index -> (from + index / 2) * 100f + index % 2 }
        }
        assertEquals(listOf(0 to 2, 2 to 2, 4 to 1), requests)
        assertArrayEquals(
            floatArrayOf(0f, 1f, 100f, 101f, 200f, 201f, 300f, 301f, 400f, 401f),
            sizes!!, 0f,
        )
    }

    @Test fun incompleteOrOversizedNativePageSlicesAreRejected() {
        assertNull(PdfImportSafety.readPageSizes(3, 2) { from, _ ->
            if (from == 0) FloatArray(4) else FloatArray(1)
        })
        assertNull(PdfImportSafety.readPageSizes(3, 2) { from, _ ->
            if (from == 0) FloatArray(4) else FloatArray(4)
        })
        assertNull(PdfImportSafety.readPageSizes(3, 2) { from, _ ->
            if (from == 0) FloatArray(4) else null
        })
    }

    @Test fun untrustedPageCountIsRejectedBeforeNativeReads() {
        assertNull(PdfImportSafety.readPageSizes(0, 128) { _, _ -> error("must not read") })
        assertNull(PdfImportSafety.readPageSizes(5_001, 128) { _, _ -> error("must not read") })
    }

    @Test fun pdfJoinPreflightAllowsValidPagesWithinTheLimit() {
        val one = floatArrayOf(595f, 842f)
        assertTrue(PdfImportSafety.canAppendPageSizes(one, one))
        val base = FloatArray(2 * 4_999) { if (it % 2 == 0) 595f else 842f }
        assertTrue(PdfImportSafety.canAppendPageSizes(base, one))
    }

    @Test fun pdfJoinPreflightRejectsCombinedPageOverflow() {
        val one = floatArrayOf(595f, 842f)
        val base = FloatArray(2 * 4_999) { if (it % 2 == 0) 595f else 842f }
        assertFalse(PdfImportSafety.canAppendPageSizes(base, one + one))
        assertFalse(PdfImportSafety.canAppendPageSizes(one + base, one))
    }

    @Test fun pdfJoinPreflightRejectsMissingOrMalformedPageDimensions() {
        val one = floatArrayOf(595f, 842f)
        assertFalse(PdfImportSafety.canAppendPageSizes(floatArrayOf(), one))
        assertFalse(PdfImportSafety.canAppendPageSizes(one, floatArrayOf()))
        assertFalse(PdfImportSafety.canAppendPageSizes(one + floatArrayOf(612f), one))
        assertFalse(PdfImportSafety.canAppendPageSizes(one, floatArrayOf(612f)))
        assertFalse(PdfImportSafety.canAppendPageSizes(one, floatArrayOf(0f, 842f)))
        assertFalse(PdfImportSafety.canAppendPageSizes(one, floatArrayOf(Float.NaN, 842f)))
        assertFalse(PdfImportSafety.canAppendPageSizes(floatArrayOf(595f, Float.POSITIVE_INFINITY), one))
    }

    @Test fun guardsPageCountAndMalformedPageDimensions() {
        assertFalse(PdfImportSafety.acceptsPageCount(0))
        assertTrue(PdfImportSafety.acceptsPageCount(1))
        assertTrue(PdfImportSafety.acceptsPageCount(5_000))
        assertFalse(PdfImportSafety.acceptsPageCount(5_001))
        assertTrue(PdfImportSafety.validPagePoints(595f, 842f))
        assertFalse(PdfImportSafety.validPagePoints(Float.NaN, 842f))
        assertFalse(PdfImportSafety.validPagePoints(Float.POSITIVE_INFINITY, 842f))
        assertFalse(PdfImportSafety.validPagePoints(1_000_000f, 842f))
        assertFalse(PdfImportSafety.validPagePoints(0f, 842f))
    }
}
