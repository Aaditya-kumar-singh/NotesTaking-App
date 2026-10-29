# Continue drawing with a resting palm — follow-up (2026-10-29)

**Branch:** `fix/pen-after-palm-contact`, based on `fix/stylus-palm-pointer-lift` at `a06ef96`; parent branch is independently passing through GitHub CI.

## Verified gap

The earlier pen-pointer-lift fix commits a pen's DRAW, ERASE or TAPE action when that pointer lifts while a finger remains. It then ignores the rest of that mixed touch stream until a fresh `ACTION_DOWN`. On Android, however, touching down again with the pen while a palm/finger remains still touching produces `ACTION_POINTER_DOWN`, so the editor cannot begin the next pen stroke. When a finger is already down before the pen arrives, the old handlers can interpret the pen as another pinch finger instead of beginning ink.

## Change

- Added `canvas/PenContactRouter.kt`, shared by paged and infinite editors. Only when a stylus or eraser tip joins an **all-finger** active contact, it routes that pen pointer as a single-pointer `ACTION_DOWN / ACTION_MOVE / ACTION_UP` stream.
- It uses the **public** `MotionEvent.obtain` multi-pointer overload to preserve the original pointer ID, tool type, pen pressure, button state, coordinate properties, source, timing and movement history. It never modifies or recycles the original Android event; generated events are recycled after dispatch.
- Starting a pen while a finger gesture is in progress cancels that prior gesture using the existing editor cancellation logic (and restores a cancelled pan view when applicable).
- A returning pen while a palm remains down starts a fresh pen action instead of waiting for the palm to lift.
- Mixed finger lifts are treated as pen movement, and only the active pen lift finishes the gesture. Remaining finger events are suppressed until another fresh contact.
- `PenContactTest.kt` now checks eligible pen/palm contact, eraser-end input, unrelated second stylus/mouse, invalid pointer index and no finger-only activation.

## Compatibility and acceptance

- This is limited to a stylus or eraser joining a stream otherwise containing **only fingers**. The existing normal single-pen gesture and two-finger pinch paths remain untouched.
- Repeated pen-downs while a palm remains in contact start independent gestures. If users want to switch the remaining finger to an active pan **without ever lifting it**, that is not included.
- **shortcut:** Android-dependent MotionEvent remapping has pure decision-policy unit tests, but not an instrumented device replay; add hardware-level tests before releasing a signed APK.
- GitHub Android CI must pass Kotlin unit tests, lint and debug APK before merging the parent and this follow-up. Real tablet testing must verify finger-first and pen-first order, pressure and historical batches, pen-tip vs eraser-tip, cancelled contact, pen while finger drawing, multi-finger tap and PDF annotations.
- No saved notes, persistent preferences or releases were changed. Untracked local `SKILLS/` is excluded from commits.

References:
- https://developer.android.com/reference/android/view/MotionEvent
