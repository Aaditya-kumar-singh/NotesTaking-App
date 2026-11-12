package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class PdfImportBatchTest {
    @Test fun allFilesImported() {
        val progress = mutableListOf<Int>()
        val result = PdfImportBatch.run(3, { false }, { true }, { progress += it })
        assertEquals(3, result.imported)
        assertEquals(3, result.processed)
        assertEquals(3, result.total)
        assertFalse(result.cancelled)
        assertEquals(listOf(1, 2, 3), progress)
    }

    @Test fun skippedAndBrokenSourcesAdvanceProgressWithoutFalseSuccess() {
        val visited = mutableListOf<Int>()
        val progress = mutableListOf<Int>()
        val result = PdfImportBatch.run(4, { false }, { index ->
            visited += index
            when (index) {
                0 -> true
                1 -> false
                2 -> throw IOException("provider disconnected")
                else -> true
            }
        }, { progress += it })
        assertEquals(2, result.imported)
        assertEquals(4, result.processed)
        assertFalse(result.cancelled)
        assertEquals(listOf(0, 1, 2, 3), visited)
        assertEquals(listOf(1, 2, 3, 4), progress)
    }

    @Test fun totalFailureIsNotReportedAsAnEmptySuccess() {
        val result = PdfImportBatch.run(2, { false }, { false }, {})
        assertEquals(0, result.imported)
        assertEquals(2, result.processed)
        assertFalse(result.cancelled)
    }

    @Test fun cancellationStopsBeforeNextSourceAndPreservesCompletedCount() {
        var cancelled = false
        val visited = mutableListOf<Int>()
        val result = PdfImportBatch.run(4, { cancelled }, { index ->
            visited += index
            if (index == 1) cancelled = true
            true
        }, {})
        assertEquals(2, result.imported)
        assertEquals(2, result.processed)
        assertTrue(result.cancelled)
        assertEquals(listOf(0, 1), visited)
    }

    @Test fun cancellationBeforeFirstSourceDoesNotReadAnything() {
        val result = PdfImportBatch.run(3, { true }, { throw AssertionError("must not import") }, {})
        assertEquals(0, result.imported)
        assertEquals(0, result.processed)
        assertTrue(result.cancelled)
    }

    @Test fun coroutineCancellationIsNotSwallowed() {
        assertThrows(CancellationException::class.java) {
            PdfImportBatch.run(3, { false }, { throw CancellationException("job cancelled") }, {})
        }
    }
}
