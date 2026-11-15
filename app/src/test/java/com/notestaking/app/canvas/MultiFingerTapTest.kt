package com.notestaking.app.canvas

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Two/three-finger gestures are classified without Android view or MotionEvent construction. */
class MultiFingerTapTest {
    private data class P(
        val id: Int,
        val x: Float,
        val y: Float = 0f,
        val tool: Int = MotionEvent.TOOL_TYPE_FINGER,
    )

    private class Event(
        override val action: Int,
        override val timeMs: Long,
        private val fingers: List<P>,
        override val actionIndex: Int = 0,
        override val flags: Int = 0,
    ) : TapFrame {
        override val pointerCount: Int get() = fingers.size
        override fun id(index: Int) = fingers[index].id
        override fun tool(index: Int) = fingers[index].tool
        override fun x(index: Int) = fingers[index].x
        override fun y(index: Int) = fingers[index].y
    }

    private val tap = MultiFingerTap()
    private fun send(action: Int, ms: Long, vararg p: P, index: Int = 0, flags: Int = 0) =
        tap.tapEndedBy(Event(action, ms, p.toList(), index, flags))

    private val a = P(10, 0f)
    private val b = P(20, 200f)
    private val c = P(30, 400f)

    @Test fun cleanTwoFingerTapFiresExactlyOnce() {
        assertEquals(0, send(MotionEvent.ACTION_DOWN, 0, a))
        assertEquals(0, send(MotionEvent.ACTION_POINTER_DOWN, 20, a, b, index = 1))
        assertEquals(0, send(MotionEvent.ACTION_POINTER_UP, 90, a, b, index = 1))
        assertEquals(2, send(MotionEvent.ACTION_UP, 100, a))
        assertEquals(0, send(MotionEvent.ACTION_UP, 110, a)) // no new DOWN
    }

    @Test fun cleanThreeFingerTapFiresOnlyAfterLastFingerLifts() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 10, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_DOWN, 30, a, b, c, index = 2)
        assertEquals(0, send(MotionEvent.ACTION_POINTER_UP, 50, a, b, c, index = 2))
        assertEquals(0, send(MotionEvent.ACTION_POINTER_UP, 60, a, b, index = 1))
        assertEquals(3, send(MotionEvent.ACTION_UP, 80, a))
    }

    @Test fun movementReportedOnlyAtLiftIsNotMistakenForATap() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 20, a, b, index = 1)
        // Android need not send a MOVE between these two touch transitions.
        assertEquals(0, send(MotionEvent.ACTION_POINTER_UP, 60, a, b.copy(x = 260f), index = 1))
        assertEquals(0, send(MotionEvent.ACTION_UP, 70, a))

        send(MotionEvent.ACTION_DOWN, 100, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 120, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 150, a, b, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 170, a.copy(x = 45f)))
    }

    @Test fun movingTheFirstFingerAsTheSecondLandsCancelsTap() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 20, a.copy(x = 50f), b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 30, a.copy(x = 50f), b, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 40, a.copy(x = 50f)))
    }

    @Test fun shortButLargeScalePinchIsNotATwoFingerTap() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 20, a, b, index = 1)
        // Neither finger travels 40 px, but their separation grows 30%: this is a pinch.
        send(MotionEvent.ACTION_MOVE, 45, a.copy(x = -30f), b.copy(x = 230f))
        send(MotionEvent.ACTION_POINTER_UP, 65, a.copy(x = -30f), b.copy(x = 230f), index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 70, a.copy(x = -30f)))
    }

    @Test fun replacementFingerCannotCreatePhantomTwoFingerTap() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 20, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 40, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_DOWN, 60, a, c, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 80, a, c, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 90, a))
    }

    @Test fun mixedPenOrMouseAndCancelledGesturesNeverFire() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 20, a,
            b.copy(tool = MotionEvent.TOOL_TYPE_STYLUS), index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 40, a,
            b.copy(tool = MotionEvent.TOOL_TYPE_STYLUS), index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 50, a))

        send(MotionEvent.ACTION_DOWN, 100, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 120, a,
            b.copy(tool = MotionEvent.TOOL_TYPE_MOUSE), index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 140, a,
            b.copy(tool = MotionEvent.TOOL_TYPE_MOUSE), index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 160, a))

        send(MotionEvent.ACTION_DOWN, 200, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 220, a, b, index = 1)
        send(MotionEvent.ACTION_CANCEL, 230, a, b)
        assertEquals(0, send(MotionEvent.ACTION_UP, 245, a))
    }

    @Test fun cancelledPointerLiftNeverTriggersConfiguredGesture() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 10, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 30, a, b, index = 1, flags = MotionEvent.FLAG_CANCELED)
        assertEquals(0, send(MotionEvent.ACTION_UP, 40, a))

        send(MotionEvent.ACTION_DOWN, 100, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 120, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 140, a, b, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 150, a, flags = MotionEvent.FLAG_CANCELED))
    }

    @Test fun longPressAndFourFingerSequenceDoNotTriggerTapActions() {
        send(MotionEvent.ACTION_DOWN, 0, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 25, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_UP, 600, a, b, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 620, a))

        send(MotionEvent.ACTION_DOWN, 1000, a)
        send(MotionEvent.ACTION_POINTER_DOWN, 1010, a, b, index = 1)
        send(MotionEvent.ACTION_POINTER_DOWN, 1020, a, b, c, index = 2)
        send(MotionEvent.ACTION_POINTER_DOWN, 1030, a, b, c, P(40, 600f), index = 3)
        send(MotionEvent.ACTION_POINTER_UP, 1040, a, b, c, P(40, 600f), index = 3)
        send(MotionEvent.ACTION_POINTER_UP, 1050, a, b, c, index = 2)
        send(MotionEvent.ACTION_POINTER_UP, 1060, a, b, index = 1)
        assertEquals(0, send(MotionEvent.ACTION_UP, 1070, a))
    }
}
