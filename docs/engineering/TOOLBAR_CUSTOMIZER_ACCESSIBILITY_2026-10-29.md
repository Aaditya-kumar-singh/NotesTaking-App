# Toolbar customizer accessibility — local source-only checkpoint

**Branch:** `feature/settings-toolbar-a11y`
**Base:** Paper & Ink PR #8 at `e181918`
**Status:** Implemented locally, not pushed or Android-build verified.

## User-facing behavior

The Settings > Toolbar customizer previously exposed visibility chips through low-level touch gestures only. TalkBack could read each chip's text but had no reliable checkbox state or action to toggle visibility. Chips were also shorter than the recommended 48dp touch target, and the section-delete button used a 26dp target.

- Each toolbar item now announces its existing tool label with **checkbox role** and **on/off state**, plus an accessibility action that invokes the same saved `onToggle` callback as a tap.
- Chip bodies, section delete controls, section grip headers, and Add Section are at least **48dp high**; delete is **48x48dp**. A section card is now at least **104dp wide inside its padding**, leaving space for both the delete control and a usable drag strip even when its body is empty. The independent floating chip and section drag previews follow the same geometry.
- Existing touch tap, long-press drag, drop-target bookkeeping, toolbar layout save, and the single-section deletion guard are unchanged.
- These customizer changes do **not** alter the in-editor toolbar buttons, saved note data, toolbar file format, or drawing engine.

## QA and acceptance gate

1. Source-only checks: `tools/ci/check_project.py`, `tools/ci/check_backup_privacy.py`, accessibility semantics/size assertions, `git diff --check`.
2. GitHub CI after a reviewed batched push: Kotlin compile, existing `ToolbarLayoutTest`, lint and verified APK on the **exact published SHA**.
3. On a real phone and tablet with TalkBack, move focus through tool chips. Confirm labels, checked/unchecked announcements and activation toggle visibility without requiring a physical long press.
4. With TalkBack off, tap and drag chips across sections, long-press section header to reorder, delete a section and add a new one. Confirm drag ghost alignment, persistence across restart, and protection of the final remaining section.
5. Test compact split-screen and 200% system font size; 48dp controls should wrap rather than clip or overlap; confirm keyboard and stylus focus where supported.

**Not verified:** Real device rendering, TalkBack gesture dispatch, keyboard activation, and executable tests. Do not mark historical audit items closed or merge the change based on source checks alone.
