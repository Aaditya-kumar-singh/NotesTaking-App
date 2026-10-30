package com.notestaking.app.gl

/** Throttle an unreadable image without permanently disabling it after a transient I/O failure. */
internal object DecodeRetryPolicy {
    const val RETRY_DELAY_NS = 2_000_000_000L

    fun mayRetry(failedAtNs: Long?, nowNs: Long): Boolean =
        failedAtNs == null || nowNs - failedAtNs >= RETRY_DELAY_NS
}
