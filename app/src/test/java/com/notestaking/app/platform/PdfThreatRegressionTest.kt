package com.notestaking.app.platform

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

import java.nio.file.Files

/** PDFBox fixture checks are JVM-only; PDFium password errors still require native Android CI/device QA. */
class PdfThreatRegressionTest {
    @Test fun passwordProtectedPdfCannotBeParsedWithoutItsPasswordAndStagingPreservesSource() {
        val encrypted = PDDocument().use { doc ->
            doc.addPage(PDPage())
            doc.protect(StandardProtectionPolicy("owner-secret", "viewer-secret", AccessPermission()))
            ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
        }
        val folder = Files.createTempDirectory("pdf-encrypted").toFile()
        try {
            val staged = PdfImportSafety.stage(ByteArrayInputStream(encrypted), folder, maxBytes = encrypted.size.toLong())
                ?: throw AssertionError("Encrypted PDF staging failed")
            assertTrue(staged.isFile)
            assertArrayEquals(encrypted, staged.readBytes())
            assertThrows(InvalidPasswordException::class.java) {
                PDDocument.load(staged).use { }
            }
            PDDocument.load(staged, "viewer-secret").use { decoded ->
                assertEquals(1, decoded.numberOfPages)
            }
            assertArrayEquals(encrypted, staged.readBytes())
            assertTrue(staged.delete())
            assertTrue(folder.listFiles()!!.isEmpty())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test fun pdfiumPasswordAndSecurityErrorsAreNotSilentlyClassifiedAsGenericFormatErrors() {
        assertEquals(PdfOpenError.FILE, pdfOpenErrorOf(2))
        assertEquals(PdfOpenError.FORMAT, pdfOpenErrorOf(3))
        assertEquals(PdfOpenError.PASSWORD, pdfOpenErrorOf(4))
        assertEquals(PdfOpenError.SECURITY, pdfOpenErrorOf(5))
        assertEquals(PdfOpenError.UNKNOWN, pdfOpenErrorOf(0))
        assertEquals(PdfOpenError.UNKNOWN, pdfOpenErrorOf(Int.MAX_VALUE))
    }

    @Test fun fakeHeaderIsNotAParsedPdfAndCannotLeaveAnImportedDocument() {
        val malformed = "%PDF-1.7\n1 0 obj\n<incomplete\n".toByteArray(Charsets.US_ASCII)
        val folder = Files.createTempDirectory("pdf-malformed").toFile()
        try {
            val staged = PdfImportSafety.stage(ByteArrayInputStream(malformed), folder)
                ?: throw AssertionError("Header-level staging must not claim to parse the PDF")
            assertTrue(staged.isFile) // signature scan does not claim structural validity
            assertThrows(Exception::class.java) {
                PDDocument.load(staged).use { }
            }
            assertArrayEquals(malformed, staged.readBytes())
            assertTrue(staged.delete())
            assertTrue(folder.listFiles()!!.isEmpty())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test fun oversizedAndUntrustedPdfBytesNeverLeaveAnImportStagingFile() {
        val prefix = "%PDF-1.7\n".toByteArray(Charsets.US_ASCII)
        val tooLong = prefix + ByteArray(4096)
        val folder = Files.createTempDirectory("pdf-limit").toFile()
        try {
            val errors = ArrayList<PdfImportSafety.StageFailure>()
            val staged = PdfImportSafety.stage(
                ByteArrayInputStream(tooLong), folder, maxBytes = tooLong.size.toLong() - 1,
                onFailure = { errors += it },
            )
            assertNull(staged)
            assertEquals(listOf(PdfImportSafety.StageFailure.TOO_LARGE), errors)
            assertTrue(folder.listFiles()!!.isEmpty())
        } finally {
            folder.deleteRecursively()
        }
    }
}
