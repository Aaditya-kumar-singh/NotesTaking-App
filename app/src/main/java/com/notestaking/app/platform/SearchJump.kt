package com.notestaking.app.platform

import com.notestaking.app.core.search.SearchHit
import com.notestaking.app.core.search.SearchQuery
import com.notestaking.app.core.search.SearchResults

/** Navigation only uses hits belonging to the live query; unread pages are not silently skipped. */
internal object SearchJump {
    fun contains(results: SearchResults?, query: SearchQuery, hit: SearchHit): Boolean =
        results != null && results.query == query && results.indexOf(hit) >= 0

    fun step(
        results: SearchResults?,
        query: SearchQuery,
        current: SearchHit?,
        page: Int,
        forward: Boolean,
    ): SearchHit? {
        if (results == null || results.query != query || results.count == 0) return null
        val index = current?.let(results::indexOf)?.takeIf { it >= 0 }
        val next = when {
            index != null && forward -> (index + 1) % results.count
            index != null -> (index - 1 + results.count) % results.count
            forward -> results.firstFrom(page) // null while an earlier matching page is still unread
            !results.done -> null
            else -> (results.count - 1 downTo 0).firstOrNull { results.hit(it).page <= page }
                ?: results.count - 1
        }
        val picked = next?.takeIf { it >= 0 } ?: return null
        if (index != null && !results.done) {
            // A known hit can still precede unread PDF pages; do not jump over them.
            val currentPage = current?.page ?: return null
            if (results.firstFrom(currentPage) == null) return null
            val target = results.hit(picked)
            if (target.page != currentPage) {
                if (!forward) return null
                val from = if (target.page < currentPage) 0 else currentPage + 1
                if (results.firstFrom(from) != picked) return null
            }
        }
        return results.hit(picked)
    }
}
