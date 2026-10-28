package com.notestaking.app.platform

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Failure-tolerant JSON persistence. Existing valid settings/session metadata are never deleted
 * as a rename fallback. Session writers can observe a failure via [writeChecked].
 */
class JsonStore(private val file: File) {

    @Synchronized
    fun read(): JSONObject = try {
        if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else JSONObject()
    } catch (_: Exception) {
        JSONObject()
    }

    /** Settings callers remain best-effort. */
    fun write(obj: JSONObject) { writeChecked(obj) }

    @Synchronized
    fun writeChecked(obj: JSONObject): Boolean = SafeFileWriter.write(file) { output ->
        output.write(obj.toString().toByteArray(Charsets.UTF_8))
    }

    companion object {
        fun settings(context: Context): JsonStore =
            JsonStore(File(File(context.filesDir, "config"), "settings.json"))

        fun viewStates(context: Context): JsonStore =
            JsonStore(File(File(context.filesDir, "config"), "view_states.json"))

        fun createdTimes(context: Context): JsonStore =
            JsonStore(File(File(context.filesDir, "config"), "created_times.json"))

        fun audioPositions(context: Context): JsonStore =
            JsonStore(File(File(context.filesDir, "config"), "audio_positions.json"))

        fun docMeta(context: Context): JsonStore =
            JsonStore(File(File(context.filesDir, "config"), "doc_meta.json"))
    }
}
