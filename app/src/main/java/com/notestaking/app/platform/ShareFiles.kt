package com.notestaking.app.platform

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/** Files for the share sheet, written alone into the cache directory the FileProvider exposes. */
object ShareFiles {

    /** [bmp] as `[stem].png`, or null when it could not be written. */
    fun png(context: Context, bmp: Bitmap, stem: String): File? =
        CachedImageFiles.create(File(context.cacheDir, "share"), stem) { file ->
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
}
