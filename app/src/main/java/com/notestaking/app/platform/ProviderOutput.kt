package com.notestaking.app.platform

import java.io.IOException
import java.io.OutputStream

/** Never report a document-provider write as successful when it supplied no stream. */
internal object ProviderOutput {
    fun write(stream: OutputStream?, body: (OutputStream) -> Unit) {
        val output = stream ?: throw IOException("Document provider returned no writable stream")
        output.use(body)
    }
}
