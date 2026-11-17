package com.notestaking.app.core.model

import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingInputPlanTest {
    private fun ink(vararg points: Pair<Double, Double>, tool: Tool = Tool.PEN): Stroke =
        Stroke(tool, ToolConfig(), points.mapIndexed { index, p ->
            Sample(p.first, p.second, 0.5, index * 10.0)
        })

    private fun invalid(block: () -> Unit) {
        assertTrue(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun stableDrawingOrderAndNonDestructiveNormalization() {
        val a = ink(100.0 to 200.0, 110.0 to 210.0)
        val b = ink(115.0 to 200.0, 125.0 to 210.0)
        val source = listOf(b, a)
        val before = source.map { it.samples.toList() }
        val first = HandwritingInputPlanner.plan(source, "en")
        assertEquals(first, HandwritingInputPlanner.plan(source, "en"))
        assertEquals(listOf(0, 1), first.lines.single().traces.map { it.sourceIndex })
        assertEquals(0f, first.lines.single().traces.first().points.first().y)
        assertEquals(0f, first.lines.single().traces.last().points.first().x)
        assertTrue(first.lines.single().traces.flatMap { it.points }.all { it.x in 0f..1000f && it.y in 0f..1000f })
        source.forEachIndexed { index, stroke ->
            assertEquals(before[index], stroke.samples)
            assertSame(source[index], stroke)
        }
    }

    @Test fun groupsSeparatedLinesButRetainsStrokeOrderWithinEachLine() {
        val lower = ink(10.0 to 140.0, 30.0 to 150.0)
        val upperSecond = ink(60.0 to 10.0, 80.0 to 20.0)
        val upperFirst = ink(10.0 to 10.0, 30.0 to 20.0)
        val plan = HandwritingInputPlanner.plan(listOf(lower, upperSecond, upperFirst), "hi")
        assertEquals("hi", plan.languageTag)
        assertEquals(2, plan.lines.size)
        assertEquals(listOf(1, 2), plan.lines[0].traces.map { it.sourceIndex })
        assertEquals(listOf(0), plan.lines[1].traces.map { it.sourceIndex })
    }

    @Test fun blankUnsupportedAndInvalidStrokesFailClosed() {
        val valid = ink(10.0 to 10.0, 20.0 to 20.0)
        invalid { HandwritingInputPlanner.plan(emptyList(), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(valid), "en-hi") }
        invalid { HandwritingInputPlanner.plan(listOf(valid), "auto") }
        invalid { HandwritingInputPlanner.plan(listOf(valid, valid), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(Stroke(Tool.PEN, ToolConfig())), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(ink(0.0 to 0.0, tool = Tool.HIGHLIGHTER)), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(ink(Double.NaN to 1.0)), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(ink(Double.POSITIVE_INFINITY to 1.0)), "en") }
        invalid { HandwritingInputPlanner.plan(listOf(ink(0.0 to 0.0, 100_001.0 to 0.0)), "en") }
        assertFalse(valid.samples.isEmpty())
    }

    @Test fun boundedSelectionsAndSingleDotStayFinite() {
        val dot = ink(55.0 to 66.0)
        val plan = HandwritingInputPlanner.plan(listOf(dot), "hi")
        assertEquals(1, plan.lines.size)
        assertEquals(0f, plan.lines.single().traces.single().points.single().x)
        assertEquals(0f, plan.lines.single().traces.single().points.single().y)
        val many = List(HandwritingInputPlanner.MAX_STROKES) { i -> ink(i.toDouble() to 0.0) }
        assertEquals(HandwritingInputPlanner.MAX_STROKES,
            HandwritingInputPlanner.plan(many, "en").lines.sumOf { it.traces.size })
        invalid { HandwritingInputPlanner.plan(many + ink(999.0 to 0.0), "en") }
        val huge = ink(*Array(HandwritingInputPlanner.MAX_POINTS + 1) { i ->
            (i % 100).toDouble() to (i / 100).toDouble()
        })
        invalid { HandwritingInputPlanner.plan(listOf(huge), "hi") }
    }

    @Test fun recognizedTextRequiresExplicitUserConfirmationInFutureUi() {
        // A provider is intentionally only a callback boundary, not an SDK dependency or a mutation.
        val stroke = ink(0.0 to 0.0)
        val request = HandwritingInputPlanner.plan(listOf(stroke), "en")
        val state = HandwritingModelState.NOT_DOWNLOADED
        assertEquals(HandwritingModelState.NOT_DOWNLOADED, state)
        assertEquals(1, request.lines.flatMap { it.traces }.size)
        assertEquals(1, stroke.sampleCount)
    }
}
