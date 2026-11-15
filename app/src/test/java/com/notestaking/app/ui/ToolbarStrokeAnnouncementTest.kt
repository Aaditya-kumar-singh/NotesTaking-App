package com.notestaking.app.ui

import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** The toolbar's spoken preview describes the settings the next stroke will use. */
class ToolbarStrokeAnnouncementTest {
    private fun describe(tool: Tool, ink: Rgba, config: ToolConfig) =
        strokeStyleDescription(
            tool = tool,
            config = config,
            activeInk = ink,
            name = tool.id,
            colourLabel = "Colour",
            widthLabel = "Width",
            pressureLabel = "Pressure",
            sensitivityLabel = "Sensitivity",
            offLabel = "Off",
        )

    @Test
    fun penAnnouncesEffectiveColourWidthAndPressureSensitivity() {
        val current = Rgba(200, 45, 30)
        val config = ToolDefaults.configFor(Tool.PEN).copy(baseWidth = 4.5)
        val result = describe(Tool.PEN, current, config)

        assertTrue(result.startsWith(Tool.PEN.id))
        assertTrue(result.contains("Colour ${Rgba.toHex(current)}"))
        assertTrue(result.contains("Width ${"%.1f".format(Locale.getDefault(), 4.5)}"))
        assertTrue(result.contains("Pressure Sensitivity 72%"))
    }

    @Test
    fun fixedColourOverridesGlobalInkAndPressureOffIsAnnounced() {
        val pinned = Rgba(18, 85, 210)
        val config = ToolDefaults.configFor(Tool.TAPER).copy(
            baseWidth = 7.0,
            colorOverride = pinned,
            pressureEnabled = false,
        )
        val result = describe(Tool.TAPER, Rgba(255, 0, 0), config)

        assertTrue(result.contains(Rgba.toHex(pinned)))
        assertFalse(result.contains("#ff0000"))
        assertTrue(result.contains("Pressure Off"))
        assertFalse(result.contains("Sensitivity"))
    }

    @Test
    fun highlighterAndDashedToolsDoNotClaimUnsupportedPressureControl() {
        for (tool in listOf(Tool.HIGHLIGHTER, Tool.DASHED)) {
            val result = describe(tool, Rgba(32, 31, 30), ToolDefaults.configFor(tool))
            assertTrue(result.contains("Colour #201f1e"))
            assertTrue(result.contains("Width"))
            assertFalse(result.contains("Pressure"))
            assertFalse(result.contains("Sensitivity"))
        }
    }

    @Test
    fun changedPresetSettingsProduceAChangedAnnouncement() {
        val ink = Rgba(1, 2, 3)
        val before = ToolDefaults.configFor(Tool.PEN)
        val after = before.copy(
            baseWidth = 12.0,
            pressureMinFactor = 0.1,
            colorOverride = Rgba(100, 101, 102),
        )
        val previous = describe(Tool.PEN, ink, before)
        val updated = describe(Tool.PEN, ink, after)
        assertTrue(previous != updated)
        assertTrue(updated.contains("Width ${"%.1f".format(Locale.getDefault(), 12.0)}"))
        assertTrue(updated.contains("Sensitivity 100%"))
        assertTrue(updated.contains("#646566"))
        assertEquals(updated, describe(Tool.PEN, Rgba(200, 200, 200), after))
    }
}
