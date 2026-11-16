package com.notestaking.app.ui

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Orientation
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.PagePattern
import com.notestaking.app.core.model.PageSize
import com.notestaking.app.core.model.PageStyle
import com.notestaking.app.core.model.PageTemplates
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.resolvedAccentColor
import com.notestaking.app.core.model.resolvedPageColor
import com.notestaking.app.core.model.resolvedPatternColor
import com.notestaking.app.core.model.resolvedSpacing
import com.notestaking.app.core.model.resolvedTemplate
import com.notestaking.app.format.DocumentCodec
import com.notestaking.app.settings.Preferences
import com.notestaking.app.settings.Settings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NP-009: read-only coordinator UI contracts plus real native style/size round trips.
 * shortcut: Source and codec checks do not establish pixel-perfect visual parity;
 * perform NT-013 screenshots with a phone/tablet before accepting the preview.
 */
class PaperCoverPreviewContractTest {
    private fun source(relative: String): String {
        val path = "src/main/java/com/notestaking/app/$relative"
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing preview source: $path" }
        return file.readText(Charsets.UTF_8)
    }

    private fun section(code: String, begin: String, end: String): String {
        val i = code.indexOf(begin)
        assertTrue("Missing source anchor: $begin", i >= 0)
        val j = code.indexOf(end, i + begin.length)
        assertTrue("Missing source boundary: $end", j > i)
        return code.substring(i, j)
    }

    private val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    private fun reopen(doc: Document): Document {
        val data = ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
        assertTrue("Native note file is not empty", data.isNotEmpty())
        return codec.read(ByteArrayInputStream(data))
    }

    /** Constructs the same page/cover layout as Editor.createNoteFile without SAF. */
    private fun createdNote(mm: Pair<Double, Double>, pageStyle: PageStyle, coverStyle: PageStyle?): Document {
        val dpi = PageSize.DEFAULT_DPI
        val w = PageSize.mmToPx(mm.first, dpi)
        val h = PageSize.mmToPx(mm.second, dpi)
        val doc = Document.blankPixels(width = w, height = h, dpi = dpi)
        doc.style = pageStyle
        coverStyle?.let { style ->
            doc.pages.add(0, Page(w, h, style = style, margins = PageMargins(0.0, 0.0, 0.0, 0.0)).also { it.cover = true })
        }
        return doc
    }

    @Test
    fun previewDimensionsMatchReopenedA4A5AndCustomPortraitLandscape() {
        val options = listOf(
            PageSize.A4.shortMm to PageSize.A4.longMm,
            PageSize.A4.longMm to PageSize.A4.shortMm,
            PageSize.A5.shortMm to PageSize.A5.longMm,
            PageSize.A5.longMm to PageSize.A5.shortMm,
            172.0 to 238.0,
            238.0 to 172.0,
        )
        for (dimensions in options) {
            val previewRatio = dimensions.second / dimensions.first
            for (withCover in listOf(false, true)) {
                val doc = createdNote(
                    dimensions,
                    PageStyle(template = PagePattern.LINES.id),
                    PageStyle(template = PageTemplates.NONE).takeIf { withCover },
                )
                val restored = reopen(doc)
                val expectedPages = if (withCover) 2 else 1
                assertEquals(expectedPages, restored.pages.size)
                assertEquals(withCover, restored.pages.first().cover)
                for (page in restored.pages) {
                    assertEquals(dimensions.first, PageSize.pxToMm(page.width, restored.dpi), 1e-8)
                    assertEquals(dimensions.second, PageSize.pxToMm(page.height, restored.dpi), 1e-8)
                    assertEquals(previewRatio, page.height / page.width, 1e-10)
                }
            }
        }
    }

    @Test
    fun selectedRuledGridDotsAndPlainPaperPersistAfterReopen() {
        val paper = Rgba(246, 240, 222, 255)
        val ink = Rgba(80, 90, 110, 140)
        val accent = Rgba(205, 90, 40, 190)
        for (key in listOf(PageTemplates.NONE, PagePattern.LINES.id, PagePattern.GRID.id, PagePattern.DOTS.id)) {
            val style = PageStyle(
                pageColor = paper, template = key,
                patternColor = ink, accentColor = accent, spacing = 58.0,
            )
            val doc = reopen(createdNote(PageSize.A5.shortMm to PageSize.A5.longMm, style, null))
            assertEquals(style, doc.style)
            assertEquals(key, doc.pages.single().resolvedTemplate(doc))
            assertEquals(paper, doc.pages.single().resolvedPageColor(doc, null))
            assertEquals(ink, doc.pages.single().resolvedPatternColor(doc))
            assertEquals(accent, doc.pages.single().resolvedAccentColor(doc))
            assertEquals(58.0, doc.pages.single().resolvedSpacing(doc)!!, 1e-8)
        }
    }

