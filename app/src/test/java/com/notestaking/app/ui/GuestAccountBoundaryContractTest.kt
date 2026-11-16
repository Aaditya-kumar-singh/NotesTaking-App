package com.notestaking.app.ui

import com.notestaking.app.settings.Settings
import org.json.JSONObject
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NP-108: account is optional and not implemented; guard the existing guest data boundary.
 * shortcut: These source/JVM checks do not authorize OAuth or prove merged-manifest
 * privacy. Coordinator approval and device tests are prerequisites for account work.
 */
class GuestAccountBoundaryContractTest {
    private fun source(path: String): String {
        val relative = "src/main/$path"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing account audit source: $relative" }
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
    fun freshGuestLaunchOffersHomeWithoutIdentityOrMandatoryInternet() {
        val main = source("java/com/notestaking/app/MainActivity.kt")
        val launch = section(main, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertTrue(launch.contains("val ed = remember { Editor(context).also { editor = it } }"))
        assertTrue(launch.contains("ed.restoreWorkspace()"))
        assertTrue(launch.contains("if (ready) EditorScreen("))
        assertFalse(launch.contains("signIn("))
        assertFalse(launch.contains("login("))
        assertFalse(launch.contains("isAuthenticated"))

        val home = source("java/com/notestaking/app/ui/Backstage.kt")
        val noRoot = section(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(noRoot.contains("stringResource(R.string.library_start_hint)"))
        assertTrue(noRoot.contains("stringResource(R.string.use_app_storage)) { editor.useInternalStorage() }"))
        assertTrue(noRoot.contains("TextButton(onClick = calls.pickRoot)"))
        assertFalse(noRoot.contains("signIn"))
        assertTrue(source("res/values/strings.xml").contains("No account needed."))
        assertFalse(source("AndroidManifest.xml").contains("android.permission.INTERNET"))
    }

    @Test
    fun guestSettingsRoundTripDropsUnrecognizedIdentityAndTokenFields() {
        val fresh = Settings()
        assertNull(fresh.browseRoot)
        assertTrue(fresh.startOnHome)
        val local = "content://com.notestaking.app.documents/tree/root"
        val original = Settings(browseRoot = local, startOnHome = false)
        val json = original.toJson()
        val identity = JSONObject()
            .put("google_id_token", "not-for-plain-settings")
            .put("microsoft_refresh_token", "not-for-plain-settings")
        json.put("oauth_identity", identity)
        json.put("access_token", "not-for-plain-settings")
        json.getJSONObject("prefs").put("refresh_token", "not-for-plain-settings")

        val restored = Settings.fromJson(json)
        assertEquals(local, restored.browseRoot)
        assertFalse(restored.startOnHome)
        val clean = restored.toJson().toString()
        assertFalse(clean.contains("not-for-plain-settings"))
        assertFalse(clean.contains("oauth_identity"))
        assertFalse(clean.contains("access_token"))
        assertFalse(clean.contains("refresh_token"))
        assertEquals(fresh.browseRoot, Settings.fromJson(fresh.toJson()).browseRoot)
    }

    @Test
    fun guestStorageCreationAndFolderPickerAreExplicitIndependentChoices() {
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val storage = section(editor, "fun useInternalStorage() {", "/** The pins under the current explorer root")
        assertTrue(storage.contains("AppStorageDocumentsProvider.treeUri(appContext).toString()"))

        val provider = source("java/com/notestaking/app/platform/AppStorageDocumentsProvider.kt")
        assertTrue(provider.contains("val base = context.getExternalFilesDir(null) ?: context.filesDir"))
        assertTrue(provider.contains("return File(base, DIR_NAME).apply { if (!exists()) mkdirs() }"))

        val main = source("java/com/notestaking/app/MainActivity.kt")
        val pick = section(main, "val pickRootLauncher = rememberLauncherForActivityResult(",
            "/** Open the \"Save as\" picker")
        assertTrue(pick.contains("ActivityResultContracts.OpenDocumentTree()"))
        assertTrue(pick.contains("uri?.let {"))
        assertTrue(pick.contains("resolver.takePersistableUriPermission(it, rwFlags)"))
        assertTrue(pick.contains("editor.updateBrowseRoot(it.toString())"))
        val create = section(source("java/com/notestaking/app/ui/Backstage.kt"),
            "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(create.contains("editor.createNoteFile(root, currentDocId, n, setup)"))
        assertTrue(create.contains("callsNow.value.openFile(uri)"))
        assertFalse(create.contains("account"))
        assertFalse(create.contains("oauth"))
    }

    @Test
    fun localExportAndShareRequireUserSelectedDestinations() {
        val main = source("java/com/notestaking/app/MainActivity.kt")
        val export = section(main, "fun exportPicked(uri: Uri?) {", "// Save a copy of an explorer file")
        assertTrue(export.contains("if (uri == null || pending == null || root == null) return"))
        assertTrue(export.contains("editor.exportPdfsAsync(root, pending.picked, uri.toString())"))
        assertTrue(export.contains("ActivityResultContracts.CreateDocument(\"application/pdf\")"))
        assertTrue(export.contains("ActivityResultContracts.CreateDocument(\"application/zip\")"))
        assertTrue(main.contains("ActivityResultContracts.CreateDocument(\"application/octet-stream\")"))
        val settingsExport = section(main, "val exportSettingsLauncher = rememberLauncherForActivityResult(",
            "var pendingBackup by remember")
        assertTrue(settingsExport.contains("if (uri == null) return@rememberLauncherForActivityResult"))
        assertTrue(settingsExport.contains("editor.exportSettings(it)"))
        assertTrue(main.contains("Intent(Intent.ACTION_SEND)"))
        assertTrue(main.contains("Intent.createChooser(send, resources.getString(R.string.share_named, stem))"))
        assertTrue(main.contains("Intent.FLAG_GRANT_READ_URI_PERMISSION"))
    }

    @Test
    fun nativeBackupsRemainDisabledAndProvidersRequireScopedGrants() {
        val manifest = source("AndroidManifest.xml")
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("@xml/backup_exclusions"))
        assertTrue(manifest.contains("@xml/data_extraction_rules"))
        assertTrue(manifest.contains("android.permission.MANAGE_DOCUMENTS"))
        assertTrue(manifest.contains("android:authorities=\"\${applicationId}.fileprovider\""))
        assertTrue(manifest.contains("android:grantUriPermissions=\"true\""))
        val backup = source("res/xml/backup_exclusions.xml")
        val extraction = source("res/xml/data_extraction_rules.xml")
        for (name in listOf("root", "file", "database", "sharedpref", "external")) {
            assertTrue(backup.contains("<exclude domain=\"$name\" path=\".\" />"))
            assertTrue(extraction.contains("<exclude domain=\"$name\" path=\".\" />"))
        }
        assertTrue(extraction.contains("<cloud-backup>"))
        assertTrue(extraction.contains("<device-transfer>"))
        val paths = source("res/xml/file_paths.xml")
        assertTrue(paths.contains("<cache-path name=\"shared\" path=\"share/\" />"))
        assertFalse(paths.contains("<root-path"))
        assertFalse(paths.contains("<external-path"))
    }
}
