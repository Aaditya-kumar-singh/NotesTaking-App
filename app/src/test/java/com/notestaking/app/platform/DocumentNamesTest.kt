package com.notestaking.app.platform

import com.notestaking.app.core.util.DocumentKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentNamesTest {
    @Test fun typedNotebookTitlesAreSanitizedWithoutChangingUnicodeWords() {
        assertEquals("Study notes.xnote", DocumentNames.unique(" .Study: notes?.xnote ", DocumentKind.NOTE, emptySet()))
        assertEquals("पढ़ाई नोट्स.xnote", DocumentNames.unique("पढ़ाई नोट्स", DocumentKind.NOTE, emptySet()))
        assertEquals("untitled.xnote", DocumentNames.unique(" /\\:*?<>| ", DocumentKind.NOTE, emptySet()))
        assertEquals("Draft.xcanvas", DocumentNames.unique("Draft.XCANVAS", DocumentKind.CANVAS, emptySet()))
        assertEquals("abc.xnote", DocumentNames.unique("ab\u007fc", DocumentKind.NOTE, emptySet()))
    }

    @Test fun windowsDeviceNamesAndDotPathsNeverBecomeReservedFiles() {
        for (raw in listOf("CON", "con.txt", "PRN", "NUL", "LPT9", "COM1", "../CON")) {
            val chosen = DocumentNames.unique(raw, DocumentKind.NOTE, emptySet())
            assertTrue(chosen.endsWith(".xnote"))
            assertFalse(chosen.substringBefore('.').uppercase() in setOf("CON", "PRN", "NUL", "LPT9", "COM1"))
            assertFalse(chosen.contains('/'))
            assertFalse(chosen.contains('\\'))
        }
        assertEquals("_con.xnote", DocumentNames.unique("con", DocumentKind.NOTE, emptySet()))
    }

    @Test fun duplicateNamesGetDeterministicCaseInsensitiveSuffixesWithoutOverwriting() {
        val existing = setOf("Notes.xnote", "notes_1.XNOTE", "notes_2.xnote", "Other.xcanvas")
        val chosen = DocumentNames.unique("notes.XNOTE", DocumentKind.NOTE, existing)
        assertEquals("notes_3.xnote", chosen)
        assertFalse(existing.any { it.equals(chosen, ignoreCase = true) })
        assertEquals("notes.xcanvas", DocumentNames.unique("notes", DocumentKind.CANVAS, existing))
        assertEquals("untitled_2.xnote", DocumentNames.unique("", DocumentKind.NOTE, listOf("untitled.xnote", "untitled_1.xnote")))
    }

    @Test fun filenamesStayUnderProviderByteLimitEvenWithLongUnicodeAndSuffixes() {
        val stem = DocumentNames.stem("界".repeat(500) + "😀")
        assertTrue(stem.toByteArray(Charsets.UTF_8).size <= 200)
        assertFalse(stem.contains('\uFFFD'))
        val name = DocumentNames.unique("界".repeat(500), DocumentKind.NOTE, listOf("$stem.xnote"))
        assertTrue(name.toByteArray(Charsets.UTF_8).size <= 240)
        assertTrue(name.endsWith("_1.xnote"))
    }

    @Test fun directRenameRejectsUnsafeAndReservedNamesButAllowsExactNames() {
        for (raw in listOf("", "   ", ".", "..", "a/b", "a\\b", "a:b", "NUL.xnote",
            "lpt1.XNOTE", "draft.xnote ", "draft\u007f.xnote", "界".repeat(100))) {
            assertFalse("Should reject: $raw", DocumentNames.validRename(raw))
        }
        assertTrue(DocumentNames.validRename("Notebook 2026.xnote"))
        assertTrue(DocumentNames.validRename("पढ़ाई.xnote"))
        assertTrue(DocumentNames.validRename("My folder"))
    }

    @Test fun twoSameTitleNotesAreIndependentNativeFilesWithNoContentOverwrite() {
        val dir = java.nio.file.Files.createTempDirectory("two-safe-notes").toFile()
        val codec = com.notestaking.app.format.DocumentCodec(
            com.notestaking.app.core.FakeImageCodec(),
            com.notestaking.app.core.FakeTextMeasurer(),
        )
        try {
            val firstName = DocumentNames.unique("Project", DocumentKind.NOTE, emptySet())
            val first = java.io.File(dir, firstName)
            first.outputStream().use { codec.write(com.notestaking.app.core.model.Document.blank(2), it) }
            val original = first.readBytes()

            val secondName = DocumentNames.unique("PROJECT.xnote", DocumentKind.NOTE, listOf(firstName))
            val second = java.io.File(dir, secondName)
            assertFalse(second.exists())
            second.outputStream().use { codec.write(com.notestaking.app.core.model.Document.blank(3), it) }

            assertEquals("Project.xnote", firstName)
            assertEquals("PROJECT_1.xnote", secondName)
            assertArrayEquals(original, first.readBytes())
            assertEquals(2, first.inputStream().use { codec.read(it).pages.size })
            assertEquals(3, second.inputStream().use { codec.read(it).pages.size })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun reusedOrOccupiedProviderDocumentCannotReceiveAWrite() {
        assertFalse(SafDestinationGuard.canWrite(5L) { throw AssertionError("should not probe nonempty file") })
        assertFalse(SafDestinationGuard.canWrite(0L) { 31 })
        assertFalse(SafDestinationGuard.canWrite(null) { null })
        assertTrue(SafDestinationGuard.canWrite(0L) { -1 })
    }
}
