package com.notestaking.app.core.model

import com.notestaking.app.core.pdf.Outline
import com.notestaking.app.core.pdf.OutlineNode
import com.notestaking.app.core.pdf.TextQuad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** NP-077: export-only bookmark and markup switches cannot mutate saved notes or page links. */
class ExportMetadataPlanTest {
    private val red = Rgba(210, 30, 50, 255)

    private fun markup(type: MarkupType, id: String, turn: Int = 0) = TextMarkup(
        id, type, red, 0.45,
        listOf(TextQuad(12f, 16f, 42f, 27f, turn)),
        "selected text", "original note", 100L, 200L,
    )

    @Test fun allMarkupKindsRemainInOriginalOrderAndKeepTheirGeometryAndNotes() {
        val originals = MarkupType.entries.mapIndexed { index, type -> markup(type, "mark-$index", index % 4) }
        val first = Page(600.0, 900.0, markups = originals, pdfPage = 5, pdfTurn = 3)
        val blank = Page(600.0, 900.0)
        val doc = Document(mutableListOf(first, blank))
        val before = doc.dirty
        val included = doc.metadataForExport()
        assertEquals(2, included.markups.size)
        assertSame(originals, included.markups[0])
        assertTrue(included.markups[1].isEmpty())
        assertEquals(listOf(5 to 0), included.sourcePageMap.entries.map { it.key to it.value })

        val excluded = doc.metadataForExport(ExportContent(markups = false))
        assertTrue(excluded.markups.all { it.isEmpty() })
        assertEquals(included.sourcePageMap, excluded.sourcePageMap)
        assertSame(originals, first.markups)
        assertEquals(MarkupType.entries.toList(), first.markups.map { it.type })
        assertEquals((0..3).toList(), first.markups.map { it.quads.single().quarter })
        assertEquals(listOf("original note"), first.markups.mapNotNull { it.note }.distinct())
        assertEquals(3, first.pdfTurn)
        assertEquals(5, first.pdfPage)
        assertEquals(before, doc.dirty)
    }

    @Test fun subsetAndReorderedPagesRemapNativeBookmarksByPageIdentity() {
        val a = Page(500.0, 700.0, pdfPage = 0)
        val b = Page(500.0, 700.0, pdfPage = 3, pdfTurn = 2)
        val c = Page(500.0, 700.0, pdfPage = 5)
        val d = Page(500.0, 700.0, pdfPage = 3, pdfTurn = 1)
        val doc = Document(mutableListOf(a, b, c, d))
        doc.bookmarks.addAll(listOf(
            Bookmark(0, "first"),
            Bookmark(1, "second"),
            Bookmark(2, "third"),
            Bookmark(3, "fourth"),
            Bookmark(-1, "invalid-low"),
            Bookmark(4, "invalid-high"),
            Bookmark(Int.MAX_VALUE, "invalid-huge"),
        ))
        val subset = listOf(d, b, b)
        val plan = doc.metadataForExport(exportPages = subset, sourcePdfPageCount = 6)
        assertEquals(listOf(ExportBookmark(1, "second"), ExportBookmark(0, "fourth")), plan.bookmarks)
        assertEquals(mapOf(3 to 0), plan.sourcePageMap)
        assertEquals(3, plan.markups.size)
        assertEquals(2, b.pdfTurn)
        assertEquals(1, d.pdfTurn)
        assertSame(b, doc.pages[1])
        assertSame(d, doc.pages[3])
        assertEquals(listOf(0, 1, 2, 3, -1, 4, Int.MAX_VALUE), doc.bookmarks.map { it.page })

        // Page order can change later; an already prepared export retains stable target positions.
        doc.pages.reverse()
        assertEquals(listOf(ExportBookmark(1, "second"), ExportBookmark(0, "fourth")), plan.bookmarks)
    }

