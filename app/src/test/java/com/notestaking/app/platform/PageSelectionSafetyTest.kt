package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test fun destructiveSelectionRejectsMissingPagesInsteadOfCuttingOnlyTheValidSubset() {
        assertEquals(listOf(1, 3), PageSelectionSafety.checkedIndices(5, listOf(3, 1, 3)))
        assertEquals(listOf(0, 1), PageSelectionSafety.checkedIndices(2, listOf(1, 0)))
        assertNull(PageSelectionSafety.checkedIndices(5, emptyList()))
        assertNull(PageSelectionSafety.checkedIndices(5, listOf(1, -1)))
        assertNull(PageSelectionSafety.checkedIndices(5, listOf(1, 5)))
        assertNull(PageSelectionSafety.checkedIndices(0, listOf(0)))
    }

    @Test fun removingLastSelectedPageExitsSelectionMode() {
        val old = PageRef("deleted")
        val selected = mutableListOf(old)
        PageSelectionSafety.prune(selected, emptyList())
        assertTrue(selected.isEmpty())
    }
}
