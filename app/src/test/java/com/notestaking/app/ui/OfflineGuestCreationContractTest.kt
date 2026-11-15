package com.notestaking.app.ui

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.format.DocumentCodec
import com.notestaking.app.platform.ProviderOutput
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NP-001: offline/guest note creation contracts.
 * shortcut: These JVM/source checks cannot simulate airplane mode, SAF grants or relaunch;
 * repeat NT-010 on an Android device before accepting the feature.
 */
class OfflineGuestCreationContractTest {
    private fun source(path: String): String {
        val relative = "src/main/$path"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing production source: $relative" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(source: String, start: String, end: String): String {
        val a = source.indexOf(start)
        assertTrue("Missing source anchor: $start", a >= 0)
        val b = source.indexOf(end, a + start.length)
        assertTrue("Missing source boundary: $end", b > a)
        return source.substring(a, b)
    }

    @Test
    fun freshLaunchOpensLibraryWithoutInternetOrAccountGate() {
        val manifest = source("AndroidManifest.xml")
        assertFalse("Core notes must not require the INTERNET permission", manifest.contains("android.permission.INTERNET"))
        val activity = source("java/com/notestaking/app/MainActivity.kt")
        val launch = section(activity, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertTrue(launch.contains("setContent {"))
        assertTrue(launch.contains("val ed = remember { Editor(context).also { editor = it } }"))
        assertTrue(launch.contains("ed.restoreWorkspace()"))
        assertTrue(launch.contains("if (ready) EditorScreen("))
        assertFalse(launch.contains("signIn("))
        assertFalse(launch.contains("login("))
        assertFalse(launch.contains("isAuthenticated"))
        val screen = section(activity, "private fun EditorScreen(", "// The second pane raises its own messages")
        assertTrue(screen.contains("Backstage is the root of the stack"))
    }

    @Test
    fun aGuestCanChooseInternalStorageWithoutSelectingAnExternalFolder() {
        val home = source("java/com/notestaking/app/ui/Backstage.kt")
        val noRoot = section(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(noRoot.contains("stringResource(R.string.use_app_storage)) { editor.useInternalStorage() }"))
        assertTrue(noRoot.contains("TextButton(onClick = calls.pickRoot)"))
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val storage = section(editor, "fun useInternalStorage() {", "/** The pins under the current explorer root")
        assertTrue(storage.contains("updateBrowseRoot(com.notestaking.app.platform.AppStorageDocumentsProvider.treeUri(appContext).toString())"))
        val provider = source("java/com/notestaking/app/platform/AppStorageDocumentsProvider.kt")
        assertTrue(provider.contains("val base = context.getExternalFilesDir(null) ?: context.filesDir"))
        assertTrue(provider.contains("return File(base, DIR_NAME).apply { if (!exists()) mkdirs() }"))
        assertTrue(provider.contains("DocumentsContract.buildTreeDocumentUri(authority(context), ROOT_DOC_ID)"))
    }

    @Test
    fun blankNoteCreationAndOpenDoNotDependOnAuthentication() {
        val home = source("java/com/notestaking/app/ui/Backstage.kt")
        assertTrue(home.contains("onCreateMode(CreateMode.FILE)"))
        val flow = section(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(flow.contains("NewNoteScreen("))
        assertTrue(flow.contains("onCreate = { n, setup ->"))
        assertTrue(flow.contains("withContext(Dispatchers.IO) { editor.createNoteFile(root, currentDocId, n, setup) }"))
        assertTrue(flow.contains("if (uri != null) {"))
        assertTrue(flow.contains("callsNow.value.openFile(uri)"))
        assertTrue(flow.contains("fieldError = resources.getString(R.string.err_create_note)"))

        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val create = section(editor, "fun createNoteFile(treeUri: String, parentDocId: String, rawName: String, setup: NewNoteSetup)", "/** Imports the PDF")
        assertTrue(create.contains("Document.blankPixels(Document.DEFAULT_NEW_PAGES, w, h, dpi)"))
        assertTrue(create.contains("return createNoteFile(treeUri, parentDocId, name) { codec.write(doc, it) }"))

        val note = source("java/com/notestaking/app/ui/NewNoteScreen.kt")
        val submit = section(note, "fun create() {", "fun setOrientation(")
        assertTrue(submit.contains("if (busy) return"))
        assertTrue(submit.contains("if (!canCreate) return"))
        assertTrue(submit.contains("onCreate(name.text, Editor.NewNoteSetup("))
    }

    @Test
    fun blankNativeDocumentReallySavesAndReopensWithoutNetworkOrCredentials() {
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val original = Document.blankPixels(width = 1240.0, height = 1754.0).also {
            it.created = 1_789_000_000_000L
        }
        val file = File.createTempFile("np001-guest-", ".xnote")
        try {
            file.outputStream().use { codec.write(original, it) }
            assertTrue("A blank file must contain native document bytes", file.length() > 0L)
            val reopened = file.inputStream().use { codec.read(it) }
            assertEquals(1, reopened.pages.size)
            assertEquals(original.pages.single().width, reopened.pages.single().width, 0.0)
            assertEquals(original.pages.single().height, reopened.pages.single().height, 0.0)
            assertEquals(original.dpi, reopened.dpi)
            assertEquals(original.created, reopened.created)
        } finally {
            file.delete()
        }
    }

    @Test
    fun deniedProviderWritesAndCancelledFolderPickerNeverReportNoteCreated() {
        assertThrows(IOException::class.java) {
            ProviderOutput.write(null) { error("A denied write must never run its body") }
        }
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val write = section(editor, "private fun createNoteFile(treeUri: String, parentDocId: String, name: String, write:", "/** Creates a blank")
        assertTrue(write.contains("android.provider.DocumentsContract.createDocument("))
        assertTrue(write.contains("appContext.contentResolver.openOutputStream(uri, \"wt\")"))
        assertTrue(write.contains("runCatching { android.provider.DocumentsContract.deleteDocument(appContext.contentResolver, uri) }"))
        assertTrue(write.contains("null"))

        val activity = source("java/com/notestaking/app/MainActivity.kt")
        val folder = section(activity, "val pickRootLauncher = rememberLauncherForActivityResult(", "/** Open the \"Save as\" picker")
        assertTrue(folder.contains("uri?.let {"))
        assertTrue(folder.contains("resolver.takePersistableUriPermission(it, rwFlags)"))
        assertTrue(folder.contains("editor.updateBrowseRoot(it.toString())"))
    }
}
