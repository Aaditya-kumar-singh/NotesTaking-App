package com.notestaking.app.platform

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
