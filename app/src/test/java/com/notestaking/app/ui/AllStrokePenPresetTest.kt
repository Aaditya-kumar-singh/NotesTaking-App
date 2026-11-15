package com.notestaking.app.ui

import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.settings.Settings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercise every stroke tool through the same saved-style settings used by both editors. */
class AllStrokePenPresetTest {
    @Test
    fun threeStylesPerStrokeToolSurviveRestartWithoutCrossToolChanges() {
        val strokeTools = Tool.entries.filter { it.isStroke }
        assertEquals(6, strokeTools.size)
        var settings = Settings()
        val expected = mutableMapOf<Tool, List<ToolConfig>>()

        for (tool in strokeTools) {
            val base = ToolDefaults.configFor(tool)
            val styles = (0 until Settings.MAX_PEN_PRESETS).map { slot ->
                base.copy(
                    baseWidth = base.baseWidth + slot + 0.25,
                    colorOverride = Rgba(25 + tool.ordinal, 70 + slot, 130 + slot),
                    pressureEnabled = slot != 1,
                    dashLength = if (tool == Tool.DASHED) 5.0 + slot else base.dashLength,
                    dashGap = if (tool == Tool.DASHED) 3.0 + slot else base.dashGap,
                    directionStrength = if (tool == Tool.CALLIGRAPHY) 0.2 + slot * 0.1 else base.directionStrength,
                    speedStrength = if (tool == Tool.SPEED) 0.2 + slot * 0.1 else base.speedStrength,
                    taperMinFactor = if (tool == Tool.TAPER) 0.2 + slot * 0.1 else base.taperMinFactor,
                    highlighterAlpha = if (tool == Tool.HIGHLIGHTER) 0.3 + slot * 0.1 else base.highlighterAlpha,
                )
            }
            expected[tool] = styles
            styles.forEach { settings = settings.withAddedPenPreset(tool, it) }
            assertSame("$tool must reject a fourth slot", settings, settings.withAddedPenPreset(tool, base))
        }

        val reopened = Settings.fromJson(JSONObject(settings.toJson().toString()))
        for (tool in strokeTools) {
            assertEquals("$tool preset values and order must survive restart", expected.getValue(tool), reopened.savedPenPresets(tool))
            assertEquals("$tool active configuration must stay independent", ToolDefaults.configFor(tool), reopened.configFor(tool))
        }
        assertTrue(reopened.savedPenPresets(Tool.ERASER).isEmpty())

        val afterDelete = reopened.withoutPenPreset(Tool.CALLIGRAPHY, 1)
        assertEquals(listOf(expected.getValue(Tool.CALLIGRAPHY)[0], expected.getValue(Tool.CALLIGRAPHY)[2]),
            afterDelete.savedPenPresets(Tool.CALLIGRAPHY))
        assertEquals(expected.getValue(Tool.PEN), afterDelete.savedPenPresets(Tool.PEN))
        val restored = Settings.fromJson(JSONObject(afterDelete.toJson().toString()))
        assertEquals(2, restored.savedPenPresets(Tool.CALLIGRAPHY).size)
        assertEquals(3, restored.withAddedPenPreset(Tool.CALLIGRAPHY, expected.getValue(Tool.CALLIGRAPHY)[1])
            .savedPenPresets(Tool.CALLIGRAPHY).size)
    }
}
