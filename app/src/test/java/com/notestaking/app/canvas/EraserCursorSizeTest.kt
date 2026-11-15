package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** NT-034: eraser size, cursor ring and erased ink all refer to the same disc at every zoom. */
class EraserCursorSizeTest {
    private fun controller(state: CanvasState, render: () -> Unit = {}): InteractionController =
        InteractionController(state, History(), FakeTextMeasurer(), requestRender = render)

    private fun state() = CanvasState(
        Document(mutableListOf(Page(600.0, 600.0))), FakeSurfaceFactory(), Palette.DEFAULT,
    ).apply {
        viewportW = 800
        viewportH = 1000
        relayout()
    }

    @Test fun cursorAndErasedSegmentMatchSliderAtQuarterToFourTimesZoom() {
        val state = state()
        val controller = controller(state)
        for (scale in listOf(true, false)) {
            for (size in listOf(4.0, 24.0, 80.0)) {
                controller.setToolConfig(
                    Tool.ERASER,
                    ToolDefaults.configFor(Tool.ERASER).copy(
                        baseWidth = size, scale = scale, eraseMode = EraseMode.AREA,
                    ),
                )
                for (zoom in listOf(0.25, 1.0, 4.0)) {
                    state.zoom = zoom
                    val expectedCursor = if (scale) size * zoom else size
                    val actualCursor = controller.eraserCursorRadiusPx()
                    val actualCutRadius = controller.eraserRadius()
                    assertEquals("ring radius scale=$scale size=$size zoom=$zoom", expectedCursor, actualCursor, 1e-6)
                    assertEquals("ring and cut use the same physical radius", actualCursor, actualCutRadius * zoom, 1e-6)

                    // Area mode removes a width of two radii, plus the ink's own half-width
                    // on each side. The visible ring denotes the eraser disc, not the nib margin.
                    val nibHalfWidth = 1.0
                    val ink = Stroke(
                        Tool.PEN, ToolDefaults.configFor(Tool.PEN).copy(baseWidth = 2.0),
                        listOf(Sample(-1000.0, 0.0, 1.0), Sample(1000.0, 0.0, 1.0)),
                        straight = true,
                    )
                    val pieces = ink.erasedBy(0.0, 0.0, actualCutRadius)
                    assertTrue("area eraser splits the line", pieces != null && pieces.size == 2)
                    val leftEnd = pieces!![0].samples.last().x
                    val rightStart = pieces[1].samples.first().x
                    assertEquals(-actualCutRadius - nibHalfWidth, leftEnd, 0.05)
                    assertEquals(actualCutRadius + nibHalfWidth, rightStart, 0.05)
                    assertEquals("erased width follows the ring",
                        actualCursor * 2.0, (rightStart - leftEnd - 2.0 * nibHalfWidth) * zoom, 0.12)
                }
            }
        }
    }

    @Test fun movingTheEraserSliderSchedulesACursorRedrawWithoutAnotherPointerEvent() {
        val state = state()
        var renders = 0
        val controller = controller(state) { renders++ }
        val default = ToolDefaults.configFor(Tool.ERASER)
        controller.setToolConfig(Tool.ERASER, default)
        assertEquals("unchanged config needs no frame", 0, renders)

        controller.setToolConfig(Tool.ERASER, default.copy(baseWidth = 4.0))
        assertEquals(1, renders)
        controller.setToolConfig(Tool.ERASER, default.copy(baseWidth = 24.0))
        assertEquals(2, renders)
        controller.setToolConfig(Tool.ERASER, default.copy(baseWidth = 80.0))
        assertEquals(3, renders)
        controller.setToolConfig(Tool.ERASER, default.copy(baseWidth = 80.0))
        assertEquals("unchanged slider value needs no extra frame", 3, renders)

        controller.setToolConfig(Tool.ERASER, default.copy(baseWidth = 80.0, scale = false))
        assertEquals("zoom mode also changes the ring", 4, renders)
        controller.setToolConfig(Tool.PEN, ToolDefaults.configFor(Tool.PEN).copy(baseWidth = 8.0))
        assertEquals("unrelated pen settings do not force eraser frames", 4, renders)
    }
}
