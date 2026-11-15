package com.notestaking.app.platform

import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page

/** The selected note pages and PDF orientations while text is loaded asynchronously. */
internal class PdfMarkupTargets private constructor(
    private val document: Document,
    private val targets: List<Target>,
) {
    private class Target(
        val index: Int, val page: Page, val pdfKey: Int?,
        val width: Double, val height: Double,
    )

    fun stillValid(current: Document): Boolean =
        current === document && targets.all { target ->
            current.pages.getOrNull(target.index) === target.page &&
                target.page.pdfKey == target.pdfKey &&
                target.page.width == target.width && target.page.height == target.height
        }

    companion object {
        fun capture(doc: Document, start: Int, end: Int): PdfMarkupTargets? {
            if (start !in doc.pages.indices || end !in doc.pages.indices || end < start) return null
            val targets = (start..end).map { index ->
                val page = doc.pages[index]
                Target(index, page, page.pdfKey, page.width, page.height)
            }
            if (targets.none { it.pdfKey != null }) return null
            return PdfMarkupTargets(doc, targets)
        }
    }
}
