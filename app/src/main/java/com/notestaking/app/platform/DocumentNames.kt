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

    fun unique(raw: String, kind: DocumentKind, taken: Collection<String>): String {
        val base = stem(raw)
        val existing = taken.mapTo(HashSet()) { it.lowercase() }
        val extension = kind.suffix
        var name = "$base$extension"
        var index = 1
        while (name.lowercase() in existing) {
            name = "${base}_${index++}$extension"
        }
        return name
    }

    fun validRename(name: String): Boolean =
        name.isNotBlank() && name.toByteArray(Charsets.UTF_8).size <= 240 &&
            name == NameTemplate.sanitize(name) &&
            name.none { Character.isISOControl(it.code) } &&
            name.substringBefore('.').uppercase() !in reserved
}
