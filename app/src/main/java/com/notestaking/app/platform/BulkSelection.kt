package com.notestaking.app.platform

import com.notestaking.app.core.util.DocKeys
import java.util.concurrent.CancellationException

/**
 * Process each distinct SAF document once, including when a picked folder contains another pick.
 * Reports failures for the actual operations so callers can retry safely after partial success.
 */
internal object BulkSelection {
    data class Result<T>(
        val completed: List<T>,
        val failed: List<T>,
        val failedOperations: List<T>,
    )

    fun <T> run(
        entries: List<T>,
        key: (T) -> String,
        isFolder: (T) -> Boolean,
        operation: (T) -> Boolean,
    ): Result<T> {
        val seen = HashSet<String>()
        val unique = entries.filter { seen.add(key(it)) }
        val folders = unique.filter(isFolder)
        val actions = unique.filter { entry ->
            folders.none { parent ->
                key(parent) != key(entry) && DocKeys.within(key(entry), key(parent))
            }
        }
        val status = HashMap<String, Boolean>()
        val failedActions = ArrayList<T>()
        for (action in actions) {
            val ok = try {
                operation(action)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            status[key(action)] = ok
            if (!ok) failedActions.add(action)
        }
        val completed = ArrayList<T>()
        val failed = ArrayList<T>()
        for (entry in unique) {
            val owner = actions.first { action ->
                key(entry) == key(action) || (isFolder(action) && DocKeys.within(key(entry), key(action)))
            }
            if (status[key(owner)] == true) completed.add(entry) else failed.add(entry)
        }
        return Result(completed, failed, failedActions)
    }
}
