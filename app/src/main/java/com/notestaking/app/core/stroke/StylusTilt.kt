package com.notestaking.app.core.stroke

import com.notestaking.app.core.tools.ToolConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Per-stroke calligraphy tilt response. Width is baked into the existing persisted brush
 * snapshot, so reopening a document does not depend on the original digitizer axes.
 */
object StylusTilt {
    fun snapshot(config: ToolConfig, tiltRadians: Double?, orientationRadians: Double?): ToolConfig {
        if (tiltRadians == null || !tiltRadians.isFinite() || tiltRadians <= 0.0 ||
            !config.baseWidth.isFinite() || config.baseWidth <= 0.0) return config

        val tilt = tiltRadians.coerceIn(0.0, PI / 2.0)
        val directional = if (orientationRadians != null && orientationRadians.isFinite()) {
            0.75 + 0.25 * abs(cos(orientationRadians % (2.0 * PI)))
        } else 1.0
        val width = config.baseWidth * (1.0 + 0.4 * sin(tilt) * directional)
        return config.copy(baseWidth = width)
    }
}
