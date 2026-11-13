package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

class ProviderCopyTest {
    @Test fun successfulCopyDoesNotDeleteDestination() {
        val bytes = "editable-note".toByteArray()
        val output = ByteArrayOutputStream()
        var removed = false

        val result = ProviderCopy.save(
            { ByteArrayInputStream(bytes) },
            { output },
            { removed = true; true },
        )

        assertEquals(ProviderCopy.Result.SAVED, result)
        assertArrayEquals(bytes, output.toByteArray())
        assertFalse(removed)
    }

    @Test fun missingSourceRemovesPickerCreatedDestinationWithoutOpeningIt() {
        var openedOutput = false
        var removed = false
        val result = ProviderCopy.save(
            { null },
            { openedOutput = true; ByteArrayOutputStream() },
            { removed = true; true },
        )

        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertFalse(openedOutput)
        assertTrue(removed)
    }

    @Test fun missingOutputClosesSourceAndRemovesDestination() {
        val source = object : ByteArrayInputStream("note".toByteArray()) {
            var closed = false
            override fun close() {
                closed = true
                super.close()
            }
        }
        val result = ProviderCopy.save({ source }, { null }, { true })

        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertTrue(source.closed)
    }

    @Test fun failedWriteRemovesPartiallyWrittenDestination() {
        val output = object : ByteArrayOutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) {
                super.write(b, off, len)
                throw IOException("storage exhausted")
            }
        }
        var removed = false
        val result = ProviderCopy.save(
            { ByteArrayInputStream("note content".toByteArray()) },
            { output },
            { removed = true; true },
        )

        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertTrue(output.size() > 0)
        assertTrue(removed)
    }

    @Test fun unsuccessfulDeletionIsReportedAsPossibleLeftover() {
        val result = ProviderCopy.save(
            { ByteArrayInputStream("note".toByteArray()) },
            { null },
            { false },
        )
        assertEquals(ProviderCopy.Result.FAILED_LEFTOVER, result)
    }

    @Test fun deletionExceptionIsReportedAsPossibleLeftover() {
        val result = ProviderCopy.save(
            { null },
            { ByteArrayOutputStream() },
            { throw SecurityException("provider refused delete") },
        )
        assertEquals(ProviderCopy.Result.FAILED_LEFTOVER, result)
    }

    @Test fun emptyNativeSourceCannotBeClaimedAsSaved() {
        val result = ProviderCopy.save(
            { ByteArrayInputStream(byteArrayOf()) },
            { ByteArrayOutputStream() },
            { true },
        )
        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
    }

    @Test fun cancellationCleansDestinationButIsNotSwallowed() {
        var removed = false
        assertThrows(CancellationException::class.java) {
            ProviderCopy.save(
                { throw CancellationException("stop") },
                { ByteArrayOutputStream() },
                { removed = true; true },
            )
        }
        assertTrue(removed)
    }

    @Test fun pdfDestinationCloseFailureIsNotReportedAsSaved() {
        val output = object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                throw IOException("provider could not finish the PDF")
            }
        }
        var removed = false
        val result = ProviderCopy.save(
            { ByteArrayInputStream("%PDF-1.7 test document".toByteArray()) },
            { output },
            { removed = true; true },
        )
        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertTrue(output.size() > 0)
        assertTrue(removed)
    }

    @Test fun stalledProviderReadFailsAndCleansIncompleteDestination() {
        val source = object : java.io.InputStream() {
            var closed = false
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int = 0
            override fun close() { closed = true }
        }
        var removed = false
        val result = ProviderCopy.save(
            { source },
            { ByteArrayOutputStream() },
            { removed = true; true },
        )
        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertTrue(removed)
        assertTrue(source.closed)
    }

    @Test fun intermittentEmptyReadsStillSaveExactProviderBytes() {
        val payload = ByteArray(128 * 1024) { (it % 251).toByte() }
        val input = object : ByteArrayInputStream(payload) {
            var emptyReads = 4
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (emptyReads-- > 0) return 0
                return super.read(b, off, len)
            }
        }
        val output = ByteArrayOutputStream()
        var removed = false
        val result = ProviderCopy.save({ input }, { output }, { removed = true; true })
        assertEquals(ProviderCopy.Result.SAVED, result)
        assertArrayEquals(payload, output.toByteArray())
        assertFalse(removed)
    }

    @Test fun interruptedPdfSourceReadRemovesIncompleteDestination() {
        val source = object : ByteArrayInputStream("%PDF-1.7 test document".toByteArray()) {
            var readCalls = 0
            var closed = false

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (readCalls++ > 0) throw IOException("source read interrupted")
                return super.read(b, off, 1)
            }

            override fun close() {
                closed = true
                super.close()
            }
        }
        val output = ByteArrayOutputStream()
        var removed = false
        val result = ProviderCopy.save(
            { source },
            { output },
            { removed = true; true },
        )
        assertEquals(ProviderCopy.Result.FAILED_REMOVED, result)
        assertTrue(source.closed)
        assertTrue(output.size() > 0)
        assertTrue(removed)
    }
}
