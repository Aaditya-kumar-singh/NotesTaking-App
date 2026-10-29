package com.notestaking.app.core.geometry

import kotlin.math.ceil
import kotlin.math.hypot

/** Fill the gaps between sampled eraser positions, in viewport coordinates (not zoomed ink).
 * Spacing <= half of the eraser radius makes consecutive discs overlap substantially.
 * Capped to avoid an unbounded event loop from malformed pointer coordinates.
 */
object EraserSweep {
    const val MAX_STEPS = 512

    fun visit(previous: Pt?, next: Pt, radiusPx: Double, erase: (Pt) -> Unit) {
        if (previous == null || !radiusPx.isFinite() || radiusPx <= 0.0) {
            erase(next)
            return
        }
        val dx = next.x - previous.x
        val dy = next.y - previous.y
        val distance = hypot(dx, dy)
        if (!distance.isFinite() || distance < 1e-7) return
        val step = (radiusPx * 0.5).coerceAtLeast(1.0)
        val count = ceil(distance / step).coerceIn(1.0, MAX_STEPS.toDouble()).toInt()
        for (i in 1..count) {
            val t = i.toDouble() / count
            erase(Pt(previous.x + dx * t, previous.y + dy * t))
        }
    }
}
