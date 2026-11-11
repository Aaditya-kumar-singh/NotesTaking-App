package com.notestaking.app.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

class ProviderOutputTest {
    private class TrackedStream : ByteArrayOutputStream() {
        var closed = false
        override fun close() {
            closed = true
            super.close()
        }
    }

    @Test fun nullOutputStreamIsAnErrorAndDoesNotRunWriter() {
        var writerCalled = false
        assertThrows(IOException::class.java) {
            ProviderOutput.write(null) { writerCalled = true }
        }
        assertFalse(writerCalled)
    }

    @Test fun successfulProviderWriteFlushesAndClosesStream() {
        val stream = TrackedStream()
        ProviderOutput.write(stream) { it.write("good note".toByteArray()) }
        assertTrue(stream.closed)
        assertEquals("good note", stream.toString(Charsets.UTF_8.name()))
    }

    @Test fun failedProviderWriteClosesStreamAndPropagatesFailure() {
        val stream = TrackedStream()
        assertThrows(IOException::class.java) {
            ProviderOutput.write(stream) {
                it.write("partial".toByteArray())
                throw IOException("simulated provider failure")
            }
        }
        assertTrue(stream.closed)
    }
}
