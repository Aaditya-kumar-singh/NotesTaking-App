package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SafeFileWriterTest {
    private fun directory(): File = Files.createTempDirectory("notestaking-atomic-").toFile()

    private fun nativeZip(entryName: String): ByteArray =
        java.io.ByteArrayOutputStream().also { bytes ->
            java.util.zip.ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry(entryName))
                zip.write("{}".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()

    @Test fun borrowedStreamUsesBulkWriteAndLeavesDescriptorOpen() {
        val destination = object : java.io.ByteArrayOutputStream() {
            var singleWrites = 0
            var bulkWrites = 0
            var closed = false
            override fun write(b: Int) {
                singleWrites++
                super.write(b)
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                bulkWrites++
                super.write(b, off, len)
            }
            override fun close() {
                closed = true
                super.close()
            }
        }
        val data = ByteArray(256 * 1024) { (it % 251).toByte() }
        val borrowed = SafeFileWriter.nonClosingStream(destination)
        borrowed.write(data)
        borrowed.close()

        assertEquals(1, destination.bulkWrites)
        assertEquals(0, destination.singleWrites)
        assertFalse(destination.closed)
        assertArrayEquals(data, destination.toByteArray())
    }

    @Test fun emptySuccessfulWriteCannotReplaceLastGoodSnapshot() {
        val dir = directory()
        try {
            val target = File(dir, "document.xnote").apply { writeText("last-good-note") }
            assertFalse(SafeFileWriter.write(target) { /* No bytes from a failed or empty encoder. */ })
            assertEquals("last-good-note", target.readText())
            assertEquals(listOf("document.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun emptySuccessfulFirstSaveLeavesNoBrokenDocument() {
        val dir = directory()
        try {
            val target = File(dir, "document.xnote")
            assertFalse(SafeFileWriter.write(target) { })
            assertFalse(target.exists())
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun truncatedNativeArchiveCannotReplaceLastGoodSnapshot() {
        val dir = directory()
        try {
            val target = File(dir, "document.xnote")
            val original = nativeZip("manifest.json")
            assertTrue(SafeFileWriter.write(target, SafeFileWriter::hasNativeManifest) {
                it.write(original)
            })
            // Losing the end-of-central-directory record still leaves nonempty ZIP bytes.
            val partial = original.copyOf(original.size - 22)
            assertTrue(partial.isNotEmpty())
            assertFalse(SafeFileWriter.write(target, SafeFileWriter::hasNativeManifest) {
                it.write(partial)
            })
            assertArrayEquals(original, target.readBytes())
            assertEquals(listOf("document.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun nativeArchiveMissingManifestCannotReplaceCanvasSnapshot() {
        val dir = directory()
        try {
            val target = File(dir, "canvas.xcanvas")
            val original = nativeZip("manifest.json")
            target.writeBytes(original)
            assertFalse(SafeFileWriter.write(target, SafeFileWriter::hasNativeManifest) {
                it.write(nativeZip("assets/other.bin"))
            })
            assertArrayEquals(original, target.readBytes())
            assertEquals(listOf("canvas.xcanvas"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun codecClosingZipStreamDoesNotCloseFileBeforeSync() {
        val dir = directory()
        try {
            val target = File(dir, "document.xnote")
            assertTrue(SafeFileWriter.write(target) { borrowed ->
                java.util.zip.ZipOutputStream(borrowed).use { zip ->
                    zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                    zip.write("{\"pageCount\":2}".toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            })
            java.util.zip.ZipInputStream(target.inputStream()).use { zip ->
                assertEquals("manifest.json", zip.nextEntry!!.name)
                assertEquals("{\"pageCount\":2}", zip.readBytes().toString(Charsets.UTF_8))
            }
            assertEquals(listOf("document.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun createsAndAtomicallyReplacesCompleteFile() {
        val dir = directory()
        try {
            val target = File(dir, "session.xnote")
            assertTrue(SafeFileWriter.write(target) { it.write("first".toByteArray()) })
            assertEquals("first", target.readText())
            assertTrue(SafeFileWriter.write(target) { it.write("second".toByteArray()) })
            assertEquals("second", target.readText())
            assertEquals(listOf("session.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun failedWriteDoesNotDeletePreviouslyCommittedData() {
        val dir = directory()
        try {
            val target = File(dir, "session.xnote").apply { writeText("last-good-copy") }
            assertFalse(SafeFileWriter.write(target) {
                it.write("partial".toByteArray())
                throw java.io.IOException("disk error after a partial write")
            })
            assertEquals("last-good-copy", target.readText())
            assertEquals(listOf("session.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun failedOverwriteCanBeRetriedWithoutLosingLastGoodNote() {
        val dir = directory()
        try {
            val target = File(dir, "document.xnote").apply { writeText("last-good-note") }
            assertFalse(SafeFileWriter.write(target) { output ->
                output.write("incomplete-note".toByteArray())
                throw java.io.IOException("simulated storage failure")
            })
            assertEquals("last-good-note", target.readText())
            assertTrue(SafeFileWriter.write(target) { it.write("recovered-note".toByteArray()) })
            assertEquals("recovered-note", target.readText())
            assertEquals(listOf("document.xnote"), dir.listFiles()!!.map { it.name })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun firstWriteFailureLeavesNoIncompleteDestination() {
        val dir = directory()
        try {
            val target = File(dir, "new-note.xnote")
            assertFalse(SafeFileWriter.write(target) { throw java.io.IOException("write failed") })
            assertFalse(target.exists())
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun simultaneousWritesNeverInterleaveOrPublishPartialContent() {
        val dir = directory()
        try {
            val target = File(dir, "settings.json")
            val ready = CountDownLatch(2)
            val go = CountDownLatch(1)
            val dataA = "a".repeat(128 * 1024)
            val dataB = "b".repeat(128 * 1024)
            val results = java.util.Collections.synchronizedList(mutableListOf<Boolean>())
            val threads = listOf(dataA, dataB).map { content ->
                Thread {
                    ready.countDown()
                    go.await(5, TimeUnit.SECONDS)
                    results.add(SafeFileWriter.write(target) { it.write(content.toByteArray()) })
                }.apply { start() }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            go.countDown()
            threads.forEach { it.join(10_000) }
            assertTrue(threads.none { it.isAlive })
            assertEquals(listOf(true, true), results.sortedBy { if (it) 0 else 1 })
            assertTrue(target.readText() == dataA || target.readText() == dataB)
            assertEquals(1, dir.listFiles()!!.size)
        } finally {
            dir.deleteRecursively()
        }
    }
}
