package com.notestaking.app.platform

import java.io.File

/** Each exported image gets its own FileProvider path; older clipboard/share URIs stay valid. */
internal object CachedImageFiles {
    fun create(directory: File, stem: String, encode: (File) -> Boolean): File? {
        if (!directory.isDirectory && !directory.mkdirs()) return null
        val safeStem = stem.take(40).map { c ->
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-') c else '_'
        }.joinToString("").ifBlank { "image" }
        val file = runCatching { File.createTempFile("img-$safeStem-", ".png", directory) }.getOrNull() ?: return null
        return try {
            if (encode(file) && file.length() > 0L) file
            else { file.delete(); null }
        } catch (_: Exception) {
            file.delete()
            null
        }
    }
}
