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
import java.util.zip.ZipOutputStream

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
