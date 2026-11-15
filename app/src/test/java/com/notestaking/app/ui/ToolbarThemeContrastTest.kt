package com.notestaking.app.ui

import com.notestaking.app.core.model.Rgba
import com.notestaking.app.settings.MaterialStyle
import com.notestaking.app.ui.theme.MaterialColors
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The selected swatch's outline must remain visible even when ink matches toolbar chrome. */
class ToolbarThemeContrastTest {
    private fun luminance(c: Rgba): Double {
        fun linear(channel: Int): Double = (channel / 255.0).let {
            if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(c.r) + 0.7152 * linear(c.g) + 0.0722 * linear(c.b)
    }

    private fun contrast(a: Rgba, b: Rgba): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    @Test
    fun selectedSwatchOutlineUsesReadableThemeTextForEveryAppearance() {
        val seeds = listOf(0x000000, 0xffffff, 0x00e676, 0x800000, 0x0000ff, 0xffff00)
        for (appearance in listOf("light", "dark", "oled")) {
            for (seed in seeds) for (style in MaterialStyle.entries) {
                val ink = Rgba.fromArgb(seed or (0xff shl 24))
                val palette = Palette.material(
                    appearance,
                    MaterialColors.seeded(ink, dark = appearance != "light", style = style),
                )
                val ratio = contrast(palette.text, palette.panel)
                assertTrue("$appearance $style seed=$seed selected swatch outline $ratio", ratio >= 4.45)
            }
        }
    }

    @Test
    fun blackAndWhiteInkCanMatchToolbarChromeWithoutChangingTheSelectedOutline() {
        val black = Rgba(0, 0, 0)
        val oled = Palette.material("oled", MaterialColors.seeded(black, dark = true))
        assertEquals(black.copy(a = 255), oled.panel)
        assertEquals(1.0, contrast(black.copy(a = 255), oled.panel), 0.001)
        assertTrue(contrast(oled.text, oled.panel) >= 4.45)

        val white = Rgba(255, 255, 255)
        val light = Palette.material("light", MaterialColors.seeded(white, dark = false))
        assertTrue(contrast(light.text, light.panel) >= 4.45)
    }
}
