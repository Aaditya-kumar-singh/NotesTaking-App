# Search tab touch targets and result accessibility — local checkpoint

**Branch:** `feature/shape-count-controls-a11y` (not pushed).
**Base:** Paper & Ink PR #8, commit `e181918`.
**Status:** Source-only implementation; Android compilation and device acceptance still required.

## What changed

- In the PDF/note Search tab, the search field uses a 48dp-tall container. Clear Search is now a labeled 48x48dp Material `IconButton`, not a 16dp directly clickable icon.
- Case-sensitive (Aa) and whole-word (W) checkboxes use **48dp** touch targets while retaining their original **28dp visual chips**, `Role.Checkbox` state, localized descriptions and existing toggle callbacks.
- Previous/Next match buttons are **48dp** and keep their existing keyboard shortcut tooltips, labels, empty-result disabled state and navigation callbacks. The match-position text ellipsizes in compact windows instead of forcing horizontal overflow.
- Search result rows have **minimum 48dp height**, retain the two-line excerpt, and expose a **selected** accessibility state when the row is the current hit.
- Search queries, focus requester, Enter/Shift+Enter behavior, scan engine, match snippets and search-session lifecycle are unchanged. No document data, drawing logic or native library changed.

## Acceptance checklist

1. Before publishing: project source check, privacy checker, focused source assertions for 48dp targets/semantics/callbacks, and `git diff --check` pass. GitHub must compile Kotlin and run tests/lint/APK verification on the exact pushed SHA. No local Android/Gradle execution.
2. On phone (320dp) and tablet split-screen (280–400dp search panel), enter and clear a query. Verify text input/IME remains usable, focus doesn't disappear unexpectedly, and Clear Search is reachable by touch and TalkBack.
3. Toggle Aa and W and confirm the search results update, checkbox state is announced, and neither toggle blocks the search field or nav controls.
4. Test previous/next buttons, disabled state with zero hits, Enter and Shift+Enter navigation on a hardware keyboard, and the active result's selected announcement. Confirm result taps navigate to the correct PDF/note page.
5. Test 1x and 2x font scales, portrait/landscape, light/dark themes, and a large number of search hits; confirm the position label may shorten but controls are not clipped.

**Unverified risk:** Actual TalkBack semantics merging, very narrow window rendering and 200% font scale need device screenshots and interaction tests. Keep legacy audit findings open until hardware acceptance.
