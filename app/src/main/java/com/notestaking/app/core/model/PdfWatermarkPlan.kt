package com.notestaking.app.core.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A render-only request: text is handed to a text painter, never concatenated into PDF operators.
 * Image keys are application-owned opaque identifiers, not URIs or filesystem paths.
 */
sealed class PdfWatermark {
    data class Text(val value: String) : PdfWatermark()
    data class Image(val assetKey: String, val aspectRatio: Double) : PdfWatermark()
}

enum class WatermarkAnchor { CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** All coordinates are in the selected output page's displayed top-left-origin point space. */
data class PdfWatermarkPage(
    val sourceIndex: Int,
    val pageUid: Long,
    val outputIndex: Int,
    val centerX: Double,
    val centerY: Double,
    val width: Double,
    val height: Double,
    val rotationDegrees: Double,
    val opacity: Double,
    val content: PdfWatermark,
)

/** PDF export layout only; never opens images, emits PDF commands, or modifies a note. */
object PdfWatermarkPlanner {
    const val MAX_PAGES = 500
    const val MAX_TEXT_LENGTH = 120
    const val DEFAULT_OPACITY = 0.18

    fun plan(
        doc: Document,
        indices: List<Int> = doc.pages.indices.toList(),
        watermark: PdfWatermark,
        anchor: WatermarkAnchor = WatermarkAnchor.CENTER,
        rotationDegrees: Double = -30.0,
        opacity: Double = DEFAULT_OPACITY,
        content: ExportContent = ExportContent(),
    ): List<PdfWatermarkPage> {
        require(indices.isNotEmpty() && indices.size <= MAX_PAGES) { "Watermark selection must have 1..$MAX_PAGES pages" }
        require(indices.all { it in doc.pages.indices }) { "Watermark page index out of range" }
        require(rotationDegrees.isFinite() && rotationDegrees in -180.0..180.0) { "Invalid watermark rotation" }
        require(opacity.isFinite() && opacity > 0.0 && opacity <= 1.0) { "Invalid watermark opacity" }

        val safeContent = when (watermark) {
            is PdfWatermark.Text -> {
                val value = watermark.value.trim()
                require(value.isNotEmpty() && value.length <= MAX_TEXT_LENGTH &&
                    value.none { Character.isISOControl(it) || it.code in 0x202A..0x202E ||
                        it.code in 0x2066..0x2069 } &&
                    value.toCharArray().let { chars ->
                        chars.indices.all { i ->
                            !Character.isHighSurrogate(chars[i]) || (i + 1 < chars.size && Character.isLowSurrogate(chars[i + 1]))
                        } && chars.indices.all { i ->
                            !Character.isLowSurrogate(chars[i]) || (i > 0 && Character.isHighSurrogate(chars[i - 1]))
                        }
                    }
                ) { "Watermark text must be printable and at most $MAX_TEXT_LENGTH characters" }
                PdfWatermark.Text(value)
            }
            is PdfWatermark.Image -> {
                require(Regex("[A-Za-z0-9_-]{1,64}").matches(watermark.assetKey)) {
                    "Image watermark requires an application-owned asset key, not a path or URL"
                }
                require(watermark.aspectRatio.isFinite() && watermark.aspectRatio in 0.05..20.0) {
                    "Invalid image watermark aspect ratio"
                }
                watermark
            }
        }
        val radians = Math.toRadians(rotationDegrees)
        val c = abs(cos(radians))
        val s = abs(sin(radians))
        return indices.mapIndexed { outputIndex, sourceIndex ->
            val page = doc.pages[sourceIndex]
            require(page.width.isFinite() && page.height.isFinite() &&
                page.width > 0.0 && page.height > 0.0) { "Invalid watermark page dimensions" }
            val frame = page.footprintForExport(doc, content)
            val pw = frame.w
            val ph = frame.h
            require(pw.isFinite() && ph.isFinite() && pw > 0.0 && ph > 0.0 &&
                pw <= 20_000.0 && ph <= 20_000.0) {
                "Invalid watermark export footprint"
            }
            val desiredW: Double
            val desiredH: Double
            when (safeContent) {
                is PdfWatermark.Text -> {
                    // Approximate a single line; the actual renderer must measure its selected font.
                    desiredH = minOf(ph * 0.07, 48.0)
                    desiredW = desiredH * 0.60 * safeContent.value.length
                }
                is PdfWatermark.Image -> {
                    desiredW = pw * 0.45
                    desiredH = desiredW / safeContent.aspectRatio
                }
            }
            val bboxW = c * desiredW + s * desiredH
            val bboxH = s * desiredW + c * desiredH
            require(bboxW.isFinite() && bboxH.isFinite() && bboxW > 0.0 && bboxH > 0.0) {
                "Unrepresentable watermark dimensions"
            }
            val scale = minOf(1.0, pw * 0.9 / bboxW, ph * 0.9 / bboxH)
            require(scale.isFinite() && scale > 0.0) { "Unrepresentable watermark scale" }
            val w = desiredW * scale
            val h = desiredH * scale
            val rotatedW = bboxW * scale
            val rotatedH = bboxH * scale
            val x = when (anchor) {
                WatermarkAnchor.TOP_LEFT, WatermarkAnchor.BOTTOM_LEFT -> pw * 0.05 + rotatedW / 2
                WatermarkAnchor.TOP_RIGHT, WatermarkAnchor.BOTTOM_RIGHT -> pw * 0.95 - rotatedW / 2
                WatermarkAnchor.CENTER -> pw / 2
            }
            val y = when (anchor) {
                WatermarkAnchor.TOP_LEFT, WatermarkAnchor.TOP_RIGHT -> ph * 0.05 + rotatedH / 2
                WatermarkAnchor.BOTTOM_LEFT, WatermarkAnchor.BOTTOM_RIGHT -> ph * 0.95 - rotatedH / 2
                WatermarkAnchor.CENTER -> ph / 2
            }
            require(listOf(x, y, w, h).all { it.isFinite() && it > 0.0 }) {
                "Unrepresentable watermark placement"
            }
            PdfWatermarkPage(sourceIndex, page.uid, outputIndex, x, y, w, h,
                rotationDegrees, opacity, safeContent)
        }
    }
}
