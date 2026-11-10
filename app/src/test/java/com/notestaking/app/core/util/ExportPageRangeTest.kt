package com.notestaking.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportPageRangeTest {
    @Test fun parsesSinglePagesAndRangesInDocumentOrder() {
        assertEquals(listOf(0, 2, 3, 4), ExportPageRange.parse("1,3-5", 9))
        assertEquals(listOf(0, 1, 2), ExportPageRange.parse("3, 1 - 2, 2", 4))
        assertEquals(listOf(0), ExportPageRange.parse("01,1", 1))
        assertEquals(listOf(0, 1, 2, 3), ExportPageRange.parse("1-4", 4))
    }

    @Test fun rejectsMalformedAndOutOfBoundsWithoutPartialExports() {
        for (input in listOf("", " ", "0", "5", "2-1", "1,", ",2", "1,,3",
            "1-7", "4-5", "2--3", "-2", "abc", "2147483648", "1-2147483648")) {
            assertNull(input, ExportPageRange.parse(input, 4))
        }
        assertNull(ExportPageRange.parse("1", 0))
        assertNull(ExportPageRange.parse("1,".repeat(300), 4))
    }
}
