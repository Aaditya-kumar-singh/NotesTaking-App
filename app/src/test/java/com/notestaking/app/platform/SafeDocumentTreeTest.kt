package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

class SafeDocumentTreeTest {
    private fun withTree(test: (SafeDocumentTree, File, File) -> Unit) {
        val base = Files.createTempDirectory("notes-provider-root").toFile()
        val outside = Files.createTempDirectory("notes-provider-outside").toFile()
        try {
            test(SafeDocumentTree(base), base, outside)
        } finally {
            base.deleteRecursively()
            outside.deleteRecursively()
        }
    }

    private fun rejected(action: () -> Unit) {
        var thrown = false
        try { action() } catch (_: FileNotFoundException) { thrown = true }
        assertTrue("Expected invalid path or unsafe operation to be rejected", thrown)
    }

    @Test fun documentIdsCannotEscapeStorageRoot() = withTree { tree, root, outside ->
        File(root, "safe.xnote").writeText("safe")
        File(outside, "private.txt").writeText("outside")
        val invalid = listOf(
            "", "/", "other", "rooted", "root/", "root//bad", "root/../private.txt",
            "root/sub/../../private.txt", "root/./safe.xnote", "root/..",
            "root/\\..\\private.txt", "root/C:\\outside", "root/safe.xnote/..",
        )
        for (id in invalid) rejected { tree.resolve(id) }
        assertEquals("safe", tree.resolve("root/safe.xnote").readText())
        assertEquals("root/safe.xnote", tree.documentId(File(root, "safe.xnote")))
        rejected { tree.documentId(File(outside, "private.txt")) }
    }

    @Test fun rootCannotBeDeletedRenamedCopiedOrMoved() = withTree { tree, root, _ ->
        File(root, "keep.xnote").writeText("safe")
        rejected { tree.delete("root") }
        rejected { tree.rename("root", "new") }
        rejected { tree.copy("root", "root") }
        rejected { tree.move("root", "root", "root") }
        assertEquals("safe", File(root, "keep.xnote").readText())
    }

    @Test fun createAndRenameRejectUnsafeNamesAndMissingParents() = withTree { tree, root, _ ->
        for (name in listOf("", ".", "..", "bad/name", "bad\\name", "x\u0000y", "   ")) {
            rejected { tree.create("root", name, false) }
        }
        rejected { tree.create("root/missing", "note.xnote", false) }
        val first = tree.create("root", "note.xnote", false)
        val second = tree.create("root", "note.xnote", false)
        assertEquals("root/note.xnote", first)
        assertEquals("root/note (1).xnote", second)
        rejected { tree.rename(first, "../hijack") }
        rejected { tree.rename(first, "note (1).xnote") }
        assertTrue(File(root, "note.xnote").exists())
        assertTrue(File(root, "note (1).xnote").exists())
    }

    @Test fun cannotCopyOrMoveDirectoryIntoItselfOrDescendant() = withTree { tree, root, _ ->
        val folder = File(root, "folder").apply { mkdir() }
        File(folder, "child").mkdir()
        File(folder, "keep.xnote").writeText("never lose this")
        rejected { tree.copy("root/folder", "root/folder") }
        rejected { tree.copy("root/folder", "root/folder/child") }
        rejected { tree.move("root/folder", "root", "root/folder/child") }
        rejected { tree.move("root/folder", "root/folder", "root") }
        assertEquals("never lose this", File(folder, "keep.xnote").readText())
        assertFalse(File(folder, "folder").exists())
    }

    @Test fun moveValidatesRealSourceParentAndConflicts() = withTree { tree, root, _ ->
        val source = File(root, "from").apply { mkdir() }
        val target = File(root, "to").apply { mkdir() }
        File(source, "a.xnote").writeText("A")
        rejected { tree.move("root/from/a.xnote", "root/to", "root/to") }
        assertEquals("root/to/a.xnote", tree.move("root/from/a.xnote", "root/from", "root/to"))
        assertFalse(File(source, "a.xnote").exists())
        assertEquals("A", File(target, "a.xnote").readText())
    }

