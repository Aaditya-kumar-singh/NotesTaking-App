package com.notestaking.app.platform

import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageStyle

/** A new sheet keeps a previous plain sheet's ruling, never its PDF or cover background. */
internal object InsertedNotePage {
    fun after(doc: Document, previous: Page?): Page {
        val (width, height) = doc.blankPageSize(previous)
        val style = previous?.takeIf { it.pdfPage == null && !it.cover }?.style ?: PageStyle()
        return Page(width, height, style = style)
    }
}
