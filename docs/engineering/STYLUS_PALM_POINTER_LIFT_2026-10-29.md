# Stylus pointer-up and palm-contact safety — 2026-10-29

**Branch:** `fix/stylus-palm-pointer-lift` (based on `main` at `821517f`; rendering integration is independently in GitHub CI).

## Verified problem

The paged and infinite canvas track an active drawing pointer ID. If a pen was touching along with an incidental finger/palm, Android reports pen-up as `ACTION_POINTER_UP`, not terminal `ACTION_UP`. The old `handlePointerUp` did nothing for DRAW/ERASE/TAPE. The remaining finger then generated a terminal UP, which could commit a stroke using its coordinates (fallback to pointer index 0), produce an unintended line tail, or defer the pen's eraser/tape commit.

The paged `CanvasView` also began its four-finger debug gesture as soon as four pointers were present, even when a stylus/eraser tip was one of those contacts, and dispatched a synthetic `CANCEL` to the active editor.

## Implemented

- `PenContact.kt`: small pure rules for classifying stylus/eraser pointers, matching the active lifted pointer ID, and ensuring the debug gesture consists of exactly four finger contacts.
- Both editors now finalize a DRAW, ERASE, or TAPE action on **the active pen's own** `ACTION_POINTER_UP`, while that pointer is still present and its position and pressure are available. They ignore later terminal events from the incidental finger, preventing a phantom ink tail or double-commit. A non-active finger lifting cannot finish the pen stroke.
- The pen's eraser end (`TOOL_TYPE_ERASER`) now has the same pen-contact protection as the writing tip.
- The debug four-finger gesture only arms on a gesture consisting exclusively of fingers; a mixed pen + three-finger contact cannot cancel a live ink stroke.
- Added `PenContactTest` for stylus/eraser recognition, active-pointer identity, idle/other-pointer rejection, and finger-only debug eligibility.

## Validation and limitations

GitHub Android CI must pass source checks, Kotlin unit tests, lint and APK assembly before merge. Unit tests verify pure ownership policy, not the Android `MotionEvent` state-machine integration. Real tablet tests are still mandatory for pen/eraser with palm contact, pen-first/finger-first ordering, hardware palm rejection, stylus button, pressure samples, finger pinch, finger tap gestures, cancelled contacts, and two-pane drawing.

**Known limitation:** After a pen is lifted while extra fingers remain down, the current gesture stream is intentionally ignored until the next fresh `ACTION_DOWN`. Another pen press while that finger remains down may not resume as a new stroke. A full multi-touch stream handoff requires a separate tested input-state change; this patch prevents corrupt/doubled ink without introducing a risky synthetic MotionEvent workaround.

The unrelated `SKILLS/` directory is not tracked, edited, or committed.
