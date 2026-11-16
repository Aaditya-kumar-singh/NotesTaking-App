package com.notestaking.app.platform

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class NoteFavoritesStoreTest {
    @Test fun starsSurviveRestartAndFolderMoveWithoutChangingNoteBytes() {
        val root = Files.createTempDirectory("favorites-").toFile()
        try {
            val note = root.resolve("original.xnote").apply { writeBytes(byteArrayOf(11, 22, 33)) }
            val original = note.readBytes()
            val path = root.resolve("favorites.json")
            val first = NoteFavoritesStore(JsonStore(path))
            assertTrue(first.set("provider|root/folder/note", true))
            assertTrue(first.set("provider|root/other", true))
            val second = NoteFavoritesStore(JsonStore(path))
            assertTrue(second.contains("provider|root/folder/note"))
            assertTrue(second.rekeyTree("provider|root/folder", "provider|root/renamed"))
            assertFalse(first.contains("provider|root/folder/note"))
            assertTrue(first.contains("provider|root/renamed/note"))
            assertTrue(second.removeTree("provider|root/renamed"))
            assertFalse(first.contains("provider|root/renamed/note"))
            assertTrue(first.contains("provider|root/other"))
            assertTrue(original.contentEquals(note.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsBadKeysDuplicateEntriesAndCollidingMoves() {
        val root = Files.createTempDirectory("favorites-invalid-").toFile()
        try {
            val path = root.resolve("favorites.json")
            val stars = NoteFavoritesStore(JsonStore(path))
            assertFalse(stars.set("relative-path", true))
            assertFalse(stars.set("authority|", true))
            assertTrue(stars.set("provider|old", true))
            assertTrue(stars.set("provider|new", true))
            assertFalse(stars.rekeyTree("provider|old", "provider|new"))
            assertTrue(stars.contains("provider|old"))
            assertTrue(stars.contains("provider|new"))
            path.writeText(JSONObject().put("version", 1).put("favorites", JSONArray().put("provider|dup").put("provider|dup")).toString())
            assertFalse(stars.set("provider|another", true))
            assertEquals(2, JSONObject(path.readText()).getJSONArray("favorites").length())
        } finally { root.deleteRecursively() }
    }

    @Test fun documentIdsMayContainSpacesAndAdditionalPipes() {
        val root = Files.createTempDirectory("favorites-ids-").toFile()
        try {
            val file = root.resolve("favorites.json")
            val store = NoteFavoritesStore(JsonStore(file))
            val id = "com.example.provider|folder with spaces/note|part two.xnote"
            assertTrue(store.set(id, true))
            assertTrue(NoteFavoritesStore(JsonStore(file)).contains(id))
            assertTrue(store.rekeyTree("com.example.provider|folder with spaces", "com.example.provider|moved folder"))
            assertFalse(store.contains(id))
            assertTrue(store.contains("com.example.provider|moved folder/note|part two.xnote"))
            assertTrue(store.removeTree("com.example.provider|moved folder"))
            assertFalse(store.contains("com.example.provider|moved folder/note|part two.xnote"))
            assertFalse(store.set("bad authority|valid", true))
            assertFalse(store.set("provider|bad\u0001id", true))
        } finally { root.deleteRecursively() }
    }

    @Test fun malformedStoredJsonCannotBeOverwrittenByStarMutation() {
        val root = Files.createTempDirectory("favorites-corrupt-").toFile()
        try {
            val file = root.resolve("favorites.json")
            val store = NoteFavoritesStore(JsonStore(file))
            assertTrue(store.set("provider|existing", true))
            val malformed = "{not valid json".toByteArray(Charsets.UTF_8)
            file.writeBytes(malformed)
            assertFalse(store.set("provider|new", true))
            assertFalse(store.rekeyTree("provider|existing", "provider|moved"))
            assertFalse(store.removeTree("provider|existing"))
            assertTrue(malformed.contentEquals(file.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun blockedStorageCannotClaimSuccessfulStar() {
        val root = Files.createTempDirectory("favorites-fail-").toFile()
        try {
            val target = root.resolve("blocked.json").apply { mkdir() }
            val stars = NoteFavoritesStore(JsonStore(target))
            assertFalse(stars.set("provider|root/item", true))
            assertFalse(stars.contains("provider|root/item"))
            assertTrue(target.isDirectory)
        } finally { root.deleteRecursively() }
    }
}
