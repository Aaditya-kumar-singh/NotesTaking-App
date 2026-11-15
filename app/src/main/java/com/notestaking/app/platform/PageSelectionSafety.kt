package com.notestaking.app.platform

/** Keep only pages still present by identity; equal page content does not mean the same page. */
internal object PageSelectionSafety {
    fun <T : Any> prune(selected: MutableList<T>, current: List<T>) {
        selected.removeAll { picked -> current.none { it === picked } }
    }

    /** Reject a stale/partial destructive selection rather than silently using its valid subset. */
    fun checkedIndices(pageCount: Int, indices: List<Int>): List<Int>? {
        if (indices.isEmpty() || indices.any { it !in 0 until pageCount }) return null
        return indices.distinct().sorted()
    }
}
