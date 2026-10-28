package com.notestaking.app.platform

import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.format.CanvasCodec
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/** Recoverable working-session snapshot for an infinite canvas, separate from the paged note slot. */
class CanvasSessionStore(private val dir: File, private val codec: CanvasCodec, private val imageDir: File) {

    private val docFile = File(dir, "canvas.xcanvas")
    private val meta = JsonStore(File(dir, "canvas-session.json"))

    @Synchronized
    fun exists(): Boolean = docFile.exists()

    /** Return true only after a complete document (if requested) and metadata commit. */
    @Synchronized
    fun save(document: InfiniteDocument, writeDocument: Boolean): Boolean {
        return try {
            if (writeDocument || !docFile.exists()) {
                if (!SafeFileWriter.write(docFile) { output -> codec.write(document, output) }) return false
            }
            val json = JSONObject()
            document.path?.let { json.put("path", it) }
            document.displayName?.let { json.put("display_name", it) }
            json.put("dirty", document.dirty)
            meta.writeChecked(json)
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun load(): InfiniteDocument? = runCatching {
        if (!docFile.exists()) return null
        val document = FileInputStream(docFile).use { codec.read(it, imageDir) }
        val json = meta.read()
        document.path = json.optString("path", "").ifEmpty { null }
        document.displayName = json.optString("display_name", "").ifEmpty { null }
        document.dirty = json.optBoolean("dirty", false)
        document
    }.getOrNull()

    @Synchronized
    fun clear() {
        runCatching {
            docFile.delete()
            meta.writeChecked(JSONObject())
        }
    }
}
