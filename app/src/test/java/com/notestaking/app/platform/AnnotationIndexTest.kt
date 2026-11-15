package com.notestaking.app.platform

import com.notestaking.app.core.FakeImageCodec
import com.notestaking.app.core.FakeTextMeasurer
import com.notestaking.app.core.history.AddMarkups
import com.notestaking.app.core.history.DeletePage
import com.notestaking.app.core.history.MovePages
import com.notestaking.app.core.history.RemoveMarkup
import com.notestaking.app.core.history.ReplaceMarkup
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.MarkupType
import com.notestaking.app.core.model.Rgba
import com.notestaking.app.core.model.TextMarkup
import com.notestaking.app.core.pdf.TextQuad
import com.notestaking.app.format.DocumentCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AnnotationIndexTest {
    private fun markup(id: String, type: MarkupType = MarkupType.HIGHLIGHT, note: String? = null) =
        TextMarkup(
            id, type, Rgba(255, 200, 0, 255), 0.55,
            listOf(TextQuad(10f, 20f, 70f, 32f, 0)),
            "Selected text for $id", note, 100L, 200L,
        )

    @Test fun annotationsAreOrderedByDocumentPageAndIncludeAllFourMarkupTypes() {
        val doc = Document.blank(count = 4)
        val under = markup("under", MarkupType.UNDERLINE)
        val high = markup("high", MarkupType.HIGHLIGHT, "first note")
        val strike = markup("strike", MarkupType.STRIKEOUT)
        val squiggle = markup("squiggle", MarkupType.SQUIGGLY)
        doc.pages[1].markups = listOf(high, under)
        doc.pages[3].markups = listOf(strike, squiggle)

        val all = AnnotationIndex.list(doc.pages)
        assertEquals(listOf("high", "under", "strike", "squiggle"), all.map { it.markup.id })
        assertEquals(listOf(1, 1, 3, 3), all.map { it.index })
        assertSame(doc.pages[1], all.first().page)
        assertSame(high, all.first().markup)
        assertEquals("first note", all.first().markup.note)
        assertEquals(listOf(high, under), AnnotationIndex.list(doc.pages, 1).map { it.markup })
        assertEquals(listOf(strike, squiggle), AnnotationIndex.list(doc.pages, 3).map { it.markup })
        assertTrue(AnnotationIndex.list(doc.pages, 0).isEmpty())
        assertTrue(AnnotationIndex.list(doc.pages, -1).isEmpty())
        assertTrue(AnnotationIndex.list(doc.pages, 4).isEmpty())
        assertEquals(4, AnnotationIndex.list(doc.pages).size)
    }

    @Test fun staleAnnotationRowCannotJumpAfterMarkupReplacementEvenIfItsIdIsUnchanged() {
        val doc = Document.blank(count = 2)
        val original = markup("same-id")
        val replacement = original.copy(note = "Edited note", modified = 300L)
        val add = AddMarkups(doc.pages[1], listOf(original))
        add.redo()
        val entry = AnnotationIndex.list(doc.pages).single()
        assertEquals(1, AnnotationIndex.resolve(doc.pages, entry))

        val edit = ReplaceMarkup(doc.pages[1], original, replacement)
        edit.redo()
        assertNull(AnnotationIndex.resolve(doc.pages, entry))
        assertEquals("Edited note", AnnotationIndex.list(doc.pages).single().markup.note)
        edit.undo()
        assertEquals(1, AnnotationIndex.resolve(doc.pages, entry))

        val remove = RemoveMarkup(doc.pages[1], original)
        remove.redo()
        assertTrue(AnnotationIndex.list(doc.pages).isEmpty())
        assertNull(AnnotationIndex.resolve(doc.pages, entry))
        remove.undo()
        assertEquals(1, AnnotationIndex.resolve(doc.pages, entry))
        add.undo()
        assertNull(AnnotationIndex.resolve(doc.pages, entry))
    }

    @Test fun pageReorderFindsSameAnnotationByIdentityAndDeleteInvalidatesOldRow() {
        val doc = Document.blank(count = 4)
        val page = doc.pages[1]
        val mark = markup("moving", note = "keep with page")
        page.markups = listOf(mark)
        val row = AnnotationIndex.list(doc.pages).single()
        val before = doc.pages.toList()
        val move = MovePages(doc, before, MovePages.order(before, listOf(page), 4))
        move.redo()
        assertEquals(3, AnnotationIndex.resolve(doc.pages, row))
        assertEquals(3, AnnotationIndex.list(doc.pages).single().index)
        assertSame(mark, doc.pages[3].markups.single())
        move.undo()
        assertEquals(1, AnnotationIndex.resolve(doc.pages, row))

        val delete = DeletePage(doc, page, 1)
        delete.redo()
        assertNull(AnnotationIndex.resolve(doc.pages, row))
        assertTrue(AnnotationIndex.list(doc.pages).isEmpty())
        delete.undo()
        assertEquals(1, AnnotationIndex.resolve(doc.pages, row))
    }

    @Test fun annotationsAndCommentsRemainFilterableAfterNativeSaveAndReopen() {
        val doc = Document.blank(count = 3)
        doc.pages[0].markups = listOf(markup("first", note = "Review"))
        doc.pages[2].markups = listOf(
            markup("last", MarkupType.STRIKEOUT),
            markup("note", MarkupType.SQUIGGLY, "Question"),
        )
        val codec = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())
        val saved = ByteArrayOutputStream().also { codec.write(doc, it) }.toByteArray()
        assertFalse(saved.isEmpty())
        val reopened = codec.read(ByteArrayInputStream(saved))

        assertEquals(listOf("first", "last", "note"), AnnotationIndex.list(reopened.pages).map { it.markup.id })
        assertEquals(listOf(0, 2, 2), AnnotationIndex.list(reopened.pages).map { it.index })
        val current = AnnotationIndex.list(reopened.pages, 2)
        assertEquals(2, current.size)
        assertEquals(MarkupType.STRIKEOUT, current[0].markup.type)
        assertEquals("Question", current[1].markup.note)
        assertEquals(2, AnnotationIndex.resolve(reopened.pages, current[1]))
        assertTrue(AnnotationIndex.list(reopened.pages, 1).isEmpty())
    }
}
