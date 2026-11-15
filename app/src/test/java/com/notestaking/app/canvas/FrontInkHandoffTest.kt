package com.notestaking.app.canvas

import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pending front-buffer callbacks must never revive a finished or abandoned stroke. */
class FrontInkHandoffTest {
    private fun pen() = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN)).apply {
        finished = false
    }

    @Test fun delayedTakeoverOnlyActivatesTheCurrentLiveStroke() {
        val first = pen()
        val second = pen()
        assertTrue(canActivateFrontInk(first, first))
        assertFalse("a cancelled stroke has no owner", canActivateFrontInk(null, first))
        assertFalse("a newer stroke owns the pad", canActivateFrontInk(second, first))
        first.finished = true
        assertFalse("pen-up must fence late frame callbacks", canActivateFrontInk(first, first))
        assertTrue(canActivateFrontInk(second, second))
    }
}
