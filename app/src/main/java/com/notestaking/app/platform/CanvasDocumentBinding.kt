package com.notestaking.app.platform

import com.notestaking.app.core.infinite.InfiniteDocument

/** Keep the open canvas and its autosave URI attached to a successfully moved SAF file. */
internal object CanvasDocumentBinding {
    fun followMove(
        document: InfiniteDocument?,
        previousUri: String,
        newUri: String,
        newName: String?,
        autosaveUri: String?,
    ): String? {
        if (document == null || document.path != previousUri) return autosaveUri
        document.path = newUri
        if (newName != null) document.displayName = newName
        return if (autosaveUri == previousUri) newUri else autosaveUri
    }
}
