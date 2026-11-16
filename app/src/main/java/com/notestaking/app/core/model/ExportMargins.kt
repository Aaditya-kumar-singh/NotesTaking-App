package com.notestaking.app.core.model

import com.notestaking.app.core.geometry.Rect

/**
 * Choose the extra paper outside a page's fixed content box for one export.
 * The existing page/doc resolver handles inheritance and out-of-range fractions.
 */
fun Page.insetsForExport(doc: Document, content: ExportContent = ExportContent()): PageInsets {
    if (!content.margins) return PageInsets.NONE
    val resolved = insets(doc)
    if (resolved.left.isFinite() && resolved.top.isFinite() &&
        resolved.right.isFinite() && resolved.bottom.isFinite()) return resolved

    // Damaged legacy settings can contain NaN; never hand invalid PDF dimensions to a renderer.
    fun safe(edge: Double) = edge.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    return PageInsets(safe(resolved.left), safe(resolved.top), safe(resolved.right), safe(resolved.bottom))
}

/** The PDF/PNG paper box in page-local coordinates; ink/text remain anchored at (0, 0). */
fun Page.footprintForExport(doc: Document, content: ExportContent = ExportContent()): Rect {
    val ins = insetsForExport(doc, content)
    return Rect(
        if (ins.left == 0.0) 0.0 else -ins.left,
        if (ins.top == 0.0) 0.0 else -ins.top,
        ins.left + width + ins.right,
        ins.top + height + ins.bottom,
    )
}
