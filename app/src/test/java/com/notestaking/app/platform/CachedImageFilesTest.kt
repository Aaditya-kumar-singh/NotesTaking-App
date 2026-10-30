package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CachedImageFilesTest {
    @Test
    fun repeatedCopyAndShareNeverOverwriteEarlierUris() {
        val dir = Files.createTempDirectory("image-exports").toFile()
        try {
            val first = CachedImageFiles.create(dir, "../same/label") { it.writeText("first"); true }!!
            val second = CachedImageFiles.create(dir, "../same/label") { it.writeText("second"); true }!!
            assertNotEquals(first.absolutePath, second.absolutePath)
            assertEquals("first", first.readText())
            assertEquals("second", second.readText())
            assertEquals(dir.canonicalPath, first.canonicalFile.parent)
            assertEquals(dir.canonicalPath, second.canonicalFile.parent)
            assertFalse(first.name.contains('/'))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun failedOrEmptyEncodesRemovePartialFiles() {
        val dir = Files.createTempDirectory("image-failures").toFile()
        try {
            assertNull(CachedImageFiles.create(dir, "empty") { true })
            assertNull(CachedImageFiles.create(dir, "false") { it.writeText("partial"); false })
            assertNull(CachedImageFiles.create(dir, "throws") { it.writeText("partial"); error("encode failed") })
            assertTrue(dir.listFiles().isNullOrEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
