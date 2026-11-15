package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.history.History
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** NP-046: only opted-in, mode-permitted PDF annotations may be erased. */
class PdfMarkupEraseOptInTest {
    private val quad = listOf(TextQuad(40f, 70f, 160f, 120f, 0))

    private fun markup(id: String, type: MarkupType, quads: List<TextQuad> = quad) = TextMarkup(
        id, type, Rgba(220, 130, 25, 255), 0.7, quads, id, null, 1L, 1L,
    )

    private fun fourMarkups() = listOf(
        markup("a", MarkupType.UNDERLINE),
        markup("b", MarkupType.HIGHLIGHT),
        markup("c", MarkupType.STRIKEOUT),
        markup("d", MarkupType.SQUIGGLY),
        markup("e", MarkupType.HIGHLIGHT),
    )

    private fun fixture(
        pages: List<Page>,
        mode: EraseMode,
        optIn: Boolean,
        rotation: Int = 0,
        zoom: Double = 1.0,
        vertical: Boolean = false,
        source: File = File("pdf-source-must-not-be-written.pdf"),
    ): Triple<CanvasState, InteractionController, History> {
        val doc = Document(pages.toMutableList(), dpi = 72, pdfFile = source)
        val state = CanvasState(doc, FakeSurfaceFactory(), Palette.DEFAULT).apply {
            viewportW = 1000
            viewportH = 1300
            verticalScroll = vertical
            viewingMode = ViewingMode.SINGLE
            rotationDeg = rotation
            this.zoom = zoom
            relayout()
            goToPage(pages.lastIndex)
            runAsync = {} // only model/background scheduling; no PDF bytes or Android work
        }
        val history = History()
        val controller = InteractionController(
            state, history, FakeTextMeasurer(), requestRender = {},
        ).apply {
            setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
                eraseMode = mode, eraseMarkups = optIn, baseWidth = 30.0,
            ))
        }
        return Triple(state, controller, history)
    }

    private fun hit(state: CanvasState, pageIndex: Int, x: Double = 100.0, y: Double = 95.0): Pt =
        state.contentToViewport(state.fromPageSpace(pageIndex, Pt(x, y)))

    private fun sweep(
        state: CanvasState,
        controller: InteractionController,
        pageIndex: Int,
        x: Double = 100.0,
        y: Double = 95.0,
    ) {
        val pos = hit(state, pageIndex, x, y)
        controller.beginErase(pos.x, pos.y)
        controller.endErase()
    }

    @Test fun optOutNeverChangesPdfMarkupOrHistoryInAnyEraserMode() {
        for (mode in EraseMode.entries) {
            val page = Page(600.0, 600.0, pdfPage = 7).apply { markups = fourMarkups() }
            val before = page.markups
            val (state, controller, history) = fixture(listOf(page), mode, optIn = false)
            val pdf = state.document.pdfFile
            sweep(state, controller, 0)

            assertEquals("mode=$mode opted out", before, page.markups)
            assertFalse("no empty markup edit should enter undo: $mode", history.canUndo)
            assertEquals(7, page.pdfPage)
            assertSame(pdf, state.document.pdfFile)
            assertFalse("no changes when nothing else is drawn", state.document.dirty)
        }
    }

    @Test fun optInErasesOnlyTypesAllowedByTheSelectedEraserMode() {
        for (mode in EraseMode.entries) {
            val page = Page(600.0, 600.0, pdfPage = 11).apply { markups = fourMarkups() }
            val original = page.markups
            val (state, controller, history) = fixture(listOf(page), mode, optIn = true)
            val pdf = state.document.pdfFile
            sweep(state, controller, 0)

            val expected = when (mode) {
                EraseMode.STROKE, EraseMode.AREA -> emptyList()
                EraseMode.MARKER -> original.filter { it.type != MarkupType.HIGHLIGHT }
                EraseMode.TAPE, EraseMode.SHAPE, EraseMode.CONNECTOR -> original
            }
            assertEquals("type filter: $mode", expected, page.markups)
            assertEquals(11, page.pdfPage)
            assertSame(pdf, state.document.pdfFile)
            if (expected.size == original.size) {
                assertFalse("disallowed mode cannot erase PDF markups: $mode", history.canUndo)
            } else {
                assertTrue("allowed markup erase is undoable: $mode", history.canUndo)
                history.undo()
                assertEquals("Undo restores exact markup order: $mode", original, page.markups)
                assertFalse("one gesture, one undo step", history.canUndo)
                history.redo()
                assertEquals("Redo restores exact selection: $mode", expected, page.markups)
            }
        }
    }

    @Test fun optInIsFrozenForWholeDragEvenWhenSettingsChange() {
        for (start in listOf(false, true)) {
            val page = Page(600.0, 600.0, pdfPage = 4).apply {
                markups = listOf(markup("highlight", MarkupType.HIGHLIGHT))
            }
            val original = page.markups
            val (state, controller, history) = fixture(listOf(page), EraseMode.MARKER, start)
            val away = hit(state, 0, 280.0, 300.0)
            val target = hit(state, 0)
            controller.beginErase(away.x, away.y)
            controller.setToolConfig(Tool.ERASER, ToolDefaults.configFor(Tool.ERASER).copy(
                eraseMode = EraseMode.STROKE, eraseMarkups = !start, baseWidth = 30.0,
            ))
            controller.eraseAlong(target.x, target.y)
            controller.endErase()

            assertEquals("pen-down opt-in=$start must remain authoritative",
                if (start) emptyList() else original, page.markups)
            assertEquals(start, history.canUndo)
            if (start) {
                history.undo()
                assertEquals(original, page.markups)
                history.redo()
                assertTrue(page.markups.isEmpty())
            }
            assertEquals(4, page.pdfPage)
        }
    }

    @Test fun optedInMarkupErasureIsPageLocalUnderRotationZoomAndMargins() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (zoom in listOf(0.5, 1.0, 2.5)) {
                for (vertical in listOf(false, true)) {
                    val neighbour = Page(600.0, 600.0, pdfPage = 1).apply {
                        markups = fourMarkups()
                    }
                    val target = Page(600.0, 600.0, pdfPage = 2).apply {
                        markups = fourMarkups()
                        margins = PageMargins(left = 0.10, top = 0.10, right = 0.05)
                    }
                    val originalNeighbour = neighbour.markups
                    val originalTarget = target.markups
                    val (state, controller, history) = fixture(
                        listOf(neighbour, target), EraseMode.MARKER, true, rotation, zoom, vertical,
                    )
                    sweep(state, controller, 1)
                    assertEquals("rotation=$rotation zoom=$zoom vertical=$vertical",
                        originalTarget.filter { it.type != MarkupType.HIGHLIGHT }, target.markups)
                    assertEquals("other page unaffected", originalNeighbour, neighbour.markups)
                    assertTrue(history.canUndo)
                    history.undo()
                    assertEquals(originalTarget, target.markups)
                    assertEquals(originalNeighbour, neighbour.markups)
                    history.redo()
                    assertEquals(3, target.markups.size)
                    assertEquals(1, neighbour.pdfPage)
                    assertEquals(2, target.pdfPage)
                }
            }
        }
    }

    @Test fun sourcePdfBytesAreNeverRewrittenByMarkupEraseAndHistory() {
        val source = File.createTempFile("np046-pdf-source-", ".pdf")
        val sentinel = "%PDF-1.4\nunchanged-source-bytes".toByteArray()
        try {
            source.writeBytes(sentinel)
            val page = Page(600.0, 600.0, pdfPage = 3).apply { markups = fourMarkups() }
            val (state, controller, history) = fixture(
                listOf(page), EraseMode.STROKE, true, source = source,
            )
            sweep(state, controller, 0)
            assertTrue(page.markups.isEmpty())
            assertSame(source, state.document.pdfFile)
            assertArrayEquals(sentinel, source.readBytes())
            history.undo()
            assertArrayEquals(sentinel, source.readBytes())
            history.redo()
            assertArrayEquals(sentinel, source.readBytes())
            assertEquals(3, page.pdfPage)
        } finally {
            source.delete()
        }
    }

    @Test fun settingIsNotRestoredByUndoButMarkupContentIs() {
        val page = Page(600.0, 600.0, pdfPage = 0).apply { markups = fourMarkups() }
        val before = page.markups
        val (state, controller, history) = fixture(listOf(page), EraseMode.MARKER, true)
        sweep(state, controller, 0)
        assertEquals(3, page.markups.size)

        controller.setToolConfig(Tool.ERASER, controller.configFor(Tool.ERASER).copy(
            eraseMarkups = false,
        ))
        history.undo()
        assertEquals(before, page.markups)
        assertFalse(controller.configFor(Tool.ERASER).eraseMarkups)
        history.redo()
        assertEquals(3, page.markups.size)
        assertFalse(controller.configFor(Tool.ERASER).eraseMarkups)
    }
}
