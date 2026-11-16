package com.notestaking.app.ui

import com.notestaking.app.core.pdf.FakePageText
import com.notestaking.app.core.pdf.TextPos
import com.notestaking.app.core.pdf.TextSelection
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * NP-105: audit PDF text selection for study/translation without changing shared UI.
 * shortcut: External ACTION_PROCESS_TEXT is not an offline translator and clears
 * selection; coordinator must implement/policy-review optional translation before acceptance.
 */
class PdfStudySelectionContractTest {
    private fun source(path: String): String {
        val relative = "src/main/$path"
        val f = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        requireNotNull(f) { "Missing PDF source: $relative" }
        return f.readText(Charsets.UTF_8)
    }

    private fun section(code: String, start: String, end: String): String {
        val i = code.indexOf(start)
        assertTrue("Missing source anchor: $start", i >= 0)
        val j = code.indexOf(end, i + start.length)
        assertTrue("Missing source boundary: $end", j > i)
        return code.substring(i, j)
    }

    @Test
    fun englishAndHindiPdfTextCanBeReadWithoutMovingOriginalSelection() {
        val en = "Read and learn"
        val hi = "नमस्ते भारत"
        val pages = mapOf(
            0 to FakePageText().line(en, 0f, 0f).build(),
            1 to FakePageText().line(hi, 0f, 0f).build(),
        )
        val selected = TextSelection(TextPos(0, 5), TextPos(1, hi.length))
        val originalStart = selected.start
        val originalEnd = selected.end

        assertEquals("and learn\n$hi", selected.text { pages[it] })
        assertEquals(5 until en.length, selected.rangeOn(0, pages.getValue(0)))
        assertEquals(0 until hi.length, selected.rangeOn(1, pages.getValue(1)))
        assertEquals(originalStart, selected.start)
        assertEquals(originalEnd, selected.end)
        assertEquals("and learn\n$hi", selected.text { pages[it] })
    }

    @Test
    fun missingPdfTextOrEmptySelectionDoesNotFabricateATranslatedAnswer() {
        val en = "Study words"
        val page = FakePageText().line(en, 0f, 0f).build()
        val selected = TextSelection(TextPos(0, 0), TextPos(2, 5))
        assertEquals(en, selected.text { if (it == 0) page else null })
        assertTrue(selected.rangeOn(3, page).isEmpty())
        assertEquals("", TextSelection(TextPos(0, 3), TextPos(0, 3)).text { page })
        assertEquals("Study", TextSelection(TextPos(0, 0), TextPos(0, 5)).text { page })

        val controller = source("java/com/notestaking/app/canvas/PdfTextController.kt")
        assertTrue(controller.contains("if (gen != pressGen) return@request"))
        assertTrue(controller.contains("if (text == null) noText()"))
        assertTrue(controller.contains("selection = null"))
    }

    @Test
    fun selectionMenuUsesOnlyDiscoveredAndroidProcessTextHandlers() {
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val discovery = section(editor, "fun pdfTextActions(): List<Pair<String, android.content.ComponentName>> {",
            "/** Hands the PDF text selection to [app] read-only")
        assertTrue(discovery.contains("android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).setType(\"text/plain\")"))
        assertTrue(discovery.contains("pm.queryIntentActivities(query"))
        assertTrue(discovery.contains("it.loadLabel(pm).toString() to android.content.ComponentName("))

        val menu = section(source("java/com/notestaking/app/ui/CanvasMenus.kt"),
            "fun PdfSelectionMenu(editor: Editor) {", "fun MarkupMenu(editor: Editor) {")
        assertTrue(menu.contains("val actions = remember { editor.pdfTextActions() }"))
        assertTrue(menu.contains("if (actions.isNotEmpty()) {"))
        assertTrue(menu.contains("ActionIcon(NotesTakingIcons.more, stringResource(R.string.more))"))
        assertTrue(menu.contains("DropdownMenuItem(text = { Text(label) }, onClick = {"))
        assertTrue(menu.contains("editor.processPdfText(app)"))
        assertTrue(menu.contains("editor.copyPdfText()"))
        assertFalse(menu.contains("R.string.translate_selected"))

        val manifest = source("AndroidManifest.xml")
        assertTrue(manifest.contains("android.intent.action.PROCESS_TEXT"))
        assertTrue(manifest.contains("android:mimeType=\"text/plain\""))
    }

    @Test
    fun externalProcessingIsUserTriggeredReadOnlyAndCapsSelectedText() {
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val dispatch = section(editor, "fun processPdfText(app: android.content.ComponentName) {",
            "/** The clipboard's text content")
        assertTrue(editor.contains("private const val PROCESS_TEXT_MAX = 100_000"))
        assertTrue(dispatch.contains("withPdfSelectionText { text ->"))
        assertTrue(dispatch.contains("android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT)"))
        assertTrue(dispatch.contains(".setComponent(app)"))
        assertTrue(dispatch.contains(".putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, text.take(PROCESS_TEXT_MAX))"))
        assertTrue(dispatch.contains(".putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, true)"))
        assertTrue(dispatch.contains("runCatching { viewContext.startActivity(intent) }"))
        assertTrue("Current external dispatch clears selection: coordinator must change for translation",
            dispatch.contains("pdfText.clear()"))
        assertFalse(dispatch.contains("applyMarkupEdit("))
        assertFalse(dispatch.contains("state.document.pages.add("))

        val extract = section(editor, "private fun withPdfSelectionText(use: (String) -> Unit) {",
            "/** Copies the PDF text selection")
        assertTrue(extract.contains("val sel = pdfText.selection ?: return"))
        assertTrue(extract.contains("val src = pdfSource ?: return"))
        assertTrue(extract.contains("src.text.peek("))
        assertTrue(extract.contains("withContext(Dispatchers.IO)"))
        assertTrue(extract.contains("if (pdfSource === src) use(text)"))
        assertFalse(source("AndroidManifest.xml").contains("android.permission.INTERNET"))
        // An external handler can still be online; lack of host INTERNET is not an offline guarantee.
    }

    @Test
    fun dismissingOrMovingSelectionMenuKeepsPdfSelectionAvailable() {
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val menu = section(editor, "private fun settlePdfTextMenu() {",
            "private fun withPdfSelectionText(use: (String) -> Unit) {")
        assertTrue(menu.contains("if (pdfText.selection == null) return"))
        assertTrue(menu.contains("pdfTextMenu = null"))
        assertTrue(menu.contains("view.postDelayed(showPdfTextMenu, PDF_TEXT_MENU_SETTLE_MS)"))
        assertTrue(menu.contains("fun dismissPdfTextMenu() {"))
        assertFalse(menu.contains("pdfText.clear()"))

        val canvas = source("java/com/notestaking/app/canvas/PdfTextController.kt")
        assertTrue(canvas.contains("fun release() {"))
        assertTrue(canvas.contains("(selecting || tool) && sel != null -> onSettled()"))
        assertTrue(canvas.contains("fun clear() {"))
        assertTrue(canvas.contains("onCleared()"))
    }

    @Ignore("Coordinator-owned feature missing: offline English/Hindi translation result must preserve the PDF selection")
    @Test
    fun optionalTranslationMustNotClearOriginalSelectedText() {
        val editor = source("java/com/notestaking/app/ui/Editor.kt")
        val dispatch = section(editor, "fun processPdfText(app: android.content.ComponentName) {",
            "/** The clipboard's text content")
        assertFalse("Translation must preserve the original selection", dispatch.contains("pdfText.clear()"))
    }
}