    @Test
    fun coverKeepsItsOwnPatternAndPaperWhilePagesKeepTheirChosenStyle() {
        val pageStyle = PageStyle(
            pageColor = Rgba(230, 234, 242),
            template = PagePattern.GRID.id,
            spacing = 44.0,
        )
        val coverStyle = PageStyle(
            pageColor = Rgba(25, 35, 70),
            template = PagePattern.DOTS.id,
            patternColor = Rgba(240, 180, 70, 210),
        )
        val doc = reopen(createdNote(PageSize.A4.shortMm to PageSize.A4.longMm, pageStyle, coverStyle))
        assertTrue(doc.pages[0].cover)
        assertFalse(doc.pages[1].cover)
        assertEquals(coverStyle, doc.pages[0].style)
        assertEquals(pageStyle, doc.style)
        assertEquals(PagePattern.DOTS.id, doc.pages[0].resolvedTemplate(doc))
        assertEquals(PagePattern.GRID.id, doc.pages[1].resolvedTemplate(doc))
        assertEquals(coverStyle.pageColor, doc.pages[0].resolvedPageColor(doc, null))
        assertEquals(pageStyle.pageColor, doc.pages[1].resolvedPageColor(doc, null))
        assertEquals(PageMargins(0.0, 0.0, 0.0, 0.0), doc.pages[0].margins)
    }

    @Test
    fun newNoteDefaultsRoundTripForPreviewAfterRestart() {
        val initial = Settings(
            newNoteStyle = PageStyle(
                pageColor = Rgba(238, 238, 223),
                template = PagePattern.DOTS.id,
                patternColor = Rgba(70, 80, 100, 165),
                spacing = 52.0,
            ),
            prefs = Preferences(
                defaultPageSize = PageSize.CUSTOM,
                defaultPageOrientation = Orientation.LANDSCAPE,
                customPageWidthMm = 238.0,
                customPageHeightMm = 172.0,
            ),
        )
        val restored = Settings.fromJson(initial.toJson())
        assertEquals(initial.newNoteStyle, restored.newNoteStyle)
        assertEquals(PageSize.CUSTOM, restored.prefs.defaultPageSize)
        assertEquals(Orientation.LANDSCAPE, restored.prefs.defaultPageOrientation)
        assertEquals(238.0, restored.prefs.customPageWidthMm, 1e-8)
        assertEquals(172.0, restored.prefs.customPageHeightMm, 1e-8)
    }

