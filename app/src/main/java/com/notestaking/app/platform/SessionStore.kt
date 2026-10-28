package com.notestaking.app.platform

import com.notestaking.app.canvas.ViewOverrides
import com.notestaking.app.core.model.Document
import com.notestaking.app.format.DocumentCodec
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Persists the open paged note and its view state, including edits not yet saved to a picked file.
 * Failure must never discard the last committed snapshot.
 *
 * This store serializes its own reads/writes; the Editor orders async requests and only records
 * a content version as saved after this method acknowledges success.
 */
class SessionStore(private val dir: File, private val codec: DocumentCodec, private val pdfDir: File, private val imageDir: File) {

    private val docFile = File(dir, "document.xnote")
    private val meta = JsonStore(File(dir, "session.json"))

    class Snapshot(
        val document: Document,
        val zoom: Double,
        val scrollX: Double,
        val scrollY: Double,
        val zoomLocked: Boolean,
        val viewOverrides: ViewOverrides,
    )

    /** Return false if either document or metadata did not commit. Preserve the previous file on failure. */
    @Synchronized
    fun save(
        document: Document,
        zoom: Double,
        scrollX: Double,
        scrollY: Double,
        zoomLocked: Boolean,
        viewOverrides: ViewOverrides,
        writeDocument: Boolean,
    ): Boolean {
        return try {
            if (writeDocument || !docFile.exists()) {
                if (!SafeFileWriter.write(docFile) { output -> codec.write(document, output) }) return false
            }
            val m = JSONObject()
                .put("dirty", document.dirty)
                .put("zoom", zoom)
                .put("scrollX", scrollX)
                .put("scrollY", scrollY)
                .put("zoomLocked", zoomLocked)
            document.path?.let { m.put("path", it) }
            document.displayName?.let { m.put("displayName", it) }
            meta.writeChecked(ViewOverridesJson.write(m, viewOverrides))
        } catch (_: Exception) {
            false
        }
    }

    /** Deliberately close the working session; never called for a failed save attempt. */
    @Synchronized
    fun clear() {
        runCatching {
            docFile.delete()
            File(dir, "session.json").delete()
        }
    }

    /** Load the latest completely committed note; missing/corrupt sessions restore nothing. */
    @Synchronized
    fun load(): Snapshot? {
        if (!docFile.exists()) return null
        val doc = runCatching { FileInputStream(docFile).use { codec.read(it, pdfDir, imageDir) } }.getOrNull() ?: return null
        val m = meta.read()
        doc.path = m.optString("path", "").ifEmpty { null }
        doc.displayName = m.optString("displayName", "").ifEmpty { null }
        doc.dirty = m.optBoolean("dirty", false)
        return Snapshot(
            doc,
            m.optDouble("zoom", 0.0),
            m.optDouble("scrollX", 0.0),
            m.optDouble("scrollY", 0.0),
            m.optBoolean("zoomLocked", false),
            ViewOverridesJson.read(m),
        )
    }
}
