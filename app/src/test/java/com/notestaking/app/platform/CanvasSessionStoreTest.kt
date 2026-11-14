package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.format.CanvasCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CanvasSessionStoreTest {
    private fun codec() = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())

    @Test fun savesAndRestoresAClosedZipStream() {
        val sessionDir = Files.createTempDirectory("notestaking-canvas-session").toFile()
        val imageDir = Files.createTempDirectory("notestaking-canvas-images").toFile()
        try {
            val canvas = InfiniteDocument().apply {
                displayName = "Recovery canvas"
                dirty = true
            }
            val store = CanvasSessionStore(sessionDir, codec(), imageDir)
            assertTrue(store.save(canvas, writeDocument = true))
            val restored = store.load()!!
            assertEquals("Recovery canvas", restored.displayName)
            assertTrue(restored.dirty)
            assertTrue(sessionDir.resolve("canvas.xcanvas").isFile)
        } finally {
            sessionDir.deleteRecursively()
            imageDir.deleteRecursively()
        }
    }

    @Test fun metadataOnlySaveRejectsBlockedCanvasSlotWithoutChangingSidecar() {
        val dir = Files.createTempDirectory("notestaking-canvas-slot").toFile()
        val images = Files.createTempDirectory("notestaking-canvas-slot-images").toFile()
        try {
            val store = CanvasSessionStore(dir, codec(), images)
            assertTrue(store.save(InfiniteDocument(dpi = 144), writeDocument = true))
            val sidecar = dir.resolve("canvas-session.json")
            val originalMetadata = sidecar.readBytes()
            val slot = dir.resolve("canvas.xcanvas")
            assertTrue(slot.delete())
            assertTrue(slot.mkdir())

            assertFalse(store.save(InfiniteDocument(dpi = 300), writeDocument = false))
            assertTrue(slot.isDirectory)
            assertArrayEquals(originalMetadata, sidecar.readBytes())
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".staged") })
        } finally {
            dir.deleteRecursively()
            images.deleteRecursively()
        }
    }

    @Test fun blockedMetadataCannotOverwriteLastGoodCanvas() {
        val sessionDir = Files.createTempDirectory("notestaking-canvas-metadata").toFile()
        val imageDir = Files.createTempDirectory("notestaking-canvas-images").toFile()
        try {
            val store = CanvasSessionStore(sessionDir, codec(), imageDir)
            val old = InfiniteDocument(dpi = 144).apply { displayName = "Last good canvas" }
            assertTrue(store.save(old, writeDocument = true))
            val canvasFile = sessionDir.resolve("canvas.xcanvas")
            val oldDocument = canvasFile.readBytes()
            val metadataFile = sessionDir.resolve("canvas-session.json")
            val oldMetadata = metadataFile.readBytes()
            assertTrue(metadataFile.delete())
            assertTrue(metadataFile.mkdir()) // A provider/filesystem issue blocks sidecar replacement.
            assertFalse(store.save(InfiniteDocument(dpi = 300), writeDocument = true))
            assertArrayEquals(oldDocument, canvasFile.readBytes())
            assertTrue(metadataFile.delete())
            metadataFile.writeBytes(oldMetadata)
            assertEquals(144, store.load()!!.dpi)
            assertEquals("Last good canvas", store.load()!!.displayName)
            assertTrue(store.save(InfiniteDocument(dpi = 300), writeDocument = true))
            assertEquals(300, store.load()!!.dpi)
            assertTrue(sessionDir.listFiles()!!.none { it.name.endsWith(".staged") })
        } finally {
            sessionDir.deleteRecursively()
            imageDir.deleteRecursively()
        }
    }

    @Test fun reportsFailureWithoutRemovingBlockedCanvasSlot() {
        val sessionDir = Files.createTempDirectory("notestaking-canvas-block").toFile()
        val imageDir = Files.createTempDirectory("notestaking-canvas-images").toFile()
        try {
            val blocked = sessionDir.resolve("canvas.xcanvas").apply { mkdir() }
            val store = CanvasSessionStore(sessionDir, codec(), imageDir)
            assertFalse(store.save(InfiniteDocument(), writeDocument = true))
            assertTrue(blocked.isDirectory)
        } finally {
            sessionDir.deleteRecursively()
            imageDir.deleteRecursively()
        }
    }
}
