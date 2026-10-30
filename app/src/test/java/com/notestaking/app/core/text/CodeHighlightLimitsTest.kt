package com.notestaking.app.core.text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHighlightLimitsTest {
    @Test
    fun utf8AndNativeOutputBudgetsAreEnforced() {
        assertFalse(CodeHighlightLimits.acceptsSourceSize(0))
        assertTrue(CodeHighlightLimits.acceptsSourceSize(CodeHighlightLimits.MAX_INPUT_BYTES))
        assertFalse(CodeHighlightLimits.acceptsSourceSize(CodeHighlightLimits.MAX_INPUT_BYTES + 1))
        assertTrue(CodeHighlightLimits.acceptsCaptureArrayLength(0))
        assertTrue(CodeHighlightLimits.acceptsCaptureArrayLength(CodeHighlightLimits.MAX_CAPTURE_TRIPLES * 3))
        assertFalse(CodeHighlightLimits.acceptsCaptureArrayLength(CodeHighlightLimits.MAX_CAPTURE_TRIPLES * 3 + 3))
        assertFalse(CodeHighlightLimits.acceptsCaptureArrayLength(4))
        assertFalse(CodeHighlightLimits.acceptsCaptureArrayLength(-3))
    }
}
