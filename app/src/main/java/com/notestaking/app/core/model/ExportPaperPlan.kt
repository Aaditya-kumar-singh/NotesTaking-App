package com.notestaking.app.core.model

/**
 * Instructions for an exporter, never a change to the page.
 *
 * [paintBlankPaper] applies only when drawing a new blank PDF/PNG page; source-PDF art is
 * handled by the PDF exporter and cannot be removed by this background policy.
 * A null [paperColor] with [paintBlankPaper] true means the caller's usual theme fallback.
 */
data class ExportPaperPlan(
    val paintBlankPaper: Boolean,
    val paperColor: Rgba?,
    val template: String,
    val linkedSourcePdfPage: Boolean,
) {
    val paintRuling: Boolean get() = template != PageTemplates.NONE
}

/** Resolve inherited page/document styles for a one-time export, without touching saved styles. */
fun Page.paperPlanForExport(
    doc: Document,
    content: ExportContent = ExportContent(),
    globalPaperColor: Rgba? = null,
): ExportPaperPlan = ExportPaperPlan(
    paintBlankPaper = content.paper,
    paperColor = if (content.paper) resolvedPageColor(doc, globalPaperColor) else null,
    template = if (content.paper) resolvedTemplate(doc) else PageTemplates.NONE,
    linkedSourcePdfPage = pdfPage != null,
)
