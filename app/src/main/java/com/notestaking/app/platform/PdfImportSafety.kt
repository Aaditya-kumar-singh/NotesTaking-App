package com.notestaking.app.platform

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Bounded, cancellable staging for an untrusted PDF picked through SAF.
 *
 * Never trusts ContentResolver size metadata. Invalid or interrupted imports remove
 * their staging file. Actual PDF parsing is still delegated to PDFium after this gate.
 */
internal object PdfImportSafety {
    const val MAX_PDF_BYTES = 256L * 1024L * 1024L
    const val MAX_PDF_PAGES = 5_000
    private const val HEADER_SCAN_BYTES = 1_024
    private val SIGNATURE = "%PDF-".toByteArray(Charsets.US_ASCII)

    fun acceptsPageCount(pageCount: Int): Boolean = pageCount in 1..MAX_PDF_PAGES

    /** Reject malformed, non-finite or implausibly huge PDF MediaBoxes before creating a Page. */
    fun validPagePoints(width: Float, height: Float): Boolean =
        width.isFinite() && height.isFinite() &&
            width in 1f..14_400f && height in 1f..14_400f

    fun stage(
        input: InputStream,
        directory: File,
        isCancelled: () -> Boolean = { false },
        maxBytes: Long = MAX_PDF_BYTES,
    ): File? {
        require(maxBytes > 0)
        var staged: File? = null
        var completed = false
        try {
            if (!directory.isDirectory && !directory.mkdirs()) return null
            staged = File.createTempFile("pdf-import-", ".pdf", directory)
            val header = ByteArray(HEADER_SCAN_BYTES)
            var prefixSize = 0
            var total = 0L
            val buffer = ByteArray(64 * 1024)
            var zeroReads = 0
            FileOutputStream(staged).use { output ->
                while (true) {
                    if (isCancelled()) throw IOException("PDF import cancelled")
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) {
                        if (++zeroReads > 16) throw IOException("Stalled PDF stream")
                        continue
                    }
                    zeroReads = 0
                    if (read.toLong() > maxBytes - total) throw IOException("PDF exceeds import budget")
                    val needed = (HEADER_SCAN_BYTES - prefixSize).coerceAtMost(read)
                    if (needed > 0) {
                        buffer.copyInto(header, destinationOffset = prefixSize, startIndex = 0, endIndex = needed)
                        prefixSize += needed
                    }
                    output.write(buffer, 0, read)
                    total += read
                }
                if (isCancelled() || !hasPdfHeader(header, prefixSize))
                    throw IOException("Missing PDF signature or import cancelled")
                output.fd.sync()
            }
            completed = true
            return staged
        } catch (_: IOException) {
            return null
        } catch (_: SecurityException) {
            return null
        } finally {
            runCatching { input.close() }
            if (!completed) staged?.delete()
        }
    }

    private fun hasPdfHeader(prefix: ByteArray, used: Int): Boolean {
        if (used < SIGNATURE.size) return false
        for (i in 0..used - SIGNATURE.size) {
            if (SIGNATURE.indices.all { n -> prefix[i + n] == SIGNATURE[n] }) return true
        }
        return false
    }
}
