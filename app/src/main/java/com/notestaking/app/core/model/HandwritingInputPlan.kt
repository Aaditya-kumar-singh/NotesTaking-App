package com.notestaking.app.core.model

import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import kotlin.math.max

/** One ephemeral recognition request. No native-note fields, schema or network access. */
data class HandwritingPoint(val x: Float, val y: Float)
data class HandwritingTrace(val sourceIndex: Int, val points: List<HandwritingPoint>)
data class HandwritingLine(val bounds: Rect, val traces: List<HandwritingTrace>)
data class HandwritingRequest(val languageTag: String, val lines: List<HandwritingLine>)

/** Available in an SDK catalog is not the same as downloaded on this device. */
enum class HandwritingModelState { UNSUPPORTED, NOT_DOWNLOADED, DOWNLOADING, READY, ERROR }

/**
 * A UI/provider adapter must check local model availability first. If offline and absent,
 * keep the original ink and offer a download action later; do not silently use a cloud API.
 */
interface HandwritingRecognitionProvider {
    fun modelState(languageTag: String, onResult: (HandwritingModelState) -> Unit)
    fun recognize(request: HandwritingRequest, onResult: (Result<List<String>>) -> Unit)
}

/** Build a read-only, per-line vector input for a future on-device recognizer. */
object HandwritingInputPlanner {
    const val MAX_STROKES = 256
    const val MAX_POINTS = 20_000
    const val MAX_SPAN = 100_000.0

    /** Input order is drawing order. Repeated selection entries are rejected to avoid duplicated ink. */
    fun plan(strokes: List<Stroke>, languageTag: String): HandwritingRequest {
        require(languageTag == "en" || languageTag == "hi") { "Only English or Devanagari Hindi is planned" }
        require(strokes.isNotEmpty() && strokes.size <= MAX_STROKES) { "Invalid handwriting stroke count" }
        require(strokes.toSet().size == strokes.size) { "Repeated stroke selection" }
        require(strokes.all { it.tool in setOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER) }) {
            "Only handwritten ink can be recognized"
        }
        val counts = strokes.map { it.sampleCount }
        require(counts.all { it in 1..MAX_POINTS } && counts.sum() <= MAX_POINTS) {
            "Handwriting point budget exceeded"
        }
        // Take each stroke's immutable sample view once. The ink remains editable and unchanged.
        val paths: List<List<Sample>> = strokes.map { it.samples.toList() }
        require(paths.all { path -> path.all { it.x.isFinite() && it.y.isFinite() } }) {
            "Nonfinite handwriting point"
        }
        val minX = paths.minOf { path -> path.minOf { it.x } }
        val maxX = paths.maxOf { path -> path.maxOf { it.x } }
        val minY = paths.minOf { path -> path.minOf { it.y } }
        val maxY = paths.maxOf { path -> path.maxOf { it.y } }
        val width = maxX - minX
        val height = maxY - minY
        require(width.isFinite() && height.isFinite() && width <= MAX_SPAN && height <= MAX_SPAN) {
            "Invalid handwriting dimensions"
        }
        val scale = 1000.0 / max(1.0, max(width, height))
        data class Entry(val sourceIndex: Int, val bounds: Rect, val trace: HandwritingTrace)
        val entries = paths.mapIndexed { index, samples ->
            val x0 = samples.minOf { it.x }
            val x1 = samples.maxOf { it.x }
            val y0 = samples.minOf { it.y }
            val y1 = samples.maxOf { it.y }
            Entry(index, Rect(x0, y0, x1 - x0, y1 - y0),
                HandwritingTrace(index, samples.map { sample ->
                    HandwritingPoint(((sample.x - minX) * scale).toFloat(),
                        ((sample.y - minY) * scale).toFloat())
                }))
        }
        // Only spatial grouping is possible: persisted stroke times are relative to pen-down.
        // Preserve drawing order inside a line; never pretend timestamps are globally ordered.
        val rowHeight = max(1.0, height / 12.0)
        val rows = mutableListOf<MutableList<Entry>>()
        for (entry in entries) {
            val y = entry.bounds.center.y
            val row = rows.firstOrNull { group ->
                val a = group.minOf { it.bounds.top }
                val b = group.maxOf { it.bounds.bottom }
                y >= a - rowHeight && y <= b + rowHeight
            }
            if (row == null) rows.add(mutableListOf(entry)) else row.add(entry)
        }
        val lines = rows.sortedBy { group -> group.minOf { it.bounds.top } }.map { group ->
            val x0 = group.minOf { it.bounds.left }
            val x1 = group.maxOf { it.bounds.right }
            val y0 = group.minOf { it.bounds.top }
            val y1 = group.maxOf { it.bounds.bottom }
            HandwritingLine(Rect(x0, y0, x1 - x0, y1 - y0), group.map { it.trace })
        }
        return HandwritingRequest(languageTag, lines)
    }
}
