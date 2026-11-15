package com.notestaking.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Test

/** NP-043: size never grows beyond the configured radius or collapses on invalid pressure. */
class EraserPressureTest {
    @Test fun validStylusPressureScalesBetweenClampedFloorAndConfiguredRadius() {
        assertEquals(6.4, EraserPressure.radius(16.0, true, 0.25, 0.2), 1e-9)
        assertEquals(10.0, EraserPressure.radius(16.0, true, 0.25, 0.5), 1e-9)
        assertEquals(16.0, EraserPressure.radius(16.0, true, 0.25, 1.0), 1e-9)
        assertEquals(16.0, EraserPressure.radius(16.0, true, 0.25, 3.0), 1e-9)
        assertEquals(1.6, EraserPressure.radius(16.0, true, -9.0, 0.001), 0.1)
        assertEquals(16.0, EraserPressure.radius(16.0, true, 8.0, 0.2), 1e-9)
        assertEquals(16.0, EraserPressure.radius(16.0, true, Double.NaN, 0.2), 1e-9)
    }

    @Test fun disabledMouseFingerAndUnreportedPenPressureUseUnmodifiedSize() {
        for (reported in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY)) {
            assertEquals("pressure=$reported", 16.0,
                EraserPressure.radius(16.0, true, 0.25, reported), 1e-9)
        }
        for (reported in listOf(null, 0.1, 0.8, 1.0)) {
            assertEquals("pressure disabled: $reported", 16.0,
                EraserPressure.radius(16.0, false, 0.25, reported), 1e-9)
        }
        assertEquals("legacy 1.0 floor also stays constant", 16.0,
            EraserPressure.radius(16.0, true, 1.0, 0.3), 1e-9)
    }

    @Test fun zoomIsHandledBeforePressureSoOnScreenSizeIsConsistent() {
        for (zoom in listOf(0.5, 1.0, 2.0, 4.0)) {
            val contentRadius = EraserPressure.radius(16.0 / zoom, true, 0.25, 0.5)
            assertEquals(10.0, contentRadius * zoom, 1e-9)
        }
    }
}
