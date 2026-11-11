package com.notestaking.app.platform

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException

/** A failed SAF copy must not leave a silently usable-looking native note. */
internal object ProviderCopy {
    enum class Result { SAVED, FAILED_REMOVED, FAILED_LEFTOVER }

    fun save(
        openSource: () -> InputStream?,
        openDestination: () -> OutputStream?,
        removeDestination: () -> Boolean,
    ): Result {
        try {
            ProviderInput.read(openSource()) { input ->
                ProviderOutput.write(openDestination()) { output ->
                    if (input.copyTo(output) == 0L) throw EOFException("Source note is empty")
                }
            }
            return Result.SAVED
        } catch (e: CancellationException) {
            runCatching { removeDestination() }
            throw e
        } catch (e: Exception) {
            return if (runCatching { removeDestination() }.getOrDefault(false)) {
                Result.FAILED_REMOVED
            } else {
                Result.FAILED_LEFTOVER
            }
        }
    }
}
