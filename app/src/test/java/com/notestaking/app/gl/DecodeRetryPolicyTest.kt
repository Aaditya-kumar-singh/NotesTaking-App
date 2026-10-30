package com.notestaking.app.gl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeRetryPolicyTest {
    @Test
    fun failedDecodeIsThrottledThenCanRecover() {
        val failedAt = 1_000_000L
        assertTrue(DecodeRetryPolicy.mayRetry(null, failedAt))
        assertFalse(DecodeRetryPolicy.mayRetry(failedAt, failedAt))
        assertFalse(DecodeRetryPolicy.mayRetry(failedAt, failedAt + DecodeRetryPolicy.RETRY_DELAY_NS - 1))
        assertTrue(DecodeRetryPolicy.mayRetry(failedAt, failedAt + DecodeRetryPolicy.RETRY_DELAY_NS))
    }
}
