package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSelectionSafetyTest {
    private data class PageRef(val label: String)

    @Test fun undoDeletePrunesAbsentPagesWithoutDroppingCoverOrOtherSelections() {
        val cover = PageRef("cover")
        val first = PageRef("first")
        val removed = PageRef("removed")
        val selected = mutableListOf(cover, removed, first)
        PageSelectionSafety.prune(selected, listOf(cover, first))
        assertEquals(2, selected.size)
        assertSame(cover, selected[0])
        assertSame(first, selected[1])
    }

    @Test fun reorderPreservesSelectionByIdentityRatherThanIndex() {
        val cover = PageRef("cover")
        val one = PageRef("one")
        val two = PageRef("two")
        val selected = mutableListOf(two, cover)
        PageSelectionSafety.prune(selected, listOf(cover, two, one))
        assertEquals(2, selected.size)
        assertSame(two, selected[0])
        assertSame(cover, selected[1])
    }

    @Test fun equalContentFromDifferentPageIsNotMistakenForSelectedObject() {
        val old = PageRef("same content")
        val replacement = PageRef("same content")
        val selected = mutableListOf(old)
        PageSelectionSafety.prune(selected, listOf(replacement))
        assertTrue(selected.isEmpty())
    }

    @Test fun removingLastSelectedPageExitsSelectionMode() {
        val old = PageRef("deleted")
        val selected = mutableListOf(old)
        PageSelectionSafety.prune(selected, emptyList())
        assertTrue(selected.isEmpty())
    }
}
