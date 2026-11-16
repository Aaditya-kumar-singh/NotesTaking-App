package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Deterministic, source-only corpus check; never a substitute for tablet latency measurements. */
class TabletBenchmarkFixtureTest {
    @Test fun twoHundredPageGeometryBaselineRoundTripsWithoutChangingPageOrder() {
        val width = 1240.0
        val height = 1754.0
        val doc = Document(dpi = 150, pages = MutableList(200) { index ->
            Page(width + (index % 2) * 10, height + (index % 3) * 10)
        })
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val out = ByteArrayOutputStream()
        codec.write(doc, out)
        val bytes = out.toByteArray()
        assertTrue(bytes.isNotEmpty())
        val reopened = codec.read(ByteArrayInputStream(bytes))
        assertEquals(200, reopened.pages.size)
        assertEquals(150, reopened.dpi)
        reopened.pages.forEachIndexed { index, page ->
            assertEquals(width + (index % 2) * 10, page.width, 0.0)
            assertEquals(height + (index % 3) * 10, page.height, 0.0)
        }
    }
}
