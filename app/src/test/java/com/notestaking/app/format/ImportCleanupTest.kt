package com.notestaking.app.format

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.infinite.InfiniteDocument

class ImportCleanupTest {
    private fun malformedBundle(): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("assets/image-000.png"))
                zip.write(byteArrayOf(1, 2, 3, 4))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("../invalid"))
                zip.write(byteArrayOf(42))
                zip.closeEntry()
            }
        }.toByteArray()

    /** Valid document with an unreferenced embedded image; no open note may retain its temp file. */
    private fun withOrphanImage(bundle: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { destination ->
            ZipInputStream(ByteArrayInputStream(bundle)).use { source ->
                while (true) {
                    val entry = source.nextEntry ?: break
                    destination.putNextEntry(ZipEntry(entry.name))
                    source.copyTo(destination)
                    destination.closeEntry()
                }
            }
            destination.putNextEntry(ZipEntry("assets/image-unused.png"))
            destination.write(byteArrayOf(1, 2, 3, 4))
            destination.closeEntry()
        }
        return output.toByteArray()
    }

    @Test fun successfulNoteImportDeletesOrphanImages() {
        val dir = Files.createTempDirectory("note-success-cleanup").toFile()
        try {
            val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
            val output = ByteArrayOutputStream()
            codec.write(Document(), output)
            codec.read(ByteArrayInputStream(withOrphanImage(output.toByteArray())), imageDir = dir)
            assertTrue("orphan files must not survive a successful note load", dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun successfulCanvasImportDeletesOrphanImages() {
        val dir = Files.createTempDirectory("canvas-success-cleanup").toFile()
        try {
            val codec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())
            val output = ByteArrayOutputStream()
            codec.write(InfiniteDocument(), output)
            codec.read(ByteArrayInputStream(withOrphanImage(output.toByteArray())), imageDir = dir)
            assertTrue("orphan files must not survive a successful canvas load", dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun failedNoteImportRemovesTemporaryImage() {
        val dir = Files.createTempDirectory("note-import-cleanup").toFile()
        try {
            val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
            assertThrows(IOException::class.java) {
                codec.read(ByteArrayInputStream(malformedBundle()), imageDir = dir)
            }
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun failedCanvasImportRemovesTemporaryImage() {
        val dir = Files.createTempDirectory("canvas-import-cleanup").toFile()
        try {
            val codec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())
            assertThrows(IOException::class.java) {
                codec.read(ByteArrayInputStream(malformedBundle()), imageDir = dir)
            }
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
