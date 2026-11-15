package com.notestaking.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class PdfSaveProofTest {
    private val complete = ("%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n" +
        "startxref\n0\n%%EOF\n").toByteArray(Charsets.US_ASCII)

    @Test fun renderCannotAdvanceToPickerWithMissingHeaderOrFooter() {
        val dir = Files.createTempDirectory("pdf-save-proof").toFile()
        try {
            val file = dir.resolve("render.pdf")
            assertFalse(PdfSaveProof.rendered(file))
            file.writeBytes(byteArrayOf())
            assertFalse(PdfSaveProof.rendered(file))
            file.writeBytes(complete.copyOf(complete.size - 6))
            assertFalse(PdfSaveProof.rendered(file))
            file.writeBytes("not pdf\n%%EOF\n".toByteArray())
            assertFalse(PdfSaveProof.rendered(file))
            file.writeBytes(complete)
            assertTrue(PdfSaveProof.rendered(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun providerReadbackMustMatchExactlyToReportSuccess() {
        val dir = Files.createTempDirectory("pdf-provider-proof").toFile()
        try {
            val file = dir.resolve("render.pdf").apply { writeBytes(complete) }
            assertTrue(PdfSaveProof.matches(file) { ByteArrayInputStream(complete) })
            assertFalse(PdfSaveProof.matches(file) { ByteArrayInputStream(complete.copyOf(complete.size - 3)) })
            assertFalse(PdfSaveProof.matches(file) { ByteArrayInputStream(complete + byteArrayOf(1)) })
            assertFalse(PdfSaveProof.matches(file) { ByteArrayInputStream(complete.copyOf().apply { this[8] = 42 }) })
            assertFalse(PdfSaveProof.matches(file) { null })
            assertFalse(PdfSaveProof.matches(file) { throw java.io.IOException("permission revoked") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun stalledProviderCannotClaimACompletedPdf() {
        val dir = Files.createTempDirectory("pdf-stalled-provider").toFile()
        try {
            val file = dir.resolve("render.pdf").apply { writeBytes(complete) }
            assertFalse(PdfSaveProof.matches(file) {
                object : ByteArrayInputStream(complete) {
                    private var first = true
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (first) { first = false; return super.read(b, off, len) }
                        return 0
                    }
                }
            })
        } finally {
            dir.deleteRecursively()
        }
    }
}
