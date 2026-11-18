package com.notestaking.app

import com.notestaking.app.ui.PagedFloatingAction
import com.notestaking.app.ui.pagedFloatingActions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coordinator-owned coverage for the floating dock's paged-note routing and tool order. */
class UniversalFloatingDockIntegrationTest {
    private fun source(path: String): String {
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        return requireNotNull(file) { "Missing source file: $path" }.readText()
    }

    @Test fun blankAndMultiPageNotesKeepTheFiveCoreDrawingActions() {
        assertEquals(
            listOf(
                PagedFloatingAction.PEN,
                PagedFloatingAction.ERASER,
                PagedFloatingAction.PEN_TYPE,
                PagedFloatingAction.SIZE,
                PagedFloatingAction.UNDO,
            ),
            pagedFloatingActions(hasPdf = false),
        )
        assertFalse(PagedFloatingAction.PDF_MODE in pagedFloatingActions(false))
    }

    @Test fun importedPdfAlwaysRetainsReadAnnotateAccessWithoutLosingDrawingActions() {
        val regular = pagedFloatingActions(false)
        val pdf = pagedFloatingActions(true)
        assertEquals(regular, pdf.dropLast(1))
        assertEquals(PagedFloatingAction.PDF_MODE, pdf.last())
        assertEquals(pdf.size, pdf.distinct().size)
    }

    @Test fun eachEditorUsesItsRealHandlersAndExistingFloatingShell() {
        val dock = source("src/main/java/com/notestaking/app/ui/FavoriteToolDock.kt")
        val entrypoint = source("src/main/java/com/notestaking/app/MainActivity.kt")
        assertTrue(dock.contains("if (host is InfiniteEditor)"))
        assertTrue(dock.contains("B101FloatingCanvasQuickTools(host, LocalFloatingCanvasColumns.current)"))
        assertTrue(dock.contains("if (host is Editor)"))
        assertTrue(dock.contains("PagedFloatingQuickTools(host, activeTool, LocalFloatingCanvasColumns.current, onSelect)"))
        assertTrue(dock.contains("onSelect(Tool.PEN)"))
        assertTrue(dock.contains("onSelect(Tool.ERASER)"))
        assertTrue(dock.contains("onSelect(type)"))
        assertTrue(dock.contains("editor.undo()"))
        assertTrue(dock.contains("PdfModeButton(editor)"))
        assertTrue(dock.contains("ToolConfigPopup(editor, widthTool)"))
        assertTrue(dock.contains("EraserConfigPopup(editor)"))
        assertTrue(entrypoint.contains("FavoriteToolDock(canvas, canvas.tool"))
        assertTrue(entrypoint.contains("FavoriteToolDock(editor, editor.tool"))
        val floating = source("src/main/java/com/notestaking/app/ui/FloatingToolbar.kt")
        assertTrue(floating.contains("favorites(if (showFavorites) count else 0)"))
        assertTrue(floating.contains("ToolbarIcon(NotesTakingIcons.chevronUp"))
    }
}
