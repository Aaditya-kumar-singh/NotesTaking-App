package com.notestaking.app.canvas

import com.notestaking.app.core.FakeSurfaceFactory
import com.notestaking.app.core.geometry.Pt
import com.notestaking.app.core.model.Document
import com.notestaking.app.core.model.Page
import com.notestaking.app.core.model.PageMargins
import com.notestaking.app.core.model.PageShape
import com.notestaking.app.core.model.PageTurn
import com.notestaking.app.core.model.PdfKey
import com.notestaking.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** NP-069: source-PDF link destinations are stable page identities, not note-list offsets. */
class PdfLinkDestinationGeometryTest {
    private fun assertNear(expected: Pt, actual: Pt) {
        assertEquals(expected.x, actual.x, 1e-6)
        assertEquals(expected.y, actual.y, 1e-6)
    }

    @Test fun internalSourcePageLinksStayOnCorrectPageAfterInsertMoveAndRotatedMargins() {
        for (rotation in listOf(0, 90, 180, 270)) {
            for (vertical in listOf(false, true)) {
                val start = Page(360.0, 480.0, pdfPage = 2)
                val target = Page(
                    360.0, 480.0, pdfPage = 7,
                    margins = PageMargins(left = 0.15, top = 0.1, right = 0.2),
                )
                val end = Page(360.0, 480.0, pdfPage = 12)
                val doc = Document(mutableListOf(start, target, end), dpi = 72).apply {
                    margins = PageMargins(bottom = 0.1)
                }
                val state = CanvasState(doc, FakeSurfaceFactory(), Palette.DEFAULT).apply {
                    viewportW = 1100
                    viewportH = 1400
                    zoom = 1.5
                    rotationDeg = rotation
                    verticalScroll = vertical
                    viewingMode = ViewingMode.SINGLE
                    relayout()
                    goToPage(1)
                }
                val point = Pt(145.0, 205.0)
                fun verifyTap(expectedPdfTurn: Int, local: Pt) {
                    val dest = doc.pages.indexOfFirst { it.pdfPage == 7 }
                    assertEquals("source PDF destination must be found by identity", target, doc.pages[dest])
                    state.goToPage(dest)
                    val content = state.fromPageSpace(dest, local)
                    assertEquals("rotation=$rotation vertical=$vertical target=$dest",
                        dest, state.pageIndexAtContent(content))
                    assertNear(local, state.toPageSpace(dest, content))
                    val onScreen = state.contentToViewport(content)
                    assertNear(local, state.toPageSpace(dest, state.viewportToContent(onScreen)))
                    assertEquals(7, target.pdfPage)
                    assertEquals(PdfKey.of(7, expectedPdfTurn), target.pdfKey)
                }

                verifyTap(0, point)
                val inserted = Page(360.0, 480.0)
                doc.pages.add(0, inserted)
                state.relayout()
                verifyTap(0, point)

                doc.pages.remove(target)
                doc.pages.add(target)
                state.relayout()
                verifyTap(0, point)
                assertSame(start, doc.pages[1])
                assertSame(end, doc.pages[2])

                val snapshot = PageShape.of(target)
                PageTurn.turn(target, doc, 1)
                state.invalidatePageGeometry()
                state.relayout()
                verifyTap(1, PageTurn.affine(360.0, 480.0, 1).apply(point))
                snapshot.restore(target)
                state.invalidatePageGeometry()
                state.relayout()
                verifyTap(0, point)
                assertSame(target, doc.pages.last())
                doc.pages.remove(target)
                state.relayout()
                assertEquals("a deleted PDF destination must never navigate to a different note page",
                    -1, doc.pages.indexOfFirst { it.pdfPage == 7 })
                assertSame(end, doc.pages.last())
            }
        }
    }
}
