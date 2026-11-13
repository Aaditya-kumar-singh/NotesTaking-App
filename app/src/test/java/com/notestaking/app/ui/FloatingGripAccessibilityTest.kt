package com.notestaking.app.ui

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
    fun interiorAllowsBothDirectionsButZeroStepsAreNotActions() {
        assertTrue(floatingGripCanNudge(100f, 0.5f, -0.1f))
        assertTrue(floatingGripCanNudge(100f, 0.5f, 0.1f))
        assertFalse(floatingGripCanNudge(100f, 0.5f, 0f))
    }
}
