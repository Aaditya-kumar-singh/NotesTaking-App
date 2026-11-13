package com.notestaking.app.ui

import com.notestaking.app.core.tools.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Only tools and persistent toggles expose selected/not-selected to TalkBack. */
class ToolbarSelectionStateTest {
    @Test
    fun drawingToolsAnnounceBothSelectedAndUnselected() {
        assertEquals(true, toolbarSelectionState(true, Tool.PEN, reportSelection = false))
        assertEquals(false, toolbarSelectionState(false, Tool.ERASER, reportSelection = false))
    }

    @Test
    fun persistentTogglesAnnounceBothStatesWithoutAGlideKey() {
        assertEquals(true, toolbarSelectionState(true, null, reportSelection = true))
        assertEquals(false, toolbarSelectionState(false, null, reportSelection = true))
    }

    @Test
    fun momentaryActionsAndOpenMenusDoNotPretendToBeSelectedTools() {
        assertNull(toolbarSelectionState(false, null, reportSelection = false))
        assertNull(toolbarSelectionState(true, null, reportSelection = false))
    }
}
