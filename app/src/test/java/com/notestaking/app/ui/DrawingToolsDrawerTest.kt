package com.notestaking.app.ui

import com.notestaking.app.core.tools.Tool
import com.notestaking.app.settings.ToolbarSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B100: drawer state never changes the armed drawing tool or its saved configuration. */
class DrawingToolsDrawerTest {
    @Test
    fun toolChangeClosesDrawerButTappingCurrentToolKeepsSettingsAccessible() {
        assertTrue(drawingDrawerDismissOnSelection(Tool.PEN, Tool.ERASER))
        assertTrue(drawingDrawerDismissOnSelection(Tool.LASSO, Tool.TEXT_BOX))
        assertFalse(drawingDrawerDismissOnSelection(Tool.PEN, Tool.PEN))
        assertFalse(drawingDrawerDismissOnSelection(Tool.SELECT, Tool.SELECT))
    }

    @Test
    fun allToolbarSizesKeepTheChevronAtLeast48Dp() {
        for (size in ToolbarSize.entries) assertTrue(barMetrics(size).button >= 48.dp)
    }

    @Test
    fun eachSplitPaneWidthCanScrollTheExpandedDockedToolCollection() {
        for (size in ToolbarSize.entries) {
            for (width in listOf(180f, 240f, 320f)) {
                assertTrue(toolbarScrollsTrailing(false, width, barMetrics(size).button.value, pinnedButtons = 2))
            }
        }
    }
}
