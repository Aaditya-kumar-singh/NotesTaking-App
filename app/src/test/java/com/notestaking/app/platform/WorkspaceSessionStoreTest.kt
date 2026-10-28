package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WorkspaceSessionStoreTest {
    private fun withDir(test: (File) -> Unit) {
        val dir = Files.createTempDirectory("notestaking-workspace").toFile()
        try { test(dir) } finally { dir.deleteRecursively() }
    }

    @Test fun roundTripsSplitAndFocusedSecondary() = withDir { dir ->
        val store = WorkspaceSessionStore(File(dir, "workspace.json"))
        val state = WorkspaceSessionStore.State(primaryOpen = true, secondaryOpen = true, focusedSecondary = true)
        assertTrue(store.save(state))
        assertEquals(state, WorkspaceSessionStore(File(dir, "workspace.json")).load())
    }

    @Test fun closedSecondaryIsNotReopenedByOldFocusState() = withDir { dir ->
        val store = WorkspaceSessionStore(File(dir, "workspace.json"))
        assertTrue(store.save(WorkspaceSessionStore.State(true, false, true)))
        assertEquals(WorkspaceSessionStore.State(true, false, false), store.load())
    }

    @Test fun closedWorkspaceOverwritesPreviousSplitMarker() = withDir { dir ->
        val store = WorkspaceSessionStore(File(dir, "workspace.json"))
        assertTrue(store.save(WorkspaceSessionStore.State(true, true, true)))
        assertTrue(store.save(WorkspaceSessionStore.State(false, false, false)))
        assertEquals(WorkspaceSessionStore.State(false, false, false), store.load())
    }

    @Test fun missingCorruptAndUnknownVersionFailClosed() = withDir { dir ->
        val f = File(dir, "workspace.json")
        val store = WorkspaceSessionStore(f)
        assertNull(store.load())
        f.writeText("not-json")
        assertNull(store.load())
        f.writeText("""{"version":99,"primaryOpen":true,"secondaryOpen":true}""")
        assertNull(store.load())
        f.writeText("""{"version":1,"secondaryOpen":true}""")
        assertNull(store.load())
        f.writeText("""{"version":1,"primaryOpen":"true","secondaryOpen":true}""")
        assertNull(store.load())
    }

    @Test fun splitRatioIsClampedAndCorruptRatioFallsBack() = withDir { dir ->
        val file = File(dir, "workspace.json")
        val store = WorkspaceSessionStore(file)
        assertTrue(store.save(WorkspaceSessionStore.State(true, true, true, 42f)))
        assertEquals(0.8f, store.load()!!.splitRatio, 0.0001f)
        file.writeText("""{"version":1,"primaryOpen":true,"secondaryOpen":true,"focus":"secondary","splitRatio":"broken"}""")
        assertEquals(0.5f, store.load()!!.splitRatio, 0.0001f)
        assertTrue(store.save(WorkspaceSessionStore.State(true, true, true, Float.NaN)))
        assertEquals(0.5f, store.load()!!.splitRatio, 0.0001f)
    }

    @Test fun failureDoesNotClobberCommittedWorkspace() = withDir { dir ->
        val file = File(dir, "workspace.json")
        val store = WorkspaceSessionStore(file)
        val original = WorkspaceSessionStore.State(true, true, false)
        assertTrue(store.save(original))
        // A writable parent is intentionally required; an invalid target is not a successful save.
        val blocked = File(dir, "blocked").apply { mkdir() }
        assertFalse(WorkspaceSessionStore(blocked).save(original))
        assertEquals(original, store.load())
    }
}
