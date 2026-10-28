package com.notestaking.app.platform

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

/**
 * SAF document-id and tree-operation boundary for the app-owned Notes directory.
 *
 * Document IDs are slash-delimited paths rooted at "root"; no absolute paths, dot
 * segments, symlinks, or separator-containing display names are accepted.
 * Intentionally independent from Android types so security rules can run under JUnit.
 */
internal class SafeDocumentTree(base: File) {
    private val root = base.canonicalFile

    init {
        if (!root.isDirectory) throw FileNotFoundException("Notes storage root unavailable")
    }

    fun resolve(documentId: String): File {
        val segments = when {
            documentId == ROOT -> emptyList()
            documentId.startsWith("$ROOT/") ->
                documentId.substring(ROOT.length + 1).split('/')
            else -> throw FileNotFoundException("Invalid document ID")
        }
        if (segments.any { !validSegment(it) }) throw FileNotFoundException("Invalid document ID")
        var node = root
        for (segment in segments) {
            node = File(node, segment)
            if (Files.isSymbolicLink(node.toPath())) throw FileNotFoundException("Symbolic links not supported")
        }
        val canonical = node.canonicalFile
        requireInside(canonical)
        if (!canonical.exists()) throw FileNotFoundException("Document not found")
        return canonical
    }

    fun documentId(file: File): String {
        val absolute = file.absoluteFile.toPath().normalize()
        val base = root.toPath()
        if (!absolute.startsWith(base)) throw FileNotFoundException("Outside Notes storage")
        var child = root
        val relative = base.relativize(absolute)
        for (segment in relative) {
            child = File(child, segment.toString())
            if (Files.isSymbolicLink(child.toPath())) throw FileNotFoundException("Symbolic links not supported")
        }
        val canonical = child.canonicalFile
        requireInside(canonical)
        if (canonical == root) return ROOT
        return "$ROOT/" + root.toPath().relativize(canonical.toPath()).joinToString("/")
    }

    fun children(parentId: String): List<File> {
        val dir = directory(parentId)
        return (dir.listFiles() ?: throw FileNotFoundException("Cannot list directory"))
            .filter { !Files.isSymbolicLink(it.toPath()) }
    }

    fun isChild(parentId: String, childId: String): Boolean {
        val parent = resolve(parentId)
        val child = resolve(childId)
        return child != parent && child.toPath().startsWith(parent.toPath())
    }

    fun directory(documentId: String): File =
        resolve(documentId).also { if (!it.isDirectory) throw FileNotFoundException("Not a directory") }

    fun create(parentId: String, displayName: String, directory: Boolean): String {
        val parent = directory(parentId)
        val target = freeChild(parent, displayName)
        val created = if (directory) target.mkdir() else target.createNewFile()
        if (!created) throw FileNotFoundException("Cannot create document")
        return documentId(target)
    }

    fun rename(documentId: String, displayName: String): String {
        val source = resolve(documentId)
        requireNotRoot(source)
        val target = child(source.parentFile, displayName)
        if (target.exists()) throw FileNotFoundException("Name already exists")
        try { Files.move(source.toPath(), target.toPath()) }
        catch (_: IOException) { throw FileNotFoundException("Rename failed") }
        return documentId(target)
    }

    fun delete(documentId: String) {
        val source = resolve(documentId)
        requireNotRoot(source)
        if (!source.deleteRecursively()) throw FileNotFoundException("Delete failed")
    }

    fun move(sourceId: String, sourceParentId: String, targetParentId: String): String {
        val source = resolve(sourceId)
        requireNotRoot(source)
        val sourceParent = directory(sourceParentId)
        val targetParent = directory(targetParentId)
        if (source.parentFile != sourceParent) throw FileNotFoundException("Incorrect source parent")
        forbidSelfDescendant(source, targetParent)
        val destination = child(targetParent, source.name)
        if (destination.exists()) throw FileNotFoundException("Destination exists")
        try { Files.move(source.toPath(), destination.toPath()) }
        catch (_: IOException) { throw FileNotFoundException("Move failed") }
        return documentId(destination)
    }

