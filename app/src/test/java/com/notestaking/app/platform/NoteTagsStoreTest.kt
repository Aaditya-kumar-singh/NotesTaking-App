package com.notestaking.app.platform

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NoteTagsStoreTest {
    @Test fun tagsPersistAndCrossFolderSearchAndMoveDeleteWork() {
        val root = Files.createTempDirectory("tags-store-").toFile()
        try {
            val note = root.resolve("a.xnote").apply { writeBytes(byteArrayOf(3, 5, 8)) }
            val before = note.readBytes()
            val path = root.resolve("tags.json")
            val a = NoteTagsStore(JsonStore(path))
            assertTrue(a.set("provider|root/home/a", listOf("Work", "Personal")))
            assertTrue(a.set("provider|root/other/b", listOf("work")))
            val reopened = NoteTagsStore(JsonStore(path))
            assertEquals(listOf("provider|root/home/a", "provider|root/other/b"), reopened.find("WORK"))
            assertEquals(listOf("Personal", "Work"), reopened.tags("provider|root/home/a"))
            assertTrue(a.rekeyTree("provider|root/home", "provider|root/archive"))
            assertTrue(reopened.tags("provider|root/home/a").isEmpty())
            assertEquals(listOf("Personal", "Work"), reopened.tags("provider|root/archive/a"))
            assertTrue(a.removeTree("provider|root/archive"))
            assertEquals(listOf("provider|root/other/b"), reopened.find("work"))
            assertTrue(before.contentEquals(note.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidTagsMalformedEntriesAndCollisionsDoNotOverwriteExistingData() {
        val root = Files.createTempDirectory("tags-invalid-").toFile()
        try {
            val path = root.resolve("tags.json")
            val tags = NoteTagsStore(JsonStore(path))
            assertFalse(tags.set("bad", listOf("ok")))
            assertFalse(tags.set("provider|root/a", listOf("Work", "work")))
            assertFalse(tags.set("provider|root/a", listOf(" ")))
            assertFalse(tags.set("provider|root/a", List(17) { "tag$it" }))
            assertTrue(tags.set("provider|root/a", listOf("Office")))
            assertTrue(tags.set("provider|root/b", listOf("Home")))
            assertFalse(tags.rekeyTree("provider|root/a", "provider|root/b"))
            assertEquals(listOf("Office"), tags.tags("provider|root/a"))
            assertEquals(listOf("Home"), tags.tags("provider|root/b"))
            path.writeText(JSONObject().put("version", 1).put("notes", JSONObject().put("provider|root/c", JSONArray().put("X").put("x"))).toString())
            assertFalse(tags.set("provider|root/d", listOf("Work")))
            assertEquals(1, JSONObject(path.readText()).getJSONObject("notes").length())
            path.writeText("{broken json")
            assertFalse(tags.set("provider|root/d", listOf("Work")))
            assertEquals("{broken json", path.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun unwritableStoreCannotClaimSuccess() {
        val root = Files.createTempDirectory("tags-block-").toFile()
        try {
            val blocked = root.resolve("tags.json").apply { mkdir() }
            val tags = NoteTagsStore(JsonStore(blocked))
            assertFalse(tags.set("provider|root/a", listOf("Work")))
            assertTrue(tags.tags("provider|root/a").isEmpty())
            assertTrue(blocked.isDirectory)
        } finally { root.deleteRecursively() }
    }
}
