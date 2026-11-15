package com.notestaking.app.ui

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Orientation
import com.notestaking.app.core.model.PageSize
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.util.DocumentKind
import com.notestaking.app.format.CanvasCodec
import com.notestaking.app.format.DocumentCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NP-005: two independent document types and their Home creation contracts.
 * shortcut: Unit codecs and read-only UI contracts do not validate SAF cancellation,
 * provider errors or stylus undo; run NT-012 on phone and tablet before acceptance.
 */
class StandardAndCanvasCreationContractTest {
    private fun source(relative: String): String {
        val path = "src/main/java/com/notestaking/app/$relative"
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing production source: $path" }
        return file.readText(Charsets.UTF_8)
    }

    private fun between(text: String, start: String, end: String): String {
        val from = text.indexOf(start)
        assertTrue("Missing anchor: $start", from >= 0)
        val to = text.indexOf(end, from + start.length)
        assertTrue("Missing boundary: $end", to > from)
        return text.substring(from, to)
    }

    @Test
    fun standardPagesRoundTripInPortraitAndLandscapeWithUniqueLivePageIds() {
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val seenIds = mutableSetOf<Long>()
        for (orientation in listOf(Orientation.PORTRAIT, Orientation.LANDSCAPE)) {
            val (width, height) = PageSize.A4.pixels(orientation, PageSize.DEFAULT_DPI)
            val original = Document.blankPixels(width = width, height = height).also {
                it.created = 1_789_000_000_000L
            }
            val page = original.pages.single()
            assertTrue(if (orientation == Orientation.PORTRAIT) page.width < page.height else page.width > page.height)
            assertTrue(page.uid > 0)
            assertTrue("Live note pages must have distinct identity", seenIds.add(page.uid))
            val bytes = ByteArrayOutputStream().also { codec.write(original, it) }.toByteArray()
            val saved = codec.read(ByteArrayInputStream(bytes))
            assertEquals(1, saved.pages.size)
            assertEquals(width, saved.pages.single().width, 1e-6)
            assertEquals(height, saved.pages.single().height, 1e-6)
            assertEquals(original.created, saved.created)
            assertTrue(saved.pages.single().uid > 0)
        }
    }

