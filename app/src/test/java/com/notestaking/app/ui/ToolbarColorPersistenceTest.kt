package com.notestaking.app.ui

import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.settings.Settings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tool overrides and recent swatches must not overwrite each other on app restart. */
class ToolbarColorPersistenceTest {
    @Test
    fun fixedPenOverrideAndRecentToolbarColorsSurviveRecolorAndReload() {
        val fixedInk = Rgba(22, 95, 211)
        val firstSwatch = Rgba(220, 45, 33)
        val secondSwatch = Rgba(31, 170, 89)
        val pen = ToolDefaults.configFor(Tool.PEN).copy(
            baseWidth = 5.5,
            colorOverride = fixedInk,
        )
        val initial = Settings(tools = mapOf(Tool.PEN to pen))
            .withAddedPenPreset(Tool.PEN, pen)
        val recolored = initial.copy(
            toolbarColors = initial.toolbarColors.toMutableList().also {
                it[0] = firstSwatch
                it[1] = secondSwatch
            },
            activeColor = 1,
        ).rememberColor(firstSwatch).rememberColor(secondSwatch).rememberColor(firstSwatch)

        val reopened = Settings.fromJson(JSONObject(recolored.toJson().toString()))

        // Changing the globally active swatch does not change a pen with fixed ink.
        assertEquals(fixedInk, reopened.configFor(Tool.PEN).colorOverride)
        assertEquals(pen, reopened.savedPenPresets(Tool.PEN).single())
        assertNull(reopened.configFor(Tool.HIGHLIGHTER).colorOverride)

        // Both custom toolbar colors and the most-recent-first history remain intact.
        assertEquals(1, reopened.activeColor)
        assertEquals(firstSwatch, reopened.toolbarColors[0])
        assertEquals(secondSwatch, reopened.toolbarColors[1])
        assertEquals(listOf(firstSwatch, secondSwatch), reopened.recentColors)
    }
}
