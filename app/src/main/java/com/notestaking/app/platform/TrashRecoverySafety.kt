package com.notestaking.app.platform

/** A Trash wrapper is restorable only when it has exactly one non-metadata entry. */
internal object TrashRecoverySafety {
    fun singlePayload(
        children: List<Triple<String, String, Boolean>>,
        infoName: String,
    ): Triple<String, String, Boolean>? =
        children.filterNot { !it.third && it.second == infoName }.singleOrNull()
}
