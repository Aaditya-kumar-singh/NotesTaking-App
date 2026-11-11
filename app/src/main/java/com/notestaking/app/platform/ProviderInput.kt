package com.notestaking.app.platform

import java.io.FileNotFoundException
import java.io.InputStream

/** Require a readable document-provider stream before starting a file copy. */
internal object ProviderInput {
    fun <T> read(stream: InputStream?, body: (InputStream) -> T): T =
        (stream ?: throw FileNotFoundException("Document provider returned no readable stream")).use(body)
}
