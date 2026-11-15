package com.notestaking.app.ui

import com.notestaking.app.core.tools.ToolbarEntry
import com.notestaking.app.core.tools.ToolbarItem
import com.notestaking.app.core.tools.ToolbarLayout
import com.notestaking.app.core.tools.ToolbarSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarCustomizerSearchTest {
    private val layout = ToolbarLayout(
        listOf(
            ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.HOME),
                ToolbarEntry(ToolbarItem.PEN, visible = false),
                ToolbarEntry(ToolbarItem.ERASER),
            )),
            ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.TEXT_BOX, visible = false),
                ToolbarEntry(ToolbarItem.ZOOM),
            )),
        ),
    )

    private val localizedLabels = mapOf(
        ToolbarItem.PEN to "Plume",
        ToolbarItem.ERASER to "Gomme",
        ToolbarItem.TEXT_BOX to "Zone de texte",
    )

    private fun search(query: String) = toolbarSearchResults(layout, query) {
        localizedLabels[it] ?: it.id
    }

    @Test
    fun searchIsCaseInsensitiveLocalizedAndIncludesHiddenTools() {
        assertEquals(listOf(Triple(0, 1, layout.sections[0].entries[1])), search("  PLUME  "))
        assertEquals(listOf(Triple(0, 2, layout.sections[0].entries[2])), search("gomme"))
        assertEquals(listOf(Triple(0, 1, layout.sections[0].entries[1])), search("pen"))
    }

    @Test
    fun searchPreservesOriginalSectionAndEntryIndexesForToggles() {
        val (section, index, entry) = search("TEXT BOX").single()
        assertEquals(ToolbarItem.TEXT_BOX, entry.item)
        assertEquals(1, section)
        assertEquals(0, index)
        assertFalse(entry.visible)

        val toggled = layout.toggleVisible(section, index)
        assertTrue(toggled.sections[1].entries[0].visible)
        assertEquals(layout.sections[0], toggled.sections[0])
        assertEquals(layout.sections[1].entries[1], toggled.sections[1].entries[1])
        assertFalse(layout.sections[1].entries[0].visible)
    }

    @Test
    fun searchMatchesRawIdsAndLeavesLayoutsUnchanged() {
        assertEquals(1, search("text_box").size)
        assertTrue(search("  ").isEmpty())
        assertTrue(search("not a tool").isEmpty())
        assertEquals(2, layout.sections.size)
        assertEquals(5, layout.sections.sumOf { it.entries.size })
    }
}
