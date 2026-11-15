package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/** SAF-like stream tests. Real ContentResolver/provider behavior still requires a device. */
class PdfSafSourcePreservationTest {
    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun pdf(): ByteArray = PDDocument().use { document ->
        document.addPage(PDPage(PDRectangle(612f, 792f)).apply { rotation = 90 })
        document.addPage(PDPage(PDRectangle(595f, 842f)))
        ByteArrayOutputStream().also { document.save(it) }.toByteArray()
    }

    private fun hash(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    @Test fun pdfFromReadOnlyProviderCanBeEmbeddedAndReopenedWithoutMutatingOriginal() {
        val dir = Files.createTempDirectory("saf-pdf-import").toFile()
        try {
            val original = java.io.File(dir, "picked.pdf").apply { writeBytes(pdf()) }
            val originalHash = hash(original.readBytes())
            val staging = java.io.File(dir, "staging").apply { mkdirs() }
            val extracted = java.io.File(dir, "reopened").apply { mkdirs() }
            val source = object : ByteArrayInputStream(original.readBytes()) {
                var closed = false
                override fun close() { closed = true; super.close() }
            }
            val staged = PdfImportSafety.stage(source, staging)
                ?: throw AssertionError("Valid PDF should stage")
            assertTrue(source.closed)
            assertArrayEquals(original.readBytes(), staged.readBytes())

            val imported = Document(dpi = 150, pdfFile = staged)
            imported.pages.add(Page(1650.0, 1275.0, pdfPage = 0))
            imported.pages.add(Page(1239.5, 1754.2, pdfPage = 1))
            val native = ByteArrayOutputStream().also { codec.write(imported, it) }.toByteArray()
            assertTrue(NativeNoteReadback.verify(
                { ByteArrayInputStream(native) }, codec, 2, true, hash(native),
            ))
            assertFalse(NativeNoteReadback.verify(
                { ByteArrayInputStream(native.copyOf(native.size - 16)) },
                codec, 2, true, hash(native),
            ))
            val reopened = codec.read(ByteArrayInputStream(native), pdfDir = extracted)
            assertEquals(listOf(0, 1), reopened.pages.map { it.pdfPage })
            val embedded = reopened.pdfFile ?: throw AssertionError("Source PDF lost during native save")
            assertArrayEquals(original.readBytes(), embedded.readBytes())
            PDDocument.load(embedded).use { copy ->
                assertEquals(2, copy.numberOfPages)
                assertEquals(90, copy.getPage(0).rotation)
                assertEquals(0, copy.getPage(1).rotation)
            }
            assertArrayEquals(originalHash, hash(original.readBytes()))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun exportThroughStreamDestinationPreservesImportedPdfAndSourceFiles() {
        val dir = Files.createTempDirectory("saf-pdf-export").toFile()
        try {
            val original = java.io.File(dir, "picked.pdf").apply { writeBytes(pdf()) }
            val originalHash = hash(original.readBytes())
            val scratch = java.io.File(dir, "scratch").apply { mkdirs() }
            val staged = PdfImportSafety.stage(original.inputStream(), dir)
                ?: throw AssertionError("Valid PDF should stage")
            val stagedHash = hash(staged.readBytes())
            val sink = ByteArrayOutputStream()
            var closed = false
            var discarded = false
            val destination = object : PdfBatch.Destination {
                override fun open(): OutputStream = object : OutputStream() {
                    override fun write(b: Int) { sink.write(b) }
                    override fun write(b: ByteArray, off: Int, len: Int) { sink.write(b, off, len) }
                    override fun close() { closed = true }
                }
                override fun discard() { discarded = true }
            }
            val batch = PdfBatch(scratch,
                { _, output, onProgress, isCancelled ->
                    PDDocument.load(staged).use { source ->
                        onProgress(0, source.numberOfPages)
                        source.save(CancellableOutputStream(output, isCancelled))
                        onProgress(source.numberOfPages, source.numberOfPages)
                    }
                }, { false }, {})
            val result = batch.single("picked", "picked.xnote", destination)
            assertTrue(result.kept)
            assertEquals(1, result.written)
            assertTrue(closed)
            assertFalse(discarded)
            PDDocument.load(sink.toByteArray()).use { exported ->
                assertEquals(2, exported.numberOfPages)
                assertEquals(90, exported.getPage(0).rotation)
            }
            assertArrayEquals(originalHash, hash(original.readBytes()))
            assertArrayEquals(stagedHash, hash(staged.readBytes()))
            assertTrue(scratch.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun failedSafOutputMustNotOverwriteSourceOrLeaveScratchPdf() {
        val dir = Files.createTempDirectory("saf-pdf-fail").toFile()
        try {
            val source = java.io.File(dir, "source.pdf").apply { writeBytes(pdf()) }
            val originalHash = hash(source.readBytes())
            val scratch = java.io.File(dir, "scratch").apply { mkdirs() }
            var discarded = false
            var bytesStarted = false
            val destination = object : PdfBatch.Destination {
                override fun open(): OutputStream = object : OutputStream() {
                    override fun write(b: Int) { bytesStarted = true; throw IOException("provider storage full") }
                    override fun write(b: ByteArray, off: Int, len: Int) {
                        bytesStarted = true
                        throw IOException("provider storage full")
                    }
                }
                override fun discard() { discarded = true }
            }
            val batch = PdfBatch(scratch,
                { _, output, _, _ -> PDDocument.load(source).use { it.save(output) } },
                { false }, {})
            val result = batch.single("source", "source.xnote", destination)
            assertFalse(result.kept)
            assertEquals(0, result.written)
            assertTrue(bytesStarted)
            assertTrue(discarded)
            assertArrayEquals(originalHash, hash(source.readBytes()))
            assertTrue(scratch.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun cancelledStagingRemovesOnlyTemporaryPdfAndKeepsProviderSource() {
        val dir = Files.createTempDirectory("saf-pdf-cancel").toFile()
        try {
            val source = java.io.File(dir, "source.pdf").apply { writeBytes(pdf()) }
            val originalHash = hash(source.readBytes())
            val stagedDir = java.io.File(dir, "stage").apply { mkdirs() }
            val cancelled = AtomicBoolean(false)
            val input = object : ByteArrayInputStream(source.readBytes()) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val count = super.read(b, off, len.coerceAtMost(32))
                    if (count > 0) cancelled.set(true)
                    return count
                }
            }
            val staged = PdfImportSafety.stage(input, stagedDir, isCancelled = { cancelled.get() })
            assertNull(staged)
            assertTrue(stagedDir.listFiles()!!.isEmpty())
            assertArrayEquals(originalHash, hash(source.readBytes()))
        } finally {
            dir.deleteRecursively()
        }
    }
}
