package com.notestaking.app.ui

import com.notestaking.app.core.infinite.CanvasViewport
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zoom arrows should not be offered when the editor's next step would be clamped away. */
class ToolbarZoomAccessibilityTest {
    @Test
    fun interiorZoomOffersBothDirections() {
        assertTrue(toolbarZoomStepEnabled(1.0, 0.25, 4.0, locked = false, zoomIn = false))
        assertTrue(toolbarZoomStepEnabled(1.0, 0.25, 4.0, locked = false, zoomIn = true))
    }

    @Test
    fun eachBoundaryDisablesOnlyTheOutwardDirection() {
        assertFalse(toolbarZoomStepEnabled(0.25, 0.25, 4.0, locked = false, zoomIn = false))
        assertTrue(toolbarZoomStepEnabled(0.25, 0.25, 4.0, locked = false, zoomIn = true))
        assertTrue(toolbarZoomStepEnabled(4.0, 0.25, 4.0, locked = false, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(4.0, 0.25, 4.0, locked = false, zoomIn = true))
    }

    @Test
    fun lockedEqualLimitsAndClampedToleranceOfferNoStep() {
        assertFalse(toolbarZoomStepEnabled(1.0, 0.25, 4.0, locked = true, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(1.0, 0.25, 4.0, locked = true, zoomIn = true))
        assertFalse(toolbarZoomStepEnabled(1.0, 1.0, 1.0, locked = false, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(1.0, 1.0, 1.0, locked = false, zoomIn = true))
        assertFalse(toolbarZoomStepEnabled(0.25 + 5e-10, 0.25, 4.0, locked = false, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(4.0 - 5e-10, 0.25, 4.0, locked = false, zoomIn = true))
    }

    @Test
    fun infiniteCanvasDisablesZoomOnlyAtTheActualViewportBounds() {
        val viewport = CanvasViewport()
        viewport.zoom = viewport.minZoom
        assertFalse(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = false, zoomIn = false))
        assertTrue(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = false, zoomIn = true))

        viewport.zoom = viewport.maxZoom
        assertTrue(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = false, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = false, zoomIn = true))

        viewport.zoom = 1.0
        assertFalse(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = true, zoomIn = false))
        assertFalse(toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, locked = true, zoomIn = true))
    }
}
