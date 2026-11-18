package com.notestaking.app

import com.notestaking.app.core.util.DocumentKind
import com.notestaking.app.platform.DocumentNames
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The new canvas title action must not rename the hidden paged note or race canvas autosaves. */
class CanvasRenameContractTest {
    private fun source(relative: String): String {
        val path = "src/main/java/com/notestaking/app/$relative"
        return listOf(File(path), File("app/$path")).first { it.isFile }.readText(Charsets.UTF_8)
    }

    @Test fun canvasRenameAlwaysKeepsItsNativeFileExtension() {
        for (raw in listOf("Drawing", "Drawing.xcanvas", "Drawing.ntcanvas", "Drawing.xnote")) {
            assertEquals("Drawing.ntcanvas", "${DocumentNames.stem(raw)}${DocumentKind.CANVAS.suffix}")
        }
        assertEquals("_CON.ntcanvas", "${DocumentNames.stem("CON")}${DocumentKind.CANVAS.suffix}")
        assertFalse("${DocumentNames.stem("../draft")}${DocumentKind.CANVAS.suffix}".contains('/'))
        assertEquals("Renamed.xcanvas", DocumentNames.canvasRename("Old.xcanvas", "Renamed.ntcanvas"))
    }

    @Test fun headerAllowsCanvasTitleEditingWithoutOpeningPagedRename() {
        val header = source("MainActivity.kt").substringAfter("private fun EditorPaneHeader(").substringBefore("private val SwallowTouches")
        assertTrue(header.contains("onClickLabel = stringResource(R.string.rename_note)"))
        assertTrue(header.contains("editor.renameCurrentCanvasDocument(name)"))
        assertTrue(header.contains("editor.renameCurrentDocument(name)"))
        assertFalse(header.contains("showingFullTitle"))
        assertTrue(header.contains("enabled = !renameBusy"))
    }

    @Test fun canvasRenameWaitsForWritesAndKeepsLiveDocumentBoundToTheNewUri() {
        val editor = source("ui/Editor.kt")
        val canvasRename = editor.substringAfter("fun renameCurrentCanvasDocument(").substringBefore("/** Deletes a document")
        val backingRename = editor.substringAfter("fun renameDocument(").substringBefore("/** Carry everything keyed")
        assertTrue(canvasRename.contains("canvasAutosaveUri != uri"))
        assertTrue(canvasRename.contains("flushCanvasThen(showOverlay = true)"))
        assertTrue(canvasRename.contains("doc.path == uri && renameDocument(uri, fileName)"))
        assertTrue(backingRename.contains("it.path = resultUri"))
        assertTrue(backingRename.contains("it.displayName = finalName"))
        assertTrue(backingRename.contains("canvasAutosaveUri = resultUri"))
        assertTrue(backingRename.contains("onDocumentMoved(docUri, resultUri, finalName)"))
        assertTrue(backingRename.contains("DocumentNames.canvasRename(originalName, newName)"))
    }
}
