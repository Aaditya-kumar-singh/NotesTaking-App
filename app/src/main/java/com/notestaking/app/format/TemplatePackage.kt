package com.notestaking.app.format

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * A packaged `.xtemplate` (SPEC §1, Packages): a ZIP holding `template.json` and the pictures its
 * `image` nodes name. Read straight from the file, so pictures never pass through the heap.
 */
object TemplatePackage {

    const val JSON_ENTRY = "template.json"
    const val MAX_BYTES = 16L shl 20

    /** Whether [head], a file's first bytes, starts a ZIP rather than plain JSON. */
    fun isPackage(head: ByteArray): Boolean = head.size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()

    fun isPackage(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val head = ByteArray(2)
            var n = 0
            while (n < 2) {
                val r = input.read(head, n, 2 - n)
                if (r < 0) break
                n += r
            }
            n == 2 && isPackage(head)
        }
    }.getOrDefault(false)

    /** [file]'s template JSON; throws [TemplateFormatException] when it is not a usable package. */
    fun readJson(file: File): String {
        if (file.length() > MAX_BYTES) throw TemplateFormatException("template package is larger than 16 MiB")
        try {
            ZipFile(file).use { zip ->
                if (zip.size() > 1_000) throw TemplateFormatException("too many template entries")
                val e = zip.getEntry(JSON_ENTRY) ?: throw TemplateFormatException("package has no $JSON_ENTRY")
                val bytes = zip.getInputStream(e).use { readAtMost(it, TemplateReader.MAX_BYTES) }
                    ?: throw TemplateFormatException("template is larger than 1 MiB")
                return String(bytes, Charsets.UTF_8)
            }
        } catch (e: ZipException) {
            throw TemplateFormatException("not a readable template package")
        }
    }

    /** Whether [src] is a path an `image` may name: relative, with no `..` segment (SPEC §5.3). */
    fun validSrc(src: String): Boolean =
        src.isNotEmpty() && src.length <= 255 && !src.startsWith("/") && '\\' !in src &&
            src.split('/').all { it.isNotEmpty() && it != "." && it != ".." } &&
            src.none { it.code < 32 || it.code == 127 }

    /** Copy picture [src] out of [file] into [dest]; false when it is missing or not allowed. */
    fun extract(file: File, src: String, dest: File): Boolean {
        if (!validSrc(src) || file.length() > MAX_BYTES) return false
        var stage: File? = null
        return try {
            val parent = dest.absoluteFile.parentFile ?: return false
            if (!parent.isDirectory) return false
            ZipFile(file).use { zip ->
                if (zip.size() > 1_000) return false
                val entry = zip.getEntry(src)?.takeIf { !it.isDirectory } ?: return false
                if (entry.size > MAX_IMAGE_BYTES) return false
                val temporary = Files.createTempFile(parent.toPath(), ".template-img-", ".tmp").toFile()
                stage = temporary
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_IMAGE_BYTES) throw java.io.IOException("Template image too large")
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                    }
                }
                Files.move(stage!!.toPath(), dest.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                true
            }
        } catch (_: Exception) {
            false
        } finally {
            stage?.delete()
        }
    }

    private const val MAX_IMAGE_BYTES = 32L * 1024L * 1024L

    /** Write a package of [json] and [pictures] (entry name to file) to [out]; pictures are stored, being compressed already. */
    fun write(out: File, json: String, pictures: List<Pair<String, File>>) {
        ZipOutputStream(FileOutputStream(out)).use { zos ->
            zos.putNextEntry(ZipEntry(JSON_ENTRY))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            for ((name, file) in pictures) {
                val crc = CRC32()
                file.inputStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        crc.update(buf, 0, n)
                    }
                }
                zos.putNextEntry(ZipEntry(name).apply {
                    method = ZipEntry.STORED
                    size = file.length()
                    compressedSize = file.length()
                    this.crc = crc.value
                })
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun readAtMost(input: InputStream, limit: Int): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) return out.toByteArray()
            if (out.size() + n > limit) return null
            out.write(buf, 0, n)
        }
    }
}
