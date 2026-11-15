package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NativeNoteReadbackTest {
    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
    private fun fingerprint(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun archive(pages: Int = 2, pdf: Boolean = false, contents: String = "independent image data"): ByteArray {
        val pageJson = List(pages) { """{"width":200,"height":300}""" }.joinToString(",")
        val manifest = """{"format":"xnote","has_pdf":$pdf,"pages":[$pageJson]}""".toByteArray(Charsets.UTF_8)
        val asset = contents.toByteArray(Charsets.US_ASCII)
        return ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                val entry = ZipEntry("assets/image-000.png").apply {
                    method = ZipEntry.STORED
                    size = asset.size.toLong()
                    compressedSize = size
                    crc = CRC32().apply { update(asset) }.value
                }
                zip.putNextEntry(entry)
                zip.write(asset)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest)
                zip.closeEntry()
            }
        }.toByteArray()
    }

    @Test fun intactNoteCanBeReadBackBeforeOriginalPagesAreRemoved() {
        val bytes = archive()
        val source = object : ByteArrayInputStream(bytes) {
            var closed = false
            override fun close() { closed = true; super.close() }
        }
        assertTrue(NativeNoteReadback.verify({ source }, codec, 2, false, fingerprint(bytes)))
        assertTrue(source.closed)
    }

    @Test fun absentStreamIncorrectPagesAndPdfFlagFailClosed() {
        val bytes = archive()
        val digest = fingerprint(bytes)
        assertFalse(NativeNoteReadback.verify({ null }, codec, 2, false, digest))
        assertFalse(NativeNoteReadback.verify({ ByteArrayInputStream(bytes) }, codec, 1, false, digest))
        assertFalse(NativeNoteReadback.verify({ ByteArrayInputStream(bytes) }, codec, 2, true, digest))
        assertFalse(NativeNoteReadback.verify({ ByteArrayInputStream("not a note".toByteArray()) }, codec, 2, false, digest))
    }

    @Test fun damagedStoredAssetFailsEvenWhenExpectedHashMatchesDamagedBytes() {
        val bytes = archive().copyOf()
        val asset = "independent image data".toByteArray(Charsets.US_ASCII)
        val start = bytes.indices.first { i ->
            i + asset.size <= bytes.size && asset.indices.all { bytes[i + it] == asset[it] }
        }
        bytes[start] = (bytes[start].toInt() xor 1).toByte()
        assertFalse(NativeNoteReadback.verify({ ByteArrayInputStream(bytes) }, codec, 2, false, fingerprint(bytes)))
    }

    @Test fun validButWrongZipBytesNeverAuthorizeDeletingOriginalPages() {
        val written = archive(contents = "original contents")
        val returned = archive(contents = "different contents")
        assertFalse(NativeNoteReadback.verify(
            { ByteArrayInputStream(returned) }, codec, 2, false, fingerprint(written),
        ))
    }

    @Test fun shortProviderStreamAndReadErrorsDoNotAuthorizeMove() {
        val bytes = archive()
        assertFalse(NativeNoteReadback.verify(
            { ByteArrayInputStream(bytes.copyOf(64)) }, codec, 2, false, fingerprint(bytes),
        ))
        val failing = object : ByteArrayInputStream(bytes) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                throw IOException("cloud provider interrupted read")
            }
        }
        assertFalse(NativeNoteReadback.verify({ failing }, codec, 2, false, fingerprint(bytes)))
    }

    @Test fun cancelledReadbackNeverAuthorizesSourceDeletion() {
        val bytes = archive()
        var checks = 0
        assertFalse(NativeNoteReadback.verify(
            { ByteArrayInputStream(bytes) }, codec, 2, false, fingerprint(bytes),
            { ++checks > 2 },
        ))
    }
}
