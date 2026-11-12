package com.notestaking.app.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

class ProviderImageBatchTest {
    @Test fun onlyWrittenPagesCountAsSaved() {
        val created = mutableListOf<Int>()
        val output = mutableMapOf<Int, ByteArrayOutputStream>()
        val result = ProviderImageBatch.save(
            listOf(0, 1, 2),
            { page -> if (page == 1) null else byteArrayOf(0x12, page.toByte()) },
            { page ->
                created += page
                ProviderImageBatch.Destination(
                    { ByteArrayOutputStream().also { output[page] = it } },
                    { false },
                )
            },
        )

        assertEquals(2, result.saved)
        assertFalse(result.leftovers)
        assertEquals(listOf(0, 2), created)
        assertArrayEquals(byteArrayOf(0x12, 0), output[0]!!.toByteArray())
        assertArrayEquals(byteArrayOf(0x12, 2), output[2]!!.toByteArray())
    }

    @Test fun missingOutputStreamIsNotCountedAndPartialFileIsRemoved() {
        val removed = mutableListOf<Int>()
        val result = ProviderImageBatch.save(
            listOf(1, 2),
            { byteArrayOf(1, 2, 3) },
            { page ->
                ProviderImageBatch.Destination(
                    { if (page == 1) null else ByteArrayOutputStream() },
                    { removed += page; true },
                )
            },
        )

        assertEquals(1, result.saved)
        assertFalse(result.leftovers)
        assertEquals(listOf(1), removed)
    }

    @Test fun failedWriteAndFailedCleanupAreReported() {
        val result = ProviderImageBatch.save(
            listOf(0, 1),
            { byteArrayOf(1, 2, 3) },
            { page ->
                ProviderImageBatch.Destination(
                    {
                        if (page == 0) object : ByteArrayOutputStream() {
                            override fun write(b: ByteArray, off: Int, len: Int) {
                                super.write(b, off, len)
                                throw IOException("provider failed")
                            }
                        } else ByteArrayOutputStream()
                    },
                    { false },
                )
            },
        )

        assertEquals(1, result.saved)
        assertTrue(result.leftovers)
    }

    @Test fun rendererOrCreateFailureDoesNotStopOtherPages() {
        val created = mutableListOf<Int>()
        val result = ProviderImageBatch.save(
            listOf(0, 1, 2, 3),
            { page -> if (page == 0) throw IOException("render failed") else byteArrayOf(5) },
            { page ->
                created += page
                if (page == 1) throw SecurityException("provider denied create")
                ProviderImageBatch.Destination({ ByteArrayOutputStream() }, { true })
            },
        )

        assertEquals(2, result.saved)
        assertFalse(result.leftovers)
        assertEquals(listOf(1, 2, 3), created)
    }

    @Test fun emptyPngDoesNotCreateDestination() {
        var created = false
        val result = ProviderImageBatch.save(
            listOf(0),
            { byteArrayOf() },
            {
                created = true
                ProviderImageBatch.Destination({ ByteArrayOutputStream() }, { true })
            },
        )

        assertEquals(0, result.saved)
        assertFalse(created)
    }

    @Test fun cancellationIsNotSwallowed() {
        assertThrows(CancellationException::class.java) {
            ProviderImageBatch.save(
                listOf(0, 1),
                { throw CancellationException("cancelled") },
                { ProviderImageBatch.Destination({ ByteArrayOutputStream() }, { true }) },
            )
        }
    }
}
