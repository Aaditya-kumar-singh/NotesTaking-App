package com.notestaking.app.core.text

import com.notestaking.app.core.FakeRenderer
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only NT-054 probes: stored text vs laid-out lines vs painter output. */
class FlowVisibilityRegressionTest {
    private val layout = FlowLayout(FakeTextMeasurer())
    private val marker = "NT054MARKER"

    private fun flowAfter(blanks: Int): TextFlow = TextFlow().apply {
        margins = FlowMargins(0.0, 0.0, 0.0, 0.0)
        repeat(blanks) { paragraphs.add(Paragraph()) }
        paragraphs.add(Paragraph(mutableListOf(Run(marker))))
    }

    private fun markerLines(frame: FlowFrame): List<Pair<Int, PlacedLine>> =
        frame.pages.flatMapIndexed { page, content ->
            content.lines.filter { line -> line.segs.any { it.text == marker } }
                .map { page to it }
        }

    private fun paintedMarker(frame: FlowFrame, page: Int, box: PageBox): Boolean {
        val renderer = FakeRenderer()
        FlowPainter.paintPage(renderer, frame, page, Rect(0.0, 0.0, box.width, box.height))
        return renderer.ops.any { it.startsWith("drawTextRun:$marker@") }
    }

    @Test fun nearTopTextAfterTwoBlankParagraphsHasVisibleInkAndLayout() {
        val box = PageBox(1240.0, 1754.0)
        val flow = flowAfter(2)
        val frame = layout.layout(flow, listOf(box), 150)
        val placed = markerLines(frame).single()
        assertEquals(0, placed.first)
        assertEquals(2, placed.second.paraIndex)
        assertEquals(marker, placed.second.segs.single().text)
        assertEquals(0, frame.extraPagesNeeded)
        assertTrue(paintedMarker(frame, 0, box))
        assertEquals(marker, flow.paragraphs[2].runs.single().text)
    }

    @Test fun lowerTextAfter48BlankParagraphsStillPaintsOnA4Page() {
        val box = PageBox(1240.0, 1754.0)
        val flow = flowAfter(48)
        val frame = layout.layout(flow, listOf(box), 150)
        val placed = markerLines(frame).single()
        assertEquals(0, placed.first)
        assertEquals(48, placed.second.paraIndex)
        assertEquals(0, frame.extraPagesNeeded)
        assertTrue(paintedMarker(frame, 0, box))
        assertEquals(marker, flow.paragraphs[48].runs.single().text)
    }

    @Test fun overflowKeepsTextInFlowUntilCallerAddsPhysicalPages() {
        val box = PageBox(1240.0, 350.0)
        val flow = flowAfter(48)
        val onePage = layout.layout(flow, listOf(box), 150)
        assertEquals(2, onePage.extraPagesNeeded)
        assertTrue(markerLines(onePage).isEmpty())
        assertFalse(paintedMarker(onePage, 0, box))
        assertEquals(marker, flow.paragraphs[48].runs.single().text)

        val expanded = layout.layout(flow, List(3) { box }, 150)
        val placed = markerLines(expanded).single()
        assertEquals(2, placed.first)
        assertEquals(48, placed.second.paraIndex)
        assertEquals(0, expanded.extraPagesNeeded)
        assertTrue(paintedMarker(expanded, 2, box))
    }
}
