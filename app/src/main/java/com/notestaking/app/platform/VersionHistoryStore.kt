package com.notestaking.app.platform

import com.notestaking.app.core.model.Document
import com.notestaking.app.format.DocumentCodec
import java.io.File
import java.io.FileInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID

/**
 * Manual, private native-note versions. No SAF writes: a restore is opened as an unsaved copy.
 * Retention is per document and never touches the user's original note or other documents.
 */
internal class VersionHistoryStore(
    private val root: File,
    private val codec: DocumentCodec,
    private val pdfDir: File,
    private val imageDir: File,
    private val maxVersionBytes: Long = 64L * 1024 * 1024,
    private val maxVersions: Int = 4,
    private val maxNoteBytes: Long = 128L * 1024 * 1024,
) {
    data class Entry(val id: String, val createdAt: Long, val sizeBytes: Long)
    data class Preview(val entry: Entry, val title: String, val pages: Int, val digest: String)

    private val names = Regex("([0-9]{13})-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.xnote")

    private fun bucket(key: String): File = File(root, sha(key.toByteArray(Charsets.UTF_8)))

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun sha(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /** Only files with our generated name and a plausible native ZIP appear in the list. */
    @Synchronized
    fun list(key: String): List<Entry> = bucket(key).listFiles()?.mapNotNull { file ->
        val match = names.matchEntire(file.name) ?: return@mapNotNull null
        val size = file.length()
        if (!file.isFile || size <= 0 || size > maxVersionBytes || !SafeFileWriter.hasNativeManifest(file)) return@mapNotNull null
        Entry(file.name, match.groupValues[1].toLong(), size)
    }?.sortedWith(compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id }).orEmpty()

    private fun checked(key: String, entry: Entry): File? {
        val match = names.matchEntire(entry.id) ?: return null
        if (match.groupValues[1].toLongOrNull() != entry.createdAt) return null
        val file = File(bucket(key), entry.id)
        if (!file.isFile || file.length() != entry.sizeBytes || file.length() !in 1L..maxVersionBytes) return null
        return file.takeIf { SafeFileWriter.hasNativeManifest(it) }
    }

    /** Fail closed if a snapshot cannot be fully encoded, fsynced and atomically published. */
    @Synchronized
    fun capture(key: String, document: Document, now: Long = System.currentTimeMillis()): Entry? {
        if (key.isBlank() || maxVersionBytes <= 0 || maxNoteBytes <= 0 || maxVersions <= 0) return null
        val dir = bucket(key)
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val name = "${now.coerceIn(1000000000000L, 9999999999999L)}-${UUID.randomUUID()}.xnote"
        val file = File(dir, name)
        if (file.exists()) return null
        val written = SafeFileWriter.write(file, { it.length() in 1L..minOf(maxVersionBytes, maxNoteBytes) && SafeFileWriter.hasNativeManifest(it) }) { output ->
            codec.write(document, limited(output))
        }
        if (!written) return null
        val entry = Entry(name, names.matchEntire(name)!!.groupValues[1].toLong(), file.length())
        prune(key, keep = name)
        return entry
    }

    /** Bounded bytes from the serializer: a too-large version cannot fill private app storage. */
    private fun limited(stream: OutputStream): OutputStream = object : FilterOutputStream(stream) {
        var written = 0L
        private fun check(n: Int) {
            if (n < 0 || written > minOf(maxVersionBytes, maxNoteBytes) - n) throw IOException("Version exceeds local limit")
            written += n
        }
        override fun write(b: Int) { check(1); out.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { check(len); out.write(b, off, len) }
    }

    private fun prune(key: String, keep: String) {
        val sorted = list(key)
        var count = 1
        var bytes = File(bucket(key), keep).length()
        for (entry in sorted) {
            if (entry.id == keep) continue
            if (count < maxVersions && bytes + entry.sizeBytes <= maxNoteBytes) {
                count++
                bytes += entry.sizeBytes
            } else File(bucket(key), entry.id).delete()
        }
    }

    /** Fully decode into disposable extraction dirs before showing restore as an option. */
    @Synchronized
    fun preview(key: String, entry: Entry): Preview? {
        val file = checked(key, entry) ?: return null
        if (!root.isDirectory && !root.mkdirs()) return null
        val scratch = runCatching { Files.createTempDirectory(root.toPath(), ".version-preview-").toFile() }.getOrNull()
            ?: return null
        return try {
            val pdf = File(scratch, "pdf").apply { mkdirs() }
            val images = File(scratch, "images").apply { mkdirs() }
            val doc = FileInputStream(file).use { codec.read(it, pdf, images) }
            Preview(entry, doc.title, doc.pages.size, sha(file))
        } catch (_: Exception) {
            null
        } finally {
            scratch.deleteRecursively()
        }
    }

    /** Caller must explicitly confirm, and must capture its current working note before swapping. */
    @Synchronized
    fun restoreCopy(key: String, preview: Preview): Document? {
        val file = checked(key, preview.entry) ?: return null
        if (runCatching { sha(file) }.getOrNull() != preview.digest) return null
        return runCatching {
            FileInputStream(file).use { codec.read(it, pdfDir, imageDir) }.apply {
                path = null
                displayName = null
                dirty = true
            }
        }.getOrNull()
    }
}
