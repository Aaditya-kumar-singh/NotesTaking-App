package com.notestaking.app.canvas

/**
 * The pen owns its ink/eraser/tape gesture until its own pointer lifts, even if
 * Android still reports an incidental finger resting on the display.
 */
internal object PenContact {
    fun isPen(toolType: Int, stylusType: Int, eraserType: Int): Boolean =
        toolType == stylusType || toolType == eraserType

    fun finishesGesture(penActive: Boolean, ownsGesture: Boolean, activeId: Int, liftedId: Int): Boolean =
        penActive && ownsGesture && activeId == liftedId

    fun canToggleDebug(toolTypes: IntArray, fingerType: Int): Boolean =
        toolTypes.size == 4 && toolTypes.all { it == fingerType }
}
