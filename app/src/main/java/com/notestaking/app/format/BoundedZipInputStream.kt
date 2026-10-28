package com.notestaking.app.format

import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Streaming limits for untrusted native note/canvas ZIP bundles.
 *
 * Limits apply to inflated bytes even when ZIP metadata declares size=-1, and include data
 * discarded by closeEntry() or skipped before the manifest. We reject nested/traversal names.
 * Does not extract by pathname; callers stream assets to generated temporary filenames.
 */
internal class BoundedZipInputStream(input: InputStream) : ZipInputStream(input) {
    private val names = HashSet<String>()
    private var count = 0
    private var entryBytes = 0L
    private var totalBytes = 0L
    private var maxEntryBytes = 0L

    override fun getNextEntry(): ZipEntry? {
        val entry = super.getNextEntry() ?: return null
        if (++count > MAX_ENTRIES) throw IOException("ZIP contains too many entries")
        val path = entry.name.removeSuffix("/")
        if (path.isEmpty() || path.length > 512 ||
            entry.name.startsWith("/") || '\\' in entry.name ||
            path.split('/').any { it.isEmpty() || it == "." || it == ".." || it.any { c -> c.code < 32 } }
        ) throw IOException("Invalid ZIP entry name")
        if (!names.add(entry.name)) throw IOException("Duplicate ZIP entry")
        entryBytes = 0L
        maxEntryBytes = entryLimit(path)
        if (entry.size > maxEntryBytes) throw IOException("ZIP entry exceeds size limit")
        return entry
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) {
            entryBytes += n
            totalBytes += n
            if (entryBytes > maxEntryBytes || totalBytes > MAX_TOTAL_BYTES)
                throw IOException("ZIP inflated-size limit exceeded")
        }
        return n
    }

    private fun entryLimit(name: String): Long = when {
        name == "manifest.json" -> 64L * MIB
        name == "assets/source.pdf" -> 512L * MIB
        name.startsWith("assets/image-") -> 64L * MIB
        name.startsWith("assets/audio-") -> 256L * MIB
        name.startsWith("assets/") -> 64L * MIB
        name.endsWith(".xml") -> 16L * MIB
        else -> 32L * MIB
    }

    private companion object {
        const val MIB = 1024L * 1024L
        const val MAX_ENTRIES = 10_000
        const val MAX_TOTAL_BYTES = 1024L * MIB
    }
}
