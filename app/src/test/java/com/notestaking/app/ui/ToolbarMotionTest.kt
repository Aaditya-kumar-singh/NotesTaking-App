package com.notestaking.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A disabled system animator must leave the selected tool visible, never mid-hop. */
class ToolbarMotionTest {
    @Test
    fun settlingInterruptedGlideRemovesResidualStretchAndCentersOnLatestTool() {
        val trip = GlideTrip().apply {
            from = 12f
            to = 112f
            across = 20f
            stretch = 32f
            carry = 13f
        }
        assertTrue(trip.drawnOut(0.5f) > 0f)

        trip.settle(along = 68f, across = 28f)

        assertEquals(68f, trip.from, 0f)
        assertEquals(68f, trip.to, 0f)
        assertEquals(28f, trip.across, 0f)
        for (progress in listOf(0f, 0.25f, 0.5f, 1f)) {
            assertEquals(68f, trip.x(progress), 0.001f)
            assertEquals(0f, trip.drawnOut(progress), 0.001f)
        }
        assertEquals(0f, trip.carry, 0f)
        assertEquals(0f, trip.stretch, 0f)
    }

    @Test
    fun consecutiveDisabledMotionSelectionsAlwaysSettleAtNewestTarget() {
        val trip = GlideTrip()
        for ((along, across) in listOf(16f to 24f, 320f to 30f, 2f to 90f)) {
            trip.stretch = 40f
            trip.carry = 12f
            trip.settle(along, across)
            assertEquals(along, trip.x(0.5f), 0.001f)
            assertEquals(across, trip.across, 0.001f)
            assertEquals(0f, trip.drawnOut(0.5f), 0.001f)
        }
    }
}
