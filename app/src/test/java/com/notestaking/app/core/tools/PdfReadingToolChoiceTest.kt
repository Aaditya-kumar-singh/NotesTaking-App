package com.notestaking.app.core.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfReadingToolChoiceTest {
    @Test fun readAnnotateEraserReadAnnotateRestoresActualLastTool() {
        val mode = PdfReadingToolChoice()
        var active = Tool.PAN
        fun switch(reading: Boolean) {
            if (reading) mode.remember(active) // matches Editor.setPdfReadingMode
            active = mode.toolFor(reading)
        }
        switch(reading = true)
        assertEquals(Tool.PAN, active)
        switch(reading = false)
        assertEquals(Tool.PEN, active)
        active = Tool.ERASER
        mode.remember(active) // matches Editor.selectTool
        switch(reading = true)
        assertEquals(Tool.PAN, active)
        switch(reading = false)
        assertEquals(Tool.ERASER, active)
        active = Tool.HIGHLIGHTER
        mode.remember(active)
        switch(reading = true)
        switch(reading = false)
        assertEquals(Tool.HIGHLIGHTER, active)
    }

    @Test fun allPdfEditingToolsCanBeRestoredAfterPan() {
        val mode = PdfReadingToolChoice()
        for (tool in Tool.entries.filterNot { it == Tool.PAN || it == Tool.SCREENSHOT || it == Tool.IMAGE }) {
            mode.remember(tool)
            assertEquals("reading remains navigation: $tool", Tool.PAN, mode.toolFor(reading = true))
            assertEquals("annotation restores exactly: $tool", tool, mode.toolFor(reading = false))
        }
    }

    @Test fun transientToolsAndRepeatedReadTapsDoNotReplaceLastAnnotation() {
        val mode = PdfReadingToolChoice()
        mode.remember(Tool.MARKUP)
        for (tool in listOf(Tool.SCREENSHOT, Tool.IMAGE, Tool.PAN, Tool.PAN)) {
            mode.remember(tool)
            assertEquals(Tool.PAN, mode.toolFor(reading = true))
        }
        assertEquals(Tool.MARKUP, mode.toolFor(reading = false))
    }

    @Test fun rememberedPdfToolsAreIndependentPerEditorPane() {
        val left = PdfReadingToolChoice()
        val right = PdfReadingToolChoice()
        left.remember(Tool.CALLIGRAPHY)
        right.remember(Tool.LASSO)
        assertEquals(Tool.PAN, left.toolFor(reading = true))
        assertEquals(Tool.PAN, right.toolFor(reading = true))
        assertEquals(Tool.CALLIGRAPHY, left.toolFor(reading = false))
        assertEquals(Tool.LASSO, right.toolFor(reading = false))
        right.remember(Tool.ERASER)
        assertEquals(Tool.CALLIGRAPHY, left.toolFor(reading = false))
        assertEquals(Tool.ERASER, right.toolFor(reading = false))
    }

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
