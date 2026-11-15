package com.notestaking.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level contracts for coordinator-owned Home/create UI.
 *
 * These deliberately check wiring while production files remain outside Chat B ownership.
 * shortcut: Source contracts cannot prove Compose semantics or tap geometry; add instrumented UI tests when allocated.
 */
class LibraryCreateRecoveryContractTest {
    private fun source(name: String): String {
        val relative = "src/main/java/com/notestaking/app/ui/$name"
        val candidate = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(candidate) { "Missing production UI source: $relative" }
        return candidate.readText(Charsets.UTF_8)
    }

    private fun between(source: String, start: String, end: String): String {
        val a = source.indexOf(start)
        assertTrue("Missing source anchor: $start", a >= 0)
        val b = source.indexOf(end, a + start.length)
        assertTrue("Missing source boundary: $end", b > a)
        return source.substring(a, b)
    }

    @Test
    fun noStorageRootOffersBothLocalStorageAndFolderPicker() {
        val home = source("Backstage.kt")
        val entry = between(home, "val calls = ExplorerCalls(", "// The backstage is the root")
        assertTrue("PDF import must request a root if none was selected",
            entry.contains("if (editor.browseRoot != null) onImportPdf() else onPickRoot()"))

        val emptyRoot = between(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(emptyRoot.contains("stringResource(R.string.library_start_hint)"))
        assertTrue("Use app storage should remain a working primary action",
            emptyRoot.contains("PrimaryButton(NotesTakingIcons.database, stringResource(R.string.use_app_storage)) { editor.useInternalStorage() }"))
        assertTrue("Choose folder must remain available without app storage",
            emptyRoot.contains("TextButton(onClick = calls.pickRoot)"))
        assertTrue(emptyRoot.contains("stringResource(R.string.choose_folder)"))
    }

    @Test
    fun newNoteActionIsDirectAndCreateMenuKeepsOtherDocumentOptions() {
        val backstage = source("Backstage.kt")
        val fab = between(backstage, "private fun CreateButton(", "// --- explorer section ---")
        assertTrue(fab.contains("if (exporting) onExport() else onCreateMode(CreateMode.FILE)"))
        assertTrue(fab.contains("stringResource(R.string.new_note_menu)"))
        assertTrue(fab.contains("stringResource(R.string.export_pdf_action)"))

        val menu = between(backstage, "private fun NewItemMenuItems(", "/** What the explorer hands back")
        assertTrue("Menu items must be at least 48dp tall", menu.contains("Modifier.height(52.dp)"))
        assertTrue("Menu must close before triggering a Create or Import action",
            menu.contains("onClick = { onClose(); onPick() }"))
        for (action in listOf(
            "onCreateMode(CreateMode.FILE)",
            "onCreateMode(CreateMode.CANVAS)",
            "onCreateMode(CreateMode.FOLDER)",
            "onImportPdf()",
        )) assertTrue("Missing menu action: $action", menu.contains(action))

        assertTrue("Overflow must still expose the shared menu",
            backstage.contains("NewItemMenuItems({ moreOpen = false }, onCreateMode, calls.importPdf)"))
    }

    @Test
    fun eachEmptyFilterHasAVisibleWayBackToTheLibrary() {
        val backstage = source("Backstage.kt")
        val empty = between(backstage, "val empty = when {", "// Files scroll under the header")
        assertTrue(empty.contains("R.string.nothing_this_colour"))
        assertTrue(empty.contains("EmptyRecovery(stringResource(R.string.clear_colour_filter)) { colorFilter = null; selection.clear() }"))
        assertTrue(empty.contains("EmptyRecovery(stringResource(R.string.clear_search)) { query = \"\"; focusManager.clearFocus() }"))
        assertTrue(empty.contains("EmptyRecovery(stringResource(R.string.home)) { showRecent = false; stack.clear(); selection.clear() }"))
        assertTrue(empty.contains("EmptyRecovery(stringResource(R.string.all_kinds)) { kindFilter = null }"))
        assertTrue(empty.contains("EmptyArt.PAGES, offersCreate = true"))
    }

    @Test
    fun emptyFolderExposesCreateImportAndRecoveryActionsWithoutADeadEnd() {
        val backstage = source("Backstage.kt")
        val section = between(backstage, "empty != null && !shelves -> Column(", "layout == ExplorerLayout.GRID ->")
        assertTrue(section.contains("if (empty.offersCreate)"))
        assertTrue(section.contains("PrimaryButton(NotesTakingIcons.edit, stringResource(R.string.new_note_menu)) { onCreateMode(CreateMode.FILE) }"))
        assertTrue(section.contains("TextButton(onClick = { onCreateMode(CreateMode.CANVAS) })"))
        assertTrue(section.contains("TextButton(onClick = calls.importPdf)"))
        assertTrue(section.contains("TextButton(onClick = action.onClick)"))
        val pane = between(backstage, "internal fun EmptyPane(", "}")
        assertTrue(pane.contains("imePadding()"))
        assertTrue(pane.contains("verticalScroll(rememberScrollState())"))
    }

    @Test
    fun phoneAndTabletChromePreservesMinimumTouchTargets() {
        val backstage = source("Backstage.kt")
        val chrome = source("ExplorerChrome.kt")
        val newNote = source("NewNoteScreen.kt")

        assertTrue(backstage.contains("private const val COMPACT_WIDTH_DP = 600"))
        assertTrue(backstage.contains("Modifier.fillMaxWidth().height(56.dp)"))
        assertTrue(chrome.contains(".height(48.dp).widthIn(min = 48.dp)"))
        assertTrue(chrome.contains("modifier = Modifier.size(48.dp)"))
        assertTrue(chrome.contains("Modifier.width(48.dp)") ||
            chrome.contains(".width(48.dp)"))
        assertTrue(newNote.contains("val wide = screenW >= 720.dp && maxHeight >= 600.dp"))
        assertTrue(newNote.contains("columns = GridCells.Adaptive(100.dp)"))
        assertTrue(newNote.contains("columns = GridCells.Adaptive(112.dp)"))
    }

    @Test
    fun noteCreationCanBeCancelledAndCannotSubmitTwiceOrWithInvalidSize() {
        val newNote = source("NewNoteScreen.kt")
        val submit = between(newNote, "fun create() {", "fun setOrientation(")
        assertTrue(submit.contains("if (busy) return"))
        assertTrue(submit.contains("if (!canCreate) return"))
        assertTrue(submit.contains("onCreate(name.text, Editor.NewNoteSetup("))

        val header = between(newNote, "IconButton(onClick = onClose)", "HorizontalDivider(color = border)")
        assertTrue(header.contains("stringResource(R.string.close_new_note)"))
        assertTrue(header.contains("enabled = !busy && (if (coverOnly) hasCover else sizeOk)"))
        assertTrue(header.contains("onClick = { create() }"))
        assertTrue(header.contains("R.string.create_note"))
    }
}
