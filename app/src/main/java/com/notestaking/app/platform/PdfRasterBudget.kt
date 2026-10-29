package com.notestaking.app.platform

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Keep filtered PDF rasterization bounded: one page can hold both raw and filtered ARGB bitmaps.
 * Eight megapixels caps their combined pixel buffers at 64 MiB before PDFium/native overhead.
 */
internal object PdfRasterBudget {
    const val MAX_DIM = 4096
    const val MAX_PIXELS = 8 * 1024 * 1024

    /** Page previews may scale uniformly; cropped viewport tiles must retain exact pixel geometry. */
    fun pageSize(requestedWidth: Int, requestedHeight: Int): Pair<Int, Int> {
        val width = requestedWidth.coerceAtLeast(1)
        val height = requestedHeight.coerceAtLeast(1)
        val pixels = width.toDouble() * height
        val factor = min(
            1.0,
            min(
                min(MAX_DIM.toDouble() / width, MAX_DIM.toDouble() / height),
                sqrt(MAX_PIXELS.toDouble() / pixels),
            ),
        )
        return (width * factor).toInt().coerceIn(1, MAX_DIM) to
            (height * factor).toInt().coerceIn(1, MAX_DIM)
    }

    fun allowsRegion(width: Int, height: Int): Boolean =
        width in 1..MAX_DIM && height in 1..MAX_DIM &&
            width.toLong() * height.toLong() <= MAX_PIXELS
}
