package com.notestaking.app.platform

import com.notestaking.app.core.model.Orientation
import com.notestaking.app.core.model.PageSize
import com.notestaking.app.core.model.PageSizeChoice
import kotlin.math.abs

/** Validates a requested paper size before a malformed value can reach a saved note. */
internal object PageSizeSafety {
    fun validMm(widthMm: Double, heightMm: Double): Boolean =
        widthMm.isFinite() && heightMm.isFinite() &&
            widthMm in PageSize.CUSTOM_MIN_MM..PageSize.CUSTOM_MAX_MM &&
            heightMm in PageSize.CUSTOM_MIN_MM..PageSize.CUSTOM_MAX_MM

    fun valid(choice: PageSizeChoice): Boolean =
        choice.size != PageSize.CUSTOM || validMm(choice.customWidthMm, choice.customHeightMm)

    /** Preserve exact custom millimetres even when they are close to a named paper size. */
    fun forNewNote(widthMm: Double, heightMm: Double, dpi: Int): PageSizeChoice? {
        if (dpi <= 0 || !validMm(widthMm, heightMm)) return null
        val w = PageSize.mmToPx(widthMm, dpi)
        val h = PageSize.mmToPx(heightMm, dpi)
        val named = PageSizeChoice.of(w, h, dpi)
        val (namedW, namedH) = named.pixels(dpi)
        return if (named.size != PageSize.CUSTOM &&
            abs(namedW - w) < 1e-6 && abs(namedH - h) < 1e-6
        ) {
            named
        } else {
            PageSizeChoice(
                PageSize.CUSTOM,
                if (widthMm > heightMm) Orientation.LANDSCAPE else Orientation.PORTRAIT,
                widthMm, heightMm,
            )
        }
    }
}
