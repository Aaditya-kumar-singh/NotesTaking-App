package com.notestaking.app.platform

import com.notestaking.app.canvas.ViewOverrides
import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.core.model.Document
import com.notestaking.app.format.CanvasCodec
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Simulates interrupted saves with orphan stages; a real process kill and full disk need Android QA. */
class SessionRecoveryFailureTest {
    private val noteCodec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
    private val canvasCodec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())

    @Test fun killedPagedSessionLeavesCommittedNoteAndMetadataReadable() {
        val root = Files.createTempDirectory("note-force-stop").toFile()
        try {
            val pdfDir = File(root, "pdf").apply { mkdirs() }
            val imgDir = File(root, "images").apply { mkdirs() }
            val sessionDir = File(root, "session").apply { mkdirs() }
            fun store() = SessionStore(sessionDir, noteCodec, pdfDir, imgDir)
            assertTrue(store().save(
                Document.blank(2), zoom = 1.5, scrollX = 24.0, scrollY = 32.0,
                zoomLocked = false, viewOverrides = ViewOverrides(), writeDocument = true,
            ))
            val committed = File(sessionDir, "document.xnote")
            val meta = File(sessionDir, "session.json")
            val originalNote = committed.readBytes()
            val originalMeta = meta.readBytes()
            // Force-stop after the temp file starts writing, before ATOMIC_MOVE publishes it.
            val interrupted = File(sessionDir, ".document.xnote.killed.staged").apply {
                writeBytes(originalNote.copyOf(originalNote.size / 2))
            }
            val interruptedMeta = File(sessionDir, ".session.json.killed.staged").apply {
                writeText("{\"zoom\":9,")
            }
            val recovered = store().load() ?: throw AssertionError("Last committed note lost")
            assertEquals(2, recovered.document.pages.size)
            assertEquals(1.5, recovered.zoom, 0.00001)
            assertArrayEquals(originalNote, committed.readBytes())
            assertArrayEquals(originalMeta, meta.readBytes())
            assertTrue(interrupted.exists())
            assertTrue(interruptedMeta.exists())

            assertTrue(store().save(
                Document.blank(3), zoom = 2.0, scrollX = 50.0, scrollY = 60.0,
                zoomLocked = true, viewOverrides = ViewOverrides(), writeDocument = true,
            ))
            assertEquals(3, store().load()!!.document.pages.size)
            assertEquals(2.0, store().load()!!.zoom, 0.00001)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun killedCanvasSessionRestoresLastCommittedCanvasIgnoringPartialStage() {
        val root = Files.createTempDirectory("canvas-force-stop").toFile()
        try {
            val images = File(root, "images").apply { mkdirs() }
            val sessionDir = File(root, "session").apply { mkdirs() }
            fun store() = CanvasSessionStore(sessionDir, canvasCodec, images)
            assertTrue(store().save(InfiniteDocument(dpi = 144).apply {
                displayName = "Last saved drawing"
                dirty = true
            }, writeDocument = true))
            val committed = File(sessionDir, "canvas.xcanvas")
            val meta = File(sessionDir, "canvas-session.json")
            val oldCanvas = committed.readBytes()
            val oldMeta = meta.readBytes()
            File(sessionDir, ".canvas.xcanvas.killed.staged").writeBytes(oldCanvas.copyOf(18))
            File(sessionDir, ".canvas-session.json.killed.staged").writeText("{")
            val restored = store().load() ?: throw AssertionError("Last committed canvas lost")
            assertEquals(144, restored.dpi)
            assertEquals("Last saved drawing", restored.displayName)
            assertTrue(restored.dirty)
            assertArrayEquals(oldCanvas, committed.readBytes())
            assertArrayEquals(oldMeta, meta.readBytes())

            assertTrue(store().save(InfiniteDocument(dpi = 300), writeDocument = true))
            assertEquals(300, store().load()!!.dpi)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun failedOrZeroByteNativeWriteCannotReplaceLastGoodNoteOrCanvas() {
        val root = Files.createTempDirectory("native-low-storage").toFile()
        try {
            val note = File(root, "document.xnote")
            assertTrue(SafeFileWriter.write(note, SafeFileWriter::hasNativeManifest) {
                noteCodec.write(Document.blank(2), it)
            })
            val canvas = File(root, "canvas.xcanvas")
            assertTrue(SafeFileWriter.write(canvas, SafeFileWriter::hasNativeManifest) {
                canvasCodec.write(InfiniteDocument(dpi = 144), it)
            })
            for (target in listOf(note, canvas)) {
                val original = target.readBytes()
                assertFalse(SafeFileWriter.write(target, SafeFileWriter::hasNativeManifest) { output ->
                    output.write(original, 0, original.size / 2)
                    throw IOException("simulated storage exhausted mid-ZIP")
                })
                assertArrayEquals(original, target.readBytes())
                assertFalse(SafeFileWriter.write(target, SafeFileWriter::hasNativeManifest) { })
                assertArrayEquals(original, target.readBytes())
                assertTrue(root.listFiles()!!.none { it.name.endsWith(".staged") })
            }
            note.inputStream().use { assertEquals(2, noteCodec.read(it).pages.size) }
            canvas.inputStream().use { assertEquals(144, canvasCodec.read(it, root).dpi) }
        } finally {
            root.deleteRecursively()
        }
    }
}
