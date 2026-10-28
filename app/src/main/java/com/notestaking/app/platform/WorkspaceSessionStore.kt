package com.notestaking.app.platform

import org.json.JSONObject
import java.io.File

/**
 * A tiny, independent workspace record. Actual notes remain in per-pane SessionStore /
 * CanvasSessionStore snapshots; no note bytes or attachments are copied into this JSON.
 */
internal class WorkspaceSessionStore(file: File) {
    private val json = JsonStore(file)

    data class State(
        val primaryOpen: Boolean,
        val secondaryOpen: Boolean,
        val focusedSecondary: Boolean,
        val splitRatio: Float = 0.5f,
    )

    /** An absent, partial, or unsupported record must not reopen a stale secondary pane. */
    fun load(): State? {
        val obj = json.read()
        if (obj.optInt("version", -1) != 1) return null
        if (!obj.has("primaryOpen") || !obj.has("secondaryOpen")) return null
        if (obj.opt("primaryOpen") !is Boolean || obj.opt("secondaryOpen") !is Boolean) return null
        val primary = obj.optBoolean("primaryOpen")
        val secondary = obj.optBoolean("secondaryOpen")
        return State(primary, secondary, secondary && obj.optString("focus") == "secondary",
            obj.optDouble("splitRatio", 0.5).takeIf { it.isFinite() }?.toFloat()?.coerceIn(0.2f, 0.8f) ?: 0.5f)
    }

    /** Save only state necessary to recreate the working split. Never clear note payloads here. */
    fun save(state: State): Boolean = json.writeChecked(
        JSONObject()
            .put("version", 1)
            .put("primaryOpen", state.primaryOpen)
            .put("secondaryOpen", state.secondaryOpen)
            .put("focus", if (state.focusedSecondary && state.secondaryOpen) "secondary" else "primary")
            .put("splitRatio", (state.splitRatio.takeIf { it.isFinite() }?.coerceIn(0.2f, 0.8f) ?: 0.5f).toDouble()),
    )
}
