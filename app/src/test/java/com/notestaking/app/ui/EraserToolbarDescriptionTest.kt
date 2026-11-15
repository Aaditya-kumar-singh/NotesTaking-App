package com.notestaking.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EraserToolbarDescriptionTest {
    @Test fun modeAndSizeAreIncludedInSpokenLabel() {
        assertEquals(
            "Eraser: Area, Size: 24.0",
            eraserAccessibilityText("Eraser", "Area", "Size", 24.0),
        )
        assertEquals(
            "Eraser: Stroke, Size: 12.5",
            eraserAccessibilityText("Eraser", "Stroke", "Size", 12.5),
        )
    }

    @Test fun existingTranslationsAreUsedWithoutHardcodedEnglish() {
        assertEquals(
            "Gomme: Zone, Taille: 8.0",
            eraserAccessibilityText("Gomme", "Zone", "Taille", 8.0),
        )
    }
}
