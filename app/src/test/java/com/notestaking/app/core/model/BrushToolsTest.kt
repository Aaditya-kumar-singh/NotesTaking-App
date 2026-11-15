package com.notestaking.app.core.model

import com.notestaking.app.core.FakeRenderer
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-030: the five brush tools keep distinct geometry and their captured settings. */
class BrushToolsTest {
    private val brushes = listOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.TAPER, Tool.SPEED)

    private fun stroke(tool: Tool, pressure: Double = 1.0, intervalMs: Double = 10.0): Stroke {
        val s = Stroke(tool, ToolDefaults.configFor(tool))
        s.finished = false
        for (i in 0..40) s.addSample(Sample(i * 3.0, 0.0, pressure, i * intervalMs))
        s.finished = true
        return s
    }

    @Test fun brushesHaveDistinctAndStableInkSignatures() {
        val light = stroke(Tool.PEN, pressure = 0.1)
        val firm = stroke(Tool.PEN)
        assertTrue("pen pressure affects width", firm.geometry().hw(20) > light.geometry().hw(20))

        val dashed = stroke(Tool.DASHED)
        val dashedPaint = FakeRenderer()
        dashed.paint(dashedPaint)
        assertTrue("dashed ink uses a polyline", dashedPaint.dashRuns.isNotEmpty())
        assertTrue(dashedPaint.ops.contains("strokePolyline"))
        val penPaint = FakeRenderer()
        firm.paint(penPaint)
        assertTrue("pen ink uses a filled ribbon", penPaint.ribbonRuns.isNotEmpty())
        assertTrue(penPaint.dashRuns.isEmpty())

        val horizontal = stroke(Tool.CALLIGRAPHY)
        val vertical = Stroke(
            Tool.CALLIGRAPHY, ToolDefaults.configFor(Tool.CALLIGRAPHY),
            (0..40).map { Sample(0.0, it * 3.0, 1.0) },
        )
        assertTrue("calligraphic nib responds to direction",
            vertical.geometry().hw(20) > horizontal.geometry().hw(20) * 1.2)

        val taper = stroke(Tool.TAPER)
        assertTrue("taper narrows toward pen-up",
            taper.geometry().hw(40) < taper.geometry().hw(5))

        val slow = stroke(Tool.SPEED, intervalMs = 100.0)
        val fast = stroke(Tool.SPEED, intervalMs = 2.0)
        assertTrue("speed pen thins at high velocity",
            fast.geometry().hw(20) < slow.geometry().hw(20) * 0.8)
    }

    @Test fun twentyStrokesPerBrushKeepSettingsAndFiniteGeometryAtDrawZooms() {
        for (tool in brushes) {
            assertTrue("$tool is a persisted preset", tool in ToolDefaults.persistedTools)
            for (i in 0 until 20) {
                val zoom = listOf(0.25, 1.0, 4.0)[i % 3]
                val cfg = ToolDefaults.configFor(tool).copy(
                    baseWidth = ToolDefaults.configFor(tool).baseWidth + i * 0.1,
                    rgba = Rgba(40 + i, 90 + i, 140 + i),
                )
                val ink = Stroke(
                    tool, cfg, speedScale = zoom,
                    smoothScale = (1.0 / zoom).coerceAtMost(1.0),
                )
                ink.finished = false
                for (j in 0..40) ink.addSample(
                    Sample(j * 3.0 / zoom, j * 0.2 / zoom,
                        if (j in 12..28) 0.85 else 0.3, j * 9.0),
                )
                ink.finished = true
                assertEquals("$tool zoom=$zoom sample count", 41, ink.sampleCount)
                assertTrue("$tool zoom=$zoom widths are finite",
                    ink.geometry().halfWidths.all { it.isFinite() && it >= 0f })
                assertEquals("$tool original style", cfg, ink.config)
                assertEquals("$tool copied style", cfg, Stroke(ink).config)
                val rendered = FakeRenderer()
                ink.paint(rendered)
                assertFalse("$tool zoom=$zoom must render ink", rendered.ops.isEmpty())
            }
        }
    }

    @Test fun restylingOneBrushDoesNotDiscardItsSignatureOrAlterAnother() {
        for (tool in brushes) {
            val original = stroke(tool)
            val untouched = stroke(tool)
            val oldStyle = DrawStyle.of(original)!!
            val originalConfig = original.config
            val replacement = DrawStyle(Rgba(12, 34, 56), original.config.baseWidth * 1.5)
            replacement.applyTo(original)
            assertEquals(replacement, DrawStyle.of(original))
            assertEquals("pressure remains configured", originalConfig.pressureEnabled, original.config.pressureEnabled)
            assertEquals("direction remains configured", originalConfig.directionStrength, original.config.directionStrength, 1e-9)
            assertEquals("dash length remains configured", originalConfig.dashLength, original.config.dashLength, 1e-9)
            assertEquals("speed strength remains configured", originalConfig.speedStrength, original.config.speedStrength, 1e-9)
            assertEquals("taper remains configured", originalConfig.taperEnabled, original.config.taperEnabled)
            assertEquals("another stroke keeps its preset", originalConfig, untouched.config)
            oldStyle.applyTo(original)
            assertEquals(originalConfig, original.config)
        }
    }
}
