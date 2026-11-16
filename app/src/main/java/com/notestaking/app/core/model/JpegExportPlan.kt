package com.notestaking.app.core.model

import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.util.NameTemplate
import kotlin.math.floor
import kotlin.math.sqrt

/** One planned image; [pageUid] lets the caller detect an intervening reorder/replacement. */
data class JpegExportPage(
    val sourceIndex: Int,
    val pageUid: Long,
    val pdfKey: Int?,
    val outputIndex: Int,
    val filename: String,
    val footprint: Rect,
    val widthPx: Int,
    val heightPx: Int,
    val paper: ExportPaperPlan,
)

data class JpegExportPlan(
    val pages: List<JpegExportPage>,
    val quality: Int,
    val content: ExportContent,
) {
    val mimeType: String get() = "image/jpeg"
}

/** JPEG page export preparation only; Android rendering and file creation belong to the UI layer. */
object JpegExportPlanner {
    const val DEFAULT_QUALITY = 92
    const val MAX_PAGES = 500
    const val MAX_SIDE_PX = 4096
    const val MAX_PIXELS = 8 * 1024 * 1024L

    /**
     * [indices] are zero-based note-page indices in requested output order. Repeated indices are
     * deliberate separate outputs, with distinct names; invalid selections never partially export.
     */
    fun plan(
        doc: Document,
        indices: List<Int> = doc.pages.indices.toList(),
        stem: String = doc.title,
        scale: Double = 1.0,
        quality: Int = DEFAULT_QUALITY,
        content: ExportContent = ExportContent(),
        globalPaperColor: Rgba? = null,
    ): JpegExportPlan {
        require(indices.isNotEmpty() && indices.size <= MAX_PAGES) { "JPEG export requires 1..$MAX_PAGES pages" }
        require(quality in 1..100) { "JPEG quality must be 1..100" }
        require(scale.isFinite() && scale > 0.0) { "JPEG scale must be finite and positive" }
        require(indices.all { it in doc.pages.indices }) { "JPEG export page index out of bounds" }

        val safeStem = NameTemplate.sanitize(stem).take(48).trimEnd(' ', '.').ifBlank { "untitled" }
        val pages = indices.mapIndexed { ordinal, sourceIndex ->
            val page = doc.pages[sourceIndex]
            require(page.width.isFinite() && page.height.isFinite() &&
                page.width > 0.0 && page.height > 0.0) { "Invalid JPEG page dimensions" }
            val cover = page.footprintForExport(doc, content)
            val area = cover.w * cover.h
            require(cover.w.isFinite() && cover.h.isFinite() && area.isFinite() &&
                cover.w > 0.0 && cover.h > 0.0 && area > 0.0) { "Invalid JPEG export footprint" }

            val factor = minOf(
                scale,
                MAX_SIDE_PX / cover.w,
                MAX_SIDE_PX / cover.h,
                sqrt(MAX_PIXELS.toDouble() / area),
            )
            val width = floor(cover.w * factor).toInt().coerceIn(1, MAX_SIDE_PX)
            val height = floor(cover.h * factor).toInt().coerceIn(1, MAX_SIDE_PX)
            JpegExportPage(
                sourceIndex = sourceIndex,
                pageUid = page.uid,
                pdfKey = page.pdfKey,
                outputIndex = ordinal,
                filename = "$safeStem-p${(sourceIndex + 1).toString().padStart(4, '0')}-o${(ordinal + 1).toString().padStart(4, '0')}.jpg",
                footprint = cover,
                widthPx = width,
                heightPx = height,
                paper = page.paperPlanForExport(doc, content, globalPaperColor),
            )
        }
        return JpegExportPlan(pages, quality, content)
    }
}
