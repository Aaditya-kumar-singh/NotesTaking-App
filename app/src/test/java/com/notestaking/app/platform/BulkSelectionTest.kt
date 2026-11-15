package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class BulkSelectionTest {
    private data class Item(val key: String, val folder: Boolean = false)

    private fun run(
        entries: List<Item>,
        action: (Item) -> Boolean,
    ) = BulkSelection.run(entries, { it.key }, { it.folder }, action)

    @Test fun duplicateDocumentIdentityIsAttemptedExactlyOnceAndOrderPreserved() {
        val a = Item("local|root/a.xnote")
        val b = Item("local|root/b.xnote")
        val tried = ArrayList<String>()
        val result = run(listOf(a, b, a.copy(), b)) { e ->
            tried.add(e.key)
            true
        }
        assertEquals(listOf(a.key, b.key), tried)
        assertEquals(listOf(a, b), result.completed)
        assertTrue(result.failed.isEmpty())
        assertTrue(result.failedOperations.isEmpty())
    }

    @Test fun partialFailureKeepsSuccessfulItemsAndReturnsActualFailuresForRetry() {
        val entries = listOf(
            Item("local|root/a.xnote"),
            Item("local|root/fails.xnote"),
            Item("local|root/c.xcanvas"),
        )
        val calls = ArrayList<String>()
        val result = run(entries + entries[1]) { e ->
            calls.add(e.key)
            e.key != entries[1].key
        }
        assertEquals(entries.map { it.key }, calls)
        assertEquals(listOf(entries[0], entries[2]), result.completed)
        assertEquals(listOf(entries[1]), result.failed)
        assertEquals(listOf(entries[1]), result.failedOperations)
    }

    @Test fun providerExceptionMarksOnlyItsDocumentFailedAndKeepsProcessing() {
        val entries = listOf(
            Item("local|root/one.xnote"),
            Item("local|root/two.xnote"),
            Item("local|root/three.xnote"),
        )
        val calls = ArrayList<String>()
        val result = run(entries) { e ->
            calls.add(e.key)
            if (e == entries[1]) throw IOException("provider permission revoked")
            true
        }
        assertEquals(entries.map { it.key }, calls)
        assertEquals(listOf(entries[0], entries[2]), result.completed)
        assertEquals(listOf(entries[1]), result.failedOperations)
    }

    @Test fun selectedFolderAndItsChildAreOnePhysicalOperationButTwoSatisfiedPicks() {
        val child = Item("local|root/project/note.xnote")
        val parent = Item("local|root/project", folder = true)
        val other = Item("local|root/other.xnote")
        val calls = ArrayList<String>()
        val result = run(listOf(child, other, parent, child)) { e ->
            calls.add(e.key)
            true
        }
        assertEquals(listOf(other.key, parent.key), calls)
        assertEquals(listOf(child, other, parent), result.completed)
        assertTrue(result.failed.isEmpty())
    }

    @Test fun failedSelectedFolderDoesNotRetryChildIndependently() {
        val parent = Item("local|root/project", folder = true)
        val child = Item("local|root/project/note.xnote")
        val result = run(listOf(parent, child)) { false }
        assertEquals(listOf(parent, child), result.failed)
        assertEquals(listOf(parent), result.failedOperations)
        assertTrue(result.completed.isEmpty())
    }

    @Test fun siblingPrefixAndDifferentProviderAuthorityRemainSeparate() {
        val folder = Item("provider-a|root/x", folder = true)
        val sibling = Item("provider-a|root/x-other/note.xnote")
        val differentAuthority = Item("provider-b|root/x/note.xnote")
        val calls = ArrayList<String>()
        val result = run(listOf(folder, sibling, differentAuthority)) {
            calls.add(it.key)
            true
        }
        assertEquals(3, calls.size)
        assertEquals(3, result.completed.size)
    }

    @Test fun actualTreeCopiesEachPickedFileOnceAndPreservesSourceOnPartialFailure() {
        val root = java.nio.file.Files.createTempDirectory("batch-copy").toFile()
        try {
            val tree = SafeDocumentTree(root)
            val dest = tree.create("root", "destination", true)
            val first = tree.create("root", "one.xnote", false)
            val second = tree.create("root", "two.xnote", false)
            tree.resolve(first).writeBytes(byteArrayOf(1, 2, 3))
            tree.resolve(second).writeBytes(byteArrayOf(5, 6, 7))
            val occupied = tree.create(dest, "two.xnote", false)
            tree.resolve(occupied).writeBytes(byteArrayOf(99))
            val tried = ArrayList<String>()
            val result = BulkSelection.run(
                listOf(first, second, first), { it }, { false },
            ) { id ->
                tried.add(id)
                runCatching { tree.copy(id, dest) }.isSuccess
            }
            assertEquals(listOf(first, second), tried)
            assertEquals(listOf(first), result.completed)
            assertEquals(listOf(second), result.failedOperations)
            assertEquals(listOf(1, 2, 3), tree.resolve(first).readBytes().map { it.toInt() })
            assertEquals(listOf(5, 6, 7), tree.resolve(second).readBytes().map { it.toInt() })
            assertEquals(listOf(1, 2, 3), tree.resolve("$dest/one.xnote").readBytes().map { it.toInt() })
            assertEquals(listOf(99), tree.resolve(occupied).readBytes().map { it.toInt() })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun actualTreeMovingSelectedFolderAndChildMovesTheWholeSubtreeOnlyOnce() {
        val root = java.nio.file.Files.createTempDirectory("batch-move").toFile()
        try {
            val tree = SafeDocumentTree(root)
            val sourceFolder = tree.create("root", "folder", true)
            val inside = tree.create(sourceFolder, "ink.xnote", false)
            tree.resolve(inside).writeBytes(byteArrayOf(33, 44))
            val dest = tree.create("root", "destination", true)
            val attempted = ArrayList<String>()
            val result = BulkSelection.run(
                listOf(inside, sourceFolder, inside),
                { it }, { it == sourceFolder },
            ) { id ->
                attempted.add(id)
                runCatching { tree.move(id, "root", dest) }.isSuccess
            }
            assertEquals(listOf(sourceFolder), attempted)
            assertEquals(listOf(inside, sourceFolder), result.completed)
            assertTrue(result.failed.isEmpty())
            assertEquals(listOf(33, 44),
                tree.resolve("$dest/folder/ink.xnote").readBytes().map { it.toInt() })
            assertTrue(!java.io.File(root, "folder").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cancellationIsNeverDowngradedToPartialFailure() {
        val one = Item("local|root/a.xnote")
        val stop = CancellationException("user cancelled")
        assertThrows(CancellationException::class.java) {
            run(listOf(one)) { throw stop }
        }
    }
}
