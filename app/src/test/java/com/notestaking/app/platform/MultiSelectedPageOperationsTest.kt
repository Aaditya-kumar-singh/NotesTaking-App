package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.AddPage
import com.notestaking.app.core.history.CompositeCommand
import com.notestaking.app.core.history.DeletePage
import com.notestaking.app.core.history.MovePages
import com.notestaking.app.core.history.ReshapePages
import com.notestaking.app.core.model.Bookmark
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageCargo
import com.notestaking.app.core.model.PageShape
import com.notestaking.app.core.model.PageTurn
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.Stroke
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.core.stroke.Sample
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolDefaults
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MultiSelectedPageOperationsTest {
    private val measurer = FakeTextMeasurer()

    private fun stroke(x: Double) = Stroke(
        Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        mutableListOf(Sample(x, 20.0, 0.7), Sample(x + 10, 30.0, 0.8)),
    )

    private fun markup() = TextMarkup(
        "picked", MarkupType.HIGHLIGHT, Rgba(255, 100, 0, 255), 0.6,
        listOf(TextQuad(10f, 20f, 50f, 30f, 0)), "selected", null, 1L, 2L,
    )

    @Test fun copyingNonAdjacentSelectionDuplicatesOnlyChosenPagesAndUndoesAsOneStep() {
        val doc = Document.blank(count = 5)
        val original = doc.pages.toList()
        doc.pages[1].items.add(stroke(10.0))
        doc.pages[2].items.add(stroke(20.0))
        doc.pages[3].items.add(stroke(30.0))
        doc.bookmarks.add(Bookmark(2, "leave alone"))
        val picked = PageSelectionSafety.checkedIndices(doc.pages.size, listOf(3, 1, 3))!!
        assertEquals(listOf(1, 3), picked)
        val copied = PageCargo.pack(doc, picked, measurer).unpackHome(doc, measurer)
        assertEquals(2, copied.size)
        val commands = copied.mapIndexed { n, page ->
            AddPage(doc, page, 4 + n).also { it.redo() }
        }
        val edit = CompositeCommand(commands)
        assertEquals(original.take(4) + copied + original.takeLast(1), doc.pages)
        assertEquals(listOf(2), doc.bookmarks.map { it.page })
        val clonedInk = copied[0].items.single() as Stroke
        assertNotSame(original[1].items.single(), clonedInk)
        clonedInk.setSamples(listOf(Sample(99.0, 20.0, 0.7)))
        assertEquals(10.0, (original[1].items.single() as Stroke).sampleAt(0).x, 1e-6)
        assertEquals(20.0, (original[2].items.single() as Stroke).sampleAt(0).x, 1e-6)
        edit.undo()
        assertEquals(original, doc.pages)
        assertEquals(listOf(2), doc.bookmarks.map { it.page })
        edit.redo()
        assertEquals(original.take(4) + copied + original.takeLast(1), doc.pages)
    }

    @Test fun cuttingNonAdjacentSelectionKeepsCoverAndRestoresPagesAndBookmarksOnUndo() {
        val doc = Document.blank(count = 5)
        doc.pages[0].cover = true
        val original = doc.pages.toList()
        original[1].items.add(stroke(11.0))
        original[3].items.add(stroke(33.0))
        doc.bookmarks.addAll(listOf(Bookmark(1, "cut 1"), Bookmark(2, "keep"), Bookmark(3, "cut 3")))
        val picked = PageSelectionSafety.checkedIndices(doc.pages.size, listOf(3, 1, 3))!!
        val clipboard = PageCargo.pack(doc, picked, measurer)
        val deletes = picked.asReversed().map { index ->
            DeletePage(doc, original[index], index).also { it.redo() }
        }
        val edit = CompositeCommand(deletes)
        assertEquals(listOf(original[0], original[2], original[4]), doc.pages)
        assertTrue(doc.pages.first().cover)
        assertEquals(listOf(Bookmark(1, "keep")), doc.bookmarks)
        assertEquals(2, clipboard.size)
        assertEquals(listOf("cut 1", "cut 3"), clipboard.bookmarks.map { it.label })
        val landed = clipboard.unpackHome(doc, measurer)
        assertEquals(11.0, (landed[0].items.single() as Stroke).sampleAt(0).x, 1e-6)
        assertEquals(33.0, (landed[1].items.single() as Stroke).sampleAt(0).x, 1e-6)
        edit.undo()
        assertEquals(original, doc.pages)
        assertEquals(listOf(Bookmark(1, "cut 1"), Bookmark(2, "keep"), Bookmark(3, "cut 3")), doc.bookmarks)
        edit.redo()
        assertEquals(listOf(original[0], original[2], original[4]), doc.pages)
    }

    @Test fun rotatingTwoSelectedPdfPagesLeavesUnselectedPageAndItsInkUnchanged() {
        val doc = Document.blank(count = 4, dpi = 72)
        val first = doc.pages[1]
        val other = doc.pages[2]
        val last = doc.pages[3]
        first.pdfPage = 2
        last.pdfPage = 5
        first.width = 200.0
        first.height = 100.0
        last.width = 200.0
        last.height = 100.0
        first.markups = listOf(markup())
        other.items.add(stroke(50.0))
        val picked = listOf(first, last)
        val before = picked.map { PageShape.of(it) }
        picked.forEach { PageTurn.turn(it, doc, 1) }
        val edit = ReshapePages(picked, before, picked.map { PageShape.of(it) })
        assertEquals(100.0, first.width, 1e-6)
        assertEquals(100.0, last.width, 1e-6)
        assertEquals(1, first.pdfTurn)
        assertEquals(1, last.pdfTurn)
        assertEquals(0, other.pdfTurn)
        assertEquals(50.0, (other.items.single() as Stroke).sampleAt(0).x, 1e-6)
        edit.undo()
        assertEquals(TextQuad(10f, 20f, 50f, 30f, 0), first.markups.single().quads.single())
        assertEquals(0, first.pdfTurn)
        assertEquals(0, last.pdfTurn)
        edit.redo()
        assertEquals(TextQuad(70f, 10f, 80f, 50f, 1), first.markups.single().quads.single())
        assertEquals(0, other.pdfTurn)
    }

    @Test fun selectedSaveAsSubsetUsesDocumentOrderWithoutMutatingUnselectedSource() {
        val doc = Document.blank(count = 5)
        doc.pages.forEachIndexed { index, page ->
            page.width = 100.0 + index
            page.items.add(stroke(index * 10.0))
        }
        val source = doc.pages.toList()
        val subset = ExportPageSelection.resolve(source, listOf(4, 1, 4))
        assertSame(source[1], subset[0])
        assertSame(source[4], subset[1])
        val output = Document(dpi = doc.dpi).also { it.pages.addAll(subset) }
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val saved = ByteArrayOutputStream().also { codec.write(output, it) }.toByteArray()
        val reopened = codec.read(ByteArrayInputStream(saved))
        assertEquals(listOf(101.0, 104.0), reopened.pages.map { it.width })
        assertEquals(listOf(10.0, 40.0), reopened.pages.map {
            (it.items.single() as Stroke).sampleAt(0).x
        })
        assertEquals(source, doc.pages)
        assertEquals(5, doc.pages.size)
        assertEquals(20.0, (doc.pages[2].items.single() as Stroke).sampleAt(0).x, 1e-6)
    }

    @Test fun selectedPageIdentitiesSurviveReorderButDisappearOnDeleteAndExplicitClear() {
        val doc = Document.blank(count = 5)
        val source = doc.pages.toList()
        val selected = mutableListOf(source[1], source[3])
        val move = MovePages(doc, source, MovePages.order(source, selected, 5))
        move.redo()
        PageSelectionSafety.prune(selected, doc.pages)
        assertEquals(listOf(source[1], source[3]), selected)
        val nowSelected = doc.pages.mapIndexedNotNull { i, p ->
            i.takeIf { selected.any { it === p } }
        }
        assertEquals(listOf(3, 4), nowSelected)
        DeletePage(doc, source[1], 3).redo()
        PageSelectionSafety.prune(selected, doc.pages)
        assertEquals(listOf(source[3]), selected)
        selected.clear()
        assertTrue(selected.isEmpty())
        assertFalse(source[2] in selected)
    }
}
