# Table editor steppers — local accessibility checkpoint (2026-10-29)

**Branch:** `feature/settings-toolbar-a11y` (not pushed).
**State:** Source-only change, not compiled or verified on an Android device.

## What changed

- Insert Table / Table Style controls for Rows, Columns, Padding and Lines use two-row layouts: a readable label followed by centered 48dp minus/value/plus controls.
- Minus and plus buttons use the existing branded icon set and localized *Decrease/Increase [setting]* accessibility labels in English and Simplified Chinese.
- The controls are disabled at the same domain limits enforced by the original clamping code: rows 1–60, columns 1–12, padding 0–24 pt and line width 0.25–6 pt.
- Button callbacks, table preview/commit, undo history, style data model, and saved table serialization are unchanged.
- The numeric row is 48 + 92 + 48 = **188dp**, fitting the 232dp content area of a 280dp Material dialog. Dialog content already scrolls vertically for smaller windows and larger text.

## Acceptance tests after GitHub compilation

1. Source validation and privacy checks, resource lookup and `git diff --check` must pass before any push.
2. On the exact published SHA, GitHub Android CI must compile the Compose UI, run existing Kotlin tests, lint, and build/verify an APK. No local Windows Gradle/build/test invocation.
3. Using a disposable document, insert a table and adjust Rows and Columns; verify minimum/maximum disable states, that each button changes by exactly one, and that the resulting table size matches its preview.
4. In both insert and restyle dialogs, adjust Padding and Lines through minimum and maximum; verify values clamp, persist, and undo/redone style changes still work.
5. Check TalkBack announcements (label plus increase/decrease and disabled state), keyboard focus and activation, and Android phone/tablet portrait/landscape with default and 200% font scale.
6. Verify narrow/landscape dialogs scroll to reveal all controls without horizontal clipping.

**Risk:** The fixed-width value may truncate very large text scale in an unusually narrow popup; real screenshots and TalkBack interaction are still required before acceptance. No historical audit item is closed by source changes.