    @Test
    fun infiniteCanvasRoundTripsWithoutConvertingToPagedNote() {
        val codec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())
        val original = InfiniteDocument(created = 1_789_000_000_000L)
        assertTrue(original.isEmpty)
        val bytes = ByteArrayOutputStream().also { codec.write(original, it) }.toByteArray()
        assertTrue(bytes.isNotEmpty())
        val restored = codec.read(ByteArrayInputStream(bytes))
        assertEquals(0, restored.itemCount)
        assertEquals(original.created, restored.created)
        assertEquals(original.background, restored.background)
        assertEquals(original.dpi, restored.dpi)
    }

    @Test
    fun distinctNativeFormatsAndInkSurviveReopenWithoutMixingFileKinds() {
        assertEquals(".xnote", DocumentKind.NOTE.suffix)
        assertEquals(".xcanvas", DocumentKind.CANVAS.suffix)
        assertEquals(DocumentKind.NOTE, DocumentKind.ofName("Notebook.XNOTE"))
        assertEquals(DocumentKind.CANVAS, DocumentKind.ofName("Board.XCANVAS"))
        assertEquals("Board.xcanvas", DocumentKind.withSuffix("Board", DocumentKind.CANVAS))
        assertNotEquals(DocumentKind.NOTE, DocumentKind.ofName("Board.xcanvas"))

        val note = Document.blank(count = 1)
        val ink = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), mutableListOf(Sample(1.0, 2.0, 1.0)))
        note.pages.single().items.add(ink)
        val noteCodec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val noteBytes = ByteArrayOutputStream().also { noteCodec.write(note, it) }.toByteArray()
        assertEquals(1, noteCodec.read(ByteArrayInputStream(noteBytes)).pages.single().items.size)

        val canvas = InfiniteDocument()
        canvas.add(Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), mutableListOf(Sample(-10.0, 20.0, 0.5))))
        val canvasCodec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())
        val canvasBytes = ByteArrayOutputStream().also { canvasCodec.write(canvas, it) }.toByteArray()
        assertEquals(1, canvasCodec.read(ByteArrayInputStream(canvasBytes)).itemCount)
    }

    @Test
    fun creationSheetRoutesToSeparateCodecsAndUniqueNamesWithoutDuplicateFiles() {
        val home = source("ui/Backstage.kt")
        val menu = between(home, "private fun NewItemMenuItems(", "/** What the explorer hands back")
        assertTrue(menu.contains("onCreateMode(CreateMode.FILE)"))
        assertTrue(menu.contains("onCreateMode(CreateMode.CANVAS)"))
        assertTrue(menu.contains("stringResource(R.string.new_note_menu)"))
        assertTrue(menu.contains("stringResource(R.string.new_canvas_menu)"))

        val editor = source("ui/Editor.kt")
        val names = between(editor, "private fun uniqueDocumentName(", "/** Creates a new `.xnote`")
        assertTrue(names.contains("browseChildren(treeUri, parentDocId)"))
        assertTrue(names.contains("DocumentKind.stripSuffix(raw.trim())"))
        assertTrue(names.contains("var n = 1"))
        assertTrue(names.contains("in taken) n++"))
        val builders = between(editor, "fun createBlankCanvasFile(", "/** Imports the PDF")
        assertTrue(builders.contains("uniqueDocumentName(treeUri, parentDocId, rawName, com.notestaking.app.core.util.DocumentKind.CANVAS)"))
        assertTrue(builders.contains("canvasCodec.write(com.notestaking.app.core.infinite.InfiniteDocument("))
        assertTrue(builders.contains("uniqueDocumentName(treeUri, parentDocId, rawName, com.notestaking.app.core.util.DocumentKind.NOTE)"))
        assertTrue(builders.contains("Document.blankPixels(Document.DEFAULT_NEW_PAGES, w, h, dpi)"))
        assertTrue(builders.contains("return createNoteFile(treeUri, parentDocId, name) { codec.write(doc, it) }"))
        val write = between(editor, "private fun createNoteFile(treeUri: String, parentDocId: String, name: String, write:", "/** Creates a blank `.xcanvas`")
        assertTrue(write.contains("createDocument(appContext.contentResolver, parent, \"application/octet-stream\", name)"))
        assertTrue(write.contains("ProviderOutput.write("))
        assertTrue(write.contains("deleteDocument(appContext.contentResolver, uri)"))
    }

    @Test
    fun noteOrientationAndBothOpenPathsPreserveEditableDocumentKind() {
        val screen = source("ui/NewNoteScreen.kt")
        val dimensions = between(screen, "val pageMm = when {", "val portrait = pageMm.first <= pageMm.second")
        assertTrue(dimensions.contains("orientation == Orientation.PORTRAIT -> size.shortMm to size.longMm"))
        assertTrue(dimensions.contains("else -> size.longMm to size.shortMm"))
        val setOrientation = between(screen, "fun setOrientation(o: Orientation)", "fun pickPage(key: String)")
        assertTrue(setOrientation.contains("customW = customH"))
        assertTrue(setOrientation.contains("customH = w"))

        val main = source("MainActivity.kt")
        val open = between(main, "suspend fun openInto(target: Editor, uriStr: String)", "/** Open two picked files")
        assertTrue(open.contains("DocumentKind.CANVAS"))
        assertTrue(open.contains("target.openCanvasAsync(uriStr, name)"))
        assertTrue(open.contains("target.openAsync(uriStr, name)"))
        val editor = source("ui/Editor.kt")
        val canvas = between(editor, "suspend fun openCanvasAsync(uri: String, name: String?)", "/** Write [doc] to [uri] through")
        assertTrue(canvas.contains("canvasCodec.read(it, imageDir)"))
        assertTrue(canvas.contains("canvasOpen = true"))
        assertTrue(canvas.contains("noteOpen = true"))
        val paged = between(editor, "suspend fun openAsync(uri: String, name: String? = null)", "/** Aborts an in-flight open")
        assertTrue(paged.contains("noteOpen = true"))
        assertTrue(paged.contains("rememberOpened(uri, name)"))
    }

    @Test
    fun createCancelAndDeniedWritesDoNotReportFalseSuccess() {
        val home = source("ui/Backstage.kt")
        val canvasDialog = between(home, "if ((createMode == CreateMode.CANVAS || createMode == CreateMode.FOLDER", "namingColor?.let")
        assertTrue(canvasDialog.contains("editor.createBlankCanvasFile(root, currentDocId, n)"))
        assertTrue(canvasDialog.contains("if (uri != null) { onCreateMode(CreateMode.NONE); refreshKey++ } else fieldError = resources.getString(R.string.err_create_canvas)"))
        assertTrue(canvasDialog.contains("onDismiss = { fieldError = null; if (pendingImport != null) editor.cancelImport() else onCreateMode(CreateMode.NONE) }"))
        val noteDialog = between(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(noteDialog.contains("if (uri != null) {"))
        assertTrue(noteDialog.contains("callsNow.value.openFile(uri)"))
        assertTrue(noteDialog.contains("fieldError = resources.getString(R.string.err_create_note)"))
        assertTrue(noteDialog.contains("onClose = { fieldError = null; onCreateMode(CreateMode.NONE) }"))

        val editor = source("ui/Editor.kt")
        val write = between(editor, "private fun createNoteFile(treeUri: String, parentDocId: String, name: String, write:", "/** Creates a blank `.xcanvas`")
        assertTrue(write.contains("}.getOrNull() ?: return null"))
        assertTrue(write.contains("getOrElse {"))
        assertTrue(write.contains("deleteDocument(appContext.contentResolver, uri)"))
        assertTrue(write.contains("null"))
        val firstRun = between(home, "if (root == null) {", "val scope = rememberCoroutineScope()")
        assertTrue(firstRun.contains("editor.useInternalStorage()"))
        assertFalse(firstRun.contains("signIn"))
    }
}
