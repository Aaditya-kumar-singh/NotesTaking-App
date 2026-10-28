package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

class BoundedBytesTest {
    @Test fun acceptsEmptyAndExactLimitWithoutTruncation() {
        assertArrayEquals(byteArrayOf(), BoundedBytes.read(ByteArrayInputStream(byteArrayOf()), 5))
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        assertArrayEquals(payload, BoundedBytes.read(ByteArrayInputStream(payload), 5))
    }

    @Test fun refusesProviderBytesBeyondLimitEvenWithoutKnownLength() {
        assertThrows(IOException::class.java) {
            BoundedBytes.read(ByteArrayInputStream(ByteArray(101)), 100)
        }
    }

    @Test fun refusesLargeRepeatedStreamWithoutAnUnboundedAllocation() {
        val repeated = object : java.io.InputStream() {
            var remaining = 1024L * 1024L * 1024L
            override fun read(): Int = if (remaining-- > 0) 42 else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = minOf(remaining, len.toLong()).toInt()
                if (n <= 0) return -1
                java.util.Arrays.fill(b, off, off + n, 42.toByte())
                remaining -= n
                return n
            }
        }
        assertThrows(IOException::class.java) { BoundedBytes.read(repeated, 64 * 1024) }
    }

    @Test fun fileReadRefusesOversizeBeforeLoading() {
        val file = Files.createTempFile("notes-img-limit", ".png").toFile()
        try {
            file.writeBytes(ByteArray(4097))
            assertThrows(IOException::class.java) { BoundedBytes.readFile(file, 4096) }
            assertEquals(4097, file.length().toInt())
        } finally {
            file.delete()
        }
    }

    @Test fun svgUtf8LimitIncludesMultibyteCharacters() {
        assertArrayEquals("ink".toByteArray(), BoundedBytes.utf8WithinLimit("ink", 3))
        assertNull(BoundedBytes.utf8WithinLimit("éé", 3))
        assertTrue(BoundedBytes.utf8WithinLimit("éé", 4)!!.size == 4)
        assertNull(BoundedBytes.utf8WithinLimit("anything", 3))
    }
}
