package com.notestaking.app.ui

import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolbarEntry
import com.notestaking.app.core.tools.ToolbarItem
import com.notestaking.app.core.tools.ToolbarLayout
import com.notestaking.app.core.tools.ToolbarSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PDF reading layout is temporary; annotated toolbar preferences are left untouched. */
class PagedToolbarReadingModeTest {
    private val customized = ToolbarLayout(
        listOf(
            ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.PEN),
                ToolbarEntry(ToolbarItem.PAGE_NAV, visible = false),
                ToolbarEntry(ToolbarItem.FIT, visible = false),
            )),
            ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.ERASER),
                ToolbarEntry(ToolbarItem.SIDEBAR),
                ToolbarEntry(ToolbarItem.HOME, visible = false),
            )),
        ),
    )

    @Test
    fun onlyPdfWithPanUsesReaderPresentation() {
        assertTrue(pdfReadingToolbarActive(hasPdf = true, tool = Tool.PAN))
        assertFalse(pdfReadingToolbarActive(hasPdf = false, tool = Tool.PAN))
        for (tool in listOf(Tool.PEN, Tool.ERASER, Tool.HIGHLIGHTER, Tool.MARKUP)) {
            assertFalse(pdfReadingToolbarActive(hasPdf = true, tool = tool))
        }
    }

    @Test
    fun readingKeepsNavigationFirstAndHidesDrawingTools() {
        val reading = pagedToolbarSections(customized, readingPdf = true)
        assertEquals(listOf(listOf(
            ToolbarItem.PAGE_NAV,
            ToolbarItem.SIDEBAR,
            ToolbarItem.FIT,
            ToolbarItem.FULLSCREEN,
            ToolbarItem.HOME,
        )), reading)
        assertFalse(reading.flatten().contains(ToolbarItem.PEN))
        assertFalse(reading.flatten().contains(ToolbarItem.ERASER))
        assertTrue(reading.flatten().contains(ToolbarItem.PAGE_NAV))
    }

    @Test
    fun returningToAnnotateRestoresExactVisibilityAndOrder() {
        val originalSections = customized.sections.map { it.copy(entries = it.entries.toList()) }
        val before = pagedToolbarSections(customized, readingPdf = false)
        assertEquals(listOf(
            listOf(ToolbarItem.PEN),
            listOf(ToolbarItem.ERASER, ToolbarItem.SIDEBAR),
        ), before)
        pagedToolbarSections(customized, readingPdf = true)
        assertEquals(before, pagedToolbarSections(customized, readingPdf = false))
        assertEquals(originalSections, customized.sections)
        assertFalse(customized.sections[0].entries[1].visible)
        assertFalse(customized.sections[0].entries[2].visible)
    }
}
