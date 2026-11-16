package com.notestaking.app.ui

import com.notestaking.app.R
import com.notestaking.app.core.tools.EraseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EraserToolbarDescriptionTest {
    @Test fun everyEraserModeHasItsOwnLocalizedLabel() {
        assertEquals(R.string.eraser_stroke, eraserModeLabelRes(EraseMode.STROKE))
        assertEquals(R.string.eraser_area, eraserModeLabelRes(EraseMode.AREA))
        assertEquals(R.string.eraser_marker, eraserModeLabelRes(EraseMode.MARKER))
        assertEquals(R.string.eraser_tape, eraserModeLabelRes(EraseMode.TAPE))
        assertEquals(R.string.eraser_shape, eraserModeLabelRes(EraseMode.SHAPE))
        assertEquals(R.string.eraser_connector, eraserModeLabelRes(EraseMode.CONNECTOR))
    }

    @Test fun markerModeIsAvailableToBothEraserSurfaces() {
        fun source(relative: String): String = listOf(
            java.io.File("src/main/java/com/notestaking/app/ui/$relative"),
            java.io.File("app/src/main/java/com/notestaking/app/ui/$relative"),
        ).first { it.isFile }.readText()
        val popup = source("Popups.kt")
        assertTrue(popup.contains("eraseMode = EraseMode.MARKER; emit()"))
        for (mode in listOf("TAPE", "SHAPE", "CONNECTOR")) {
            assertTrue(popup.contains("eraseMode = EraseMode.$mode; emit()"))
        }
        assertTrue(popup.contains("R.string.eraser_marker"))
        val infinite = source("InfiniteInteraction.kt")
        assertTrue(infinite.contains("eraseModeForGesture = configFor(Tool.ERASER).eraseMode"))
        assertTrue(infinite.contains("session.erase(content.x, content.y, eraserRadius(), eraseModeForGesture)"))
        assertFalse(infinite.contains("session.erase(content.x, content.y, eraserRadius(), areaErase())"))
    }

    @Test fun stylusPressureIsExplicitlyEnabledInEraserPopup() {
        val popup = listOf(
            java.io.File("src/main/java/com/notestaking/app/ui/Popups.kt"),
            java.io.File("app/src/main/java/com/notestaking/app/ui/Popups.kt"),
        ).first { it.isFile }.readText()
        val eraser = popup.substringAfter("fun EraserConfigPopup(").substringBefore("fun SelectConfigPopup(")
        assertTrue(eraser.contains("mutableStateOf(base.pressureEnabled)"))
        assertTrue(eraser.contains("pressureEnabled = pressure"))
        assertTrue(eraser.contains("pressureMinFactor = ToolConversions.sensitivityToMinFactor(sensitivity.toDouble())"))
        assertTrue(eraser.contains("ToggleRow(stringResource(R.string.caption_pressure), pressure)"))
        assertTrue(eraser.contains("SliderRow(stringResource(R.string.caption_sensitivity), sensitivity, 0f..100f, enabled = pressure)"))
    }

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
