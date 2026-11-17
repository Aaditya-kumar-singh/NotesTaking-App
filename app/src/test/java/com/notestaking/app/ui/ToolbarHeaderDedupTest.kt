package com.notestaking.app.ui

import com.notestaking.app.core.tools.ToolbarEntry
import com.notestaking.app.core.tools.ToolbarItem
import com.notestaking.app.core.tools.ToolbarLayout
import com.notestaking.app.core.tools.ToolbarSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Header chrome owns Back, filename and split closing only after integration opts in. */
class ToolbarHeaderDedupTest {
    private val layout = ToolbarLayout(listOf(
        ToolbarSection(listOf(ToolbarEntry(ToolbarItem.HOME), ToolbarEntry(ToolbarItem.TITLE))),
        ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PEN), ToolbarEntry(ToolbarItem.UNDO))),
        ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PAGE_NAV), ToolbarEntry(ToolbarItem.IMAGE))),
    ))

    @Test fun legacyPagedAndCanvasToolbarsRemainUnchangedByDefault() {
        val old = pagedToolbarSections(layout, readingPdf = false)
        assertEquals(old, pagedToolbarSections(layout, readingPdf = false, headerChromeVisible = false))
        assertEquals(layout.visibleSections.map { s -> s.visibleEntries.map { it.item } },
            toolbarSectionsWithoutHeader(old, false))
        assertTrue(old.flatten().containsAll(listOf(ToolbarItem.HOME, ToolbarItem.TITLE)))
    }

    @Test fun newHeaderRemovesOnlyDuplicateChromeWithoutEmptySections() {
        val before = layout.sections.map { it.entries.toList() }
        val paged = pagedToolbarSections(layout, readingPdf = false, headerChromeVisible = true)
        val canvas = toolbarSectionsWithoutHeader(layout.visibleSections.map { it.visibleEntries.map { e -> e.item } }, true)
        assertEquals(listOf(listOf(ToolbarItem.PEN, ToolbarItem.UNDO),
            listOf(ToolbarItem.PAGE_NAV, ToolbarItem.IMAGE)), paged)
        assertEquals(paged, canvas)
        assertFalse(paged.flatten().contains(ToolbarItem.HOME))
        assertFalse(paged.flatten().contains(ToolbarItem.TITLE))
        assertEquals(before, layout.sections.map { it.entries })
    }

    @Test fun pdfReadingRetainsPageControlsAndOmitsOnlyHomeWithHeader() {
        val old = pagedToolbarSections(layout, readingPdf = true)
        val dedup = pagedToolbarSections(layout, readingPdf = true, headerChromeVisible = true)
        assertTrue(old.flatten().contains(ToolbarItem.HOME))
        assertFalse(dedup.flatten().contains(ToolbarItem.HOME))
        assertTrue(dedup.flatten().containsAll(listOf(ToolbarItem.PAGE_NAV,
            ToolbarItem.SIDEBAR, ToolbarItem.FIT, ToolbarItem.FULLSCREEN)))
        assertEquals(old, pagedToolbarSections(layout, readingPdf = true, headerChromeVisible = false))
    }
}
