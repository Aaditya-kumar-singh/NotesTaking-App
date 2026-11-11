package com.notestaking.app.core.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfReadingToolChoiceTest {
    @Test fun readingUsesPanWithoutLosingLastAnnotationTool() {
        val mode = PdfReadingToolChoice()
        assertEquals(Tool.PEN, mode.toolFor(reading = false))
        mode.remember(Tool.HIGHLIGHTER)
        assertEquals(Tool.PAN, mode.toolFor(reading = true))
        mode.remember(Tool.PAN)
        mode.remember(Tool.SCREENSHOT)
        mode.remember(Tool.IMAGE)
        assertEquals(Tool.HIGHLIGHTER, mode.toolFor(reading = false))
        mode.remember(Tool.MARKUP)
        assertEquals(Tool.MARKUP, mode.toolFor(reading = false))
    }
}