    @Test
    fun createAndFullPreviewShareStyleSizeAndTemplateInputs() {
        val picker = source("ui/NewNoteScreen.kt")
        val format = section(picker, "val pageMm = when {", "fun create() {")
        assertTrue(format.contains("custom -> (cw ?: prefs.customPageWidthMm) to (ch ?: prefs.customPageHeightMm)"))
        assertTrue(format.contains("orientation == Orientation.PORTRAIT -> size.shortMm to size.longMm"))
        assertTrue(format.contains("else -> size.longMm to size.shortMm"))
        assertTrue(format.contains("val sizeOk = !custom || (cw != null && ch != null)"))
        assertTrue(format.contains("val coverPaper = coverStyle.pageColor ?: look.paper"))
        assertTrue(format.contains("val hasCover = coverOn && coverT != null"))
        val create = section(picker, "fun create() {", "fun setOrientation(o: Orientation)")
        assertTrue(create.contains("if (!canCreate) return"))
        assertTrue(create.contains("Editor.NewNoteSetup(pageMm.first, pageMm.second, style, coverStyle.takeIf { hasCover })"))
        val preview = section(picker, "fun Sheets(maxW: Dp, maxH: Dp)", "fun ColourRow(")
        assertTrue(preview.contains("val aspect = (pageMm.second / pageMm.first).toFloat()"))
        assertTrue(preview.contains("Thumb(coverT, coverKey!!, coverStyle.settings, coverPaper, w, h, keep = false)"))
        assertTrue(preview.contains("Thumb(pageT, pageKey, style.settings, look.paper, pw, ph, keep = false)"))
        assertTrue(picker.contains("TemplateDrawing.of(t, own, null, paper, DPI)"))
        assertTrue(picker.contains("TemplateThumbs.Spec(key, pageMm, d.look.ink, d.look.accent, paper"))
        val editor = source("ui/Editor.kt")
        val saved = section(editor, "fun createNoteFile(treeUri: String, parentDocId: String, rawName: String, setup: NewNoteSetup)", "/** Imports the PDF")
        assertTrue(saved.contains("PageSize.mmToPx(setup.widthMm, dpi)"))
        assertTrue(saved.contains("PageSize.mmToPx(setup.heightMm, dpi)"))
        assertTrue(saved.contains("doc.style = setup.style"))
        assertTrue(saved.contains("Page(w, h, style = style, margins = PageMargins(0.0, 0.0, 0.0, 0.0)).also { it.cover = true }"))
        assertTrue(saved.contains("TemplateLibrary.embed(doc, setup.style.template, imageDir)"))
        assertTrue(saved.contains("TemplateLibrary.embed(doc, style.template, imageDir)"))
    }

    @Test
    fun invalidDimensionsCancelAndSelectionAccessibilityHaveSourceGuards() {
        val picker = source("ui/NewNoteScreen.kt")
        val format = section(picker, "val custom = size == PageSize.CUSTOM", "fun create() {")
        assertTrue(format.contains("PageSize.CUSTOM_MIN_MM..PageSize.CUSTOM_MAX_MM"))
        assertTrue(format.contains("val canCreate = !busy && sizeOk"))
        val button = section(picker, "Button(", "HorizontalDivider(color = border)")
        assertTrue(button.contains("enabled = !busy && (if (coverOnly) hasCover else sizeOk)"))
        assertTrue(picker.contains("Dialog(onDismissRequest = onClose"))
        assertTrue(picker.contains("IconButton(onClick = onClose)"))
        assertTrue(picker.contains(".semantics { contentDescription = label }"))
        assertTrue(picker.contains(".semantics { contentDescription = desc; selected = on }"))
        assertTrue(picker.contains(".semantics { this.selected = selected }"))
        assertTrue(picker.contains("Image(bmp, contentDescription = t.name"))
        assertTrue(picker.contains("stringResource(R.string.none), pageKey == PageTemplates.NONE"))
        assertTrue(picker.contains("coverOn = false"))
        val home = source("ui/Backstage.kt")
        val createDialog = section(home, "if (createMode == CreateMode.FILE && pendingImport == null", "// Name entry for a new canvas")
        assertTrue(createDialog.contains("onClose = { fieldError = null; onCreateMode(CreateMode.NONE) }"))
        assertTrue(createDialog.contains("if (uri != null) {"))
        assertTrue(createDialog.contains("fieldError = resources.getString(R.string.err_create_note)"))
    }

    @Test
    fun previewThumbnailUsesSameTemplateLayoutButBoostsGalleryInkForReadability() {
        val thumb = source("ui/TemplatePicker.kt")
        val drawing = section(thumb, "internal object TemplateThumbs {", "/** The effective colours a style level draws its template with.")
        assertTrue(drawing.contains("ink.copy(a = max(ink.a, 170))"))
        assertTrue(drawing.contains("accent.copy(a = max(accent.a, 190))"))
        assertTrue(drawing.contains("TemplateLibrary.layouts.layout(t, TemplatePage(w, h"))
        assertTrue(drawing.contains("TemplatePainter.paint(AndroidRenderer(c), out"))
        assertTrue(drawing.contains("if (keep) synchronized(cache) { cache[s] = img }"))
        val newNote = source("ui/NewNoteScreen.kt")
        assertTrue(newNote.contains("if (tile) {"))
        assertTrue(newNote.contains("TemplateThumbs.spec(key, pageMm, d, thumbPx(w))"))
        assertTrue(newNote.contains("TemplateThumbs.Spec(key, pageMm, d.look.ink, d.look.accent, paper"))
    }
}
