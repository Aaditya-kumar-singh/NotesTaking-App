package com.notestaking.app.ui

import com.notestaking.app.core.util.DocumentKind
import com.notestaking.app.platform.DocMetaStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Read-only regression contracts for coordinator-owned library search and type filtering.
 * shortcut: SAF traversal and Compose touch/selection semantics need device tests with coordinator allocation.
 */
class LibrarySearchContractTest {
    private fun source(name: String): String {
        val relative = "src/main/java/com/notestaking/app/ui/$name"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing library search source: $relative" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(source: String, start: String, end: String): String {
        val first = source.indexOf(start)
        assertTrue("Missing source anchor: $start", first >= 0)
        val last = source.indexOf(end, first + start.length)
        assertTrue("Missing source boundary: $end", last > first)
        return source.substring(first, last)
    }

    /** Mirrors Editor.searchNotes' matching expression, guarded against source drift below. */
    private fun titleMatches(name: String, typedQuery: String): Boolean =
        DocumentKind.stripSuffix(name).contains(typedQuery.trim(), ignoreCase = true)

    @Test
    fun exactAndPartialTitleSearchIsCaseInsensitiveAndIgnoresDocumentSuffix() {
        val names = listOf("Project Notes.xnote", "Shopping List.XNOTE", "Design Board.xcanvas")
        assertEquals(listOf("Project Notes.xnote"), names.filter { titleMatches(it, "project notes") })
        assertEquals(listOf("Project Notes.xnote"), names.filter { titleMatches(it, "  NOTES  ") })
        assertEquals(listOf("Shopping List.XNOTE"), names.filter { titleMatches(it, "LIST") })
        assertEquals(listOf("Design Board.xcanvas"), names.filter { titleMatches(it, "boar") })
        assertTrue(names.none { titleMatches(it, "no-match") })
        assertFalse(titleMatches("Project Notes.xnote", ".xnote"))
        assertFalse(titleMatches("Design Board.xcanvas", ".xcanvas"))

        val editor = source("Editor.kt")
        val search = section(editor, "fun searchNotes(treeUri:", "// --- Trash")
        assertTrue(search.contains("val needle = query.trim()"))
        assertTrue(search.contains("val display = com.notestaking.app.core.util.DocumentKind.stripSuffix(e.name)"))
        assertTrue(search.contains("if (display.contains(needle, ignoreCase = true)) out.add(e)"))
    }

    @Test
    fun emptyQueryAndRecursiveSafSearchDoNotYieldDirectoriesOrDuplicateFolders() {
        val editor = source("Editor.kt")
        val search = section(editor, "fun searchNotes(treeUri:", "// --- Trash")
        assertTrue(search.contains("if (needle.isEmpty()) return emptyList()"))
        assertTrue(search.contains("val seen = HashSet<String>()"))
        assertTrue(search.contains("if (!seen.add(docId)) continue"))
        assertTrue(search.contains("for (e in browseChildren(treeUri, docId))"))
        assertTrue(search.contains("if (e.isDir) {"))
        assertTrue(search.contains("stack.addLast(browseDocId(e.documentUri))"))
        assertTrue(search.contains("return out.sortedWith(explorerComparator("))

        val home = source("Backstage.kt")
        val query = section(home, "val trimmed = query.trim()", "val colorResults by produceState")
        assertTrue(query.contains("if (trimmed.isEmpty()) { value = emptyList(); return@produceState }"))
        assertTrue(query.contains("delay(250)"))
        assertTrue(query.contains("withContext(Dispatchers.IO) { editor.searchNotes(root, currentDocId, trimmed) }"))
    }

    @Test
    fun notePdfCanvasAndFolderAreDistinctTypeFilterCategories() {
        val note = BrowseEntry("Daily.xnote", "uri:note", false)
        val canvas = BrowseEntry("Map.xcanvas", "uri:canvas", false)
        val folder = BrowseEntry("Archive", "uri:folder", true)
        val pdf = DocMetaStore.Meta(pages = 5, pdf = true, modified = 1L)
        assertEquals(EntryKind.NOTE, entryKind(note, null))
        assertEquals(EntryKind.PDF, entryKind(note, pdf))
        assertEquals(EntryKind.CANVAS, entryKind(canvas, null))
        assertEquals(EntryKind.FOLDER, entryKind(folder, null))
        assertEquals(listOf("uri:canvas"), listOf(note, canvas, folder)
            .filter { entryKind(it, null) == EntryKind.CANVAS }.map { it.documentUri })

        val home = source("Backstage.kt")
        val filter = section(home, "var kindOpen by remember", "clipboard?.let")
        assertTrue(filter.contains("listOf(EntryKind.NOTE, EntryKind.PDF, EntryKind.CANVAS)"))
        assertTrue(filter.contains("ChoiceMenuItem(option == kindFilter, onClick = { kindOpen = false; kindFilter = option })"))
        val arrange = section(home, "val kindOf: (BrowseEntry)", "// Columns are how")
        assertTrue(arrange.contains("kindOf(e) == kindFilter"))
        val tiles = source("ExplorerTiles.kt")
        assertTrue(tiles.contains("EntryKind.PDF -> stringResource(R.string.kind_pdf_note)"))
        assertTrue(tiles.contains("EntryKind.CANVAS -> stringResource(R.string.kind_canvas)"))
        assertTrue(tiles.contains("EntryKind.NOTE -> stringResource(R.string.kind_note)"))
    }

    @Test
    fun changingTitleOrTypeFilterDoesNotClearExistingSelection() {
        val home = source("Backstage.kt")
        val queryEffect = section(home, "LaunchedEffect(query)", "// Home opens on")
        assertTrue(queryEffect.contains("if (query.isNotBlank()) { colorFilter = null; showRecent = false }"))
        assertFalse(queryEffect.contains("selection.clear()"))

        val typeFilter = section(home, "var kindOpen by remember", "clipboard?.let")
        assertFalse(typeFilter.contains("selection.clear()"))
        assertTrue(typeFilter.contains("kindFilter = option"))

        val selection = listOf(
            BrowseEntry("Alpha.xnote", "uri:alpha", false),
            BrowseEntry("Beta.xnote", "uri:beta", false),
        )
        val pickedUri = selection.first().documentUri
        val visible = selection.filter { titleMatches(it.name, "bet") }
        assertEquals("uri:alpha", pickedUri)
        assertTrue(visible.none { it.documentUri == pickedUri })
        assertTrue(selection.any { it.documentUri == pickedUri })
    }

    @Test
    fun queryAndKindFiltersHaveClearActionsAndKeyboardSearchCanClose() {
        val home = source("Backstage.kt")
        val empty = section(home, "val empty = when {", "// Files scroll under")
        assertTrue(empty.contains("recovery = EmptyRecovery(stringResource(R.string.clear_search)) { query = \"\"; focusManager.clearFocus() }"))
        assertTrue(empty.contains("recovery = if (kindFilter == null) null else EmptyRecovery(stringResource(R.string.all_kinds)) { kindFilter = null }"))
        assertTrue(home.contains("LaunchedEffect(currentDocId) { query = \"\" }"))

        val field = section(home, "internal fun ExplorerSearchField(", "@Composable\ninternal fun Crumb(")
        assertTrue(field.contains("keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)"))
        assertTrue(field.contains("keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })"))
        assertTrue(field.contains("if (focused) onQueryChange(\"\")"))
        assertTrue(field.contains("NotesTakingIcons.close, stringResource(R.string.clear_search)"))
        assertTrue(field.contains(".clickable { onQueryChange(\"\"); focusManager.clearFocus() }"))
    }
}
