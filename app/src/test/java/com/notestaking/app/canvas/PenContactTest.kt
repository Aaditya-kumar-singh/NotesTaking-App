package com.notestaking.app.canvas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenContactTest {
    private val finger = 1
    private val stylus = 2
    private val mouse = 3
    private val eraser = 4

    @Test fun stylusAndEraserNibOwnInkButFingerAndMouseDoNot() {
        assertTrue(PenContact.isPen(stylus, stylus, eraser))
        assertTrue(PenContact.isPen(eraser, stylus, eraser))
        assertFalse(PenContact.isPen(finger, stylus, eraser))
        assertFalse(PenContact.isPen(mouse, stylus, eraser))
    }

    @Test fun onlyActivePenLiftCommitsItsGesture() {
        assertTrue(PenContact.finishesGesture(true, true, activeId = 7, liftedId = 7))
        assertFalse(PenContact.finishesGesture(true, true, activeId = 7, liftedId = 4))
        assertFalse(PenContact.finishesGesture(false, true, activeId = 7, liftedId = 7))
        assertFalse(PenContact.finishesGesture(true, false, activeId = 7, liftedId = 7))
    }

    @Test fun penCanBeginWhilePalmRemainsButNotFromAnotherPen() {
        assertTrue(PenContact.canStartWhileFingersDown(
            intArrayOf(finger, stylus), 1, finger, stylus, eraser,
        ))
        assertTrue(PenContact.canStartWhileFingersDown(
            intArrayOf(finger, finger, eraser), 2, finger, stylus, eraser,
        ))
        assertFalse(PenContact.canStartWhileFingersDown(
            intArrayOf(stylus, finger), 1, finger, stylus, eraser,
        ))
        assertFalse(PenContact.canStartWhileFingersDown(
            intArrayOf(finger, finger), 1, finger, stylus, eraser,
        ))
        assertFalse(PenContact.canStartWhileFingersDown(
            intArrayOf(finger, stylus, eraser), 2, finger, stylus, eraser,
        ))
        assertFalse(PenContact.canStartWhileFingersDown(
            intArrayOf(finger, stylus), 7, finger, stylus, eraser,
        ))
    }

    @Test fun debugGestureRejectsMixedPalmPenAndExtraPointers() {
        assertTrue(PenContact.canToggleDebug(intArrayOf(finger, finger, finger, finger), finger))
        assertFalse(PenContact.canToggleDebug(intArrayOf(stylus, finger, finger, finger), finger))
        assertFalse(PenContact.canToggleDebug(intArrayOf(eraser, finger, finger, finger), finger))
        assertFalse(PenContact.canToggleDebug(intArrayOf(finger, finger, finger), finger))
        assertFalse(PenContact.canToggleDebug(intArrayOf(finger, finger, finger, finger, finger), finger))
    }
}
