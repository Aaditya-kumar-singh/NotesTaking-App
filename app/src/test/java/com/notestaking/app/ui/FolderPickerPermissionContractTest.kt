package com.notestaking.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only guard: a failed SAF tree grant must never replace the saved library root. */
class FolderPickerPermissionContractTest {
    @Test
    fun pickedFolderIsPersistedOnlyAfterDurableReadWriteGrant() {
        val relative = "src/main/java/com/notestaking/app/MainActivity.kt"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing source: $relative" }
        val source = file.readText(Charsets.UTF_8)
        val start = source.indexOf("val pickRootLauncher = rememberLauncherForActivityResult(")
        val end = source.indexOf("/** Open the \"Save as\" picker", start)
        assertTrue("Missing folder-picker callback", start >= 0 && end > start)
        val picker = source.substring(start, end)

        val checkedGrant = "if (runCatching { resolver.takePersistableUriPermission(it, rwFlags) }.isSuccess) {"
        val updateRoot = "editor.updateBrowseRoot(it.toString())"
        val showError = "editor.message = resources.getString(R.string.err_open_folder)"
        assertTrue("Persistent read/write permission must gate the stored root", picker.contains(checkedGrant))
        assertTrue("Successful grants must still select the folder", picker.contains(updateRoot))
        assertTrue("Failed grants must show an error without switching roots", picker.contains(showError))
        assertTrue(picker.indexOf(checkedGrant) < picker.indexOf(updateRoot))
        assertTrue(picker.indexOf(updateRoot) < picker.indexOf(showError))
    }
}
