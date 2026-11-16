package com.notestaking.app.ui

import com.notestaking.app.core.model.Rgba
import com.notestaking.app.settings.Preferences
import com.notestaking.app.ui.theme.MaterialColors
import com.notestaking.app.ui.theme.Palette
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * NP-110: original brand resource contracts, kept separate from coordinator-owned art.
 * shortcut: Resource presence and source checks cannot establish visual originality or
 * launcher masking/reduced-motion behavior; verify screenshots and licensing before release.
 */
class BrandArtworkMotionContractTest {
    private fun file(relative: String): File =
        listOf(File(relative), File("../$relative")).firstOrNull { it.isFile }
            ?: error("Missing project file: $relative")

    private fun source(relative: String): String = file(relative).readText(Charsets.UTF_8)
    private fun resource(relative: String) = source("app/src/main/res/$relative")
    private fun ui(relative: String) = source("app/src/main/java/com/notestaking/app/$relative")

    @Test
    fun masterVectorGeneratorAndDisplayedMarkShareOneFoldAndInkGeometry() {
        val master = source("design/brand/notestaking-symbol.svg")
        val generator = source("tools/branding/generate_assets.py")
        val mark = resource("drawable/ic_brand_mark.xml")
        val monochrome = resource("drawable/ic_launcher_monochrome.xml")
        val inkPath = "M39,71 C44,62 50,78 57,70 C63,62 67,60 72,63"
        val paperPath = "M33,24 H63 L79,40 V79 Q79,84 74,84 H33 Q29,84 29,80 V28 Q29,24 33,24 Z"
        for (text in listOf(master, generator, mark, monochrome)) {
            assertTrue(text.contains(inkPath))
            assertTrue(text.contains(paperPath))
        }
        assertTrue(master.contains("viewBox=\"0 0 108 108\""))
        assertTrue(mark.contains("android:viewportWidth=\"108\""))
        assertTrue(monochrome.contains("android:viewportHeight=\"108\""))
        assertTrue(generator.contains("RES = ROOT / \"app\" / \"src\" / \"main\" / \"res\""))
        assertTrue(generator.contains("draw_mark("))
        assertTrue(generator.contains("notestaking-symbol.svg"))
        assertFalse(master.contains("Notein"))
    }

    @Test
    fun adaptiveIconsRoundAndMonochromeUseSharedArtworkInAllAppearances() {
        for (name in listOf("ic_launcher.xml", "ic_launcher_round.xml")) {
            val adaptive = resource("mipmap-anydpi-v26/$name")
            assertTrue(adaptive.contains("@color/ic_launcher_background"))
            assertTrue(adaptive.contains("@drawable/ic_brand_mark"))
            assertTrue(adaptive.contains("@drawable/ic_launcher_monochrome"))
        }
        val colors = resource("values/colors.xml")
        assertTrue(colors.contains("<color name=\"ic_launcher_background\">#17243B</color>"))
        assertTrue(colors.contains("<color name=\"loader_bg\">#FF17243B</color>"))
        assertTrue(resource("values-v31/themes.xml").contains("android:windowSplashScreenAnimatedIcon\">@drawable/ic_brand_mark"))
        assertTrue(resource("values/themes.xml").contains("@color/loader_bg"))
        assertTrue(resource("values/strings.xml").contains("<string name=\"app_name\" translatable=\"false\">NotesTaking App</string>"))

        val about = ui("ui/AboutPane.kt")
        assertTrue(about.contains("Image(painterResource(R.drawable.ic_brand_mark), contentDescription = null"))
        val backstage = ui("ui/Backstage.kt")
        assertTrue(backstage.contains("BrandMark(32.dp)"))
        assertTrue(ui("MainActivity.kt").contains("NotesTakingTheme(ed.palette, ed.cornerStyle)"))
        val palettes = ui("ui/theme/Palette.kt")
        for (setting in listOf("materialLight(m)", "materialDark(m)", "materialOled(m)")) {
            assertTrue(palettes.contains(setting))
        }
        val compose = ui("ui/theme/NotesTakingTheme.kt")
        assertTrue(compose.contains("if (palette.isDark)"))
        assertTrue(compose.contains("lightColorScheme("))
        assertTrue(compose.contains("darkColorScheme("))
    }

