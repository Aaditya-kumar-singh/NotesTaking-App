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

    @Test fun metadataOnlySaveRejectsEmptyCanvasAndFullSaveCanRecover() {
        val dir = Files.createTempDirectory("notestaking-canvas-empty").toFile()
        val images = Files.createTempDirectory("notestaking-canvas-empty-images").toFile()
        try {
            val store = CanvasSessionStore(dir, codec(), images)
            assertTrue(store.save(InfiniteDocument(dpi = 144), writeDocument = true))
            val sidecar = dir.resolve("canvas-session.json")
            val originalMetadata = sidecar.readBytes()
            val slot = dir.resolve("canvas.xcanvas")
            assertTrue(slot.delete())
            assertTrue(slot.createNewFile())

            assertFalse(store.save(InfiniteDocument(dpi = 300), writeDocument = false))
            assertEquals(0L, slot.length())
            assertArrayEquals(originalMetadata, sidecar.readBytes())
            assertTrue(store.save(InfiniteDocument(dpi = 300), writeDocument = true))
            assertEquals(300, store.load()!!.dpi)
        } finally {
            dir.deleteRecursively()
            images.deleteRecursively()
        }
    }

    @Test fun metadataOnlySaveRejectsCorruptNonemptyCanvasAndFullSaveRepairs() {
        val dir = Files.createTempDirectory("notestaking-canvas-corrupt").toFile()
        val images = Files.createTempDirectory("notestaking-canvas-corrupt-images").toFile()
        try {
            val store = CanvasSessionStore(dir, codec(), images)
            assertTrue(store.save(InfiniteDocument(dpi = 144), writeDocument = true))
            val sidecar = dir.resolve("canvas-session.json")
            val oldMetadata = sidecar.readBytes()
            val slot = dir.resolve("canvas.xcanvas")
            slot.writeText("nonempty but not a canvas ZIP")

            assertFalse(store.save(InfiniteDocument(dpi = 200), writeDocument = false))
            assertArrayEquals(oldMetadata, sidecar.readBytes())
            assertEquals("nonempty but not a canvas ZIP", slot.readText())
            val withoutManifest = java.io.ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(withoutManifest).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("assets/unused.bin"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
            slot.writeBytes(withoutManifest.toByteArray())
            assertFalse(store.save(InfiniteDocument(dpi = 200), writeDocument = false))
            assertArrayEquals(oldMetadata, sidecar.readBytes())
            assertArrayEquals(withoutManifest.toByteArray(), slot.readBytes())
            val malformed = java.io.ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(malformed).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                zip.write("{\"format\":\"xcanvas\",\"items\":[broken]}".toByteArray())
                zip.closeEntry()
            }
            slot.writeBytes(malformed.toByteArray())
            assertFalse(store.save(InfiniteDocument(dpi = 200), writeDocument = false))
            assertArrayEquals(oldMetadata, sidecar.readBytes())
            assertArrayEquals(malformed.toByteArray(), slot.readBytes())
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".staged") })
            assertTrue(store.save(InfiniteDocument(dpi = 300), writeDocument = true))
            assertEquals(300, store.load()!!.dpi)
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

    @Test fun blockedFullSaveKeepsLegacyCanvasUntilRenameRetry() {
        val dir = Files.createTempDirectory("notestaking-legacy-canvas-retry").toFile()
        val images = Files.createTempDirectory("notestaking-legacy-canvas-images").toFile()
        try {
            val store = CanvasSessionStore(dir, codec(), images)
            val legacy = InfiniteDocument(dpi = 144).apply {
                path = "content://test.documents/tree/root/document/root%2FBoard.xcanvas"
                displayName = "Board.xcanvas"
                dirty = true
            }
            assertTrue(store.save(legacy, writeDocument = true))
            val slot = dir.resolve("canvas.xcanvas")
            val lastGoodArchive = slot.readBytes()
            val sidecar = dir.resolve("canvas-session.json")
            val lastGoodMetadata = sidecar.readBytes()

            // Ordinary rename must keep the old extension, even if the UI proposes the new one.
            val renamedName = DocumentNames.canvasRename("Board.xcanvas", "Renamed Board.ntcanvas")
            assertEquals("Renamed Board.xcanvas", renamedName)
            val renamed = InfiniteDocument(dpi = 300).apply {
                path = "content://test.documents/tree/root/document/root%2FRenamed%20Board.xcanvas"
                displayName = renamedName
                dirty = true
            }
            assertTrue(sidecar.delete())
            assertTrue(sidecar.mkdir()) // Simulate a blocked sidecar before the full save begins.
            assertFalse(store.save(renamed, writeDocument = true))
            assertArrayEquals(lastGoodArchive, slot.readBytes())
            assertTrue(sidecar.isDirectory)

            // Restore the disposable test sidecar to simulate removal of the storage fault.
            assertTrue(sidecar.delete())
            sidecar.writeBytes(lastGoodMetadata)
            val recovered = store.load()!!
            assertEquals(144, recovered.dpi)
            assertEquals(legacy.path, recovered.path)
            assertEquals(legacy.displayName, recovered.displayName)

            assertTrue(store.save(renamed, writeDocument = true))
            val reopened = store.load()!!
            assertEquals(300, reopened.dpi)
            assertEquals(renamed.path, reopened.path)
            assertEquals("Renamed Board.xcanvas", reopened.displayName)
            assertTrue(SafeFileWriter.hasNativeManifest(slot))
            assertFalse(dir.resolve("canvas.ntcanvas").exists())
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".staged") })
        } finally {
            dir.deleteRecursively()
            images.deleteRecursively()
        }
    }

    @Test fun newCanvasUsesLegacyPrivateRecoverySlotDuringMetadataOnlySave() {
        val dir = Files.createTempDirectory("notestaking-new-canvas-recovery").toFile()
        val images = Files.createTempDirectory("notestaking-new-canvas-images").toFile()
        try {
            val store = CanvasSessionStore(dir, codec(), images)
            val created = InfiniteDocument(dpi = 144).apply {
                path = "content://test.documents/tree/root/document/root%2FBoard.ntcanvas"
                displayName = "Board.ntcanvas"
            }
            assertTrue(store.save(created, writeDocument = true))
            val slot = dir.resolve("canvas.xcanvas")
            val archive = slot.readBytes()
            val renamed = InfiniteDocument(dpi = 300).apply {
                path = "content://test.documents/tree/root/document/root%2FBoard%20Two.ntcanvas"
                displayName = "Board Two.ntcanvas"
            }
            assertTrue(store.save(renamed, writeDocument = false))
            val reopened = store.load()!!
            assertEquals(144, reopened.dpi) // Metadata-only save does not rewrite the canvas.
            assertEquals(renamed.path, reopened.path)
            assertEquals(renamed.displayName, reopened.displayName)
            assertArrayEquals(archive, slot.readBytes())
            assertFalse(dir.resolve("canvas.ntcanvas").exists())
            assertTrue(dir.listFiles()!!.none { it.name.endsWith(".staged") })
        } finally {
            dir.deleteRecursively()
            images.deleteRecursively()
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
