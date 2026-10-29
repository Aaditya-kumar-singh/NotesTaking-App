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

    fun canStartWhileFingersDown(toolTypes: IntArray, downIndex: Int, fingerType: Int, stylusType: Int, eraserType: Int): Boolean =
        toolTypes.size > 1 && downIndex in toolTypes.indices &&
            isPen(toolTypes[downIndex], stylusType, eraserType) &&
            toolTypes.indices.all { it == downIndex || toolTypes[it] == fingerType }

    fun canToggleDebug(toolTypes: IntArray, fingerType: Int): Boolean =
        toolTypes.size == 4 && toolTypes.all { it == fingerType }
}
