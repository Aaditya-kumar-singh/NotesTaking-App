package com.notestaking.app.canvas

import android.view.MotionEvent

/**
 * When a pen touches while fingers are already down, route only that pen as a normal
 * single-pointer stream. Using public MotionEvent.obtain preserves tool type, pressure
 * and historical samples; the original mixed stream never reaches the ink controller.
 */
internal class PenContactRouter {
    private var pointerId = -1
    private var penDownTime = 0L

    fun route(event: MotionEvent, onTakeOver: () -> Unit): MotionEvent? {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            pointerId = -1
            return event
        }
        if (pointerId < 0) {
            if (event.actionMasked != MotionEvent.ACTION_POINTER_DOWN) return event
            val types = IntArray(event.pointerCount) { event.getToolType(it) }
            if (!PenContact.canStartWhileFingersDown(
                    types, event.actionIndex,
                    MotionEvent.TOOL_TYPE_FINGER,
                    MotionEvent.TOOL_TYPE_STYLUS,
                    MotionEvent.TOOL_TYPE_ERASER,
                )
            ) return event
            onTakeOver()
            pointerId = event.getPointerId(event.actionIndex)
            penDownTime = event.eventTime
            return copyOne(event, event.actionIndex, MotionEvent.ACTION_DOWN)
        }
        val index = event.findPointerIndex(pointerId)
        if (index < 0) {
            pointerId = -1
            return null
        }
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> MotionEvent.ACTION_MOVE
            MotionEvent.ACTION_POINTER_UP ->
                if (event.getPointerId(event.actionIndex) == pointerId) MotionEvent.ACTION_UP
                else MotionEvent.ACTION_MOVE
            MotionEvent.ACTION_UP -> MotionEvent.ACTION_UP
            MotionEvent.ACTION_CANCEL -> MotionEvent.ACTION_CANCEL
            else -> return null
        }
        val isolated = copyOne(event, index, action)
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pointerId = -1
        return isolated
    }

    private fun copyOne(event: MotionEvent, index: Int, action: Int): MotionEvent {
        val props = MotionEvent.PointerProperties()
        event.getPointerProperties(index, props)
        val current = MotionEvent.PointerCoords()
        event.getPointerCoords(index, current)
        val withHistory = action == MotionEvent.ACTION_MOVE && event.historySize > 0
        val initial = if (withHistory) MotionEvent.PointerCoords().also {
            event.getHistoricalPointerCoords(index, 0, it)
        } else current
        val startTime = if (withHistory) event.getHistoricalEventTime(0) else event.eventTime
        val copy = MotionEvent.obtain(
            penDownTime, startTime, action, 1,
            arrayOf(props), arrayOf(initial),
            event.metaState, event.buttonState,
            event.xPrecision, event.yPrecision,
            event.deviceId, event.edgeFlags, event.source, event.flags,
        )
        if (withHistory) {
            for (h in 1 until event.historySize) {
                val coords = MotionEvent.PointerCoords()
                event.getHistoricalPointerCoords(index, h, coords)
                copy.addBatch(event.getHistoricalEventTime(h), arrayOf(coords), event.metaState)
            }
            copy.addBatch(event.eventTime, arrayOf(current), event.metaState)
        }
        return copy
    }
}
