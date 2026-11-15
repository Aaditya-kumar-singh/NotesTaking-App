package com.notestaking.app.core.model

import com.notestaking.app.core.FakeRenderer
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.infinite.InkPass
import com.notestaking.app.core.infinite.ItemMesher
import com.notestaking.app.core.pal.BlendMode
import com.notestaking.app.core.pal.Renderer
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlighterBlendTest {
    private fun stroke(alpha: Double, inverse: Boolean): Stroke = Stroke(
        Tool.HIGHLIGHTER,
        ToolDefaults.configFor(Tool.HIGHLIGHTER).copy(
            highlighterAlpha = alpha,
            highlighterInverse = inverse,
            rgba = Rgba(250, 210, 20),
        ),
        listOf(Sample(0.0, 0.0, 1.0), Sample(20.0, 0.0, 1.0)),
    )

    @Test fun pagedAndInfiniteHighlightersKeepTheirBlendAtEveryOpacity() {
        for (inverse in listOf(false, true)) {
            val expectedBlend = if (inverse) BlendMode.SCREEN else BlendMode.MULTIPLY
            val expectedPass = if (inverse) InkPass.SCREEN else InkPass.MULTIPLY
            for (intensity in listOf(0.10, 0.35, 0.90, 1.0)) {
                val ink = stroke(intensity, inverse)
                val backing = FakeRenderer()
                var layerBlend: BlendMode? = null
                var layerAlpha = -1.0
                var layerCount = 0
                val renderer = object : Renderer by backing {
                    override fun saveLayerBlended(bounds: Rect, alpha: Double, blend: BlendMode) {
                        layerBlend = blend
                        layerAlpha = alpha
                        layerCount++
                        backing.saveLayerAlpha(bounds, alpha)
                    }
                }
                ink.paint(renderer)
                assertEquals("paged blend inverse=$inverse intensity=$intensity", expectedBlend, layerBlend)
                assertEquals("single layer for overlapping discs", 1, layerCount)
                assertEquals(ink.renderColor.a / 255.0, layerAlpha, 1e-9)
                assertTrue("opaque ribbon painted once", backing.ops.contains("fillCircle"))
                assertEquals("infinite canvas blend inverse=$inverse", expectedPass, ItemMesher.passFor(ink))
            }
        }
    }

    @Test fun ordinaryFullOpacityPenStillPaintsWithoutAnOffscreenBlend() {
        val ink = Stroke(
            Tool.PEN, ToolDefaults.configFor(Tool.PEN),
            listOf(Sample(0.0, 0.0, 1.0), Sample(20.0, 0.0, 1.0)),
        )
        val backing = FakeRenderer()
        var blend: BlendMode? = null
        val renderer = object : Renderer by backing {
            override fun saveLayerBlended(bounds: Rect, alpha: Double, mode: BlendMode) {
                blend = mode
            }
        }
        ink.paint(renderer)
        assertNull(blend)
        assertTrue(backing.ops.contains("fillCircle"))
    }
}
