package com.notestaking.app.ui

import com.notestaking.app.settings.ToolbarSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** B101 compact Canvas sizing contracts; actual Compose rendering still needs GitHub and device QA. */
class B101CompactCanvasToolbarTest {
    @Test fun phoneWidthsFitSixActionsInTwoNonScrollingRows() {
        for (width in listOf(320f, 360f)) {
            val columns = compactCanvasColumns(width, 48f, pinnedButtons = 1)
            assertEquals(3, columns)
            assertEquals(2, (6 + columns - 1) / columns)
            assertTrue(columns * (48f + 8f) + 1 * (48f + 8f) + 8f <= width)
        }
    }

    @Test fun narrowSplitPanesKeepAllQuickActionsWithoutHorizontalOverflow() {
        assertEquals(2, compactCanvasColumns(180f, 48f, pinnedButtons = 1))
        assertEquals(1, compactCanvasColumns(180f, 48f, pinnedButtons = 2))
        assertEquals(3, compactCanvasColumns(320f, 48f, pinnedButtons = 2))
        assertEquals(1, compactCanvasColumns(320f, 48f, pinnedButtons = 0, vertical = true))
        for (size in ToolbarSize.entries) assertTrue(barMetrics(size).button.value >= 48f)
    }

    @Test fun existingPenEraserUndoAndMoreActionsAreWiredToRealEditor() {
        val relative = "src/main/java/com/notestaking/app/ui/InfiniteToolbar.kt"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        val code = requireNotNull(file).readText()
        val quick = code.substringAfter("private fun B101CanvasQuickTools(").substringBefore("/** Preserve both failure messages")
        assertTrue(quick.contains("editor.armTool(Tool.PEN)"))
        assertTrue(quick.contains("editor.armTool(Tool.ERASER)"))
        assertTrue(quick.contains("editor.armTool(type)"))
        assertTrue(quick.contains("ToolConfigPopup(editor, widthTool)"))
        assertTrue(quick.contains("EraserConfigPopup(editor)"))
        assertTrue(quick.contains("editor.toolConfig(widthTool).baseWidth"))
        assertTrue(quick.contains("editor.undo()"))
        assertTrue(quick.contains("onClick = expand"))
    }
}
