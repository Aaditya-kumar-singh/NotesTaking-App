package com.notestaking.app.platform

import com.notestaking.app.core.model.CanvasItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.pdf.PageFrame
import com.notestaking.app.core.pdf.PdfNumbers
import com.notestaking.app.core.tools.Tool
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationInk
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/** Opt-in /Ink objects. Unsupported looks remain ordinary page content instead of pretending to be editable. */
internal object PdfInkAnnotations {
    private const val MAX_POINTS = 8192

    fun eligible(stroke: Stroke): Boolean {
        val config = stroke.config
        return stroke.tool == Tool.PEN && stroke.sampleCount in 2..MAX_POINTS &&
            !config.pressureEnabled && !config.taperEnabled && !config.neon &&
            config.directionStrength == 0.0 && config.speedStrength == 0.0 &&
            config.rgba.a == 255 && config.baseWidth.isFinite() && config.baseWidth in 0.2..128.0
    }

    /** shortcut: annotate trailing simple ink only; arbitrary overlapping z-order needs an explicit stacking policy. */
    fun trailing(items: List<CanvasItem>): List<Stroke> =
        items.asReversed().takeWhile { it is Stroke && eligible(it) }.asReversed().filterIsInstance<Stroke>()

    fun write(doc: PDDocument, page: PDPage, strokes: List<Stroke>, frame: PageFrame) {
        if (strokes.isEmpty()) return
        val annotations = page.annotations
        for (stroke in strokes) {
            val geometry = stroke.geometry()
            val count = geometry.pointCount
            if (count < 2 || count > MAX_POINTS) throw IOException("Invalid editable ink sample count")
            val points = FloatArray(2 * count)
            var left = Float.POSITIVE_INFINITY
            var bottom = Float.POSITIVE_INFINITY
            var right = Float.NEGATIVE_INFINITY
            var top = Float.NEGATIVE_INFINITY
            var valid = true
            for (i in 0 until count) {
                val p = frame.toUser(geometry.cx(i), geometry.cy(i))
                if (!p.x.isFinite() || !p.y.isFinite() ||
                    p.x !in -100000.0..100000.0 || p.y !in -100000.0..100000.0) {
                    valid = false
                    break
                }
                val x = p.x.toFloat()
                val y = p.y.toFloat()
                points[2 * i] = x
                points[2 * i + 1] = y
                left = min(left, x)
                bottom = min(bottom, y)
                right = max(right, x)
                top = max(top, y)
            }
            if (!valid) throw IOException("Invalid editable ink coordinates")
            val width = (stroke.config.baseWidth * frame.s).toFloat()
            if (!width.isFinite() || width <= 0f) throw IOException("Invalid editable ink width")
            val pad = max(width, 1f)
            val box = PDRectangle(left - pad, bottom - pad, max(1f, right - left + 2 * pad), max(1f, top - bottom + 2 * pad))
            val color = stroke.renderColor
            val annot = PDAnnotationInk().apply {
                rectangle = box
                inkList = arrayOf(points)
                this.color = PDColor(floatArrayOf(color.r / 255f, color.g / 255f, color.b / 255f), PDDeviceRGB.INSTANCE)
                borderStyle = PDBorderStyleDictionary().apply { this.width = width }
                isPrinted = true
                this.page = page
                contents = "Handwriting"
            }
            val ap = PDAppearanceStream(doc).apply {
                bBox = box
                resources = PDResources()
            }
            val path = StringBuilder(128 + count * 30)
            path.append("q 1 J 1 j ")
            PdfNumbers.append(path, width.toDouble())
            path.append(" w ")
            PdfNumbers.append(path, color.r / 255.0, 5)
            path.append(' ')
            PdfNumbers.append(path, color.g / 255.0, 5)
            path.append(' ')
            PdfNumbers.append(path, color.b / 255.0, 5)
            path.append(" RG ")
            for (i in 0 until count) {
                PdfNumbers.append(path, points[2 * i].toDouble())
                path.append(' ')
                PdfNumbers.append(path, points[2 * i + 1].toDouble())
                path.append(if (i == 0) " m " else " l ")
            }
            path.append("S Q\n")
            ap.contentStream.createOutputStream(COSName.FLATE_DECODE).use {
                it.write(path.toString().toByteArray(Charsets.US_ASCII))
            }
            annot.appearance = PDAppearanceDictionary().apply { setNormalAppearance(ap) }
            annotations.add(annot)
        }
        page.annotations = annotations
    }
}
