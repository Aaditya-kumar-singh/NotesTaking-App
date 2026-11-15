package com.notestaking.app.platform

import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

/** A successful render/write isn't a successful PDF until the bytes are complete and readable back. */
internal object PdfSaveProof {
    private const val TAIL_BYTES = 2048
    private val header = "%PDF-".toByteArray(Charsets.US_ASCII)

    /** Reject empty/truncated output before opening a system Save picker or showing success. */
    fun rendered(file: File): Boolean = runCatching {
        if (!file.isFile || file.length() < 16L) return false
        RandomAccessFile(file, "r").use { input ->
            val prefix = ByteArray(header.size)
            input.readFully(prefix)
            if (!prefix.contentEquals(header)) return false
            val size = minOf(input.length(), TAIL_BYTES.toLong()).toInt()
            val tail = ByteArray(size)
            input.seek(input.length() - size)
            input.readFully(tail)
            String(tail, Charsets.ISO_8859_1).trimEnd().endsWith("%%EOF")
        }
    }.getOrDefault(false)

    /** Fail closed when a SAF provider accepts a close but stores different, missing or unreadable bytes. */
    fun matches(local: File, openSaved: () -> InputStream?): Boolean = runCatching {
        if (!rendered(local)) return false
        val bytes = local.length()
        val source = local.inputStream().use { sha256(it, bytes) } ?: return false
        val saved = (openSaved() ?: return false).use { sha256(it, bytes) } ?: return false
        source.first == bytes && saved.first == bytes && MessageDigest.isEqual(source.second, saved.second)
    }.getOrDefault(false)

    private fun sha256(input: InputStream, maxBytes: Long): Pair<Long, ByteArray>? {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        var empty = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) return total to digest.digest()
            if (n == 0) {
                if (++empty > 16) return null
                continue
            }
            if (n.toLong() > maxBytes - total) return null
            empty = 0
            digest.update(buffer, 0, n)
            total += n
        }
    }
}
