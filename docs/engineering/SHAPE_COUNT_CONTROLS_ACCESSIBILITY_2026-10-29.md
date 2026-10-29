# Shape count controls — local accessibility checkpoint (2026-10-29)

**Branch:** `feature/shape-count-controls-a11y`, based on Paper & Ink PR #8 at `e181918`.
**Status:** Source-only implementation; GitHub build and hardware acceptance are pending on the published batch SHA.

## Implemented
- The Shape popup's star-point/polygon-side count and the selection-style popup's existing shape-count editor use **48 x 48 dp** Material IconButton hit targets with 8dp horizontal spacing.
- The existing bordered **30dp visible circles** remain inside those larger hit targets. +/- glyph text is replaced by existing NotesTakingIcons plus/minus vectors with localized `count_fewer` / `count_more` accessibility descriptions.
- A label above a centered control row avoids squeezing the count between the caption and buttons in 284dp and 250dp popups. The count is center-aligned in a 56dp value slot.
- The existing `count > range.first` and `count < range.last` guards still disable buttons at the limits. Callbacks remain `onChange(count +/- 1)` in ShapePopup and `host.setSelectionCount(count +/- 1)` in CanvasMenus; existing selection history/undo handling is untouched.
- No drawing algorithm, document serialization, native build configuration, saved settings or third-party attribution changed.

## Acceptance when published
1. Local source XML/import/callback checks and `git diff --check`; Kotlin compilation/tests, Android lint and APK verification run only on GitHub for the **exact pushed SHA**.
2. On an Android phone and tablet, open the Shape settings for star/polygon; verify the point/side counts change exactly once per press, disable at min/max and do not overflow the popup.
3. Select a star/polygon and edit its count through Change style; confirm the selected shape updates and Undo/Redo preserves the previous geometry.
4. Enable TalkBack: both count buttons announce More/Fewer, announce disabled at boundaries, and can be activated via accessibility actions without hit-target overlap.
5. Verify 100% and 200% font scale, keyboard focus, light/dark theme, and landscape/split-screen width. Check that the tool tip does not obscure an essential action.

**Pending:** GitHub CI, screenshots, TalkBack, stylus/keyboard and device-level undo verification. Audit findings remain open until evidence is collected.
