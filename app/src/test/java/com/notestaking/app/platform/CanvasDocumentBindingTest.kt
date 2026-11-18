package com.notestaking.app.platform

import com.notestaking.app.core.infinite.InfiniteDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasDocumentBindingTest {
    private val legacyUri = "content://test.documents/document/Board.xcanvas"
    private val renamedUri = "content://test.documents/document/Board%20Two.xcanvas"

    @Test fun successfulLegacyRenameUpdatesOpenCanvasAndAutosaveDestination() {
        val canvas = InfiniteDocument().apply {
            path = legacyUri
            displayName = "Board.xcanvas"
        }
        val name = DocumentNames.canvasRename("Board.xcanvas", "Board Two.ntcanvas")
        assertEquals("Board Two.xcanvas", name)
        val autosave = CanvasDocumentBinding.followMove(
            canvas, legacyUri, renamedUri, name, legacyUri,
        )
        assertEquals(renamedUri, canvas.path)
        assertEquals(name, canvas.displayName)
        assertEquals(renamedUri, autosave)
    }

    @Test fun movingCanvasAcrossFoldersKeepsFilenameAndExternalAutosavePolicy() {
        val canvas = InfiniteDocument().apply {
            path = "content://test.documents/document/folder/Board.ntcanvas"
            displayName = "Board.ntcanvas"
        }
        val movedUri = "content://test.documents/document/new-folder/Board.ntcanvas"
        assertNull(CanvasDocumentBinding.followMove(canvas, canvas.path!!, movedUri, null, null))
        assertEquals(movedUri, canvas.path)
        assertEquals("Board.ntcanvas", canvas.displayName)
    }

    @Test fun unrelatedDocumentAndItsAutosaveUriRemainUntouched() {
        val canvas = InfiniteDocument().apply {
            path = legacyUri
            displayName = "Board.xcanvas"
        }
        val autosave = CanvasDocumentBinding.followMove(
            canvas, "content://test.documents/document/Other.xcanvas",
            renamedUri, "Other renamed.xcanvas", legacyUri,
        )
        assertEquals(legacyUri, canvas.path)
        assertEquals("Board.xcanvas", canvas.displayName)
        assertEquals(legacyUri, autosave)
    }
}
