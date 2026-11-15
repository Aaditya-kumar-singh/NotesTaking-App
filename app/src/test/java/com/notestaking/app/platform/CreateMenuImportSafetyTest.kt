package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.format.DocumentCodec
import com.notestaking.app.format.XNoteFormatException
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Create > Import PDF is deliberately distinct from opening an editable .xnote. */
class CreateMenuImportSafetyTest {
    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun docxBytes(): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip ->
            for ((name, payload) in listOf(
                "[Content_Types].xml" to "<Types/>",
                "word/document.xml" to "<document><body>Not a PDF</body></document>",
            )) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(payload.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }.toByteArray()

    @Test fun aValidPdfCanBeStagedWithoutAlteringThePickedInput() {
        val original = PDDocument().use { pdf ->
            pdf.addPage(PDPage())
            ByteArrayOutputStream().also { pdf.save(it) }.toByteArray()
        }
        val dir = Files.createTempDirectory("create-pdf").toFile()
        try {
            val received = ArrayList<PdfImportSafety.StageFailure>()
            val staged = PdfImportSafety.stage(
                ByteArrayInputStream(original), dir, onFailure = { received.add(it) },
            ) ?: throw AssertionError("A valid PDF should reach the PDF importer")
            assertArrayEquals(original, staged.readBytes())
            PDDocument.load(staged).use { assertEquals(1, it.numberOfPages) }
            assertTrue(received.isEmpty())
            assertArrayEquals(original, staged.readBytes())
            assertTrue(staged.delete())
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun docxAndOtherUnsupportedFilesFailThePdfImportBeforeCreatingANote() {
        val dir = Files.createTempDirectory("create-not-pdf").toFile()
        try {
            val docx = docxBytes()
            val failures = ArrayList<PdfImportSafety.StageFailure>()
            for (source in listOf(
                docx,
                "This is an unsupported DOCX".toByteArray(Charsets.US_ASCII),
                byteArrayOf(0x50, 0x4b, 0x03, 0x04),
            )) {
                val unchanged = source.copyOf()
                assertNull(PdfImportSafety.stage(
                    ByteArrayInputStream(source), dir,
                    onFailure = { failures.add(it) },
                ))
                assertArrayEquals(unchanged, source)
                assertTrue(dir.listFiles()!!.isEmpty())
            }
            assertEquals(List(3) { PdfImportSafety.StageFailure.INVALID_PDF }, failures)
            assertThrows(XNoteFormatException::class.java) {
                codec.read(ByteArrayInputStream(docx))
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun nativeNoteHasItsOwnEditableOpenRouteAndCannotMasqueradeAsAPdf() {
        val native = ByteArrayOutputStream().also {
            codec.write(Document.blank(count = 2), it)
        }.toByteArray()
        val dir = Files.createTempDirectory("create-native").toFile()
        try {
            assertEquals(2, codec.read(ByteArrayInputStream(native)).pages.size)
            val failed = ArrayList<PdfImportSafety.StageFailure>()
            assertNull(PdfImportSafety.stage(
                ByteArrayInputStream(native), dir,
                onFailure = { failed.add(it) },
            ))
            assertEquals(listOf(PdfImportSafety.StageFailure.INVALID_PDF), failed)
            assertTrue(dir.listFiles()!!.isEmpty())
            assertEquals(2, codec.read(ByteArrayInputStream(native)).pages.size)
        } finally {
            dir.deleteRecursively()
        }
    }
}
