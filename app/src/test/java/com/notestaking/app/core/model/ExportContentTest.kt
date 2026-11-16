package com.notestaking.app.core.model

import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.geometry.Rect
import com.notestaking.app.core.shapes.ShapeKind
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-078: selective PDF export must be a read-only view over the existing editable note. */
class ExportContentTest {
    private val color = Rgba(40, 90, 160, 255)
    private val measurer = FakeTextMeasurer()

    private fun fill(): ShapeItem = ShapeItem.fill(
        listOf(listOf(Pt(0.0, 0.0), Pt(25.0, 0.0), Pt(25.0, 25.0))), color,
    )

    private fun contents(): List<CanvasItem> = listOf(
        fill(),
        Stroke(Tool.PEN, ToolConfig(), listOf(Sample(30.0, 40.0, 0.7), Sample(45.0, 60.0, 0.7))),
        ImageItem(ImageData(File("original.png"), 40, 30), Rect(50.0, 40.0, 100.0, 80.0)),
        TextItem(Pt(20.0, 80.0), text = "editable text", measurer = measurer),
        ShapeItem(ShapeKind.RECTANGLE, Pt(130.0, 30.0), Pt(180.0, 80.0), color),
        ConnectorItem(
            Routing.STRAIGHT, ConnectorEnd(Pt(50.0, 50.0)), ConnectorEnd(Pt(150.0, 100.0)),
            color,
        ),
        TapeItem(TapeShape(TapeMode.STRAIGHT, listOf(Pt(10.0, 20.0), Pt(50.0, 20.0)), 10.0), color),
        AudioItem(AudioData(File("original.m4a"), "m4a", 12_345, ByteArray(0)),
            Rect(10.0, 100.0, 120.0, 35.0), "lecture", 4, measurer),
    )

    @Test fun allEightFlagCombinationsKeepOnlyRequestedContentInItsOriginalZOrder() {
        val contents = contents()
        val original = contents.toList()
        for (mask in 0 until 8) {
            val selection = ExportContent(
                strokes = mask and 1 != 0,
                images = mask and 2 != 0,
                text = mask and 4 != 0,
            )
            val actual = contents.forExport(selection)
            val expected = ArrayList<CanvasItem>().apply {
                if (selection.strokes) addAll(contents.subList(0, 2))
                if (selection.images) add(contents[2])
                if (selection.text) add(contents[3])
                if (selection.strokes) addAll(contents.subList(4, 6))
            }
            assertEquals("mask=$mask", expected, actual)
            assertTrue("mask=$mask must keep source item identities", actual.all { item ->
                contents.any { it === item }
            })
            assertFalse("audio pills are never part of PDF output", actual.any { it is AudioItem })
            assertFalse("revision tape is never part of PDF output", actual.any { it is TapeItem })
            assertEquals("filter cannot reorder or modify source", original, contents)
        }
        assertTrue(contents.forExport(ExportContent(false, false, false)).isEmpty())
        assertTrue(emptyList<CanvasItem>().forExport(ExportContent(false, false, false)).isEmpty())
    }

    @Test fun originalNoArgumentExportAndOptInDefaultAreIdentical() {
        val contents = contents()
        val before = contents.forExport()
        assertEquals(before, contents.forExport(ExportContent()))
        val printable = contents.filter { it.exported }
        assertSame("unchanged normal export keeps its allocation-free path",
            printable, printable.forExport())
        assertSame("all categories selected keeps the same list",
            printable, printable.forExport(ExportContent()))
        assertEquals(6, before.size)
    }

    @Test fun filteredPageLayersKeepBackgroundFillBelowOptionalTypedFlowAndInkAboveIt() {
        val contents = contents()
        val expected = contents.take(6)
        for (mask in 0 until 8) {
            val selection = ExportContent(
                strokes = mask and 1 != 0,
                images = mask and 2 != 0,
                text = mask and 4 != 0,
            )
            val painted = ArrayList<Any>()
            PageLayers.paint(
                contents.forExport(selection),
                { if (selection.text) painted.add("typed-flow") },
            ) { painted.add(it) }
            val itemOrder = painted.filterIsInstance<CanvasItem>()
            assertEquals(expected.filter { it in contents.forExport(selection) }, itemOrder)
            assertEquals(selection.text, painted.contains("typed-flow"))
            if (selection.strokes) {
                assertEquals("background fill must paint before typed text", expected.first(), painted.first())
            }
        }
    }

    @Test fun turningOffPaperAloneNeverChangesPrintableItemsOrTheirOrder() {
        val contents = contents()
        for (mask in 0 until 8) {
            val content = ExportContent(
                strokes = mask and 1 != 0,
                images = mask and 2 != 0,
                text = mask and 4 != 0,
            )
            val withPaper = contents.forExport(content)
            val withoutPaper = contents.forExport(content.copy(paper = false))
            assertEquals("mask=$mask", withPaper, withoutPaper)
            assertTrue(withoutPaper.indices.all { i -> withoutPaper[i] === withPaper[i] })
        }
        assertEquals(contents.forExport(), contents.forExport(ExportContent(paper = false)))
    }

    @Test fun metadataFlagsCannotChangePrintableItemsOrTheirIdentity() {
        val contents = contents()
        val unchanged = contents.forExport()
        for (bookmarks in listOf(false, true)) {
            for (markups in listOf(false, true)) {
                val selected = contents.forExport(ExportContent(bookmarks = bookmarks, markups = markups))
                assertEquals(unchanged, selected)
                assertTrue(selected.indices.all { i -> selected[i] === unchanged[i] })
            }
        }
    }

    @Test fun filteringNeitherMarksTheNoteDirtyNorMutatesAudioImageOrInkMetadata() {
        val contents = contents()
        val doc = Document(mutableListOf(Page(600.0, 900.0, contents.toMutableList())))
        val page = doc.pages.single()
        val before = page.items.toList()
        val image = before.filterIsInstance<ImageItem>().single()
        val ink = before.filterIsInstance<Stroke>().single()
        val text = before.filterIsInstance<TextItem>().single()
        val sound = before.filterIsInstance<AudioItem>().single()
        val originalImage = image.image
        val originalAudio = sound.audio
        val initialSamples = ink.samples.toList()
        for (strokes in listOf(true, false)) for (images in listOf(true, false)) {
            for (textVisible in listOf(true, false)) {
                page.items.forExport(ExportContent(strokes, images, textVisible))
            }
        }
        assertFalse(doc.dirty)
        assertEquals(before, page.items)
        assertSame(originalImage, image.image)
        assertSame(originalAudio, sound.audio)
        assertEquals(initialSamples, ink.samples)
        assertEquals("editable text", text.text)
        assertEquals(4, sound.rid)
    }
}