    @Test fun copiesCompleteTreeWithoutMutatingSource() = withTree { tree, root, _ ->
        val source = File(root, "source").apply { mkdir() }
        File(source, "sub").mkdir()
        File(source, "sub/a.xnote").writeText("ink")
        File(root, "target").mkdir()
        val result = tree.copy("root/source", "root/target")
        assertEquals("root/target/source", result)
        assertEquals("ink", File(root, "target/source/sub/a.xnote").readText())
        assertEquals("ink", File(root, "source/sub/a.xnote").readText())
        assertEquals(listOf("source"), File(root, "target").listFiles()!!.map { it.name })
    }

    @Test fun failedDeepCopyLeavesNoPublishedPartialFolder() = withTree { tree, root, _ ->
        val source = File(root, "deep").apply { mkdir() }
        var cursor = source
        repeat(66) { cursor = File(cursor, "d").apply { mkdir() } }
        File(root, "dest").mkdir()
        var failed = false
        try { tree.copy("root/deep", "root/dest") }
        catch (_: java.io.IOException) { failed = true }
        assertTrue("Excessive copy depth must be rejected", failed)
        assertFalse(File(root, "dest/deep").exists())
        assertTrue(File(root, "dest").listFiles()!!.isEmpty())
        assertTrue(source.isDirectory) // failed copy never modifies the original
    }

    @Test fun isChildUsesRealDescendantsNotPrefixMatching() = withTree { tree, root, _ ->
        File(root, "a").mkdir()
        File(root, "ab").mkdir()
        File(root, "a/note").writeText("x")
        assertTrue(tree.isChild("root", "root/a"))
        assertTrue(tree.isChild("root/a", "root/a/note"))
        assertFalse(tree.isChild("root/a", "root/a"))
        assertFalse(tree.isChild("root/a", "root/ab"))
    }

    @Test fun deletionDoesNotFollowNestedSymbolicLinks() = withTree { tree, root, outside ->
        val folder = File(root, "folder").apply { mkdir() }
        File(folder, "note.xnote").writeText("notes")
        val privateFile = File(outside, "other-user-document").apply { writeText("must survive") }
        val linked = runCatching {
            Files.createSymbolicLink(File(folder, "escape").toPath(), outside.toPath())
        }.isSuccess
        if (linked) {
            tree.delete("root/folder")
            assertFalse(folder.exists())
            assertEquals("must survive", privateFile.readText())
        }
    }

    @Test fun createRenameAndCopyRejectSymlinkDestinations() = withTree { tree, root, outside ->
        val outsideFile = File(outside, "private.xnote").apply { writeText("private") }
        val link = File(root, "linked.xnote")
        val supported = runCatching {
            Files.createSymbolicLink(link.toPath(), outsideFile.toPath())
        }.isSuccess
        if (supported) {
            rejected { tree.create("root", "linked.xnote", false) }
            File(root, "original.xnote").writeText("original")
            rejected { tree.rename("root/original.xnote", "linked.xnote") }
            assertEquals("original", File(root, "original.xnote").readText())
            assertEquals("private", outsideFile.readText())
            val dest = File(root, "destination").apply { mkdir() }
            Files.createSymbolicLink(File(dest, "original.xnote").toPath(), outsideFile.toPath())
            rejected { tree.copy("root/original.xnote", "root/destination") }
            assertEquals("private", outsideFile.readText())
        }
    }

    @Test fun symbolicLinksAreRejectedWhenSupported() = withTree { tree, root, outside ->
        File(outside, "secret").writeText("secret")
        val link = File(root, "link")
        val made = runCatching {
            Files.createSymbolicLink(link.toPath(), outside.toPath())
        }.isSuccess
        if (made) {
            rejected { tree.resolve("root/link/secret") }
            rejected { tree.copy("root/link", "root") }
        }
    }
}
