package com.notestaking.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** A floating text bar must remain visible inside its own editor pane. */
class TextStyleBarPlacementTest {

    @Test
    fun staysCenteredWhenThereIsSpace() {
        assertEquals(350f, textStyleBarLeft(500f, 300f, 1000f, 8f), 0.01f)
    }

    @Test
    fun clampsNearLeftEdge() {
        assertEquals(8f, textStyleBarLeft(10f, 264f, 320f, 8f), 0.01f)
    }

    @Test
    fun clampsNearRightEdge() {
        assertEquals(48f, textStyleBarLeft(310f, 264f, 320f, 8f), 0.01f)
    }

    @Test
    fun narrowSplitPaneStillKeepsTheBarVisible() {
        assertEquals(8f, textStyleBarLeft(200f, 192f, 208f, 8f), 0.01f)
    }
}
