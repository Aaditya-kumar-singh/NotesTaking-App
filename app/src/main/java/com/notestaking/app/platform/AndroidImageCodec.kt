package com.notestaking.app.platform

import com.notestaking.app.core.pal.ImageCodec
import com.notestaking.app.core.pal.ImageSize

/** Reads image sizes via the Android framework (spec 01 §3).
 *  Inserted-image decoding goes through [ImageDecoder] (file-backed, on demand), not this codec. */
class AndroidImageCodec : ImageCodec {

    override fun probeFile(path: String): ImageSize? = ImageDecoder.probeFile(path)
}
