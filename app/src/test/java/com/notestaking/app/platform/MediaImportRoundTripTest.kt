package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.model.AudioData
import com.notestaking.app.core.model.AudioItem
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.ImageData
import com.notestaking.app.core.model.ImageItem
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class MediaImportRoundTripTest {
    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    @Test fun importedImageAndAudioSurviveNativeSaveAfterProviderCopiesAreRemoved() {
        val dir = Files.createTempDirectory("media-import").toFile()
        try {
            val imageBytes = byteArrayOf(1, 4, 9, 16)
            val audioBytes = byteArrayOf(24, 18, 12, 6, 0)
            val sourceImage = File(dir, "picked.png").apply {
                writeBytes(BoundedBytes.read(ByteArrayInputStream(imageBytes)))
            }
            val sourceAudio = File(dir, "picked.ogg").apply {
                outputStream().use { out ->
                    assertEquals(audioBytes.size.toLong(), BoundedBytes.copy(
                        ByteArrayInputStream(audioBytes), out, 256L,
                    ))
                }
            }
            val doc = Document.blank(count = 1)
            doc.pages[0].items += ImageItem(
                ImageData(sourceImage, 32, 24), Rect(10.0, 20.0, 120.0, 90.0),
            )
            doc.pages[0].items += AudioItem(
                AudioData(sourceAudio, "ogg", 1_250, byteArrayOf(5, 7, 9)),
                Rect(30.0, 60.0, 220.0, 80.0), "picked audio", 17, FakeTextMeasurer(),
            )

            val saved = ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
            assertFalse(saved.isEmpty())
            sourceImage.delete()
            sourceAudio.delete()

            val extractedDir = File(dir, "reopened").apply { mkdirs() }
            val reopened = codec.read(ByteArrayInputStream(saved), imageDir = extractedDir)
            assertEquals(1, reopened.pages.size)
            val items = reopened.pages.single().items
            assertEquals(2, items.size)
            val image = items[0] as ImageItem
            val audio = items[1] as AudioItem
            assertArrayEquals(imageBytes, image.image.file.readBytes())
            assertEquals(Rect(10.0, 20.0, 120.0, 90.0), image.rect)
            assertArrayEquals(audioBytes, audio.audio.file.readBytes())
            assertEquals("ogg", audio.audio.ext)
            assertEquals(1_250, audio.audio.durationMs)
            assertArrayEquals(byteArrayOf(5, 7, 9), audio.audio.peaks)
            assertEquals("picked audio", audio.name)
            assertEquals(17, audio.rid)
        } finally {
            dir.deleteRecursively()
        }
    }
}
