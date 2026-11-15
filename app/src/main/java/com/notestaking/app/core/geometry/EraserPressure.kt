package com.notestaking.app.core.geometry

/** Pressure-sensitive eraser radius in content space; invalid or missing pen pressure keeps the set size. */
object EraserPressure {
    fun radius(base: Double, enabled: Boolean, minFactor: Double, stylusPressure: Double?): Double {
        val p = stylusPressure
        if (!enabled || p == null || !p.isFinite() || p <= 0.0) return base
        val floor = if (minFactor.isFinite()) minFactor.coerceIn(0.1, 1.0) else 1.0
        return base * (floor + (1.0 - floor) * p.coerceIn(0.0, 1.0))
    }
}
