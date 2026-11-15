package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.AddItem
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.stroke.ShapeRecognizer
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** NP-036: a held stroke only becomes a shape after a confident page-local recognition. */
class ShapeHoldSnapRegressionTest {
    private fun circle(): List<Sample> = (0 until 48).map { i ->
        val a = 2.0 * PI * i / 48.0
        Sample(270.0 + 90.0 * cos(a), 290.0 + 90.0 * sin(a), 0.7, i * 16.0)
    }

    @Test fun shortAndStationaryPenTracesNeverBecomeShapesOrLoseRawInk() {
        val cfg = ToolDefaults.configFor(Tool.PEN)
        val traces = listOf(
            (0 until InteractionController.SHAPE_MIN_SAMPLES - 1).map { i ->
                Sample(20.0 + 35.0 * i, 180.0, 0.8, i * 16.0)
            },
            (0 until 48).map { i ->
                val a = 2.0 * PI * i / 48.0
                Sample(270.0 + 3.0 * cos(a), 290.0 + 3.0 * sin(a), 0.6, i * 16.0)
            },
        )
        for (samples in traces) {
            val stroke = Stroke(Tool.PEN, cfg, samples)
            val original = stroke.samples.toList()
            assertNull("not enough distance or samples to snap", ShapeRecognizer.recognize(stroke.samples))
            assertEquals("failed recognition must not edit handwriting samples", original, stroke.samples)
            assertEquals(original.size, stroke.sampleCount)
        }
    }

    @Test fun intentionalHeldCircleIsOneUndoableShapeAndDoesNotTouchOtherPages() {
        val cfg = ToolDefaults.configFor(Tool.DASHED)
        val live = Stroke(Tool.DASHED, cfg, circle())
        val samples = live.samples.toList()
        val rec = ShapeRecognizer.recognize(live.samples)
        assertNotNull("an intentional closed outline is recognizable", rec)
        assertEquals(ShapeKind.ELLIPSE, rec!!.kind)

        val otherPage = Page(600.0, 900.0)
        val targetPage = Page(600.0, 900.0)
        val separateInk = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN),
            listOf(Sample(20.0, 20.0, 0.5), Sample(180.0, 70.0, 0.8)))
        otherPage.items.add(separateInk)
        val history = History()
        val shape = rec.toShape(cfg.rgba, cfg.baseWidth * InteractionController.SHAPE_PEN_PARITY,
            null, cfg.neon, cfg.neonStrength, true, cfg.dashLength, cfg.dashGap)

        // InteractionController commits the newly recognized shape, not the uncommitted wet stroke.
        targetPage.items.add(shape)
        history.push(AddItem(targetPage, shape))
        assertSame(shape, targetPage.items.single())
        assertTrue(targetPage.items.none { it === live })
        assertEquals(cfg.rgba, shape.strokeRgba)
        assertEquals(cfg.baseWidth * InteractionController.SHAPE_PEN_PARITY, shape.strokeWidth, 1e-9)
        assertTrue(shape.dashed)
        assertEquals(samples, live.samples)

        history.undo()
        assertTrue("one undo removes the snapped shape", targetPage.items.isEmpty())
        assertTrue(history.canRedo)
        assertFalse(history.canUndo)
        assertSame(separateInk, otherPage.items.single())
        history.redo()
        assertSame("redo restores the same editable shape", shape, targetPage.items.single())
        assertEquals(1, targetPage.items.size)
        assertSame(separateInk, otherPage.items.single())
    }

    @Test fun recognitionStaysPageLocalAcrossZoomRotationMarginsAndPageModes() {
        val original = ShapeRecognizer.recognize(circle())!!
        for (vertical in listOf(true, false)) {
            for (mode in ViewingMode.values()) {
                for (turn in listOf(0, 90, 180, 270)) {
                    for (scale in listOf(0.5, 1.0, 3.0)) {
                        val pages = MutableList(3) { Page(600.0, 900.0) }
                        pages[1].margins = PageMargins(left = 0.15, top = 0.1, right = 0.05)
                        val state = CanvasState(Document(pages), FakeSurfaceFactory(), Palette.DEFAULT).apply {
                            viewportW = 1000
                            viewportH = 1300
                            verticalScroll = vertical
                            viewingMode = mode
                            rotationDeg = turn
                            zoom = scale
                            relayout()
                            goToPage(1)
                        }
                        assertEquals("page mapping must find the target page", 1,
                            state.pageIndexAtContent(state.fromPageSpace(1, Pt(270.0, 290.0))))
                        val restored = circle().map { sample ->
                            val pagePoint = Pt(sample.x, sample.y)
                            val screen = state.contentToViewport(state.fromPageSpace(1, pagePoint))
                            val back = state.toPageSpace(1, state.viewportToContent(screen))
                            assertEquals(pagePoint.x, back.x, 1e-6)
                            assertEquals(pagePoint.y, back.y, 1e-6)
                            Sample(back.x, back.y, sample.pressure, sample.t)
                        }
                        val recognized = ShapeRecognizer.recognize(restored)
                        assertNotNull("mode=$mode vertical=$vertical turn=$turn scale=$scale", recognized)
                        assertEquals(original.kind, recognized!!.kind)
                        assertEquals(original.start.x, recognized.start.x, 1e-4)
                        assertEquals(original.start.y, recognized.start.y, 1e-4)
                        assertEquals(original.end.x, recognized.end.x, 1e-4)
                        assertEquals(original.end.y, recognized.end.y, 1e-4)
                    }
                }
            }
        }
    }
}
