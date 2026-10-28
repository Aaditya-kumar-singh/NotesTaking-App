package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.format.CanvasCodec
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
