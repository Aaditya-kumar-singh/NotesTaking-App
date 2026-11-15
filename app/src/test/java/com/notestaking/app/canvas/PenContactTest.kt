package com.notestaking.app.canvas

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test fun scrollbarNeverTakesStylusOrEraserTipFromDrawing() {
        assertTrue(PenContact.canStartScrollbar(finger, stylus, eraser))
        assertTrue(PenContact.canStartScrollbar(mouse, stylus, eraser))
        assertFalse(PenContact.canStartScrollbar(stylus, stylus, eraser))
        assertFalse(PenContact.canStartScrollbar(eraser, stylus, eraser))
        // An existing finger drag may yield when the next pointer is a pen.
        assertTrue(PenContact.canStartWhileFingersDown(intArrayOf(finger, stylus), 1, finger, stylus, eraser))
        assertTrue(PenContact.canStartWhileFingersDown(intArrayOf(finger, eraser), 1, finger, stylus, eraser))
    }

    @Test fun scrollbarDragStaysWithTheFingerThatStartedIt() {
        assertFalse(PenContact.scrollbarOwnerLifted(ownerId = 7, liftedId = 9))
        assertTrue(PenContact.scrollbarOwnerLifted(ownerId = 7, liftedId = 7))
        assertFalse(PenContact.scrollbarOwnerLifted(ownerId = -1, liftedId = 9))
        assertFalse(PenContact.scrollbarOwnerLifted(ownerId = -1, liftedId = -1))
        assertTrue(PenContact.scrollbarOwnerLifted(ownerId = 0, liftedId = 0))
    }

    @Test fun onlyActivePenLiftCommitsItsGesture() {
        assertTrue(PenContact.finishesGesture(true, true, activeId = 7, liftedId = 7))
        assertFalse(PenContact.finishesGesture(true, true, activeId = 7, liftedId = 4))
        assertFalse(PenContact.finishesGesture(false, true, activeId = 7, liftedId = 7))
        assertFalse(PenContact.finishesGesture(true, false, activeId = 7, liftedId = 7))
    }

    @Test fun mixedPenAndFingerEventsKeepThePenUntilItsOwnLift() {
        val penId = 7
        val palmId = 4
        assertEquals(MotionEvent.ACTION_MOVE, PenContact.routedPenAction(MotionEvent.ACTION_POINTER_DOWN, penId, palmId))
        assertEquals(MotionEvent.ACTION_MOVE, PenContact.routedPenAction(MotionEvent.ACTION_MOVE, penId, palmId))
        assertEquals(MotionEvent.ACTION_MOVE, PenContact.routedPenAction(MotionEvent.ACTION_POINTER_UP, penId, palmId))
        assertEquals(MotionEvent.ACTION_UP, PenContact.routedPenAction(MotionEvent.ACTION_POINTER_UP, penId, penId))
        assertEquals(MotionEvent.ACTION_UP, PenContact.routedPenAction(MotionEvent.ACTION_UP, penId, penId))
        assertEquals(MotionEvent.ACTION_CANCEL, PenContact.routedPenAction(MotionEvent.ACTION_CANCEL, penId, palmId))
        assertNull(PenContact.routedPenAction(MotionEvent.ACTION_HOVER_MOVE, penId, palmId))
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
