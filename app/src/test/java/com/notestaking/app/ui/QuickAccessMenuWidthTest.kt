package com.notestaking.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Quick-access popups should fit Android multi-window without changing their normal widths. */
class QuickAccessMenuWidthTest {
    @Test
    fun normalWindowsPreserveBothMenuWidths() {
        assertEquals(224.dp, quickAccessMenuWidth(320, 224.dp))
        assertEquals(248.dp, quickAccessMenuWidth(320, 248.dp))
    }

    @Test
    fun narrowWindowsLeaveEightDpOnEachSide() {
        assertEquals(224.dp, quickAccessMenuWidth(240, 224.dp))
        assertEquals(224.dp, quickAccessMenuWidth(240, 248.dp))
        assertEquals(164.dp, quickAccessMenuWidth(180, 224.dp))
        assertEquals(164.dp, quickAccessMenuWidth(180, 248.dp))
    }

    @Test
    fun tinyWindowsNeverProduceANonpositiveWidth() {
        assertEquals(1.dp, quickAccessMenuWidth(16, 224.dp))
        assertEquals(1.dp, quickAccessMenuWidth(0, 248.dp))
    }
}
