package com.notestaking.app.core.util

/**
 * The document types the app stores, and the file extension each uses.
 *
 * The extension is spelled once here rather than at the two dozen sites that used to hard-code
 * `.xnote`. That matters now there are two: every place that lists, names, renames or opens a file
 * has to agree on which extensions are documents, and a place that forgets one silently hides the
 * user's files from the explorer.
 */
enum class DocumentKind(val extension: String, private vararg val legacyExtensions: String) {
    /** The paged notebook. */
    NOTE("xnote"),

    /** The infinite canvas. New files use ntcanvas; xcanvas stays readable. */
    CANVAS("ntcanvas", "xcanvas"),
    ;

    /** The extension with its dot, as it appears at the end of a new file name. */
    val suffix: String get() = ".$extension"

    /** Names already using this kind, including older filenames, keep their extension. */
    private fun matchedSuffix(name: String): String? =
        sequenceOf(extension, *legacyExtensions).map { ".$it" }
            .firstOrNull { name.endsWith(it, ignoreCase = true) }

    companion object {
        /** The kind [name] denotes, or null when it is not a document at all. */
        fun ofName(name: String): DocumentKind? =
            entries.firstOrNull { it.matchedSuffix(name) != null }

        fun isDocument(name: String): Boolean = ofName(name) != null

        /** [name] without its document extension, or unchanged when it has none. */
        fun stripSuffix(name: String): String {
            val suffix = ofName(name)?.matchedSuffix(name) ?: return name
            return name.dropLast(suffix.length)
        }

        /** Append the new suffix only when [name] does not already use this document kind. */
        fun withSuffix(name: String, kind: DocumentKind): String =
            if (ofName(name) == kind) name else "$name${kind.suffix}"
    }
}
