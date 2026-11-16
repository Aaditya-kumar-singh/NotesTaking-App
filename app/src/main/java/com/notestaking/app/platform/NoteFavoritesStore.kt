package com.notestaking.app.platform

import com.notestaking.app.core.util.DocKeys
import org.json.JSONArray
import org.json.JSONObject

/** Local star metadata; never changes a note, its native bundle, or sidebar pin records. */
internal class NoteFavoritesStore(private val store: JsonStore) {
    private companion object {
        val writeLock = Any() // Both split editors can update the same settings file.
        const val MAX_FAVORITES = 10_000
        const val MAX_KEY_LENGTH = 2048
    }

    private fun valid(key: String): Boolean {
        val delimiter = key.indexOf('|')
        return key.length in 3..MAX_KEY_LENGTH && delimiter in 1 until key.lastIndex &&
            key.substring(0, delimiter).none { it.isWhitespace() } &&
            key.none { it.code < 32 || it.code == 127 }
    }

    private fun read(): LinkedHashSet<String>? {
        val out = LinkedHashSet<String>()
        val json = store.readChecked() ?: return null
        if (json.length() == 0) return out
        if (json.optInt("version", -1) != 1) return null
        val entries = json.optJSONArray("favorites") ?: return null
        if (entries.length() > MAX_FAVORITES) return null
        for (i in 0 until entries.length()) {
            val key = entries.optString(i)
            if (!valid(key) || !out.add(key)) return null
        }
        return out
    }

    private fun write(keys: Set<String>): Boolean {
        if (keys.size > MAX_FAVORITES || keys.any { !valid(it) }) return false
        return store.writeChecked(JSONObject().put("version", 1).put("favorites", JSONArray(keys.toList())))
    }

    fun contains(key: String): Boolean = synchronized(writeLock) { valid(key) && read()?.contains(key) == true }

    /** Commit first; a failed write cannot advertise a state that will vanish on restart. */
    fun set(key: String, favorite: Boolean): Boolean = synchronized(writeLock) {
        if (!valid(key)) return false
        val keys = read() ?: return false
        if ((key in keys) == favorite) return true
        if (favorite) keys.add(key) else keys.remove(key)
        write(keys)
    }

    /** Apply folder renames/moves, including descendants, without discarding unrelated stars. */
    fun rekeyTree(from: String, to: String): Boolean = synchronized(writeLock) {
        if (!valid(from) || !valid(to)) return false
        val keys = read() ?: return false
        val moved = keys.map { DocKeys.moved(it, from, to) ?: it }
        if (moved == keys.toList()) return true
        // A collision must never silently erase another favorite.
        if (moved.size != moved.toSet().size || moved.any { !valid(it) }) return false
        write(moved.toSet())
    }

    fun removeTree(root: String): Boolean = synchronized(writeLock) {
        if (!valid(root)) return false
        val keys = read() ?: return false
        if (!keys.removeAll { DocKeys.within(it, root) }) return true
        write(keys)
    }
}
