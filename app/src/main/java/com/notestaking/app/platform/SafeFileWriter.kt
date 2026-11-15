package com.notestaking.app.platform

import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.io.InputStreamReader
import com.notestaking.app.format.JsonPull
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

/**
 * Replaces a file only after a complete, fsynced write.
 *
 * The staged file lives beside the destination, so ATOMIC_MOVE stays on the same filesystem.
 * A failed write or unsupported atomic rename MUST leave the previous destination intact.
 * This intentionally fails closed rather than deleting the last recoverable copy.
 *
 * Callers still need to serialize competing saves for the same logical document: atomic
 * replacement protects file integrity, not application-level write ordering.
 */
internal object SafeFileWriter {
    fun write(
        target: File,
        validateStaged: (File) -> Boolean = { true },
        body: (OutputStream) -> Unit,
    ): Boolean {
        var staged: File? = null
        return try {
            val parent = target.absoluteFile.parentFile ?: return false
            if (!parent.isDirectory && !parent.mkdirs()) return false
            staged = Files.createTempFile(parent.toPath(), ".${target.name}.", ".staged").toFile()
            FileOutputStream(staged).use { output ->
                // DocumentCodec/CanvasCodec close their ZipOutputStream when finished.
                // Keep the real descriptor open until fsync succeeds, even if the codec
                // closes the stream it received.
                val borrowed = nonClosingStream(output)
                body(borrowed)
                borrowed.flush()
                output.fd.sync()
            }
            // Never replace the last recoverable copy with an empty "successful" write.
            if (staged.length() == 0L || !validateStaged(staged)) return false
            Files.move(
                staged.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            true
        } catch (_: Exception) {
            false
        } finally {
            staged?.delete() // Never touch the previous, successfully committed target on failure.
        }
    }

    /** A native session must have a finished ZIP index and nonempty manifest before it replaces a recoverable copy. */
    internal fun hasNativeManifest(file: File): Boolean = runCatching {
        ZipFile(file).use { zip ->
            val manifest = zip.getEntry("manifest.json")
            manifest != null && !manifest.isDirectory && manifest.size > 0L
        }
    }.getOrDefault(false)

    /** Validate a complete native manifest without materializing large JSON or trusting the ZIP index alone. */
    internal fun hasReadableNativeManifest(file: File, expectedFormat: String): Boolean = runCatching {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("manifest.json") ?: return@use false
            if (entry.isDirectory || entry.size <= 0L || entry.size > 256L * 1024 * 1024) return@use false
            CheckedInputStream(zip.getInputStream(entry), CRC32()).use { input ->
                val json = JsonPull(InputStreamReader(input, Charsets.UTF_8))
                var formatOk = false
                var contentOk = false
                json.beginObject()
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "format" -> formatOk = json.nextString() == expectedFormat
                        "pages" -> {
                            if (expectedFormat == "xnote" && json.peek() == JsonPull.Token.BEGIN_ARRAY) contentOk = true
                            json.skipValue()
                        }
                        "items" -> {
                            if (expectedFormat == "xcanvas" && json.peek() == JsonPull.Token.BEGIN_ARRAY) contentOk = true
                            json.skipValue()
                        }
                        else -> json.skipValue()
                    }
                }
                json.endObject()
                val buffer = ByteArray(64 * 1024)
                while (input.read(buffer) >= 0) { /* Complete the CRC even if parsing buffered ahead. */ }
                formatOk && contentOk && input.checksum.value == entry.crc
            }
        }
    }.getOrDefault(false)

    /** Pass ZIP byte arrays through in bulk while letting the caller retain the file descriptor. */
    internal fun nonClosingStream(output: OutputStream): OutputStream = object : FilterOutputStream(output) {
        override fun close() { flush() }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len) }
    }
}
