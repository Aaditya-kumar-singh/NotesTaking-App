package com.notestaking.app.canvas

import android.view.MotionEvent

/**
 * Keep the active pen as a single-pointer stream whether it touches before or after
 * fingers. Public MotionEvent.obtain preserves tool type, pressure and historical samples;
 * the original mixed stream never reaches the ink controller.
 */
internal class PenContactRouter {
    private var pointerId = -1
    private var penDownTime = 0L

    fun route(event: MotionEvent, onTakeOver: () -> Unit): MotionEvent? {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // A pen-first gesture must stay single-pointer when a palm lands later.
            pointerId = if (PenContact.isPen(event.getToolType(0), MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER))
                event.getPointerId(0) else -1
            penDownTime = event.downTime
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
            // Android dropped the pen ID: cancel the live ink instead of leaving it stuck.
            return MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        }
        val actionPointerId = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP)
            event.getPointerId(event.actionIndex) else -1
        val action = PenContact.routedPenAction(event.actionMasked, pointerId, actionPointerId) ?: return null
        val isolated = copyOne(event, index, action)
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pointerId = -1
        return isolated
    }

    private fun copyOne(event: MotionEvent, index: Int, action: Int): MotionEvent {
        val props = MotionEvent.PointerProperties()
        event.getPointerProperties(index, props)
        val current = MotionEvent.PointerCoords()
        event.getPointerCoords(index, current)
        // A pen joining an older finger gesture must not inherit pre-pen history.
        val firstHistory = if (action == MotionEvent.ACTION_MOVE)
            (0 until event.historySize).firstOrNull { event.getHistoricalEventTime(it) >= penDownTime }
        else null
        val initial = if (firstHistory != null) MotionEvent.PointerCoords().also {
            event.getHistoricalPointerCoords(index, firstHistory, it)
        } else current
        val startTime = if (firstHistory != null) event.getHistoricalEventTime(firstHistory) else event.eventTime
        val copy = MotionEvent.obtain(
            penDownTime, startTime, action, 1,
            arrayOf(props), arrayOf(initial),
            event.metaState, event.buttonState,
            event.xPrecision, event.yPrecision,
            event.deviceId, event.edgeFlags, event.source, event.flags,
        )
        if (firstHistory != null) {
            for (h in firstHistory + 1 until event.historySize) {
                val coords = MotionEvent.PointerCoords()
                event.getHistoricalPointerCoords(index, h, coords)
                copy.addBatch(event.getHistoricalEventTime(h), arrayOf(coords), event.metaState)
            }
            copy.addBatch(event.eventTime, arrayOf(current), event.metaState)
        }
        return copy
    }
}
