package com.notestaking.app.platform

import com.notestaking.app.core.search.SearchHit
import com.notestaking.app.core.search.SearchQuery
import com.notestaking.app.core.search.SearchResults
import com.notestaking.app.core.search.SearchTarget
import com.notestaking.app.core.search.SearchText
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SearchJumpTest {
    private val query = SearchQuery("東京")
    private val pdfText = SearchText.ofPdf(intArrayOf(0x6771, 0x4EAC), byteArrayOf(0, 0))

    private fun hit(page: Int): SearchHit = SearchHit(page, SearchTarget.Pdf, pdfText, 0, 2)

    private fun results(
        hits: List<List<SearchHit>>,
        complete: BooleanArray = BooleanArray(hits.size) { true },
        done: Boolean = true,
    ): SearchResults = SearchResults(
        query, hits.toTypedArray(), complete,
        complete.count { it }, complete.size, done, capped = false,
    )

    @Test fun nonLatinPdfTextRetainsSourceOffsetsForJumpAndHighlight() {
        assertArrayEquals(intArrayOf(0, 2), pdfText.find(query))
        val first = hit(0)
        val rotatedView = hit(2)
        val r = results(listOf(listOf(first), emptyList(), listOf(rotatedView)))
        assertSame(rotatedView, SearchJump.step(r, query, null, 2, forward = true))
        assertSame(first, SearchJump.step(r, query, rotatedView, 2, forward = true))
        assertSame(rotatedView, SearchJump.step(r, query, first, 0, forward = false))
        assertSame(first, SearchJump.step(r, query, null, 1, forward = false))
        org.junit.Assert.assertEquals(0, rotatedView.sourceStart)
        org.junit.Assert.assertEquals(2, rotatedView.sourceEnd)
    }

    @Test fun doNotJumpToWrongFirstHitWhileCurrentPdfPageIsUnscanned() {
        val known = hit(0)
        val r = results(listOf(listOf(known), emptyList(), emptyList()),
            booleanArrayOf(true, false, false), done = false)
        // Page 2 may contain an earlier visible match; firstFrom cannot choose yet.
        assertNull(SearchJump.step(r, query, null, 2, forward = true))
        assertNull(SearchJump.step(r, query, null, 2, forward = false))
    }

    @Test fun previousFromUnselectedPageUsesNearestVisiblePageWithHits() {
        val earlier = hit(1)
        val later = hit(3)
        val r = results(listOf(emptyList(), listOf(earlier), emptyList(), listOf(later)))
        assertSame(earlier, SearchJump.step(r, query, null, 2, forward = false))
        assertSame(later, SearchJump.step(r, query, null, 2, forward = true))
        assertSame(later, SearchJump.step(r, query, null, 0, forward = false))
    }

    @Test fun partialPdfScanNeverSkipsAnUnreadPageBetweenKnownHits() {
        val first = hit(0)
        val distant = hit(2)
        val partial = results(listOf(listOf(first), emptyList(), listOf(distant)),
            booleanArrayOf(true, false, true), done = false)
        assertNull(SearchJump.step(partial, query, first, 0, forward = true))
        assertNull(SearchJump.step(partial, query, distant, 2, forward = false))

        val completeMiddle = results(listOf(listOf(first), emptyList(), listOf(distant)),
            booleanArrayOf(true, true, true), done = false)
        assertSame(distant, SearchJump.step(completeMiddle, query, first, 0, forward = true))
    }

    @Test fun knownHitsOnTheSameCompletedPageCanStillStepDuringScan() {
        val one = hit(0)
        val two = hit(0)
        val r = results(listOf(listOf(one, two), emptyList()),
            booleanArrayOf(true, false), done = false)
        assertSame(two, SearchJump.step(r, query, one, 0, forward = true))
        assertSame(one, SearchJump.step(r, query, two, 0, forward = false))
    }

    @Test fun staleRowsAndOldQueryHitsCannotTriggerNavigation() {
        val active = hit(1)
        val stale = hit(1)
        val r = results(listOf(emptyList(), listOf(active)))
        assertFalse(SearchJump.contains(r, SearchQuery("other"), active))
        assertFalse(SearchJump.contains(r, query, stale))
        assertNull(SearchJump.step(r, SearchQuery("other"), active, 1, forward = true))
        assertSame(active, SearchJump.step(r, query, stale, 1, forward = true))
    }

    @Test fun noMatchesOrNoResultNeverNavigates() {
        val empty = results(listOf(emptyList(), emptyList()))
        assertNull(SearchJump.step(empty, query, null, 0, forward = true))
        assertNull(SearchJump.step(null, query, null, 0, forward = false))
    }
}
