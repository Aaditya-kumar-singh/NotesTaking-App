package com.notestaking.app.platform

import com.notestaking.app.core.util.DocumentKind
import com.notestaking.app.core.util.NameTemplate

/** Portable document names for providers whose filesystem rules differ from Android's. */
internal object DocumentNames {
    private const val MAX_STEM_BYTES = 200
    private val reserved = setOf("CON", "PRN", "AUX", "NUL") +
        (1..9).flatMap { listOf("COM$it", "LPT$it") }

    fun stem(raw: String): String {
        val cleaned = NameTemplate.sanitize(DocumentKind.stripSuffix(raw.trim()))
        val out = StringBuilder()
        var bytes = 0
        var index = 0
        while (index < cleaned.length) {
            val cp = Character.codePointAt(cleaned, index)
            index += Character.charCount(cp)
            if (Character.isISOControl(cp)) continue
            val width = when {
                cp < 0x80 -> 1
                cp < 0x800 -> 2
                cp < 0x10000 -> 3
                else -> 4
            }
            if (bytes + width > MAX_STEM_BYTES) break
            out.appendCodePoint(cp)
            bytes += width
        }
        val safe = out.toString().trim().trim('.').trim().ifEmpty { "untitled" }
        return if (safe.substringBefore('.').uppercase() in reserved) "_$safe" else safe
    }

    private val canvasSuffixes = listOf(".xcanvas", ".ntcanvas")

    private fun canvasStem(name: String): String? {
        var base = name
        var found = false
        while (true) {
            val suffix = canvasSuffixes.firstOrNull { base.endsWith(it, ignoreCase = true) }
                ?: return base.takeIf { found }
            base = base.dropLast(suffix.length)
            found = true
        }
    }

    fun unique(raw: String, kind: DocumentKind, taken: Collection<String>): String {
        val base = stem(if (kind == DocumentKind.CANVAS) canvasStem(raw.trim()) ?: raw else raw)
        val existing = if (kind == DocumentKind.CANVAS) {
            taken.mapNotNullTo(HashSet()) { canvasStem(it)?.lowercase() }
        } else {
            taken.mapTo(HashSet()) { it.lowercase() }
        }
        val extension = kind.suffix
        var index = 1
        var candidate = base
        while (if (kind == DocumentKind.CANVAS) candidate.lowercase() in existing
            else "$candidate$extension".lowercase() in existing) {
            candidate = "${base}_${index++}"
        }
        return "$candidate$extension"
    }

    /** Keep a legacy canvas extension on ordinary rename, even when the UI proposes the new suffix. */
    fun canvasRename(original: String, requested: String): String {
        val suffix = canvasSuffixes.firstOrNull { original.endsWith(it, ignoreCase = true) } ?: return requested
        val base = canvasStem(requested) ?: requested
        return "$base${original.takeLast(suffix.length)}"
    }

    /** Canvas aliases share one visible name when file extensions are hidden. */
    fun sameCanvasTitle(first: String, second: String): Boolean {
        val a = canvasStem(first) ?: return false
        val b = canvasStem(second) ?: return false
        return a.equals(b, ignoreCase = true)
    }

    fun validRename(name: String): Boolean =
        name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= 240 &&
            name == NameTemplate.sanitize(name) &&
            name.none { Character.isISOControl(it.code) } &&
            name.substringBefore('.').uppercase() !in reserved
}
