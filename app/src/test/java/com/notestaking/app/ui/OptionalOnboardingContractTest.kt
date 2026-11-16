package com.notestaking.app.ui

import com.notestaking.app.settings.Settings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * NP-002: preserve guest/offline access while the coordinator decides whether to add onboarding.
 * shortcut: Skip UI and persistence do not exist yet; enable the ignored acceptance contract
 * after COORD implements an explicit, consent-free Skip and device-verifies its touch target.
 */
class OptionalOnboardingContractTest {
    private fun source(relative: String): String {
        val path = "src/main/$relative"
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing source: $path" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(code: String, begin: String, end: String): String {
        val first = code.indexOf(begin)
        assertTrue("Missing source anchor: $begin", first >= 0)
        val last = code.indexOf(end, first + begin.length)
        assertTrue("Missing source boundary: $end", last > first)
        return code.substring(first, last)
    }

    @Test
    fun freshLaunchReachesLibraryWithoutAccountGateOrImplicitStorageConsent() {
        val activity = source("java/com/notestaking/app/MainActivity.kt")
        val launch = section(activity, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertTrue(launch.contains("val ed = remember { Editor(context).also { editor = it } }"))
        assertTrue(launch.contains("ed.restoreWorkspace()"))
        assertTrue(launch.contains("if (ready) EditorScreen("))
        assertFalse(launch.contains("login("))
        assertFalse(launch.contains("signIn("))
        assertFalse(launch.contains("recordPermissions.launch("))

        val home = source("java/com/notestaking/app/ui/Backstage.kt")
        val noRoot = section(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(noRoot.contains("stringResource(R.string.library_start_hint)"))
        assertTrue(noRoot.contains("stringResource(R.string.use_app_storage)) { editor.useInternalStorage() }"))
        assertTrue(noRoot.contains("TextButton(onClick = calls.pickRoot)"))
        assertFalse(noRoot.contains("signIn"))
    }

    @Test
    fun freshSettingsAndExplicitFolderChoiceSurviveRestartWithoutExtraConsent() {
        val fresh = Settings()
        assertNull(fresh.browseRoot)
        assertTrue(fresh.startOnHome)
        assertFalse(fresh.fingerDrawAutoChecked)
        assertFalse(fresh.prefs.fingerDraws)

        val restored = Settings.fromJson(fresh.toJson())
        assertNull(restored.browseRoot)
        assertTrue(restored.startOnHome)
        assertFalse(restored.fingerDrawAutoChecked)
        assertFalse(restored.prefs.fingerDraws)

        val chosen = "content://com.notestaking.app.documents/tree/notes"
        val stored = Settings.fromJson(fresh.copy(browseRoot = chosen).toJson())
        assertEquals(chosen, stored.browseRoot)
        assertFalse(stored.fingerDrawAutoChecked)
    }

    @Test
    fun microphoneAndNotificationRequestsAreNotTriggeredByOpeningOrCreatingNotes() {
        val manifest = source("AndroidManifest.xml")
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.RECORD_AUDIO"))
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"))

        val activity = source("java/com/notestaking/app/MainActivity.kt")
        val launch = section(activity, "override fun onCreate(savedInstanceState: Bundle?)", "// Stylus side-button keys")
        assertFalse(launch.contains("RequestMultiplePermissions"))
        assertFalse(launch.contains("recordPermissions.launch"))
        val recording = section(activity, "onRecordAudio = { pane ->", "onImportTemplate = {")
        assertTrue(recording.contains("if (!mic) add(android.Manifest.permission.RECORD_AUDIO)"))
        assertTrue(recording.contains("recordPermissions.launch(wanted.toTypedArray())"))

        val home = source("java/com/notestaking/app/ui/Backstage.kt")
        val create = section(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(create.contains("editor.createNoteFile(root, currentDocId, n, setup)"))
        assertFalse(create.contains("recordPermissions.launch"))
        assertFalse(source("java/com/notestaking/app/ui/NewNoteScreen.kt").contains("requestPermissions("))
    }

    @Ignore("COORD must implement the visible Skip control and persist its choice independently of permission grants")
    @Test
    fun visibleAccessibleSkipPersistsSeparatelyFromAnyConsent() {
        val ui = source("java/com/notestaking/app/MainActivity.kt") +
            source("java/com/notestaking/app/ui/Backstage.kt")
        val settings = source("java/com/notestaking/app/settings/Settings.kt")
        assertTrue("Expose a visible labelled Skip action", ui.contains("R.string.skip_onboarding"))
        assertTrue("Persist Skip independently of microphone, sync or storage access",
            settings.contains("onboardingSkipped"))
        assertTrue("Skip must survive Settings save/reload",
            settings.contains("onboarding_skipped"))
    }
}
