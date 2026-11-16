package com.notestaking.app.platform

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class JsonStoreCheckedWriteTest {
    @Test fun checkedSettingsWriteReportsFailureWithoutReplacingOtherData() {
        val root = Files.createTempDirectory("settings-write-test-").toFile()
        try {
            val target = root.resolve("settings.json")
            val store = JsonStore(target)
            assertTrue(store.writeChecked(JSONObject().put("toolbar_color_count", 3)))
            val original = target.readBytes()
            val blocker = root.resolve("blocked")
            assertTrue(blocker.mkdir())
            assertFalse(JsonStore(blocker).writeChecked(JSONObject().put("toolbar_color_count", 9)))
            assertTrue(original.contentEquals(target.readBytes()))
            assertEquals(3, store.read().getInt("toolbar_color_count"))
        } finally {
            root.deleteRecursively()
        }
    }
}