    @Test fun sourcePdfDestinationsSkipInvalidAndOutOfRangeIndicesAndKeepFirstDuplicate() {
        val pages = listOf(
            Page(100.0, 150.0, pdfPage = -1),
            Page(100.0, 150.0, pdfPage = 99),
            Page(100.0, 150.0, pdfPage = 4, pdfTurn = 3),
            Page(100.0, 150.0, pdfPage = 4, pdfTurn = 1),
            Page(100.0, 150.0, pdfPage = 0, pdfTurn = 2),
            Page(100.0, 150.0),
        )
        val doc = Document(pages.toMutableList())
        val plan = doc.metadataForExport(sourcePdfPageCount = 5)
        assertEquals(mapOf(4 to 2, 0 to 4), plan.sourcePageMap)
        assertEquals(6, plan.markups.size)
        assertEquals(mapOf(4 to 2, 0 to 4), doc.metadataForExport().sourcePageMap.filterKeys { it != 99 })
        assertEquals(mapOf(4 to 2, 0 to 4), doc.metadataForExport(sourcePdfPageCount = 5).sourcePageMap)
        assertEquals(emptyMap<Int, Int>(), doc.metadataForExport(sourcePdfPageCount = 0).sourcePageMap)
        assertEquals(3, pages[2].pdfTurn)
        assertEquals(1, pages[3].pdfTurn)
    }

    @Test fun turningOffBookmarksNeverDropsInkOrMarkupAndSwitchesAreIndependent() {
        val first = Page(120.0, 130.0, markups = listOf(markup(MarkupType.HIGHLIGHT, "one")), pdfPage = 8)
        val second = Page(120.0, 130.0, markups = listOf(markup(MarkupType.STRIKEOUT, "two")))
        val doc = Document(mutableListOf(first, second))
        doc.bookmarks.addAll(listOf(Bookmark(0, "A"), Bookmark(1, "B")))

        for (includeBookmarks in listOf(false, true)) {
            for (includeMarkups in listOf(false, true)) {
                val options = ExportContent(
                    strokes = false, images = false, text = false, paper = false,
                    bookmarks = includeBookmarks, markups = includeMarkups,
                )
                val plan = doc.metadataForExport(options)
                assertEquals(includeBookmarks, plan.bookmarks.isNotEmpty())
                assertEquals(includeBookmarks, plan.sourcePageMap.isNotEmpty())
                assertEquals(includeMarkups, plan.markups.any { it.isNotEmpty() })
                assertEquals(2, plan.markups.size)
                assertEquals(if (includeBookmarks) listOf(0, 1) else emptyList<Int>(), plan.bookmarks.map { it.page })
            }
        }
        assertEquals(listOf(Bookmark(0, "A"), Bookmark(1, "B")), doc.bookmarks)
        assertSame(first.markups.single(), doc.pages[0].markups.single())
        assertFalse(doc.dirty)
    }

    @Test fun outlineNodesForMissingDestinationsDropWithoutSynthesizingExternalActions() {
        val doc = Document(mutableListOf(
            Page(120.0, 200.0, pdfPage = 3),
            Page(120.0, 200.0, pdfPage = 8),
            Page(120.0, 200.0, pdfPage = 3),
        ))
        val mapping = doc.metadataForExport(
            exportPages = listOf(doc.pages[2], doc.pages[1]), sourcePdfPageCount = 4,
        ).sourcePageMap
        assertEquals(mapOf(3 to 0), mapping)
        val outline = listOf(
            OutlineNode(8, listOf(OutlineNode(3))),
            OutlineNode(-2),
            OutlineNode(3),
        )
        val exported = Outline.remap(outline) { sourceIndex -> mapping[sourceIndex] }
        assertEquals(listOf(0, 0), exported.map { it.value })
        assertTrue(exported.all { it.kids.isEmpty() })
        assertTrue("core metadata only stores local page indices, never external URI actions",
            exported.all { it.value in 0..1 })
    }

    @Test fun preparedMarkupAndBookmarkSnapshotSurvivesLaterNoteEdits() {
        val original = markup(MarkupType.UNDERLINE, "source")
        val page = Page(200.0, 300.0, markups = listOf(original))
        val doc = Document(mutableListOf(page))
        doc.bookmarks.add(Bookmark(0, "Saved title"))
        val plan = doc.metadataForExport()
        doc.bookmarks.single().label = "Changed title"
        page.markups = listOf(markup(MarkupType.SQUIGGLY, "replacement"))
        assertEquals(listOf(ExportBookmark(0, "Saved title")), plan.bookmarks)
        assertSame(original, plan.markups.single().single())
        assertEquals("Changed title", doc.bookmarks.single().label)
        assertEquals(MarkupType.SQUIGGLY, page.markups.single().type)
        assertEquals(200.0, page.width, 0.0)
        assertEquals(300.0, page.height, 0.0)
    }
}
