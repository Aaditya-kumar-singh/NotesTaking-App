package com.notestaking.app.platform

import java.util.concurrent.CancellationException

/** Continue past an invalid PDF, but distinguish files processed from notes successfully created. */
internal object PdfImportBatch {
    data class Result(val imported: Int, val processed: Int, val total: Int, val cancelled: Boolean)

    fun run(
        total: Int,
        isCancelled: () -> Boolean,
        importOne: (Int) -> Boolean,
        onProcessed: (Int) -> Unit,
    ): Result {
        var imported = 0
        var processed = 0
        for (index in 0 until total) {
            if (isCancelled()) break
            try {
                if (importOne(index)) imported++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A failed provider or PDF must not prevent importing the remaining files.
            }
            processed++
            onProcessed(processed)
        }
        return Result(imported, processed, total, isCancelled())
    }
}
