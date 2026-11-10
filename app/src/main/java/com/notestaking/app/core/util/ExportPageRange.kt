package com.notestaking.app.core.util

/** Parse human-readable, one-based page ranges into sorted, unique zero-based indices. */
object ExportPageRange {
    private val entry = Regex("""(\d+)(?:\s*-\s*(\d+))?""")

    /** Invalid, missing, reversed or out-of-bounds ranges return null, not a partial export. */
    fun parse(input: String, totalPages: Int): List<Int>? {
        if (totalPages < 1 || input.length > 512 || input.isBlank()) return null
        val result = sortedSetOf<Int>()
        for (part in input.split(',')) {
            val match = entry.matchEntire(part.trim()) ?: return null
            val first = match.groupValues[1].toIntOrNull() ?: return null
            val lastText = match.groupValues[2]
            val last = if (lastText.isEmpty()) first else lastText.toIntOrNull() ?: return null
            if (first !in 1..totalPages || last !in first..totalPages) return null
            for (page in first..last) result.add(page - 1)
        }
        return result.toList()
    }
}
