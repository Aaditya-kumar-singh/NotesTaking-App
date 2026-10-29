package com.notestaking.app.platform

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Strict cap on untrusted image and clipboard bytes before allocating an in-memory copy.
 * Reads and enforces the *actual* byte count; provider-reported size can be missing or false.
 */
internal object BoundedBytes {
    const val MAX_IMAGE_BYTES = 32 * 1024 * 1024
    const val MAX_SVG_BYTES = 2 * 1024 * 1024

    @Throws(IOException::class)
    fun read(input: InputStream, maxBytes: Int = MAX_IMAGE_BYTES): ByteArray {
        require(maxBytes > 0)
        val out = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return out.toByteArray()
            if (n == 0) continue
            if (n > maxBytes - total) throw IOException("Imported item exceeds size limit")
            total += n
            out.write(buffer, 0, n)
        }
    }

    @Throws(IOException::class)
    fun readFile(file: File, maxBytes: Int = MAX_IMAGE_BYTES): ByteArray {
        if (!file.isFile || file.length() > maxBytes) throw IOException("Image exceeds size limit")
        return file.inputStream().use { read(it, maxBytes) }
    }

    fun utf8WithinLimit(text: String, maxBytes: Int = MAX_SVG_BYTES): ByteArray? {
        // UTF-8 is at least one byte for every UTF-16 code unit except surrogate pairs.
        if (text.length > maxBytes) return null
        val bytes = text.toByteArray(Charsets.UTF_8)
        return bytes.takeIf { it.size <= maxBytes }
    }
}
