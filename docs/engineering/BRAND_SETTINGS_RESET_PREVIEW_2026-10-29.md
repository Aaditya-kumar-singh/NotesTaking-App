# Paper & Ink — Settings safety and live theme preview (2026-10-29)

**Branch:** `feature/modern-paper-ink-foundation`
**Status:** Local implementation; **not pushed**, no local Android build or executable tests. Will be included in the next approved batch after the previous GitHub CI finishes.

## Problem and change

- The Preferences header previously called `update(Preferences())` **immediately** from `Reset to defaults`. A stray tap could unexpectedly replace saved appearance, input, page and other preferences.
- The button now opens the app's existing themed Material AlertDialog. The dialog title and body state what will change and clarify that saved notes/PDFs are not deleted. **Cancel**, system Back and outside dismiss only clear the dialog state; **Reset to defaults** explicitly confirms the existing `update(Preferences())` operation. The reset button uses the shared semantic destructive button styling. No persistence code was altered.
- English and Simplified Chinese strings were added consistently with the project's existing localized resources. No note text, page drawing, PDF, import/export or file format is modified.
- The settings pane now includes a small **non-interactive theme preview**, for System, Light, Dark, OLED and custom themes. It renders the current `LocalPalette` background, panel, accent and text roles using existing Compose Material typography and shape tokens. It does **not** draw a mock interactive button or replace any user's palette settings.

## Source-only validation gate

- `python tools/ci/check_project.py`
- `python tools/ci/check_backup_privacy.py`
- Parse both `values/strings.xml` and `values-b+zh+Hans/strings.xml`; ensure all three new message/preview resources exist and that the original `app_name`, `cancel` and `new_note` resources remain.
- Assert that `PreferencesPane` no longer calls `update(Preferences())` from the header directly, and that the confirm/dismiss code paths are distinct.
- Confirm preview references `LocalPalette` only and has no `onClick` or persistence callback; see `MaterialColourPicker.kt`.
- `git diff --check`; no Gradle, Kotlin compilation, lint, NDK or APK test locally.

## Cloud and device checks before accepting

1. On the *current PR head*, run GitHub Android CI (Kotlin unit tests, lint, native debug APK / APK verifier). Do not treat these source assertions as compilation evidence.
2. Using expendable test preferences, customize the selected theme/gesture/page settings; tap Reset and cancel via Cancel, Back and outside dismiss. Verify they remain unchanged after each cancellation and after app restart.
3. Confirm reset deliberately and verify default settings are applied/persisted, while existing notes and PDF files remain accessible. Note the custom toolbar layouts are stored separately and not cleared by `Preferences()`; do not imply otherwise.
4. Inspect the theme preview at 320dp phone and tablet width under System/Light/Dark/OLED/Dual and custom material seeds, including 200% font scaling, TalkBack, high contrast and orientation changes. Preview must never recolor saved ink or PDF content.

## Scope and status

This local batch addresses the **Settings UI**, not an original XN issue closure. The 45 historical audit rows remain unchanged. PR #7's full ABI CI must finish before the pending feature-batch push; PR #8 stays draft until the CI branch is merged and the branding branch is reconciled with `main`.
