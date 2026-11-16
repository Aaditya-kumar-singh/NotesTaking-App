package com.notestaking.app.core.stroke

import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/** NP-024: tilt is opt-in at the canvas and can only change a persisted per-stroke style. */
class StylusTiltTest {
    private val config = ToolDefaults.configFor(Tool.CALLIGRAPHY)

    @Test fun supportedTiltAndAzimuthChangeCalligraphyWidthWithoutChangingPressureOrInkStyle() {
        val upright = StylusTilt.snapshot(config, 0.0, 0.0)
        val tilted = StylusTilt.snapshot(config, PI / 3.0, 0.0)
        val sideways = StylusTilt.snapshot(config, PI / 3.0, PI / 2.0)

        assertSame(config, upright)
        assertTrue(tilted.baseWidth > config.baseWidth)
        assertTrue("pen bearing affects the stroke snapshot", sideways.baseWidth < tilted.baseWidth)
        assertEquals(config.pressureEnabled, tilted.pressureEnabled)
        assertEquals(config.pressureMinFactor, tilted.pressureMinFactor, 0.0)
        assertEquals(config.directionStrength, tilted.directionStrength, 0.0)
        assertEquals(config.rgba, tilted.rgba)
        assertEquals(config.baseWidth, upright.baseWidth, 0.0)
    }

    @Test fun unsupportedAndMalformedAxesKeepUnmodifiedBrushForFingerMouseAndOldDevices() {
        for (tilt in listOf<Double?>(null, 0.0, -1.0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertSame("missing or invalid tilt=$tilt", config,
                StylusTilt.snapshot(config, tilt, PI / 2.0))
        }
        val withUnknownBearing = StylusTilt.snapshot(config, PI / 3.0, null)
        val withInvalidBearing = StylusTilt.snapshot(config, PI / 3.0, Double.NaN)
        assertEquals(withUnknownBearing.baseWidth, withInvalidBearing.baseWidth, 1e-9)
        assertEquals(withUnknownBearing.baseWidth,
            StylusTilt.snapshot(config, PI / 3.0, 0.0).baseWidth, 1e-9)
    }

    @Test fun tiltAndBearingAreClampedAndAnglesNormalized() {
        val base = StylusTilt.snapshot(config, PI / 3.0, PI / 4.0)
        assertEquals(base.baseWidth,
            StylusTilt.snapshot(config, PI / 3.0, PI / 4.0 + 2.0 * PI).baseWidth, 1e-9)
        assertEquals(base.baseWidth,
            StylusTilt.snapshot(config, PI / 3.0, PI / 4.0 - 2.0 * PI).baseWidth, 1e-9)
        assertEquals(StylusTilt.snapshot(config, PI / 2.0, 0.0).baseWidth,
            StylusTilt.snapshot(config, PI * 10.0, 0.0).baseWidth, 1e-9)
        val badWidth = config.copy(baseWidth = Double.NaN)
        assertSame(badWidth, StylusTilt.snapshot(badWidth, PI / 3.0, 0.0))
        val zeroWidth = config.copy(baseWidth = 0.0)
        assertSame(zeroWidth, StylusTilt.snapshot(zeroWidth, PI / 3.0, 0.0))
    }

    @Test fun strokeGeometryAndClonesKeepTheBakedWidthWithoutNewSampleFields() {
        val adjusted = StylusTilt.snapshot(config, PI / 3.0, 0.0)
        val samples = listOf(
            Sample(40.0, 40.0, 0.5),
            Sample(80.0, 70.0, 0.5),
            Sample(120.0, 100.0, 0.5),
            Sample(160.0, 130.0, 0.5),
        )
        val original = Stroke(Tool.CALLIGRAPHY, config, samples)
        val tilted = Stroke(Tool.CALLIGRAPHY, adjusted, samples)
        assertTrue("larger recorded brush width changes visible ribbon",
            tilted.geometry().hw(2) > original.geometry().hw(2))
        val restored = Stroke(tilted)
        assertEquals(adjusted.baseWidth, restored.config.baseWidth, 0.0)
        assertEquals(tilted.geometry().hw(2), restored.geometry().hw(2), 0.0)
        assertEquals(samples, restored.samples)
    }
}
