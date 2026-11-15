package com.notestaking.app.platform

import com.notestaking.app.core.model.Bookmark
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page

/** Resolve navigation against the current page list, not positions cached before a reorder. */
internal object PageNavigation {
    fun bookmarkTarget(document: Document, bookmark: Bookmark): Int? =
        bookmark.page.takeIf { index ->
            index in document.pages.indices && document.bookmarks.any { it === bookmark }
        }

    /** Source PDF page numbers need not match the note's order after inserting or moving pages. */
    fun outlineTarget(pages: List<Page>, sourcePage: Int): Int? {
        if (sourcePage < 0) return null
        var preceding = -1
        var target: Int? = null
        for ((index, page) in pages.withIndex()) {
            val pdfPage = page.pdfPage ?: continue
            if (pdfPage == sourcePage) return index
            if (pdfPage > preceding && pdfPage in 0..sourcePage) {
                preceding = pdfPage
                target = index
            }
        }
        return target
    }
}
