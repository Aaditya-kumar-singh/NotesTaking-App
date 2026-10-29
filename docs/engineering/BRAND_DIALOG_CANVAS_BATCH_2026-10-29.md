# Paper & Ink — dialogs, menus and toolbar chrome (2026-10-29)

**Branch:** `feature/modern-paper-ink-foundation`
**Status:** Implemented in source. Static dry run only; GitHub Kotlin/Android compilation and device visual acceptance pending.

## Scope

- Updated the shared Compose popup layer to add `DestructiveTextButton`, using the current Material 3 error color through `LocalPalette.current.danger`. Normal Cancel/Save/Restore controls remain unchanged.
- Applied the destructive action to **Trash → Empty trash**, **Trash → Delete permanently**, the file explorer delete confirmation, the explorer name/remove dialog, and deletion of a markup note. The Trash menu's Delete permanently label and the Empty Trash icon also adopt the semantic warning color.
- Kept the existing irreversible-action **confirmation dialogs**, translated labels, click handlers, error handling and storage calls intact.
- Updated the paged **+ Page** menu with a readable Material title and divider. **All tools** now places its menu rows directly inside the existing shared DropdownMenu (rather than one large nested Column), allowing Material 3's own scrolling and sizing; tool choices, pen/eraser settings, undo/redo and extras remain wired to their original callbacks.
- Added a subtle 1dp palette-driven border on the **canvas-facing edge** of docked toolbars. Top, bottom, left and right docks follow their position. Floating toolbars preserve their previous shadow, clip and outline.
- Added a shared-toolbar accessibility pass: active drawing-tool buttons now expose their selected state, while ink swatches announce a localized color label with a hexadecimal value and selection state. The existing click and drag callbacks remain untouched.

## Source-only checks

- Verify `tools/ci/check_project.py` and `tools/ci/check_backup_privacy.py` pass.
- `git diff --check` passes, and every new Kotlin reference and imported symbol resolves to an existing project component by source inspection.
- Confirm the dangerous buttons delegate the **exact existing** closures; no document model, file extension, storage, XML, PDF, eraser engine, or native code changed.
- The shared menu helper remains the single source of panel color, outline, shape and positional offset. No new dependency or hardcoded light/dark color was introduced.

## Pending GitHub and tablet acceptance

1. Open a ready PR into `main` and run GitHub **Android CI** on the current batch head: Kotlin unit tests, lint, ARM64 debug APK (or manually select full ABIs for native changes). Do not claim this batch is Android tested before CI reports success.
2. On a test Android tablet and phone, inspect Paper & Ink menus in light, dark, OLED, dynamic and custom theme modes. Check content contrast, constrained popup height, nested menus, overflow placement and dismissal behavior.
3. Validate both toolbar orientations (top/bottom/left/right), floating mode, compact and comfortable sizes, keyboard navigation, accessibility and enlarged font scales. With TalkBack, confirm selected tools and named ink swatches are announced accurately; test that the color drag gesture remains unchanged. The divider must remain adjacent to the canvas, not user paper or PDF content.
4. On expendable test files, verify Trash → Restore, Empty Trash cancel/confirm, permanent delete cancel/confirm, file explorer delete cancel/confirm, and markup note delete/cancel. Avoid testing destructive operations on real notes.
5. Capture real-device screenshots for BR-3 and BR-4; source-level assertions are not visual or integration testing.

## Work still outstanding

- Complete app-wide typography, dialog iconography, consistent progress and feedback overlays, selectable pen favorites, tablet layout polish and larger touch targets.
- CI policy PR #7 was merged into `main` after full-three-ABI GitHub checks passed. GitHub's repository default is still `master`; change it to `main` separately with repository-admin access. This design batch is tracked in draft PR #8; do not push directly to `main`.
- Reconcile all 45 original XNotes findings with GitHub evidence; this UI batch closes **none** of the historical defects by itself.
