package com.notestaking.app.format

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Tests ZIP budget and integrity failures through both native importers, not only the ZIP stream. */
class HostileArchiveRegressionTest {
    private val note = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
    private val canvas = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun oversizedImageAfterValidImage(): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("assets/image-000.png"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("assets/image-001.png"))
                val chunk = ByteArray(64 * 1024) { 42 }
                repeat(1024) { zip.write(chunk) } // exactly 64 MiB inflated
                zip.write(42) // untrusted extra byte must fail, even when ZIP metadata has size=-1
                zip.closeEntry()
            }
        }.toByteArray()

    private fun corruptedStoredImage(): ByteArray {
        val raw = byteArrayOf(10, 20, 30, 40)
        val name = "assets/image-000.png"
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val crc = CRC32().also { it.update(raw) }
            zip.putNextEntry(ZipEntry(name).apply {
                method = ZipEntry.STORED
                size = raw.size.toLong()
                compressedSize = raw.size.toLong()
                this.crc = crc.value
            })
            zip.write(raw)
            zip.closeEntry()
        }
        val bytes = output.toByteArray()
        // Mutate a STORED entry's bytes but not its declared CRC.
        fun u16(at: Int) = (bytes[at].toInt() and 255) or ((bytes[at + 1].toInt() and 255) shl 8)
        val at = 30 + u16(26) + u16(28) // local filename and extra lengths, not guessed offsets
        bytes[at] = (bytes[at].toInt() xor 1).toByte()
        return bytes
    }

    private fun assertOnlySentinelRemains(source: ByteArray) {
        val dir = Files.createTempDirectory("hostile-zip").toFile()
        try {
            val sentinel = java.io.File(dir, "keep.txt").apply { writeText("unchanged") }
            val original = source.clone()
            assertThrows(IOException::class.java) {
                note.read(ByteArrayInputStream(source), imageDir = dir)
            }
            assertEquals(listOf("keep.txt"), dir.listFiles()!!.map { it.name })
            assertEquals("unchanged", sentinel.readText())
            assertThrows(IOException::class.java) {
                canvas.read(ByteArrayInputStream(source), imageDir = dir)
            }
            assertEquals(listOf("keep.txt"), dir.listFiles()!!.map { it.name })
            assertEquals("unchanged", sentinel.readText())
            org.junit.Assert.assertArrayEquals(original, source)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun highlyCompressedImageOverBudgetNeverLeavesPartialImportedAssets() {
        assertOnlySentinelRemains(oversizedImageAfterValidImage())
    }

    @Test fun corruptStoredImageCrcNeverLeavesPartialImportedAssets() {
        assertOnlySentinelRemains(corruptedStoredImage())
    }
}
