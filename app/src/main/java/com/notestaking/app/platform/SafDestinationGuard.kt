package com.notestaking.app.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/** A picked SAF destination must be demonstrably empty before it may be overwritten or deleted. */
internal object SafDestinationGuard {
    fun isEmptyDocument(resolver: ContentResolver, uri: Uri): Boolean {
        val size = try {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
            }
        } catch (_: Exception) {
            null
        }
        return canWrite(size) {
            try {
                resolver.openInputStream(uri)?.use { it.read() }
            } catch (_: Exception) {
                null
            }
        }
    }

    /** When size metadata is unavailable, read one byte; failure to inspect must not authorize a write. */
    fun canWrite(reportedSize: Long?, firstByte: () -> Int?): Boolean {
        if (reportedSize != null && reportedSize >= 0L) return reportedSize == 0L
        return try {
            firstByte() == -1
        } catch (_: Exception) {
            false
        }
    }
}
