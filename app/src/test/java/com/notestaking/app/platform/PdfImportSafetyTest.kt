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
