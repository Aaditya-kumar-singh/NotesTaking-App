package com.notestaking.app.ui

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A movement is exposed to TalkBack only when it can change the floating grip position. */
class FloatingGripAccessibilityTest {
    @Test
    fun edgesExposeOnlyMovementsBackIntoThePane() {
        assertFalse(floatingGripCanNudge(100f, 0f, -0.1f))
        assertTrue(floatingGripCanNudge(100f, 0f, 0.1f))
        assertTrue(floatingGripCanNudge(100f, 1f, -0.1f))
        assertFalse(floatingGripCanNudge(100f, 1f, 0.1f))
    }

    @Test
    fun noFreeSpaceExposesNoMovementOnThatAxis() {
        assertFalse(floatingGripCanNudge(0f, 0.5f, -0.1f))
        assertFalse(floatingGripCanNudge(0f, 0.5f, 0.1f))
    }

    @Test
    fun keyboardArrowsUseExistingBoundedTenPercentNudges() {
        assertEquals(-0.1f to 0f, floatingGripKeyStep(Key.DirectionLeft))
        assertEquals(0.1f to 0f, floatingGripKeyStep(Key.DirectionRight))
        assertEquals(0f to -0.1f, floatingGripKeyStep(Key.DirectionUp))
        assertEquals(0f to 0.1f, floatingGripKeyStep(Key.DirectionDown))
        assertEquals(null, floatingGripKeyStep(Key.Enter))
    }

    @Test
    fun parentPreviewYieldsOnlyPlainArrowsToFocusedGrip() {
        assertTrue(floatingGripOwnsArrow(true, Key.DirectionLeft, false, false, false))
        assertTrue(floatingGripOwnsArrow(true, Key.DirectionDown, false, false, false))
        assertFalse(floatingGripOwnsArrow(false, Key.DirectionLeft, false, false, false))
        assertFalse(floatingGripOwnsArrow(true, Key.Enter, false, false, false))
        assertFalse(floatingGripOwnsArrow(true, Key.DirectionLeft, true, false, false))
        assertFalse(floatingGripOwnsArrow(true, Key.DirectionLeft, false, true, false))
        assertFalse(floatingGripOwnsArrow(true, Key.DirectionLeft, false, false, true))
    }

    @Test
    fun resetIsOfferedOnlyWhenThePositionDiffersFromTheDefault() {
        assertFalse(floatingGripCanReset(0.5f, 0f, 0.5f, 0f))
        assertFalse(floatingGripCanReset(1f, 0.5f, 1f, 0.5f))
        assertTrue(floatingGripCanReset(0.25f, 0f, 0.5f, 0f))
        assertTrue(floatingGripCanReset(0.5f, 0.25f, 0.5f, 0f))
    }

    @Test
    fun interiorAllowsBothDirectionsButZeroStepsAreNotActions() {
        assertTrue(floatingGripCanNudge(100f, 0.5f, -0.1f))
        assertTrue(floatingGripCanNudge(100f, 0.5f, 0.1f))
        assertFalse(floatingGripCanNudge(100f, 0.5f, 0f))
    }
}
