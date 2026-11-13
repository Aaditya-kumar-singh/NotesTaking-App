package com.notestaking.app.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hardware-keyboard activation must not interfere with chip drag or keyboard navigation. */
class ToolbarCustomizerKeyboardTest {
    @Test
    fun enterNumpadEnterAndSpaceActivateOnceOnKeyRelease() {
        for (key in listOf(Key.Enter, Key.NumPadEnter, Key.Spacebar)) {
            assertFalse(toolbarChipActivationKey(key, KeyEventType.KeyDown))
            assertTrue(toolbarChipActivationKey(key, KeyEventType.KeyUp))
        }
    }

    @Test
    fun tabArrowsAndEscapeRemainAvailableForNavigation() {
        for (key in listOf(Key.Tab, Key.DirectionUp, Key.DirectionDown, Key.Escape)) {
            assertFalse(toolbarChipActivationKey(key, KeyEventType.KeyDown))
            assertFalse(toolbarChipActivationKey(key, KeyEventType.KeyUp))
        }
    }
}
