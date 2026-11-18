package com.notestaking.app.ui

import com.notestaking.app.settings.ToolbarSize
import com.notestaking.app.core.tools.Tool
import org.junit.Assert.assertNull
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

    @Test fun sizeControlNeverMislabelsSelectionOrNavigationAsPenWidth() {
        for (tool in listOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.HIGHLIGHTER, Tool.ERASER)) {
            assertEquals(tool, canvasSizeTool(tool))
        }
        for (tool in listOf(Tool.SELECT, Tool.LASSO, Tool.PAN, Tool.SHAPE, Tool.TAPE)) {
            assertNull(canvasSizeTool(tool))
        }
    }

    @Test fun floatingWidthsReserveTheGripAndMoreWithoutHorizontalScrolling() {
        val expected = mapOf(180f to 1, 240f to 2, 320f to 3, 360f to 3)
        for ((width, columns) in expected) {
            assertEquals(columns, floatingCanvasColumns(width, 48f, vertical = false))
            val remaining = width - 16f - 56f -
                (if (floatingGripStacks(width, false)) 0f else 56f)
            assertTrue(columns * 56f <= remaining)
            assertTrue((5 + columns - 1) / columns in 2..5)
        }
        assertTrue(floatingGripStacks(180f, false))
        assertTrue(floatingGripStacks(240f, false))
        assertTrue(!floatingGripStacks(320f, false))
        assertEquals(1, floatingCanvasColumns(360f, 48f, vertical = true))
    }

    @Test fun floatingRendererUsesRealEditorAndRetainsSeparateMoreAction() {
        val relative = "src/main/java/com/notestaking/app/ui/InfiniteToolbar.kt"
        val file = listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
        val code = requireNotNull(file).readText()
        val renderer = code.substringAfter("internal fun B101FloatingCanvasQuickTools(")
            .substringBefore("/** Preserve both failure messages")
        assertTrue(renderer.contains("B101CanvasQuickTools(editor, icons, columns, expand = {}, showMore = false)"))
        val floating = "src/main/java/com/notestaking/app/ui/FloatingToolbar.kt"
        val floatingFile = listOf(File(floating), File("app/$floating")).firstOrNull { it.isFile }
        val layout = requireNotNull(floatingFile).readText()
        assertTrue(layout.contains("LocalFloatingCanvasColumns provides canvasColumns"))
        assertTrue(layout.contains("if (expanded)"))
        assertTrue(layout.contains("BackHandler(enabled = expanded)"))
        assertTrue(layout.contains("grip()"))
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
        assertTrue(quick.contains("widthTool?.let { editor.toolConfig(it).baseWidth }"))
        assertTrue(quick.contains("editor.undo()"))
        assertTrue(quick.contains("onClick = expand"))
    }
}
