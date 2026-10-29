package com.notestaking.app.core.model

import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Area erase now clips continuous stroke segments, not just the points sampled by the pen. */
class StrokeEraseTest {
    private fun stroke(vararg pts: Pair<Double, Double>): Stroke =
        Stroke(Tool.PEN, ToolConfig(), pts.map { Sample(it.first, it.second, 1.0) }.toMutableList())

    private fun near(expected: Double, actual: Double, tolerance: Double = 0.002) =
        assertEquals(expected, actual, tolerance)

    @Test fun untouchedReturnsNull() {
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0)
        assertNull(s.erasedBy(100.0, 100.0, 5.0))
    }

    @Test fun emptyStrokeReturnsNull() {
        assertNull(Stroke(Tool.PEN, ToolConfig()).erasedBy(0.0, 0.0, 5.0))
    }

    @Test fun fullCoverReturnsEmpty() {
        val s = stroke(0.0 to 0.0, 1.0 to 0.0, 2.0 to 0.0)
        assertTrue(s.erasedBy(1.0, 0.0, 10.0)!!.isEmpty())
    }

    @Test fun endTouchTrimsToOneFragmentWithInterpolatedBoundary() {
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0, 30.0 to 0.0, 40.0 to 0.0, 50.0 to 0.0)
        val frags = s.erasedBy(5.0, 0.0, 8.0)!!
        assertEquals(1, frags.size)
        near(14.5, frags[0].samples.first().x) // r 8 plus 1.5px half nib
        near(50.0, frags[0].samples.last().x)
        assertTrue(frags[0].samples.size >= 5)
    }

    @Test fun middleCircleSplitsSparseStrokeWithoutDeletingUntouchedInk() {
        val s = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0, 30.0 to 0.0, 40.0 to 0.0, 50.0 to 0.0)
        val parts = s.erasedBy(25.0, 0.0, 8.0)!!
        assertEquals(2, parts.size)
        near(0.0, parts[0].samples.first().x)
        near(15.5, parts[0].samples.last().x)
        near(34.5, parts[1].samples.first().x)
        near(50.0, parts[1].samples.last().x)
        assertTrue(parts[0].samples.zipWithNext().all { it.first.x <= it.second.x })
    }

    @Test fun multipleHolesProduceThreeFragments() {
        val s = stroke(10.0 to 0.0, 0.0 to 0.0, -10.0 to 0.0, -10.0 to 1.0, 0.0 to 1.0, 10.0 to 1.0)
        val frags = s.erasedBy(0.0, 0.0, 5.0)!!
        assertEquals(3, frags.size)
        assertTrue(frags.all { it.samples.isNotEmpty() })
    }

    @Test fun singleOriginalDotsAndSparseSegmentsAreHandled() {
        val dot = stroke(0.0 to 0.0)
        assertTrue(dot.erasedBy(0.0, 0.0, 5.0)!!.isEmpty())
        assertNull(dot.erasedBy(30.0, 0.0, 5.0))
        val s = stroke(0.0 to 0.0, 20.0 to 0.0)
        val parts = s.erasedBy(10.0, 0.0, 5.0)!!
        assertEquals(2, parts.size)
        near(3.5, parts[0].samples.last().x)
        near(16.5, parts[1].samples.first().x)
    }

    @Test fun straightTwoPointLineIsTrimmedInsteadOfDeleted() {
        val s = Stroke(
            Tool.PEN, ToolConfig(baseWidth = 2.0),
            listOf(Sample(0.0, 0.0, 0.4, 1.0), Sample(100.0, 0.0, 0.8, 101.0)),
            straight = true,
        )
        val parts = s.erasedBy(50.0, 0.0, 5.0)!!
        assertEquals(2, parts.size)
        assertTrue(parts.all { it.straight })
        near(44.0, parts[0].samples.last().x)
        near(56.0, parts[1].samples.first().x)
        near(0.576, parts[0].samples.last().pressure)
        near(45.0, parts[0].samples.last().t)
        near(0.624, parts[1].samples.first().pressure)
        near(57.0, parts[1].samples.first().t)
        near(100.0, parts[1].samples.last().x)
    }

    @Test fun offCenterCutInterpolatesCircleBoundaries() {
        val s = stroke(0.0 to 0.0, 100.0 to 0.0)
        // Expanded r=10+1.5: at y=8, half-chord sqrt(11.5^2 - 8^2).
        val fragments = s.erasedBy(50.0, 8.0, 10.0)!!
        val halfChord = kotlin.math.sqrt(11.5 * 11.5 - 8.0 * 8.0)
        assertEquals(2, fragments.size)
        near(50.0 - halfChord, fragments[0].samples.last().x)
        near(50.0 + halfChord, fragments[1].samples.first().x)
    }

    @Test fun fragmentsShareToolConfigAndSpeedScale() {
        val cfg = ToolConfig(baseWidth = 7.0)
        val orig = Stroke(
            Tool.HIGHLIGHTER, cfg,
            listOf(Sample(0.0, 0.0, 1.0), Sample(10.0, 0.0, 1.0), Sample(20.0, 0.0, 1.0)),
            speedScale = 2.5,
        )
        orig.group = 4
        for (frag in orig.erasedBy(0.0, 0.0, 5.0)!!) {
            assertEquals(Tool.HIGHLIGHTER, frag.tool)
            assertSame(cfg, frag.config)
            assertEquals(2.5, frag.speedScale, 1e-12)
            assertEquals(4, frag.group)
        }
    }

    @Test fun fragmentsAreIndependentCopiesAndCanBeErasedAgain() {
        val orig = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 0.0)
        val frag = orig.erasedBy(0.0, 0.0, 5.0)!!.first()
        val before = frag.samples.size
        orig.setSamples(emptyList())
        assertEquals(before, frag.samples.size)
        near(6.5, frag.samples.first().x)
        val again = frag.erasedBy(14.0, 0.0, 2.0)!!
        assertTrue(again.isNotEmpty())
        assertTrue(again.first().samples.first().x < 10.0)
    }

    @Test fun agreesWithIntersectsCircleForSparseAndStraightInk() {
        val curved = stroke(0.0 to 0.0, 10.0 to 0.0, 20.0 to 10.0, 30.0 to 10.0)
        val straight = Stroke(Tool.PEN, ToolConfig(), listOf(Sample(0.0, 0.0, 1.0), Sample(100.0, 0.0, 1.0)), straight = true)
        val cases = listOf(
            Triple(5.0, 0.0, 6.0), Triple(100.0, 100.0, 5.0),
            Triple(20.0, 10.0, 4.0), Triple(15.0, 5.0, 2.0),
            Triple(25.0, 10.0, 50.0),
        )
        for (s in listOf(curved, straight)) for ((cx, cy, r) in cases) {
            assertEquals("center=($cx,$cy) r=$r", s.intersectsCircle(cx, cy, r), s.erasedBy(cx, cy, r) != null)
        }
    }

    @Test fun eraserMissesParallelLineWithoutMutatingIt() {
        val s = Stroke(Tool.PEN, ToolConfig(baseWidth = 2.0), listOf(Sample(0.0, 0.0, 1.0), Sample(100.0, 0.0, 1.0)), straight = true)
        assertNull(s.erasedBy(50.0, 25.0, 5.0))
        assertFalse(s.samples.isEmpty())
        near(100.0, s.samples.last().x)
    }
}
