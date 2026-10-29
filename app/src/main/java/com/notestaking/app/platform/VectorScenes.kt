package com.notestaking.app.platform

import android.util.Log
import android.util.LruCache
import com.notestaking.app.core.vector.VectorScene
import com.notestaking.app.format.SvgReader
import java.io.File

/**
 * Parsed vector documents by file path, for the GPU-resident canvas.
 *
 * A derived render artifact and never persisted: the bundle keeps the SVG file itself, and this is
 * rebuilt on demand. Count-bounded rather than byte-bounded, the same way [ImageDecoder]'s parsed
 * document cache is, since a scene has no cheap byte size. Image files are immutable temp files, so
 * the path alone is a sound key.
 *
 * Whatever the reader could not draw is logged once per file. The infinite canvas skips an
 * unsupported construct rather than falling back to a raster, so the log line is the only way a
 * missing drop shadow ever gets explained.
 */
object VectorScenes {

    private val cache = LruCache<String, VectorScene>(4)
    private val reported = HashSet<String>()
    private val glyphs = AndroidGlyphOutliner()

    /** The parsed scene for [file], or null when it is not a vector or will not read. */
    @Synchronized
    fun sceneFor(file: File): VectorScene? {
        val path = file.path
        cache.get(path)?.let { return it }
        if (!ImageDecoder.isVector(path)) return null
        val length = runCatching { file.length() }.getOrDefault(0L)
        if (!com.notestaking.app.core.util.Svg.withinParseLimit(file)) {
            Log.w(TAG, "svg not meshed, $length bytes is past the ${com.notestaking.app.core.util.Svg.MAX_PARSE_BYTES} parse ceiling: $path")
            return null
        }
        val bytes = runCatching { BoundedBytes.readFile(file, com.notestaking.app.core.util.Svg.MAX_PARSE_BYTES.toInt()) }.getOrNull() ?: return null
        val scene = SvgReader.parse(bytes, glyphs)
        cache.put(path, scene)
        report(path, scene)
        return scene
    }

    private fun report(path: String, scene: VectorScene) {
        if (!reported.add(path)) return
        if (scene.skipped.isNotEmpty()) {
            Log.w(TAG, "svg drawn without ${scene.skipped.joinToString(", ")}: $path")
        }
        if (scene.isEmpty) Log.w(TAG, "svg has nothing this pipeline can draw: $path")
    }

    private const val TAG = "VectorScenes"
}