    /**
     * Stage a whole copy under a random name, then publish only the completed copy.
     * A failed copy removes its temporary subtree without touching the original.
     */
    fun copy(sourceId: String, targetParentId: String): String {
        val source = resolve(sourceId)
        requireNotRoot(source)
        val targetParent = directory(targetParentId)
        forbidSelfDescendant(source, targetParent)
        val destination = child(targetParent, source.name)
        if (destination.exists()) throw FileNotFoundException("Destination exists")
        val staged = if (source.isDirectory)
            Files.createTempDirectory(targetParent.toPath(), ".notes-copy-").toFile()
        else Files.createTempFile(targetParent.toPath(), ".notes-copy-", ".tmp").toFile()
        try {
            val budget = CopyBudget()
            copyContents(source, staged, budget, 0)
            if (destination.exists()) throw IOException("Destination exists before publishing")
            Files.move(staged.toPath(), destination.toPath()) // do not overwrite an existing note
            return documentId(destination)
        } finally {
            if (staged.exists()) staged.deleteRecursively()
        }
    }

    private fun copyContents(source: File, destination: File, budget: CopyBudget, depth: Int) {
        if (depth > MAX_COPY_DEPTH || ++budget.entries > MAX_COPY_ENTRIES)
            throw IOException("Folder copy limit exceeded")
        if (Files.isSymbolicLink(source.toPath())) throw IOException("Symbolic links not supported")
        if (source.isDirectory) {
            if (!destination.isDirectory && !destination.mkdir()) throw IOException("Cannot create copy directory")
            val children = source.listFiles() ?: throw IOException("Cannot read copy directory")
            for (child in children) copyContents(child, File(destination, child.name), budget, depth + 1)
        } else if (source.isFile) {
            source.inputStream().buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val amount = input.read(buffer)
                        if (amount == -1) break
                        budget.bytes += amount
                        if (budget.bytes > MAX_COPY_BYTES) throw IOException("Folder copy byte limit exceeded")
                        output.write(buffer, 0, amount)
                    }
                }
            }
        } else throw IOException("Not a regular file")
    }

    private fun forbidSelfDescendant(source: File, targetParent: File) {
        if (source.isDirectory && targetParent.toPath().startsWith(source.toPath()))
            throw FileNotFoundException("Cannot put a directory inside itself")
    }

    private fun freeChild(parent: File, name: String): File {
        validateName(name)
        val first = child(parent, name)
        if (!first.exists()) return first
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (i in 1..9999) {
            val candidate = child(parent, "$stem ($i)$ext")
            if (!candidate.exists()) return candidate
        }
        throw FileNotFoundException("Cannot find an available name")
    }

    private fun child(parent: File, name: String): File {
        validateName(name)
        val next = File(parent, name)
        requireInside(next.canonicalFile)
        if (Files.isSymbolicLink(next.toPath())) throw FileNotFoundException("Symbolic links not supported")
        return next
    }

    private fun requireInside(file: File) {
        if (file != root && !file.toPath().startsWith(root.toPath()))
            throw FileNotFoundException("Outside Notes storage")
    }

    private fun requireNotRoot(file: File) {
        if (file == root) throw FileNotFoundException("Cannot modify the Notes storage root")
    }

    private fun validateName(name: String) {
        if (!validSegment(name) || name.length > 240) throw FileNotFoundException("Invalid name")
    }

    private fun validSegment(value: String): Boolean =
        value.isNotBlank() && value != "." && value != ".." &&
            '/' !in value && '\\' !in value && value.none { it.code < 32 || it.code == 127 }

    private class CopyBudget(var entries: Int = 0, var bytes: Long = 0)
    private companion object {
        const val ROOT = "root"
        const val MAX_COPY_DEPTH = 64
        const val MAX_COPY_ENTRIES = 10_000
        const val MAX_COPY_BYTES = 1024L * 1024L * 1024L
    }
}
