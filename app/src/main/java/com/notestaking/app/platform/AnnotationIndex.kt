package com.notestaking.app.platform

import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.TextMarkup

/** A live, read-only index of text markups. Page identity survives reorder and invalidates stale taps. */
internal object AnnotationIndex {
    data class Entry(val page: Page, val index: Int, val markup: TextMarkup)

    fun list(pages: List<Page>, onlyPage: Int? = null): List<Entry> {
        if (onlyPage != null && onlyPage !in pages.indices) return emptyList()
        return pages.flatMapIndexed { index, page ->
            if (onlyPage != null && index != onlyPage) emptyList()
            else page.markups.map { Entry(page, index, it) }
        }
    }

    fun resolve(pages: List<Page>, entry: Entry): Int? {
        val index = pages.indexOfFirst { it === entry.page }
        return index.takeIf { it >= 0 && pages[it].markups.any { m -> m === entry.markup } }
    }
}
