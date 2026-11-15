package com.notestaking.app.canvas

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** The small touch-data surface used by the detector, so pointer transitions can be unit-tested. */
internal interface TapFrame {
    val action: Int
    val actionIndex: Int
    val pointerCount: Int
    val timeMs: Long
    val flags: Int
    fun id(index: Int): Int
    fun tool(index: Int): Int
    fun x(index: Int): Float
    fun y(index: Int): Float
}

/**
 * Recognizes a clean two- or three-finger tap (finger-only, brief, near-stationary) for the
 * configurable gesture actions. Both canvases' views feed it, so the gesture cannot drift apart.
 *
 * It never swallows a gesture mid-flight, so pinch-zoom keeps working: it only names the terminal
 * UP of a recognized tap, and the view then cancels the (already-ended) pinch and consumes that UP.
 */
class MultiFingerTap {
    private var downMs = 0L
    private var maxPointers = 0
    private val downX = HashMap<Int, Float>()
    private val downY = HashMap<Int, Float>()
    private var moved = false
    private var allFingers = true
    private var firstId = -1
    private var secondId = -1
    private var initialSpan = 0.0

    // Reuse the adapter on high-frequency MOVE events instead of allocating an event wrapper.
    private val androidFrame = object : TapFrame {
        lateinit var event: MotionEvent
        override val action get() = event.actionMasked
        override val actionIndex get() = event.actionIndex
        override val pointerCount get() = event.pointerCount
        override val timeMs get() = event.eventTime
        override val flags get() = event.flags
        override fun id(index: Int) = event.getPointerId(index)
        override fun tool(index: Int) = event.getToolType(index)
        override fun x(index: Int) = event.getX(index)
        override fun y(index: Int) = event.getY(index)
    }

    /** Feed every touch event; returns 2 or 3 only on the terminal UP of a clean tap. */
    fun tapEndedBy(e: MotionEvent): Int {
        androidFrame.event = e
        return tapEndedBy(androidFrame)
    }

    internal fun tapEndedBy(e: TapFrame): Int {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                downMs = e.timeMs
                maxPointers = 1
                allFingers = e.tool(0) == MotionEvent.TOOL_TYPE_FINGER
                recordDown(e, 0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (maxPointers == 0) return 0
                // The first finger may move in the same event that introduces the second.
                checkMovement(e, skipIndex = e.actionIndex)
                maxPointers = max(maxPointers, e.pointerCount)
                if (e.tool(e.actionIndex) != MotionEvent.TOOL_TYPE_FINGER) allFingers = false
                if (downX.containsKey(e.id(e.actionIndex))) moved = true
                else recordDown(e, e.actionIndex)
                if (e.pointerCount == 2 && !moved) {
                    firstId = e.id(0)
                    secondId = e.id(1)
                    initialSpan = hypot(
                        (e.x(0) - e.x(1)).toDouble(), (e.y(0) - e.y(1)).toDouble(),
                    )
                }
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_UP -> {
                if (e.action == MotionEvent.ACTION_POINTER_UP && (e.flags and MotionEvent.FLAG_CANCELED) != 0) moved = true
                checkMovement(e)
            }
            MotionEvent.ACTION_UP -> {
                if ((e.flags and MotionEvent.FLAG_CANCELED) != 0) moved = true
                checkMovement(e)
                val quick = e.timeMs - downMs in 0L..TAP_TIMEOUT_MS
                val result = if (maxPointers in 2..3 && downX.size == maxPointers &&
                    quick && !moved && allFingers
                ) maxPointers else 0
                reset() // a duplicate/later UP cannot fire the same gesture again
                return result
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
        return 0
    }

    private fun checkMovement(e: TapFrame, skipIndex: Int = -1) {
        if (moved) return
        var a = -1
        var b = -1
        for (i in 0 until e.pointerCount) {
            if (i == skipIndex) continue
            val id = e.id(i)
            val x = downX[id]
            val y = downY[id]
            if (x == null || y == null || hypot((e.x(i) - x).toDouble(), (e.y(i) - y).toDouble()) > TAP_SLOP) {
                moved = true
                return
            }
            if (id == firstId) a = i
            if (id == secondId) b = i
        }
        // A short pinch can move each finger less than the tap slop yet zoom the page substantially.
        if (a >= 0 && b >= 0 && initialSpan > 0.0) {
            val span = hypot((e.x(a) - e.x(b)).toDouble(), (e.y(a) - e.y(b)).toDouble())
            if (abs(span - initialSpan) > max(MIN_SPAN_CHANGE, initialSpan * MAX_SPAN_CHANGE_FRACTION)) {
                moved = true
            }
        }
    }

    private fun recordDown(e: TapFrame, index: Int) {
        downX[e.id(index)] = e.x(index)
        downY[e.id(index)] = e.y(index)
    }

    private fun reset() {
        downMs = 0L
        maxPointers = 0
        downX.clear()
        downY.clear()
        moved = false
        allFingers = true
        firstId = -1
        secondId = -1
        initialSpan = 0.0
    }

    private companion object {
        const val TAP_TIMEOUT_MS = 500L
        const val TAP_SLOP = 40.0
        const val MIN_SPAN_CHANGE = 8.0
        const val MAX_SPAN_CHANGE_FRACTION = 0.12
    }
}
