package com.notestaking.app.ui

import com.notestaking.app.settings.ExplorerLayout
import com.notestaking.app.settings.ExplorerSortKey
import com.notestaking.app.settings.ExplorerView
import com.notestaking.app.settings.FolderPlacement
import com.notestaking.app.settings.GroupBy
import com.notestaking.app.settings.TileSize
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coordinator-owned Home/Explorer production files stay read-only in Chat B.
 * shortcut: Source contracts cannot prove responsive menu visibility or selection semantics;
 * upgrade to Compose/device tests with coordinator ownership.
 */
class ExplorerSortLayoutContractTest {
    private fun source(name: String): String {
        val path = "src/main/java/com/notestaking/app/ui/$name"
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing explorer UI source: $path" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(text: String, start: String, end: String): String {
        val first = text.indexOf(start)
        assertTrue("Missing source anchor: $start", first >= 0)
        val last = text.indexOf(end, first + start.length)
        assertTrue("Missing source boundary: $end", last > first)
        return text.substring(first, last)
    }

    @Test
    fun switchingGridGalleryAndListPreservesSortAndPersistsTheView() {
        val original = ExplorerView(
            layout = ExplorerLayout.GRID,
            sortKey = ExplorerSortKey.SIZE,
            descending = false,
            tileSize = TileSize.L,
            groupBy = GroupBy.KIND,
            folders = FolderPlacement.MIXED,
        )
        for (layout in listOf(ExplorerLayout.GRID, ExplorerLayout.GALLERY, ExplorerLayout.LIST)) {
            val updated = original.copy(layout = layout)
            assertEquals(original.sortKey, updated.sortKey)
            assertEquals(original.descending, updated.descending)
            assertEquals(original.groupBy, updated.groupBy)
            assertEquals(original.folders, updated.folders)
            assertEquals(original.tileSize, updated.tileSize)
            assertEquals(updated, ExplorerView.fromJson(updated.toJson()))
        }
    }

    @Test
    fun resortingAndSwitchingLayoutsDoNotChangeSelectedFileIdentity() {
        val entries = listOf(
            BrowseEntry("Smaller", "uri:smaller", false, size = 10),
            BrowseEntry("Folder", "uri:folder", true, size = 1),
            BrowseEntry("Larger", "uri:larger", false, size = 500),
        )
        val selectedUri = "uri:larger"
        for (layout in listOf(ExplorerLayout.GRID, ExplorerLayout.GALLERY, ExplorerLayout.LIST)) {
            for (descending in listOf(true, false)) {
                val view = ExplorerView(layout = layout, sortKey = ExplorerSortKey.SIZE, descending = descending)
                val sorted = entries.sortedWith(
                    explorerComparator(view.sortKey, view.descending) { it.created }
                )
                assertEquals("uri:folder", sorted.first().documentUri)
                assertTrue(sorted.any { it.documentUri == selectedUri })
                assertEquals(3, sorted.map { it.documentUri }.distinct().size)
                assertEquals(
                    if (descending) "uri:larger" else "uri:smaller",
                    sorted[1].documentUri,
                )
            }
        }
    }

    @Test
    fun librarySwitcherAndViewOptionsWriteTheSameFolderViewWithoutClearingSelection() {
        val home = source("Backstage.kt")
        val wiring = section(home, "val folderKey = editor.folderKey(", "// Listing.")
        assertTrue(wiring.contains("val view = editor.viewFor(folderKey)"))
        assertTrue(wiring.contains("editor.setView(folderKey, it)"))

        val controls = section(home, "if (wide && switcherLayouts.size > 1)", "var moreOpen by remember")
        assertTrue(controls.contains("LayoutSwitcher(switcherLayouts, layout, liftNow) { setView(view.copy(layout = it)); columnsPick = null }"))
        assertTrue(controls.contains("ExplorerIcon(NotesTakingIcons.sliders, stringResource(R.string.view_options))"))
        assertTrue(controls.contains("ViewOptionsContent("))
        assertTrue(controls.contains("onChange = { v -> setView("))
        assertTrue(controls.contains("onReset = { editor.setView(folderKey, null) }"))
        assertFalse("Changing only the view must not clear selected files", controls.contains("selection.clear()"))

        val editor = source("Editor.kt")
        val persistence = section(editor, "fun viewFor(key: String?): ExplorerView =", "/** Sets the layout every folder")
        assertTrue(persistence.contains("settings.prefs.perFolderViews && key != null"))
        assertTrue(persistence.contains("folderViews + (key to view)"))
        assertTrue(persistence.contains("settings = settings.copy(folderViews = folderViews)"))
        assertTrue(persistence.contains("settings = settings.copy(explorerView = explorerView)"))
        assertTrue(persistence.contains("saveSettingsSoon()"))
    }

    @Test
    fun sortMenuAndFallbackViewOptionsRemainAvailableOnPhonesAndTablets() {
        val home = source("Backstage.kt")
        val chrome = source("ExplorerChrome.kt")
        val layout = source("ExplorerLayouts.kt")

        assertTrue(home.contains("val switcherLayouts = prefs.switcherLayouts.filter { !(compactScreen && it == ExplorerLayout.COLUMNS) }"))
        assertTrue(home.contains("flat && (view.layout == ExplorerLayout.COLUMNS || view.layout == ExplorerLayout.TIMELINE) -> ExplorerLayout.GRID"))
        assertTrue(home.contains("compactScreen && view.layout == ExplorerLayout.COLUMNS -> ExplorerLayout.LIST"))
        assertTrue(home.contains("ExplorerSortKey.entries.forEach { k -> SortOption("))
        assertTrue(home.contains("setView(view.copy(sortKey = k, descending = d))"))
        assertTrue(home.contains("onSort = if (showRecent) null else"))
        assertTrue(chrome.contains("layouts.forEach { l ->"))
        assertTrue(chrome.contains("onChange(view.copy(layout = l))"))
        assertTrue(chrome.contains("view.copy(descending = !view.descending)"))
        assertTrue(layout.contains("ColumnHead(stringResource(R.string.sort_name), ExplorerSortKey.NAME"))
        assertTrue(layout.contains("ColumnHead(stringResource(R.string.sort_size), ExplorerSortKey.SIZE"))
    }
}
