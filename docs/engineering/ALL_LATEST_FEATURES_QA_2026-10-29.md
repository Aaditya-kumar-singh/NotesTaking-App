# All latest implemented features — consolidated Android QA build

**QA branch:** `qa/all-latest-notes-2026-10-29`
**Starting baseline:** `qa/combined-paper-ink-accessibility` at `1d51e00`, whose Kotlin tests, Android lint, debug APK, package verification and CI completed successfully.
**Purpose:** Give an Android phone/tablet user **one debug APK** with the latest *implemented* app and UI changes. This is **not** a production release or an approval to merge outstanding UI pull requests into `main`.

## Included and source provenance

| Component | Implementation and source |
| --- | --- |
| NotesTaking branding | Original launcher icon, splash/loader, app namespace, light/dark/OLED Paper & Ink theme and dialogs; unchanged from the prior combined QA build |
| Home and Explorer | Navigation, folder/empty/filter states, responsive layouts and accessibility from PR #9 on Paper & Ink PR #8 |
| Canvas and PDF workspace | Existing paged notes, PDF viewer/search/annotation, infinite canvas and discoverable shortcuts; canvas quick-access phase-1 already integrated in `main` |
| Editor and Settings | Toolbar customize, 48dp PDF/zoom/table/shape/text/search controls, limit/disabled semantics and floating-toolbar width clamping from PRs #11 and #12 |
| Drawing reliability | Existing integrated precision area eraser, new-user Area default, guarded Undo/Redo, PDF bounds and private-note backup safety from `main`; verified source/implementation equality against related original fix branches |
| **Stylus + palm** | `fix/stylus-main-integration`: tracked active pen commits even with other fingers down, repeated pen-down with a resting palm and shared route for paged/infinite editors; pure Kotlin decision tests |
| **Font and theme import** | `fix/bounded-font-theme-imports`: 16 MiB font and 1 MiB code-theme budgets, 64 MiB total settings ZIP budget, bounded reads and I/O-thread import; regression tests |
| **Image-to-template safety** | `fix/bounded-template-image-staging`: 32 MiB maximum staging, finite 64 KiB streamed buffer and rejection of stalled document providers; regression tests |
| **Security CI** | Independent, previously GitHub-verified CodeQL `chore/codeql-compile-only` workflow and docs. This is build/security automation, not an additional app feature |

The older `fix/pen-after-palm-contact` branch has the same stylus application patch as `fix/stylus-main-integration`; one implementation is included to avoid duplicate conflicts. Other original fix branch heads remain distinct when their changes are already present in `main` via newer integration commits. The QA branch contains the original four UI PR heads without rewriting them.

## Required checks

1. Source-only: `tools/ci/check_project.py`, `tools/ci/check_backup_privacy.py`, `git diff --check`, YAML syntax, all four UI PRs + latest safety branch ancestry, checksums of icon/splash resource preservation and static tests for font/theme/PDF/eraser/undo/stylus and staged image import.
2. **GitHub Actions on the exact pushed SHA:** `Source, resources and attribution`, `JDK 17 / tests / lint / debug APK` and the package verifier all must succeed. The automatic branch-push workflow produces an ARM64 debug APK; check Android device ABI compatibility before installing. Failures must be fixed before describing the build as ready.
3. **Device QA:** use disposable .xnote/.xcanvas/PDFs; verify launch and icon; enter Home, Explorer, Editor, settings and PDF pages; draw/erase and Undo/Redo; save, close, relaunch and reopen; split-screen; imported fonts/themes and image templates within limits; rejection of oversized files; Touch/keyboard/TalkBack/200% font.
4. **Stylus scenarios:** draw/erase with a pen while resting a palm; pen-first and finger-first gesture order; repeated separate pen strokes without lifting the palm; eraser nib and stylus pressure; pinch/pan must not create stray ink; alternate paged and infinite editors. Test on real hardware; passing JVM tests **does not prove** accurate Android multitouch routing.
5. **Safety scenarios:** deliberately fail writes or revoke temporary SAF permission using expendable notes; test save failure and reopen; malformed PDF and ZIP files; low storage/memory and large PDFs. Do not run destructive tests on irreplaceable data.
6. Record exact GitHub run/SHA, Android version/device, ABI, PASS/FAIL per scenario and screenshots of no private documents.

## Explicitly not promised

- OCR, scanned PDF recognition, editable DOCX/EPUB, cloud sync and advanced brushes are **researched/planned**, not implemented features to merge.
- Android instrumentation, physical stylus/TalkBack, low-storage/process-death, fuzzing and security audit closure remain pending.
- This consolidated branch does **not** modify `main`, `master`, or existing PR #8/#9/#11/#12/#13 heads; it must not be merged as a replacement for those individually reviewed integrations.
- This is a **debug-signed QA APK**, not a production release. Signed release setup and old 45 finding acceptances remain separate gates.
