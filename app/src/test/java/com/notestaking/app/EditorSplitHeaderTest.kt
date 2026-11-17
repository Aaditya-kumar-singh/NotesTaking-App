package com.notestaking.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A split picker must not replace the primary note with itself or an unsupported file. */
class EditorSplitHeaderTest {
    @Test fun acceptsDistinctNativeNoteAndCanvasFromSinglePrimaryPane() {
        assertTrue(canOpenBeside(true, 1, "draft.xnote", false))
        assertTrue(canOpenBeside(true, 1, "drawing.XCANVAS", false))
    }

    @Test fun rejectsSameDocumentAndInvalidOrStalePickerState() {
        assertFalse(canOpenBeside(true, 1, "draft.xnote", true))
        assertFalse(canOpenBeside(true, 1, "draft.pdf", false))
        assertFalse(canOpenBeside(true, 1, "", false))
        assertFalse(canOpenBeside(true, 1, null, false))
        assertFalse(canOpenBeside(true, 2, "draft.xnote", false))
        assertFalse(canOpenBeside(false, 1, "draft.xnote", false))
        assertFalse(canOpenBeside(true, 0, "draft.xnote", false))
    }
}
