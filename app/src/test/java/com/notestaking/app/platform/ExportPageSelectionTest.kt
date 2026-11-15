package com.notestaking.app.platform

import com.notestaking.app.core.util.ExportPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class ExportPageSelectionTest {
    private val pages = List(6) { Any() }

    @Test fun thisSelectedAllAndCustomRangesExportExactlyRequestedPages() {
        assertEquals(listOf(pages[2]), ExportPageSelection.resolve(pages, listOf(2)))
        assertEquals(listOf(pages[0], pages[4]), ExportPageSelection.resolve(pages, listOf(4, 0)))
        assertEquals(pages, ExportPageSelection.resolve(pages, pages.indices.toList()))
        val custom = ExportPageRange.parse("1,3-5", pages.size)!!
        assertEquals(listOf(pages[0], pages[2], pages[3], pages[4]), ExportPageSelection.resolve(pages, custom))
    }

    @Test fun overlapsNormalizeToOneExportPerPageInDocumentOrder() {
        val result = ExportPageSelection.resolve(pages, listOf(4, 1, 4, 0, 1))
        assertEquals(3, result.size)
        assertSame(pages[0], result[0])
        assertSame(pages[1], result[1])
        assertSame(pages[4], result[2])
    }

    @Test fun missingOrOutOfBoundsPageFailsWholeSelectionWithoutDroppingAny() {
        for (invalid in listOf(
            emptyList(), listOf(-1), listOf(6), listOf(0, 3, 6), listOf(0, -1, 5),
            listOf(Int.MAX_VALUE), listOf(Int.MIN_VALUE),
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                ExportPageSelection.resolve(pages, invalid)
            }
        }
    }

    @Test fun selectedPageIdentityAndOriginalOrderAreUnchanged() {
        val snapshot = pages.toList()
        val result = ExportPageSelection.resolve(snapshot, listOf(5, 2))
        assertSame(pages[2], result[0])
        assertSame(pages[5], result[1])
        assertEquals(pages, snapshot)
    }

    @Test fun dialogOnlyAcceptsTheExactOriginalPageObjects() {
        val before = listOf(Any(), Any(), Any())
        assertEquals(true, ExportPageSelection.matchesSnapshot(before, before.toList(), listOf(0, 2)))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, listOf(before[1], before[0], before[2]), listOf(0)))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, before.drop(1), listOf(2)))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, listOf(Any(), before[1], before[2]), listOf(0)))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, before, emptyList()))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, before, listOf(-1, 1)))
        assertEquals(false, ExportPageSelection.matchesSnapshot(before, before, listOf(1, 3)))
    }

    @Test fun equalContentOnReplacementPageDoesNotPassIdentityCheck() {
        data class PageLabel(val value: String)
        val original = listOf(PageLabel("ink"), PageLabel("cover"))
        val replaced = listOf(PageLabel("ink"), original[1])
        assertEquals(false, ExportPageSelection.matchesSnapshot(original, replaced, listOf(0)))
        assertEquals(true, ExportPageSelection.matchesSnapshot(original, replaced, listOf(1)))
    }

    @Test fun emptyDocumentCannotProduceAZeroPagePdf() {
        assertThrows(IllegalArgumentException::class.java) {
            ExportPageSelection.resolve(emptyList<Any>(), listOf(0))
        }
    }
}
