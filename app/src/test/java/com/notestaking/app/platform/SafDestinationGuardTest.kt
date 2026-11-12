package com.notestaking.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SafDestinationGuardTest {
    @Test fun reportedEmptyDestinationIsWritableWithoutOpeningInput() {
        assertTrue(SafDestinationGuard.canWrite(0L) {
            throw AssertionError("Zero-size destination does not need a second read")
        })
    }

    @Test fun existingDataIsNeverWritableOrProbed() {
        assertFalse(SafDestinationGuard.canWrite(12L) {
            throw AssertionError("Existing bytes must remain untouched")
        })
    }

    @Test fun unknownSizeButEmptyStreamIsWritable() {
        assertTrue(SafDestinationGuard.canWrite(null) { -1 })
        assertTrue(SafDestinationGuard.canWrite(-1L) { -1 })
    }

    @Test fun unknownSizeAndNonemptyStreamAreBlocked() {
        assertFalse(SafDestinationGuard.canWrite(null) { 37 })
        assertFalse(SafDestinationGuard.canWrite(-1L) { 0 })
    }

    @Test fun unreadableOrMissingDestinationIsBlocked() {
        assertFalse(SafDestinationGuard.canWrite(null) { null })
        assertFalse(SafDestinationGuard.canWrite(null) { throw IOException("provider unavailable") })
        assertFalse(SafDestinationGuard.canWrite(null) { throw SecurityException("permission denied") })
    }
}
