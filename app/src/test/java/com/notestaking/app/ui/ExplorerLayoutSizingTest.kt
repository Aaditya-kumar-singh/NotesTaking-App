package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import com.notestaking.app.settings.TileSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** The actual Explorer pane (not the whole screen) determines the number of readable cards. */
class ExplorerLayoutSizingTest {

    @Test
    fun gridAdaptsToPhoneAndSplitScreenWidths() {
        assertEquals(1, gridColumns(280.dp, TileSize.M))
        assertEquals(2, gridColumns(520.dp, TileSize.M))
        assertEquals(3, gridColumns(780.dp, TileSize.M))
    }

    @Test
    fun gridKeepsLargeTilesReadableAndClampsEdgeCases() {
        assertEquals(1, gridColumns(320.dp, TileSize.L))
        assertEquals(2, gridColumns(652.dp, TileSize.L))
        assertEquals(1, gridColumns(0.dp, TileSize.S))
        assertEquals(10, gridColumns(6000.dp, TileSize.S))
    }

    @Test
    fun galleryUsesTheSameMeasuredPaneConcept() {
        assertEquals(1, galleryColumns(280.dp, TileSize.M))
        assertEquals(4, galleryColumns(840.dp, TileSize.M))
    }
}
