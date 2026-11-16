package com.notestaking.app.core.model

import com.notestaking.app.core.pdf.Outline
import java.util.IdentityHashMap

/** Immutable export destination; [page] is an index in the selected output pages, not the note. */
data class ExportBookmark(val page: Int, val label: String)

/**
 * One export's read-only annotation plan. Each markup list corresponds to the same position in the
 * selected output pages; source PDF page numbers are remapped to their first output occurrence.
 */
data class ExportMetadataPlan(
    val bookmarks: List<ExportBookmark>,
    val markups: List<List<TextMarkup>>,
    val sourcePageMap: Map<Int, Int>,
)

/**
 * Resolve note-local destinations through page identity, so a subset or reordered export never
 * points a bookmark at a different page. The renderer handles original-PDF actions separately.
 */
fun Document.metadataForExport(
    content: ExportContent = ExportContent(),
    exportPages: List<Page> = pages,
    sourcePdfPageCount: Int? = null,
): ExportMetadataPlan {
    val pageIndex = IdentityHashMap<Page, Int>(exportPages.size)
    exportPages.forEachIndexed { index, page -> if (!pageIndex.containsKey(page)) pageIndex[page] = index }
    val bookmarks = if (content.bookmarks) this.bookmarks.mapNotNull { mark ->
        val page = pages.getOrNull(mark.page) ?: return@mapNotNull null
        pageIndex[page]?.let { ExportBookmark(it, mark.label) }
    } else emptyList()
    val sourceMap = if (content.bookmarks) {
        Outline.pageMap(exportPages.map { page ->
            page.pdfPage?.takeIf { it >= 0 && (sourcePdfPageCount == null || it < sourcePdfPageCount) }
        })
    } else emptyMap()
    return ExportMetadataPlan(
        bookmarks = bookmarks,
        markups = exportPages.map { if (content.markups) it.markups else emptyList() },
        sourcePageMap = sourceMap,
    )
}
