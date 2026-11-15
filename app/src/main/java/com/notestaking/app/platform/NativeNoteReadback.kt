package com.notestaking.app.platform

import com.notestaking.app.format.BoundedZipInputStream
import com.notestaking.app.format.DocumentCodec
import com.notestaking.app.format.NotePeek
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/** Before deleting moved pages, prove the new provider note can be read back with intact entries. */
internal object NativeNoteReadback {
    fun verify(
        open: () -> InputStream?,
        codec: DocumentCodec,
        expectedPages: Int,
        expectedPdf: Boolean,
        expectedSha256: ByteArray,
        isCancelled: () -> Boolean = { false },
    ): Boolean = runCatching {
        if (expectedPages < 1 || expectedSha256.size != 32 || isCancelled()) return false
        val source = open() ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        val tracked = DigestInputStream(source, digest)
        BoundedZipInputStream(tracked).use { zip ->
            val buffer = ByteArray(64 * 1024)
            var manifest: NotePeek? = null
            var entry = zip.nextEntry
            while (entry != null) {
                if (isCancelled()) return false
                if (entry.name == "manifest.json") {
                    manifest = codec.peekManifest(zip) ?: return false
                }
                var zeroReads = 0
                while (true) {
                    if (isCancelled()) return false
                    val n = zip.read(buffer)
                    if (n < 0) break
                    if (n == 0 && ++zeroReads > 16) return false
                    if (n > 0) zeroReads = 0
                }
                zip.closeEntry() // verifies each entry's CRC, including stored images/audio/PDF
                entry = zip.nextEntry
            }
            if (isCancelled() || manifest?.let { it.pages == expectedPages && it.hasPdf == expectedPdf } != true) return false
            // ZIP stream stops at the central directory; hash its remaining raw bytes too.
            var zeroReads = 0
            while (true) {
                if (isCancelled()) return false
                val n = tracked.read(buffer)
                if (n < 0) break
                if (n == 0 && ++zeroReads > 16) return false
                if (n > 0) zeroReads = 0
            }
            MessageDigest.isEqual(digest.digest(), expectedSha256)
        }
    }.getOrDefault(false)
}
