package com.notestaking.app.platform

import com.notestaking.app.core.util.DocumentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files

class QuickScratchCreationTest {
    @Test fun missingOrCancelledStorageDoesNotCreateAnything() {
        var calls = 0
        val write: (String, String) -> String? = { _, _ -> calls++; "made" }
        assertNull(QuickScratchCreation.create(null, "folder", write))
        assertNull(QuickScratchCreation.create("tree", null, write))
        assertNull(QuickScratchCreation.create("", "folder", write))
        assertNull(QuickScratchCreation.create("tree", " ", write))
        assertEquals(0, calls)
    }

    @Test fun successfulCreationReturnsPersistedUriAndSupportsReopen() {
        val folder = Files.createTempDirectory("quick-scratch-").toFile()
        try {
            val original = folder.resolve("existing.xnote").apply { writeText("unchanged") }
            val uri = QuickScratchCreation.create("content://tree", "folder") { _, _ ->
                val name = DocumentNames.unique("Untitled", DocumentKind.NOTE, folder.list()!!.toList())
                val made = folder.resolve(name)
                made.writeText("persisted blank note fixture")
                made.toURI().toString()
            }
            val made = java.io.File(java.net.URI(uri!!))
            assertTrue(made.isFile)
            assertEquals("persisted blank note fixture", made.readText())
            assertEquals("unchanged", original.readText())
            assertEquals("Untitled.xnote", made.name)
            assertFalse(made.readText().isEmpty())
        } finally { folder.deleteRecursively() }
    }

    @Test fun duplicateNamesDoNotOverwriteOldNotes() {
        val original = listOf("Untitled.xnote", "Untitled_1.xnote")
        assertEquals("Untitled_2.xnote", DocumentNames.unique("Untitled", DocumentKind.NOTE, original))
        assertEquals("Untitled_3.xnote", DocumentNames.unique("Untitled", DocumentKind.NOTE, original + "Untitled_2.xnote"))
    }

    @Test fun refusedOrStalledProviderDoesNotReturnAFalseCreateOrMutateOtherNotes() {
        val folder = Files.createTempDirectory("quick-scratch-refused-").toFile()
        try {
            val old = folder.resolve("old.xnote").apply { writeBytes(byteArrayOf(3, 1, 4)) }
            val saved = old.readBytes()
            val errors: List<Exception> = listOf(SecurityException("revoked"), IllegalArgumentException("bad tree"), IOException("provider timeout"))
            for (failure in errors) {
                assertNull(QuickScratchCreation.create("content://tree", "folder") { _, _ -> throw failure })
                assertTrue(saved.contentEquals(old.readBytes()))
            }
            assertNull(QuickScratchCreation.create("content://tree", "folder") { _, _ -> null })
            assertTrue(saved.contentEquals(old.readBytes()))
        } finally { folder.deleteRecursively() }
    }
}
