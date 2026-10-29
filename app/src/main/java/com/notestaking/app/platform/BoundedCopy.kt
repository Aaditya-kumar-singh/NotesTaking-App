package com.notestaking.app.platform

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Stream untrusted provider bytes to disk without unbounded heap or temporary disk usage. */
internal object BoundedCopy {
    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream, maxBytes: Long): Long {
        require(maxBytes > 0)
        val buffer = ByteArray(64 * 1024)
        var copied = 0L
        var emptyReads = 0
        while (true) {
            val size = input.read(buffer)
            if (size < 0) return copied
            if (size == 0) {
                if (++emptyReads > 16) throw IOException("Import stream stalled")
                continue
            }
            emptyReads = 0
            if (size > maxBytes - copied) throw IOException("Imported file exceeds size limit")
            output.write(buffer, 0, size)
            copied += size
        }
    }
}
