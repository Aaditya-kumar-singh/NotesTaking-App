package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class BoundedCopyTest {
    @Test fun acceptsExactBudgetWithoutChangingBytes() {
        val data = ByteArray(128 * 1024) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()
        assertEquals(data.size.toLong(), BoundedCopy.copy(ByteArrayInputStream(data), output, data.size.toLong()))
        assertArrayEquals(data, output.toByteArray())
    }

    @Test fun oversizedProviderCannotWriteBeyondBudget() {
        val input = object : java.io.InputStream() {
            var remaining = 1_000_000_000
            override fun read(): Int = if (remaining-- > 0) 5 else -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val n = minOf(length, remaining)
                if (n <= 0) return -1
                java.util.Arrays.fill(buffer, offset, offset + n, 5.toByte())
                remaining -= n
                return n
            }
        }
        val output = ByteArrayOutputStream()
        assertThrows(IOException::class.java) { BoundedCopy.copy(input, output, 100_000L) }
        assertEquals(65_536, output.size())
    }

    @Test fun zeroByteStreamsCannotSpinForever() {
        val input = object : java.io.InputStream() {
            override fun read(): Int = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0
        }
        assertThrows(IOException::class.java) {
            BoundedCopy.copy(input, ByteArrayOutputStream(), 1024)
        }
    }

    @Test fun intermittentEmptyReadsAndEmptyFileRemainValid() {
        val output = ByteArrayOutputStream()
        assertEquals(0, BoundedCopy.copy(ByteArrayInputStream(byteArrayOf()), output, 1024))
        val input = object : java.io.InputStream() {
            var n = 0
            override fun read(): Int = -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                n++
                if (n <= 4) return 0
                if (n == 5) { buffer[offset] = 19; return 1 }
                return -1
            }
        }
        assertEquals(1, BoundedCopy.copy(input, output, 1024))
        assertArrayEquals(byteArrayOf(19), output.toByteArray())
    }

    @Test fun providerExceptionIsNotSwallowed() {
        val input = object : java.io.InputStream() {
            override fun read(): Int = throw IOException("provider disconnected")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw IOException("provider disconnected")
        }
        assertThrows(IOException::class.java) { BoundedCopy.copy(input, ByteArrayOutputStream(), 1024) }
    }
}
