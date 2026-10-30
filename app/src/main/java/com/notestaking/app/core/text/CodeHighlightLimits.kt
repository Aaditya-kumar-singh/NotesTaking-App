package com.notestaking.app.core.text

/** Matches the JNI parser's input and capture budgets; exceeding them disables highlighting only. */
internal object CodeHighlightLimits {
    const val MAX_INPUT_BYTES = 1024 * 1024
    const val MAX_CAPTURE_TRIPLES = 65_536

    fun acceptsSourceSize(bytes: Int): Boolean = bytes in 1..MAX_INPUT_BYTES

    fun acceptsCaptureArrayLength(length: Int): Boolean =
        length >= 0 && length % 3 == 0 && length <= MAX_CAPTURE_TRIPLES * 3
}
