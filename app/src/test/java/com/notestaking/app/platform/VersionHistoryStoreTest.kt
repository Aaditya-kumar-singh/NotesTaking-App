package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.format.DocumentCodec
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VersionHistoryStoreTest {
    private fun fixture(block: (File, DocumentCodec, File, File, VersionHistoryStore) -> Unit) {
        val temp = Files.createTempDirectory("xnotes-version-test").toFile()
        try {
            val root = File(temp, "versions")
            val pdfDir = File(temp, "pdf").apply { mkdirs() }
            val imageDir = File(temp, "images").apply { mkdirs() }
            val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
            val store = VersionHistoryStore(root, codec, pdfDir, imageDir)
            block(root, codec, pdfDir, imageDir, store)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test fun capturePreviewAndRestoreOpenUnboundCopyWithoutChangingOriginalWorkingNote() = fixture { _, _, _, _, store ->
        val working = Document.blank(count = 3)
        working.path = "content://one/documents/note-1"
        val entry = store.capture("provider|note-1", working, now = 1760000000000L)!!
        val preview = store.preview("provider|note-1", entry)!!
        assertEquals(entry, preview.entry)
        assertEquals(3, preview.pages)
        assertTrue(preview.digest.length == 64)
        working.pages.removeAt(0)
        val copied = store.restoreCopy("provider|note-1", preview)!!
        assertEquals(3, copied.pages.size)
        assertEquals(2, working.pages.size)
        assertEquals("content://one/documents/note-1", working.path)
        assertNull(copied.path)
        assertNull(copied.displayName)
        assertTrue(copied.dirty)
        assertEquals(1, store.list("provider|note-1").size)
        assertTrue(store.list("provider|unrelated-note").isEmpty())
    }

    @Test fun retentionLimitsVersionCountWhileKeepingOtherNoteVersions() = fixture { _, _, _, _, store ->
        val different = store.capture("provider|other", Document.blank(count = 7), 1760000000000L)!!
        val saved = (1..7).map { n ->
            store.capture("provider|first", Document.blank(count = n), 1760000001000L + n)!!
        }
        val remaining = store.list("provider|first")
        assertEquals(4, remaining.size)
        assertEquals(saved.takeLast(4).map { it.id }.reversed(), remaining.map { it.id })
        assertNull(store.preview("provider|first", saved.first()))
        assertEquals(1, store.list("provider|other").size)
        assertNotNull(store.preview("provider|other", different))
    }

    @Test fun perNoteByteBudgetPrunesOldSnapshotsWithoutTouchingOtherNotes() = fixture { root, codec, pdf, images, store ->
        val unchanged = store.capture("other", Document.blank(count = 3), 1760000000000L)!!
        val base = store.capture("first", Document.blank(count = 2), 1760000001000L)!!
        val budget = base.sizeBytes * 2 + 8
        val bounded = VersionHistoryStore(root, codec, pdf, images, maxNoteBytes = budget, maxVersions = 9)
        repeat(4) { n ->
            assertNotNull(bounded.capture("first", Document.blank(count = 2), 1760000002000L + n))
        }
        val retained = store.list("first")
        assertTrue(retained.sumOf { it.sizeBytes } <= budget)
        assertTrue(retained.size <= 2)
        assertEquals(listOf(unchanged), store.list("other"))
        assertNotNull(store.preview("other", unchanged))
    }

    @Test fun malformedManifestZipIsNotEligibleForPreviewOrRestore() = fixture { root, _, _, _, store ->
        val entry = store.capture("one", Document.blank(count = 2))!!
        val preview = store.preview("one", entry)!!
        val folder = root.listFiles()!!.single()
        val file = File(folder, entry.id)
        java.util.zip.ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zip.write("{invalid json".toByteArray())
            zip.closeEntry()
        }
        assertTrue(store.list("one").isEmpty()) // Reject malformed JSON even when the ZIP index exists.
        assertNull(store.preview("one", entry))
        assertNull(store.restoreCopy("one", preview))
        assertTrue(file.isFile)
    }

    @Test fun byteBudgetFailureLeavesExistingSnapshotAndNoStagedFiles() = fixture { root, codec, pdf, images, store ->
        val note = Document.blank(count = 2)
        val entry = store.capture("same", note)!!
        val original = store.preview("same", entry)!!
        val tight = VersionHistoryStore(root, codec, pdf, images, maxVersionBytes = 96L, maxNoteBytes = 96L)
        assertNull(tight.capture("same", Document.blank(count = 3)))
        assertEquals(listOf(entry), store.list("same"))
        assertEquals(original.digest, store.preview("same", entry)!!.digest)
        assertTrue(root.walkTopDown().none { it.name.endsWith(".staged") })
    }

    @Test fun corruptVersionFailsPreviewAndRestoreWithoutTouchingUnrelatedNotes() = fixture { root, _, _, _, store ->
        val previous = store.capture("one", Document.blank(count = 2), 1760000000000L)!!
        val preview = store.preview("one", previous)!!
        val unrelated = store.capture("two", Document.blank(count = 5), 1760000001000L)!!
        val originalOther = store.preview("two", unrelated)!!
        val folder = root.listFiles()!!.single { it.listFiles()?.any { f -> f.name == previous.id } == true }
        File(folder, previous.id).writeText("bad-native-zip")
        assertTrue(store.list("one").isEmpty())
        assertNull(store.preview("one", previous))
        assertNull(store.restoreCopy("one", preview))
        assertEquals(originalOther.digest, store.preview("two", unrelated)!!.digest)
        assertEquals(5, store.restoreCopy("two", originalOther)!!.pages.size)
    }

    @Test fun obsoleteOrForgedPreviewCannotRestoreAnotherVersion() = fixture { _, _, _, _, store ->
        val saved = store.capture("one", Document.blank(count = 2))!!
        val valid = store.preview("one", saved)!!
        val stale = valid.copy(digest = "0".repeat(64))
        val forged = valid.copy(entry = valid.entry.copy(createdAt = valid.entry.createdAt + 1))
        val traversal = valid.copy(entry = VersionHistoryStore.Entry("../other.xnote", 0L, 100L))
        assertNull(store.restoreCopy("one", stale))
        assertNull(store.restoreCopy("one", forged))
        assertNull(store.restoreCopy("one", traversal))
        assertNull(store.restoreCopy("two", valid))
        assertNotNull(store.restoreCopy("one", valid))
    }

    @Test fun cancellingAtPreviewStageLeavesBothWorkingMemoryAndSourcePdfUntouched() = fixture { root, _, _, _, store ->
        val scratch = Files.createTempDirectory("xnotes-original-pdf").toFile()
        try {
            val pdfFile = File(scratch, "original.pdf")
            PDDocument().use { pdf ->
                pdf.addPage(PDPage(PDRectangle(250f, 350f)))
                pdf.save(pdfFile)
            }
            val originalBytes = pdfFile.readBytes()
            val live = Document.blank(count = 1, dpi = 72)
            live.path = "content://one/note-a"
            live.pdfFile = pdfFile
            live.pages[0].pdfPage = 0
            val entry = store.capture("one", live)!!
            val preview = store.preview("one", entry)!!
            assertEquals(1, preview.pages)
            // User dismisses the confirmation: the preview is read-only; no restoreCopy is called.
            assertEquals("content://one/note-a", live.path)
            assertArrayEquals(originalBytes, pdfFile.readBytes())
            assertEquals(1, store.list("one").size)
            assertFalse(root.walkTopDown().any { it.name.startsWith(".version-preview-") })
            val copy = store.restoreCopy("one", preview)!!
            assertNull(copy.path)
            assertArrayEquals(originalBytes, copy.pdfFile!!.readBytes())
            assertArrayEquals(originalBytes, pdfFile.readBytes())
        } finally {
            scratch.deleteRecursively()
        }
    }
}