    @Test
    fun legacyLauncherDensitiesAndLoaderFramesRemainValidPngs() {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        fun png(path: String) {
            val art = file("app/src/main/res/$path")
            assertTrue("$path missing or empty", art.length() > 100)
            assertArrayEquals("$path not a PNG", signature, art.inputStream().use { it.readNBytes(8) })
        }
        for (dpi in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            for (name in listOf("ic_launcher.png", "ic_launcher_round.png", "ic_launcher_foreground.png")) {
                png("mipmap-$dpi/$name")
            }
        }
        for (frame in 0 until 24) {
            png("drawable-nodpi/notestaking_frame_${frame.toString().padStart(2, '0')}.png")
        }
    }

    @Test
    fun loaderUsesBrandStageWithFiniteFramesButNeedsReducedMotionReview() {
        val loader = resource("drawable/notestaking_loader.xml")
        val frameRefs = Regex("@drawable/notestaking_frame_\\d{2}").findAll(loader).map { it.value }.toList()
        assertEquals(24, frameRefs.size)
        assertEquals(24, frameRefs.distinct().size)
        assertEquals(24, Regex("android:duration=\"67\"").findAll(loader).count())
        assertTrue(loader.contains("android:oneshot=\"false\""))
        val composable = ui("ui/NotesTakingLoader.kt")
        assertTrue(composable.contains("R.drawable.notestaking_loader"))
        assertTrue(composable.contains("colorResource(R.color.loader_bg)"))
        assertTrue(composable.contains("if (!it.isRunning) it.start()"))
        assertTrue(composable.contains("minOf(shortEdge * 0.55f, 288f).dp"))
        val startup = ui("MainActivity.kt")
        assertTrue(startup.contains("com.notestaking.app.ui.NotesTakingLoader()"))
        assertTrue(startup.contains("visible = !ready"))
        // The looping drawable has no explicit system reduced-motion check; flag, do not simulate.
    }

    @Test
    fun iconAttributionAndDefaultPaletteContrastHaveTraceableSources() {
        val notice = source("NOTICE.md")
        assertTrue(notice.contains("derivative of XNotes"))
        assertTrue(notice.contains("MIT"))
        assertTrue(file("LICENSE").length() > 200)
        val icons = ui("ui/icons/NotesTakingIcons.kt")
        assertTrue(icons.contains("Feather/Lucide"))
        assertTrue(icons.contains("ImageVector.Builder("))
        assertTrue(icons.contains("viewportWidth = 24f"))
        assertTrue(icons.contains("viewportHeight = 24f"))
        val scheme = Palette.material(
            "light",
            MaterialColors.seeded(Preferences.DEFAULT_MATERIAL_DUAL, dark = false),
        )
        val night = Palette.material(
            "dark",
            MaterialColors.seeded(Preferences.DEFAULT_MATERIAL_DUAL, dark = true),
        )
        val oled = Palette.material(
            "oled",
            MaterialColors.seeded(Preferences.DEFAULT_MATERIAL_DUAL, dark = true),
        )
        assertFalse(scheme.isDark)
        assertTrue(night.isDark)
        assertTrue(oled.isDark)
        assertEquals(Rgba(0, 0, 0, 255), oled.bg)
        for (palette in listOf(scheme, night, oled)) {
            val c = contrast(palette.text, palette.bg)
            assertTrue("Text/background contrast too low in ${palette.isDark}: $c", c >= 4.45)
        }
    }

    private fun contrast(a: Rgba, b: Rgba): Double {
        fun luminance(color: Rgba): Double {
            fun linear(channel: Int): Double = (channel / 255.0).let {
                if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * linear(color.r) + 0.7152 * linear(color.g) + 0.0722 * linear(color.b)
        }
        val light = luminance(a)
        val dark = luminance(b)
        return (max(light, dark) + 0.05) / (min(light, dark) + 0.05)
    }
}
