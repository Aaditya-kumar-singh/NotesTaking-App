# Editor accessibility batch — integration review and GitHub acceptance

**Branch:** `feature/shape-count-controls-a11y`
**Base:** `feature/modern-paper-ink-foundation` at `e181918` (Paper & Ink PR #8)
**Scope:** Shape counters, selection counters, text size bars and Search tab; no document format, canvas algorithms, PDF decoding, app-storage, private data or native code changes.

## Review results

Three local accessibility commits were inspected against their callers in `MainActivity.kt`, `InteractionController.kt`, `Editor.kt`, the selection-style host and the existing search navigation.

The material integration issue was the **floating text bar clipping at the right edge of a narrow editor pane**. Its offset only clamped to the left, and the wider 48dp controls increased the overflow. The review fix now measures actual pane width, clamps the left coordinate at both edges, and constrains the toolbar viewport with horizontal scrolling to reach trailing controls. `TextStyleBarPlacementTest.kt` supplies four unit regression cases (centered, left edge, right edge, 208dp split pane). The test is authored, **not executed locally**.

The remaining source changes preserve original behavior:
- Shape and selected-shape counters: labeled 48dp controls, min/max disabled states and original count callbacks.
- Floating and rich-text font controls: 48dp steppers/Done, existing 6–96pt model limits and edit/Undo callbacks.
- Search panel: 48dp clear, toggle, navigation and hit targets; active-match selected semantics; Enter/Shift+Enter and session lifecycle unchanged.

## Publishing and CI

- Source validation, backup privacy checker, whitespace/diff check, and targeted size/accessibility/placement checks are the **local gate**.
- This branch carries the exact `.github/workflows/android-ci.yml` from the independent CI PR #10. This temporary workflow-only overlap starts **automatic CI on the first batch push** even before PR #10 merges. Reconcile/remove the duplicate diff when PR #10 lands, without altering the feature commits.
- Open a **draft PR targeting `feature/modern-paper-ink-foundation`**, since PR #8 still awaits device acceptance. Do not merge this stacked PR before PR #8, and do not edit PR #8, PR #9 or PR #11.
- GitHub must run source checks, Kotlin unit tests (including `TextStyleBarPlacementTest`), Android lint, APK assemble/verification and ABI coverage on the **exact published SHA**. If its workflow file is counted as build/system change, all ABIs should run by policy.
- Phone/tablet QA: 280/320/360dp phone widths, tablet split panes, portrait/landscape, light/dark, 100%/200% font, physical TalkBack and keyboard focus. Verify shape +/- and Undo, text Size +/- and Done at 6/96pt, narrow/right-edge toolbar scrolling, search toggle state, query Clear, previous/next, active-hit semantics and correct result page.
- No merging, release, hardware acceptance, old audit finding closure or user document mutation as part of this batch.

**Residual risk:** No native UI screenshot or TalkBack interaction has been captured. Unit tests and GitHub builds do not prove stylus behavior, font-scale layout or keyboard selection; require device evidence.
