package com.notestaking.app.ui

import com.notestaking.app.settings.RecentDoc
import com.notestaking.app.settings.Settings
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NP-003: startup and home/recent navigation contracts.
 * shortcut: Source contracts cannot measure cold launch or assert actual SAF/session recovery;
 * accept only after NT-011 physical phone QA and exact integrated-SHA CI.
 */
class FastLaunchRecentHomeContractTest {
    private fun source(path: String): String {
        val relative = "src/main/java/com/notestaking/app/$path"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing startup source: $relative" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(text: String, start: String, end: String): String {
        val i = text.indexOf(start)
        assertTrue("Missing source anchor $start", i >= 0)
        val j = text.indexOf(end, i + start.length)
        assertTrue("Missing source boundary $end", j > i)
        return text.substring(i, j)
    }

    @Test
    fun coldLaunchRestoresWorkspaceWithoutBlockingLibraryPrewarm() {
        val main = source("MainActivity.kt")
        val launch = section(main, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertTrue(launch.contains("val ed = remember { Editor(context).also { editor = it } }"))
        assertTrue(launch.contains("ed.restoreWorkspace()"))
        assertTrue(launch.contains("ready = true"))
        assertTrue(launch.contains("ed.prewarmBackstage()"))
        assertTrue(launch.indexOf("ready = true") < launch.indexOf("ed.prewarmBackstage()"))
        assertTrue(launch.contains("if (ready) EditorScreen("))

        val screen = section(main, "private fun EditorScreen(", "var showShareChooser by remember")
        assertTrue(screen.contains("var backstageView by remember { mutableStateOf(com.notestaking.app.ui.BackstageView.HOME) }"))
        val editor = source("ui/Editor.kt")
        val warm = section(editor, "fun prewarmBackstage() {", "// --- per-file actions")
        assertTrue(warm.contains("autosaveScope.launch {"))
        assertTrue(warm.contains("kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO)"))
        assertTrue(warm.contains("browseChildren(root, browseRootDocId(root))"))
        assertTrue(warm.contains("runCatching {"))
    }

    @Test
    fun corruptOrMissingWorkspaceCannotForceTheEditorOnTop() {
        val editor = source("ui/Editor.kt")
        val restore = section(editor, "suspend fun restoreWorkspace() {", "private fun refreshView()")
        assertTrue(restore.contains("runCatching { restoreSession(openDocument = saved?.primaryOpen == true) }"))
        assertTrue(restore.contains(".onFailure { sessionLoaded = true }"))
        assertTrue(restore.contains("val restored = runCatching { other.restoreSession(openDocument = true) }.getOrDefault(false)"))
        assertTrue(restore.contains("abandonSecondary()"))
        assertTrue(restore.contains("workspaceReady = true"))
        assertTrue(restore.contains("noteOpen = openDocument"))
        assertTrue(restore.contains("sessionLoaded = true"))
        val main = source("MainActivity.kt")
        assertTrue(main.contains("if (focused.noteOpen) {"))
        assertTrue(main.contains("editor.goHomeAll()"))
    }

    @Test
    fun recentNotesPersistInOrderAndMalformedEntriesDoNotBlockHome() {
        val recent = listOf(
            RecentDoc("content://files/tree/a", "Newest.xnote", 300L),
            RecentDoc("content://files/tree/b", "Older.xnote", 100L),
        )
        val settings = Settings(recentDocs = recent, lastFolder = "content://files/tree/notes")
        val restored = Settings.fromJson(settings.toJson())
        assertEquals(recent, restored.recentDocs)
        assertEquals(settings.lastFolder, restored.lastFolder)

        val mixed = settings.toJson()
        mixed.put(
            "recent_docs",
            JSONArray()
                .put(JSONObject().put("name", "missing URI"))
                .put(JSONObject().put("uri", recent.first().uri).put("name", recent.first().name).put("opened", 300L)),
        )
        assertEquals(listOf(recent.first()), Settings.fromJson(mixed).recentDocs)

        val editor = source("ui/Editor.kt")
        val entries = section(editor, "fun recentEntries(treeUri: String): List<RecentEntry> {", "/** The folder the explorer last showed")
        assertTrue(entries.contains("recentDocs.filter { isUnderTree(it.uri, treeUri) }"))
        assertTrue(entries.contains("stampOf(uri.toString()) ?: run { gone.add(r.uri); return@mapNotNull null }"))
        assertTrue(entries.contains("setRecents(recentDocs.filterNot { it.uri in gone })"))
        assertTrue(entries.contains("out.distinctBy { it.entry.documentUri }"))
    }

    @Test
    fun homeQuickCreateOpensEditableNoteInTwoActionsAfterStorageSetup() {
        val home = source("ui/Backstage.kt")
        val button = section(home, "private fun CreateButton(", "// --- explorer section ---")
        assertTrue(button.contains("onClick = { if (exporting) onExport() else onCreateMode(CreateMode.FILE) }"))
        val dialog = section(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(dialog.contains("NewNoteScreen("))
        assertTrue(dialog.contains("onCreate = { n, setup ->"))
        assertTrue(dialog.contains("withContext(Dispatchers.IO) { editor.createNoteFile(root, currentDocId, n, setup) }"))
        assertTrue(dialog.contains("if (uri != null) {"))
        assertTrue(dialog.contains("callsNow.value.openFile(uri)"))

        val main = source("MainActivity.kt")
        val opening = section(main, "suspend fun openInto(target: Editor, uriStr: String)", "/** Open two picked files")
        assertTrue(opening.contains("target.openAsync(uriStr, name)"))
        val editor = source("ui/Editor.kt")
        val open = section(editor, "suspend fun openAsync(uri: String, name: String? = null)", "/** Aborts an in-flight open")
        assertTrue(open.contains("noteOpen = true"))
        assertTrue(open.contains("rememberOpened(uri, name)"))
    }

    @Test
    fun staleRecentOrLastFolderHasHomeRecoveryWithoutAForcedLogin() {
        val home = source("ui/Backstage.kt")
        val last = section(home, "// Home opens on the folder last shown", "// Changing folders drops a stale query")
        assertTrue(last.contains("if (prefs.homeOpensTo == \"last\" && nav == null && last != null && stack.isEmpty())"))
        assertTrue(last.contains("editor.folderChain(root, last)"))
        assertTrue(last.contains("?.let { chain -> if (stack.isEmpty()) stack.addAll(chain) }"))
        assertTrue(last.contains("settled = true"))

        val empty = section(home, "val empty = when {", "// Files scroll under")
        assertTrue(empty.contains("stringResource(R.string.recent_empty)"))
        assertTrue(empty.contains("EmptyRecovery(stringResource(R.string.home)) { showRecent = false; stack.clear(); selection.clear() }"))
        val launch = section(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(launch.contains("editor.useInternalStorage()"))
        assertTrue(launch.contains("calls.pickRoot"))
        assertFalse(launch.contains("signIn"))
    }
}
