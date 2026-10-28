package com.notestaking.app.platform

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
    fun write(target: File, body: (FileOutputStream) -> Unit): Boolean {
        var staged: File? = null
        return try {
            val parent = target.absoluteFile.parentFile ?: return false
            if (!parent.isDirectory && !parent.mkdirs()) return false
            staged = Files.createTempFile(parent.toPath(), ".${target.name}.", ".staged").toFile()
            FileOutputStream(staged).use { output ->
                body(output)
                output.fd.sync()
            }
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
}
