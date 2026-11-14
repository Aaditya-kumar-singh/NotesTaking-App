package com.notestaking.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Sticker tiles stay 72dp wide while the picker fits small Android windows. */
class StickerPickerLayoutTest {
    @Test
    fun normalWindowsKeepThreeColumns() {
        assertEquals(3, stickerGridColumns(320))
        assertEquals(3, stickerGridColumns(280))
        assertEquals(3, stickerGridColumns(600))
    }

    @Test
    fun narrowWindowsReduceColumnCountAtFitBoundaries() {
        assertEquals(2, stickerGridColumns(279))
        assertEquals(2, stickerGridColumns(240))
        assertEquals(2, stickerGridColumns(202))
        assertEquals(1, stickerGridColumns(201))
        assertEquals(1, stickerGridColumns(180))
    }

    @Test
    fun extremelyNarrowWindowsStillHaveOneColumn() {
        assertEquals(1, stickerGridColumns(124))
        assertEquals(1, stickerGridColumns(0))
    }
}
