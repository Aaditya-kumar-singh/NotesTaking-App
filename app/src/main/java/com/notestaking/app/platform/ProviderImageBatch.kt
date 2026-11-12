package com.notestaking.app.platform

import java.io.OutputStream
import java.util.concurrent.CancellationException

/** Save each rendered page independently so one failed provider write cannot count as a saved image. */
internal object ProviderImageBatch {
    class Destination(val open: () -> OutputStream?, val remove: () -> Boolean)
    class Result(val saved: Int, val leftovers: Boolean)

    fun save(
        pages: List<Int>,
        render: (Int) -> ByteArray?,
        create: (Int) -> Destination?,
    ): Result {
        var saved = 0
        var leftovers = false
        for (page in pages) {
            val png = try {
                render(page)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (png == null || png.isEmpty()) continue

            val destination = try {
                create(page)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } ?: continue

            when (ProviderCopy.save({ png.inputStream() }, destination.open, destination.remove)) {
                ProviderCopy.Result.SAVED -> saved++
                ProviderCopy.Result.FAILED_LEFTOVER -> leftovers = true
                ProviderCopy.Result.FAILED_REMOVED -> Unit
            }
        }
        return Result(saved, leftovers)
    }
}
