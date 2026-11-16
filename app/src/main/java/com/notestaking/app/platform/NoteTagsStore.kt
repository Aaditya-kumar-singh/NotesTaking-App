package com.notestaking.app.platform

import com.notestaking.app.core.util.DocKeys
import org.json.JSONArray
import org.json.JSONObject

/** Private local note tags, separate from native note bytes and folder color sidecars. */
internal class NoteTagsStore(private val store: JsonStore) {
    private companion object {
        val lock = Any() // Both editor panes can operate on the same backing file.
        const val MAX_NOTES = 10_000
        const val MAX_TAGS = 16
        const val MAX_TAG_LENGTH = 40
        const val MAX_KEY_LENGTH = 2048
    }

    private fun validKey(key: String): Boolean {
        val bar = key.indexOf('|')
        return key.length in 3..MAX_KEY_LENGTH && bar in 1 until key.lastIndex &&
            key.indexOf('|', bar + 1) < 0 && key.none { it.code < 32 || it.code == 127 }
    }

    private fun normalize(tags: Collection<String>): List<String>? {
        if (tags.size > MAX_TAGS) return null
        val cleaned = tags.map { it.trim() }
        if (cleaned.any { it.isEmpty() || it.length > MAX_TAG_LENGTH || it.any { c -> c.code < 32 || c.code == 127 } }) return null
        if (cleaned.map { it.lowercase(java.util.Locale.ROOT) }.toSet().size != cleaned.size) return null
        return cleaned.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /** Invalid data is not interpreted as an empty database: writes fail closed. */
    private fun read(): LinkedHashMap<String, List<String>>? {
        val data = store.readChecked() ?: return null
        val result = LinkedHashMap<String, List<String>>()
        if (data.length() == 0) return result
        if (data.optInt("version", -1) != 1) return null
        val entries = data.optJSONObject("notes") ?: return null
        if (entries.length() > MAX_NOTES) return null
        for (key in entries.keys()) {
            if (!validKey(key)) return null
            val array = entries.optJSONArray(key) ?: return null
            if (array.length() == 0 || array.length() > MAX_TAGS) return null
            val tags = (0 until array.length()).map { i -> array.optString(i) }
            val canonical = normalize(tags) ?: return null
            result[key] = canonical
        }
        return result
    }

    private fun write(data: Map<String, List<String>>): Boolean {
        if (data.size > MAX_NOTES || data.any { (key, value) -> !validKey(key) || value.isEmpty() || normalize(value) == null }) return false
        val entries = JSONObject()
        for ((key, tags) in data.toSortedMap()) entries.put(key, JSONArray(tags))
        return store.writeChecked(JSONObject().put("version", 1).put("notes", entries))
    }

    fun tags(key: String): List<String> = synchronized(lock) { if (validKey(key)) read()?.get(key).orEmpty() else emptyList() }

    fun set(key: String, tags: Collection<String>): Boolean = synchronized(lock) {
        if (!validKey(key)) return false
        val canonical = normalize(tags) ?: return false
        val current = read() ?: return false
        if (current[key] == canonical || (canonical.isEmpty() && key !in current)) return true
        if (canonical.isEmpty()) current.remove(key) else current[key] = canonical
        write(current)
    }

    fun find(tag: String): List<String> = synchronized(lock) {
        val needle = normalize(listOf(tag))?.singleOrNull() ?: return emptyList()
        read()?.filterValues { labels -> labels.any { it.equals(needle, ignoreCase = true) } }?.keys?.sorted().orEmpty()
    }

    fun rekeyTree(from: String, to: String): Boolean = synchronized(lock) {
        if (!validKey(from) || !validKey(to)) return false
        val current = read() ?: return false
        val mapped = LinkedHashMap<String, List<String>>()
        for ((key, tags) in current) {
            val target = DocKeys.moved(key, from, to) ?: key
            if (!validKey(target) || mapped.put(target, tags) != null) return false
        }
        if (mapped == current) return true
        write(mapped)
    }

    fun removeTree(root: String): Boolean = synchronized(lock) {
        if (!validKey(root)) return false
        val current = read() ?: return false
        if (current.keys.removeAll { DocKeys.within(it, root) }) write(current) else true
    }
}
