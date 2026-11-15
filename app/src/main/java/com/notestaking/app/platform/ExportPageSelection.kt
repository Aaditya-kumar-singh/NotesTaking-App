package com.notestaking.app.platform

/** Fail the entire export when any requested page is no longer present. */
internal object ExportPageSelection {
    fun <T> resolve(pages: List<T>, indices: List<Int>): List<T> {
        require(indices.isNotEmpty() && indices.all { it in pages.indices }) {
            "Cannot export an empty or out-of-range page selection"
        }
        return indices.distinct().sorted().map(pages::get)
    }

    fun <T : Any> matchesSnapshot(before: List<T>, after: List<T>, indices: List<Int>): Boolean =
        indices.isNotEmpty() && indices.all { i ->
            i in before.indices && i in after.indices && before[i] === after[i]
        }
}
