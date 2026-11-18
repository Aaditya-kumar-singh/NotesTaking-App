package com.notestaking.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentKindTest {
    @Test fun newCanvasesUseTheNtcanvasExtension() {
        assertEquals("ntcanvas", DocumentKind.CANVAS.extension)
        assertEquals(".ntcanvas", DocumentKind.CANVAS.suffix)
        assertEquals("Board.ntcanvas", DocumentKind.withSuffix("Board", DocumentKind.CANVAS))
        assertEquals(".xnote", DocumentKind.NOTE.suffix)
        assertEquals("Notebook.xnote", DocumentKind.withSuffix("Notebook", DocumentKind.NOTE))
    }

    @Test fun bothCanvasExtensionsAreRecognizedRegardlessOfCase() {
        for (name in listOf("Board.ntcanvas", "Board.NTCANVAS", "Board.xcanvas", "Board.XCANVAS")) {
            assertEquals(name, DocumentKind.CANVAS, DocumentKind.ofName(name))
            assertTrue(name, DocumentKind.isDocument(name))
            assertEquals(name, "Board", DocumentKind.stripSuffix(name))
            assertEquals("keep existing extension", name, DocumentKind.withSuffix(name, DocumentKind.CANVAS))
        }
    }

    @Test fun existingNamesAreNotGivenADuplicateExtension() {
        assertEquals("Board.xcanvas", DocumentKind.withSuffix("Board.xcanvas", DocumentKind.CANVAS))
        assertEquals("Board.NTCANVAS", DocumentKind.withSuffix("Board.NTCANVAS", DocumentKind.CANVAS))
        assertEquals("Board.xnote", DocumentKind.withSuffix("Board.xnote", DocumentKind.NOTE))
        assertEquals("Board", DocumentKind.stripSuffix("Board"))
        assertEquals("Board.pdf", DocumentKind.stripSuffix("Board.pdf"))
        assertNull(DocumentKind.ofName("Board.pdf"))
        assertFalse(DocumentKind.isDocument("Board.ntcanvas.zip"))
    }
}
