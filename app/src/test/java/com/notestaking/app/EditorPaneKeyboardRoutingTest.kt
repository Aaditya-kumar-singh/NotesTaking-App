package com.notestaking.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure keyboard-routing guard; Compose focus traversal still needs device QA. */
class EditorPaneKeyboardRoutingTest {
    @Test fun directPaneFocusRetainsUnmodifiedDocumentShortcuts() {
        assertTrue(shouldDispatchPaneShortcut(paneFocused = true, ctrlPressed = false, gripOwnsArrow = false))
    }

    @Test fun focusedToolbarAndTextControlsReceiveTheirOwnPlainKeys() {
        assertFalse(shouldDispatchPaneShortcut(paneFocused = false, ctrlPressed = false, gripOwnsArrow = false))
    }

    @Test fun focusedToolbarStillAllowsControlDocumentShortcuts() {
        assertTrue(shouldDispatchPaneShortcut(paneFocused = false, ctrlPressed = true, gripOwnsArrow = false))
    }

    @Test fun floatingGripKeepsArrowKeysEvenWhenPaneOwnsFocus() {
        assertFalse(shouldDispatchPaneShortcut(paneFocused = true, ctrlPressed = false, gripOwnsArrow = true))
        assertFalse(shouldDispatchPaneShortcut(paneFocused = false, ctrlPressed = true, gripOwnsArrow = true))
    }
}
