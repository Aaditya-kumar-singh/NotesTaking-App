package com.notestaking.app.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EraserSweepTest {
    @Test fun localPenRepairLeavesDistantInkOutsideItsDirtyRegion() {
        // A short erase along a page-wide line must not clear/redraw other lines.
        val dirty = EraserSweep.penRepairBounds(100.0, 120.0, 12.0, 3.0, 1.0)
        assertTrue(dirty.contains(Pt(112.0, 120.0)))
        assertTrue(dirty.w < 120.0)
        assertTrue(!dirty.intersects(Rect(350.0, 110.0, 50.0, 20.0)))
    }

    @Test fun repairPaddingKeepsItsScreenSizeWhenZoomed() {
        val normal = EraserSweep.penRepairBounds(100.0, 120.0, 12.0, 3.0, 1.0)
        val zoomed = EraserSweep.penRepairBounds(100.0, 120.0, 6.0, 3.0, 2.0)
        assertTrue(zoomed.w < normal.w)
        assertTrue(zoomed.contains(Pt(106.0, 120.0)))
    }

    @Test fun firstPointIsOneDiscreteEraserTap() {
        val visited = mutableListOf<Pt>()
        EraserSweep.visit(null, Pt(10.0, 20.0), 5.0, visited::add)
        assertEquals(listOf(Pt(10.0, 20.0)), visited)
    }

    @Test fun fastHorizontalDragHasNoGapsBetweenEraserDiscs() {
        val positions = mutableListOf<Pt>()
        EraserSweep.visit(Pt(0.0, 0.0), Pt(100.0, 0.0), 5.0, positions::add)
        assertTrue(positions.size > 20)
        assertEquals(Pt(100.0, 0.0), positions.last())
        val all = listOf(Pt(0.0, 0.0)) + positions
        assertTrue(all.zipWithNext().all { (a, b) -> b.x - a.x <= 2.5 + 1e-9 })
    }

    @Test fun fastDiagonalDragInterpolatesBothCoordinates() {
        val positions = mutableListOf<Pt>()
        EraserSweep.visit(Pt(2.0, 3.0), Pt(42.0, 43.0), 10.0, positions::add)
        assertEquals(Pt(42.0, 43.0), positions.last())
        assertTrue(positions.all { kotlin.math.abs((it.x - 2.0) - (it.y - 3.0)) <= 1e-9 })
    }

    @Test fun extremelyLongOrInvalidMovementIsBounded() {
        val positions = mutableListOf<Pt>()
        EraserSweep.visit(Pt(0.0, 0.0), Pt(1_000_000.0, 0.0), 1.0, positions::add)
        assertEquals(EraserSweep.MAX_STEPS, positions.size)
    }

    @Test fun stationaryPointerDoesNotCutTwice() {
        val positions = mutableListOf<Pt>()
        EraserSweep.visit(Pt(5.0, 5.0), Pt(5.0, 5.0), 8.0, positions::add)
        assertTrue(positions.isEmpty())
    }

    @Test fun hoverAndTouchSequencesCanResetBetweenGestures() {
        val positions = mutableListOf<Pt>()
        EraserSweep.visit(Pt(0.0, 0.0), Pt(20.0, 0.0), 4.0, positions::add)
        val before = positions.size
        EraserSweep.visit(null, Pt(1000.0, 1000.0), 4.0, positions::add)
        assertEquals(before + 1, positions.size)
        assertEquals(Pt(1000.0, 1000.0), positions.last())
    }
}
