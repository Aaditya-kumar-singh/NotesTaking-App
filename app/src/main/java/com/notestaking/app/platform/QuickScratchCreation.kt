package com.notestaking.app.platform

import java.io.IOException

/** A Quick Scratch request must not create a file without a selected writable SAF folder. */
internal object QuickScratchCreation {
    fun create(treeUri: String?, parentDocId: String?, write: (String, String) -> String?): String? {
        if (treeUri.isNullOrBlank() || parentDocId.isNullOrBlank()) return null
        return try {
            write(treeUri, parentDocId)
        } catch (_: SecurityException) {
            null // permission revoked while the picker/folder was open
        } catch (_: IllegalArgumentException) {
            null // stale or invalid provider/tree URI
        } catch (_: IOException) {
            null // SAF provider refused the operation
        }
    }
}
