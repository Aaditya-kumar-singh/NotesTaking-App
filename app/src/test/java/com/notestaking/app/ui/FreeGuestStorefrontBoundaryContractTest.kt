package com.notestaking.app.ui

import com.notestaking.app.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * NP-107: keep offline guest creation independent of monetization.
 * shortcut: Source contracts do not prove merged APK billing absence or accessibility bounds;
 * rerun exact-SHA CI and device QA if the coordinator approves a commercial feature.
 */
class FreeGuestStorefrontBoundaryContractTest {
    private fun file(vararg paths: String): String {
        val source = paths.map(::File).firstOrNull { it.isFile }
        requireNotNull(source) { "Missing source: ${paths.joinToString()}" }
        return source.readText(Charsets.UTF_8)
    }

    private fun app(path: String) = file("src/main/$path", "app/src/main/$path")

    private fun section(text: String, start: String, end: String): String {
        val i = text.indexOf(start)
        assertTrue("Missing anchor: $start", i >= 0)
        val j = text.indexOf(end, i + start.length)
        assertTrue("Missing boundary: $end", j > i)
        return text.substring(i, j)
    }

    @Test
    fun packagingHasNoBillingDependencyOrNetworkPermission() {
        val build = file("build.gradle.kts", "app/build.gradle.kts")
        val catalog = file("../gradle/libs.versions.toml", "gradle/libs.versions.toml")
        val billingTokens = listOf("billingclient", "com.android.billingclient", "purchasesupdatedlistener", "stripe", "revenuecat", "paddle")
        for (token in billingTokens) {
            assertFalse("Unexpected billing dependency $token", build.lowercase().contains(token))
            assertFalse("Unexpected billing version alias $token", catalog.lowercase().contains(token))
        }
        val manifest = app("AndroidManifest.xml")
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        val labels = app("res/values/strings.xml")
        assertTrue(labels.contains("No account needed."))
        assertFalse(labels.contains("name=\"subscribe\""))
        assertFalse(labels.contains("name=\"unlock_premium\""))
        assertFalse(labels.contains("name=\"buy_now\""))
    }

    @Test
    fun coldStartAndFirstRunRemainFreeGuestPathsWithStorageFallback() {
        val activity = app("java/com/notestaking/app/MainActivity.kt")
        val launch = section(activity, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertTrue(launch.contains("ed.restoreWorkspace()"))
        assertTrue(launch.contains("if (ready) EditorScreen("))
        for (gate in listOf("startPurchase(", "isPremium", "isSubscribed", "signIn(", "login(")) {
            assertFalse("Launch should not depend on $gate", launch.contains(gate))
        }

        val home = app("java/com/notestaking/app/ui/Backstage.kt")
        val noRoot = section(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(noRoot.contains("PrimaryButton(NotesTakingIcons.database, stringResource(R.string.use_app_storage)) { editor.useInternalStorage() }"))
        assertTrue(noRoot.contains("TextButton(onClick = calls.pickRoot)"))
        assertTrue(noRoot.contains("Text(stringResource(R.string.choose_folder))"))
        for (gate in listOf("isPremium", "paywall", "checkout", "purchase", "signIn")) {
            assertFalse("Guest storage should not be gated by $gate", noRoot.contains(gate))
        }
    }

    @Test
    fun createActionsStayAccessibleWithoutPaymentAndRetainFailureRecovery() {
        val home = app("java/com/notestaking/app/ui/Backstage.kt")
        val menu = section(home, "private fun NewItemMenuItems(", "/** What the explorer hands back")
        assertTrue(menu.contains("Modifier.height(52.dp)"))
        assertTrue(menu.contains("item(NotesTakingIcons.edit, stringResource(R.string.new_note_menu)) { onCreateMode(CreateMode.FILE) }"))
        assertTrue(menu.contains("item(NotesTakingIcons.canvas, stringResource(R.string.new_canvas_menu)) { onCreateMode(CreateMode.CANVAS) }"))
        val action = section(home, "private fun CreateButton(", "// --- explorer section ---")
        assertTrue(action.contains("onClick = { if (exporting) onExport() else onCreateMode(CreateMode.FILE) }"))
        val create = section(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(create.contains("editor.createNoteFile(root, currentDocId, n, setup)"))
        assertTrue(create.contains("callsNow.value.openFile(uri)"))
        assertTrue(create.contains("fieldError = resources.getString(R.string.err_create_note)"))
        assertTrue(create.contains("onClose = { fieldError = null; onCreateMode(CreateMode.NONE) }"))
        for (gate in listOf("purchase", "billing", "premium", "subscription")) {
            assertFalse(create.contains(gate))
        }

        val note = app("java/com/notestaking/app/ui/NewNoteScreen.kt")
        val submit = section(note, "fun create() {", "fun setOrientation(o: Orientation)")
        assertTrue(submit.contains("if (busy) return"))
        assertTrue(submit.contains("if (!canCreate) return"))
        assertTrue(submit.contains("onCreate(name.text, Editor.NewNoteSetup("))
        assertTrue(note.contains("Dialog(onDismissRequest = onClose"))
        assertTrue(note.contains("IconButton(onClick = onClose)"))
    }

    @Test
    fun unknownEntitlementFieldsCannotConvertGuestSettingsIntoAPaywall() {
        val local = "content://com.notestaking.app.documents/tree/root"
        val raw = Settings(browseRoot = local, startOnHome = true).toJson()
        raw.put("subscription_active", true)
        raw.put("premium_unlocked", true)
        raw.put("store_receipt", "purchase-token")
        raw.getJSONObject("prefs").put("billing_customer_id", "sensitive-id")

        val restored = Settings.fromJson(raw)
        assertEquals(local, restored.browseRoot)
        assertTrue(restored.startOnHome)
        val persisted = restored.toJson().toString()
        for (token in listOf("subscription_active", "premium_unlocked", "store_receipt", "purchase-token", "billing_customer_id", "sensitive-id")) {
            assertFalse("Unknown purchase metadata must not enter settings: $token", persisted.contains(token))
        }
        assertTrue(Settings.fromJson(Settings().toJson()).startOnHome)

        val main = app("java/com/notestaking/app/MainActivity.kt")
        val export = section(main, "fun exportPicked(uri: Uri?) {", "// Save a copy of an explorer file")
        assertTrue(export.contains("if (uri == null || pending == null || root == null) return"))
        assertTrue(export.contains("ActivityResultContracts.CreateDocument(\"application/pdf\")"))
        assertTrue(export.contains("ActivityResultContracts.CreateDocument(\"application/zip\")"))
    }
}
