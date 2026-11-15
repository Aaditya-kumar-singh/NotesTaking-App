package com.notestaking.app.canvas

import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The side button as each kind of pen reports it, from the stylus-diagnostics reports. */
class StylusButtonLatchTest {

    private val latch = StylusButtonLatch()

    @After
    fun releaseKey() = StylusButtonLatch.releaseKey()

    @Test
    fun aHeldKeySurvivesHoverWithNoButton() {
        // OnePlus Pad Go 2 Stylo: F21 down, then hover events with an empty buttonState.
        assertTrue(latch.onKey(KeyEvent.KEYCODE_F21, down = true))
        assertFalse(latch.onStylusMotion(0))
        assertTrue(latch.heldWith(0))
        latch.onKey(KeyEvent.KEYCODE_F21, down = false)
        assertFalse(latch.heldWith(0))
    }

    @Test
    fun aButtonInButtonStateLatchesAndLetsGo() {
        // S-Pen: the button rides in buttonState, on the touch itself and while hovering.
        assertTrue(latch.heldWith(MotionEvent.BUTTON_STYLUS_PRIMARY))
        assertFalse(latch.onStylusMotion(MotionEvent.BUTTON_STYLUS_PRIMARY))
        assertTrue(latch.heldWith(0))
        assertTrue(latch.onStylusMotion(0))
        assertFalse(latch.heldWith(0))
    }

    @Test
    fun aPenOnBothRoutesIsUpOnlyOnceBothSaySo() {
        latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, down = true)
        latch.onStylusMotion(MotionEvent.BUTTON_STYLUS_PRIMARY)
        latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, down = false)
        assertTrue(latch.held)
        assertTrue(latch.onStylusMotion(0))
        assertFalse(latch.held)
    }

    @Test
    fun aKeyUpInTheOtherPaneLetsGoHereToo() {
        val otherPane = StylusButtonLatch()
        latch.onKey(KeyEvent.KEYCODE_F21, down = true)
        otherPane.onKey(KeyEvent.KEYCODE_F21, down = false)
        assertFalse(latch.held)
    }

    @Test
    fun losingFocusLetsGoOfTheKey() {
        latch.onKey(KeyEvent.KEYCODE_F21, down = true)
        StylusButtonLatch.releaseKey()
        assertFalse(latch.held)
    }

    @Test
    fun resetForgetsHoverButNotAHeldKey() {
        latch.onStylusMotion(MotionEvent.BUTTON_STYLUS_PRIMARY)
        latch.reset()
        assertFalse(latch.held)
        latch.onKey(KeyEvent.KEYCODE_F21, down = true)
        latch.reset()
        assertTrue(latch.held)
    }

    @Test
    fun simultaneousStylusKeysReleaseIndependentlyAcrossPanes() {
        val otherPane = StylusButtonLatch()
        assertTrue(latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, down = true))
        assertTrue(otherPane.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, down = true))
        assertTrue(otherPane.heldWith(0))
        assertTrue(latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, down = false))
        assertTrue("another held button still controls the pen", latch.heldWith(0))
        assertTrue(otherPane.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY, down = false))
        assertTrue("an unpressed key-up must not release the held button", latch.held)
        assertTrue(otherPane.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, down = false))
        assertFalse(latch.heldWith(0))
    }

    @Test
    fun rearEraserAndFingerCannotInheritAButtonOverride() {
        latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL, down = true)
        assertTrue(latch.heldFor(MotionEvent.TOOL_TYPE_STYLUS, 0))
        assertFalse("the eraser nib always uses the eraser tool", latch.heldFor(MotionEvent.TOOL_TYPE_ERASER, 0))
        assertFalse("a finger never inherits a held pen button", latch.heldFor(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.BUTTON_STYLUS_PRIMARY))
        assertFalse("a mouse never inherits a held pen button", latch.heldFor(MotionEvent.TOOL_TYPE_MOUSE, 0))
        latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL, down = false)
        assertFalse(latch.heldFor(MotionEvent.TOOL_TYPE_STYLUS, 0))
    }

    @Test
    fun losingFocusReleasesAllHeldButtons() {
        latch.onKey(KeyEvent.KEYCODE_F21, down = true)
        latch.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, down = true)
        assertTrue(latch.held)
        StylusButtonLatch.releaseKey()
        assertFalse(latch.held)
        assertFalse(latch.heldFor(MotionEvent.TOOL_TYPE_STYLUS, 0))
    }

    @Test
    fun tapKeysAndOrdinaryKeysAreNotTheButton() {
        // Honor 333, Lenovo 600-604, Huawei 718 and Redmi's page keys fire tap actions instead.
        val codes = listOf(333, 600, 604, 718, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_A)
        for (code in codes) assertFalse("$code", latch.onKey(code, down = true))
        assertFalse(latch.held)
    }
}
