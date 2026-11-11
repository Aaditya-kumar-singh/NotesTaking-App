package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

class ProviderInputTest {
    private class TrackedStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false
        override fun close() {
            closed = true
            super.close()
        }
    }

    @Test fun absentSourceCannotSilentlyCreateAnEmptyCopy() {
        var writerCalled = false
        assertThrows(FileNotFoundException::class.java) {
            ProviderInput.read(null) { writerCalled = true }
        }
        assertFalse(writerCalled)
    }

    @Test fun sourceIsCopiedAndClosedAfterSuccessfulRead() {
        val source = TrackedStream("native-note".toByteArray())
        val dest = ByteArrayOutputStream()
        ProviderInput.read(source) { it.copyTo(dest) }
        assertTrue(source.closed)
        assertArrayEquals("native-note".toByteArray(), dest.toByteArray())
    }

    @Test fun sourceIsClosedWhenDestinationWriteFails() {
        val source = TrackedStream("native-note".toByteArray())
        assertThrows(IOException::class.java) {
            ProviderInput.read(source) { it.copyTo(object : ByteArrayOutputStream() {
                override fun write(b: ByteArray, off: Int, len: Int) {
                    throw IOException("provider write failed")
                }
            }) }
        }
        assertTrue(source.closed)
    }
}
