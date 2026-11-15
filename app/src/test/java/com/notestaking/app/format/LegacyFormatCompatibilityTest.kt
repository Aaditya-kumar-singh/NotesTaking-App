package com.notestaking.app.format

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.infinite.InfiniteDocument
import com.notestaking.app.core.model.ImageItem
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.model.TextItem
import com.notestaking.app.core.template.TemplateKind
import com.notestaking.app.core.tools.Tool
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Frozen version-1 layouts, without later writer markers: read old -> save new -> reopen, never overwrite old. */
class LegacyFormatCompatibilityTest {
    private val images = byteArrayOf(9, 7, 5, 3, 1)
    private val noteCodec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
    private val canvasCodec = CanvasCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun archive(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                for ((name, data) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    private fun fingerprint(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    @Test fun v1XnoteWithAssetsBookmarksTextAndEditableInkReopensWithoutTouchingSource() {
        // Earlier writers placed manifest first and omitted "writer" and modern layout keys.
        val manifest = """
            {"format":"xnote","version":1,"dpi":150,"has_pdf":false,
             "bookmarks":[{"page":1,"label":"Saved section"}],
             "pages":[
                {"width":400,"height":600,"items":[
                    {"kind":"stroke","tool":"taper","config":{"taper_length":40},
                     "samples":[[10,20,0.5],[100,220,0.75]]},
                    {"kind":"image","asset":"assets/image-000.png",
                     "rect":[0,0,20,30],"src_w":2,"src_h":2}
                ]},
                {"width":400,"height":600,"items":[
                    {"kind":"text","pos":[12,24],"width":200,"text":"Older note text"}
                ]}
             ]}
        """.trimIndent().toByteArray()
        val old = archive("manifest.json" to manifest, "assets/image-000.png" to images)
        val originalSha = fingerprint(old)
        val folder = Files.createTempDirectory("legacy-note").toFile()
        try {
            val original = File(folder, "original.xnote").apply { writeBytes(old) }
            val loaded = original.inputStream().use { noteCodec.read(it, imageDir = folder) }
            assertEquals(2, loaded.pages.size)
            assertEquals(2, loaded.pages[0].items.size)
            val ink = loaded.pages[0].items[0] as Stroke
            assertEquals(Tool.TAPER, ink.tool)
            assertTrue(ink.config.taperEnabled)
            assertEquals(10.0, ink.sampleAt(0).x, 0.01)
            assertArrayEquals(images, (loaded.pages[0].items[1] as ImageItem).image.file.readBytes())
            assertEquals("Older note text", (loaded.pages[1].items.single() as TextItem).text)
            assertEquals("Saved section", loaded.bookmarks.single().label)

            val migratedBytes = ByteArrayOutputStream().also { noteCodec.write(loaded, it) }.toByteArray()
            val reopened = noteCodec.read(ByteArrayInputStream(migratedBytes), imageDir = folder)
            assertEquals(2, reopened.pages.size)
            assertEquals(2, reopened.pages[0].items.size)
            assertEquals("Saved section", reopened.bookmarks.single().label)
            assertEquals("Older note text", (reopened.pages[1].items.single() as TextItem).text)
            assertArrayEquals(images, (reopened.pages[0].items[1] as ImageItem).image.file.readBytes())
            assertTrue((reopened.pages[0].items[0] as Stroke).config.taperEnabled)
            assertArrayEquals(originalSha, fingerprint(original.readBytes()))
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test fun v1XcanvasKeepsWaypointBackgroundStrokeAndStoredImageOnReopen() {
        val manifest = """
            {"format":"xcanvas","version":1,"dpi":150,
             "background":{"pattern":"dots","pattern_color":[10,20,30,220],"spacing":45},
             "view":{"cx":12,"cy":-25,"zoom":2},
             "waypoints":[{"name":"Older view","cx":6,"cy":7,"zoom":1.5}],
             "items":[
                {"kind":"stroke","tool":"pen","samples":[[5,6,1],[12,40,0.5]]},
                {"kind":"image","asset":"assets/image-000.png",
                 "rect":[0,0,20,20],"src_w":2,"src_h":2}
             ]}
        """.trimIndent().toByteArray()
        val old = archive("manifest.json" to manifest, "assets/image-000.png" to images)
        val originalSha = fingerprint(old)
        val folder = Files.createTempDirectory("legacy-canvas").toFile()
        try {
            val original = File(folder, "original.xcanvas").apply { writeBytes(old) }
            val canvas = original.inputStream().use { canvasCodec.read(it, imageDir = folder) }
            assertEquals(2, canvas.itemCount)
            assertEquals(45.0, canvas.background.spacing, 0.001)
            assertEquals(2.0, canvas.lastView!!.zoom, 0.001)
            assertEquals("Older view", canvas.waypoints.single().name)
            assertEquals(Tool.PEN, (canvas.items[0] as Stroke).tool)
            assertArrayEquals(images, (canvas.items[1] as ImageItem).image.file.readBytes())

            val migratedBytes = ByteArrayOutputStream().also { canvasCodec.write(canvas, it) }.toByteArray()
            val reopened: InfiniteDocument = canvasCodec.read(ByteArrayInputStream(migratedBytes), imageDir = folder)
            assertEquals(2, reopened.itemCount)
            assertEquals(45.0, reopened.background.spacing, 0.001)
            assertEquals("Older view", reopened.waypoints.single().name)
            assertEquals(2.0, reopened.lastView!!.zoom, 0.001)
            assertArrayEquals(images, (reopened.items[1] as ImageItem).image.file.readBytes())
            assertArrayEquals(originalSha, fingerprint(original.readBytes()))
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test fun v1XtemplatePlainAndPackagedStayReadableWithIdenticalImageBytes() {
        val json = """{"xtemplate":1,"name":"Old lines","kind":"page",
            "items":[{"type":"hline","y":0}]}"""
        val plain = json.toByteArray(Charsets.UTF_8)
        val folder = Files.createTempDirectory("legacy-template").toFile()
        try {
            val originalPlain = File(folder, "original.xtemplate").apply { writeBytes(plain) }
            val template = TemplateReader.read(originalPlain.readText(Charsets.UTF_8))
            assertEquals("Old lines", template.name)
            assertEquals(TemplateKind.PAGE, template.kind)
            assertEquals(1, template.root.children.size)
            assertArrayEquals(plain, originalPlain.readBytes())

            val packageJson = """{"xtemplate":1,"name":"Old cover","kind":"cover",
                "items":[{"type":"image","src":"images/a.png","fit":"cover"}]}"""
            val packed = archive(
                "template.json" to packageJson.toByteArray(Charsets.UTF_8),
                "images/a.png" to images,
            )
            val original = File(folder, "old-cover.xtemplate").apply { writeBytes(packed) }
            assertTrue(TemplatePackage.isPackage(original))
            assertEquals(packageJson, TemplatePackage.readJson(original))
            val savedCover = TemplateReader.read(TemplatePackage.readJson(original))
            assertEquals(TemplateKind.COVER, savedCover.kind)
            assertEquals(1, savedCover.root.children.size)
            val extracted = File(folder, "picture.png")
            assertTrue(TemplatePackage.extract(original, "images/a.png", extracted))
            assertArrayEquals(images, extracted.readBytes())
            assertArrayEquals(packed, original.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }
}
