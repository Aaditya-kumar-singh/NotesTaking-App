# NotesTaking App — repository naming and native Canvas migration audit
**Snapshot:** 2026-11-18. **Read-only audit base:** `8ed0b50ac4eb12b8b0f78b5b320e7f9a864933e4`, before MIG-01 coordinator-only UI cleanup. **Tracking:** [MIG-01, GitHub issue #33](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/33); [B101 compact toolbar #32](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/32).

## What this repository does
A local-first Android notes and infinite drawing Canvas app written in Kotlin/Jetpack Compose, with native PDFium and Tree-sitter libraries. Users create notes/canvases, write ink and text, save to Android storage, reopen documents and export PDF. Assume one active Android user/device per app instance; session recovery and external SAF providers are the highest-risk paths.

## Audit coverage
- Enumerated **all 2,867 Git-tracked paths** through the index; inspected the top-level directory tree, build configs, app entry points, license/NOTICE, docs, workflow policies, and source/test directory families.
- Tracked file breakdown: **739 Kotlin files** (415 production + 324 tests), **1,002 headers**, **416 C++ files**, **256 C files**, **112 Markdown files** and other assets/templates.
- External code is substantial: **1,579 vendored PDFium paths**, **125 native grammar paths**, **49 Tree-sitter paths**. Their contents/attribution are not ours to rename.
- Broad tracked-text pattern searches checked old project names, native file extensions, XML/backup identifiers, author and repository links, test hooks and task annotations. The `xcanvas` term has **177 text matches across 46 files** in app/tests/docs/config.
- Followed the Canvas paths in `DocumentKind.kt`, `InfiniteDocument.kt`, `CanvasCodec.kt`, `CanvasSessionStore.kt`, `DocumentNames.kt`, `Editor.kt`, `MainActivity.kt`, `PreferencesPane.kt`, `ExplorerTiles.kt`, `ExportZip.kt`, `SafeFileWriter.kt`, and their focused regression tests.
- **Limit:** This is an automated inventory + risk-focused source review, **not a manual line-by-line review of all 2,867 files or compiled/physical-device evidence**. Binary fonts/images and upstream PDFium/grammar code were not manually audited.

## Must fix before switching new Canvas files to .ntcanvas

### 1. Legacy canvases could disappear from the library
- **Location:** `app/src/main/java/com/notestaking/app/core/util/DocumentKind.kt:11-37`; consumers `ui/ExplorerTiles.kt:173-177`, `MainActivity.kt:844,912`, `ui/Editor.kt:4223,6191`.
- **Case:** Simply replacing `CANVAS("xcanvas")` with `CANVAS("ntcanvas")` makes `ofName("Old.xcanvas")` return null. Library and export paths can then classify it as a note.
- **Fix:** Canonical new `.ntcanvas`, case-insensitive legacy `.xcanvas` alias in one shared file-type model; regression tests for both.
- **If skipped:** Users may think old drawings were lost.

### 2. Double extensions and same-title conflicts
- **Location:** `DocumentKind.kt:29-37`; `platform/DocumentNames.kt:12-45`; `ui/Editor.kt:4636-4675,5040-5059`.
- **Case:** `withSuffix("Board.xcanvas", CANVAS)` would append `.ntcanvas` after the rename; `Board.xcanvas` and `Board.ntcanvas` can occupy the same folder while showing identical extension-hidden titles.
- **Fix:** Strip recognized aliases before canonical creation and reserve same-stem names across the old/new Canvas suffixes. On ordinary rename preserve a legacy file's actual suffix and URI.
- **If skipped:** Confusing duplicate names or failed reopen/rename.

### 3. Saved-format marker must not be renamed
- **Location:** `format/CanvasCodec.kt:1315-1320`; `platform/CanvasSessionStore.kt:12-43`; `platform/SafeFileWriter.kt`; `format/LegacyFormatCompatibilityTest.kt:91-127`.
- **Case:** Changing `manifest.json` from `"format":"xcanvas"` to `"format":"ntcanvas"`, or blindly renaming the internal session slot `canvas.xcanvas`, can invalidate old ZIPs and recovery.
- **Fix:** Change only the **external new-file suffix**. Keep native JSON marker, ZIP structure, private recovery filename, and old tests as durable contracts.
- **If skipped:** Stored ink, images and waypoints may become unreadable after updates.

### 4. Protect existing notes during APK upgrades
- **Location:** `platform/AppStorageDocumentsProvider.kt:126-130` stores local files under app-specific storage; `app/build.gradle.kts:24-29,77-83` defines the package and debug/release variants.
- **Case:** Android removes app-specific storage on uninstall. Removing the previous APK to bypass a signer mismatch can delete personal local documents.
- **Fix:** Never uninstall/clear data as an installation workaround. Verify signer/package, make a user-controlled export/backup, then test a safe in-place update with disposable QA notes.
- **If skipped:** Irrecoverable loss of app-only files.

## Should fix during migration or subsequent cleanup

### 5. All new-file entry points must follow the single extension setting
- **Location:** `MainActivity.kt:728-733`, `ui/Editor.kt:4670-4675,5040-5059,5401-5408`, and `InfiniteDocument.kt:308-310`.
- **Case:** Save As currently embeds `.xcanvas` and open-Canvas rename does too, while ordinary create/fork uses `DocumentKind.CANVAS`. Updating only one route generates inconsistent filenames.
- **Fix:** Coordinator Save As should use `DocumentKind.CANVAS.suffix`; A reviews rename/create/fork; C owns the canonical suffix and old alias tests.
- **If skipped:** Some new canvases still have the old extension or a renamed legacy file changes extension unexpectedly.

### 6. Avoid source-check and test breakages that hide real bugs
- **Location:** `tools/ci/check_project.py:73-79`, `test/.../StandardAndCanvasCreationContractTest.kt:85-130`, `test/.../DocumentNamesTest.kt`, `test/.../CanvasRenameContractTest.kt`.
- **Case:** Hard-coded assertions for `CANVAS("xcanvas")` will fail after changing the canonical suffix, even if the actual app supports both.
- **Fix:** Require new canonical behavior **and** legacy compatibility; keep the existing old ZIP, crash-recovery and export cases, and add dual-extension tests.
- **If skipped:** CI might be green only after weakening tests or be red on a legitimate migration.

### 7. UI should use human labels rather than storage extensions
- **Location:** `ui/PreferencesPane.kt:635-636` currently shows `xnote` and `xcanvas` literally; English and Simplified Chinese string resources.
- **Case:** Users adjusting toolbars see internal implementation details instead of the editor types.
- **Fix:** Translate labels **Notes** / **Canvas**, independent of the underlying file extension. Explain the new extension only where “Show file extensions” is explicitly enabled.
- **If skipped:** Unnecessarily confusing settings and harder future format migration.
- **Checkpoint:** A coordinator-only local change is being prepared, with the UI labels and Save As sourcing `DocumentKind.CANVAS.suffix`. This does not itself switch the file suffix.

### 8. Old brand references are not all alike
- **Location:** `format/FlowXml.kt:70-79`, `format/MarkupsXfdf.kt:35-48`, `settings/SettingsBackup.kt:12`, `ui/AboutPane.kt:103`, `README.md`, `LICENSE`, `NOTICE.md`.
- **Case:** Bulk replace of `xnotes` can corrupt XML/backup interoperability; deleting the original author credit violates the existing MIT licensing notice.
- **Fix:** Rewrite *first-party prose/comments* that incorrectly describe the current app; retain serialized identifiers, upstream license/NOTICE/credits and vendored library names.
- **If skipped:** Old notes/backups may stop importing or legal attribution becomes inaccurate.

### 9. Physical workspace name and historical documents are not app branding
- **Location:** A/B/C worktrees, current LocalMCP project root and historic `docs/notein-parity/**`.
- **Case:** Renaming the physical checkout while live agent sessions have pinned paths breaks their tooling; mass rewriting historical issue/QA reports changes the meaning of evidence.
- **Fix:** Use current official GitHub and Android brand without renaming the working directory; do any workspace relocation only as a separately coordinated operation.
- **If skipped:** Tool sessions and evidence links become unreliable.

## Implementation ownership and gates
- **Chat C:** `core/util/DocumentKind.kt` and `core/infinite/InfiniteDocument.kt`, matching C core tests.
- **Chat A:** `ui/Editor.kt`, `platform/DocumentNames.kt`, codec/session compatibility and matching A tests.
- **Coordinator:** `MainActivity.kt`, `ui/PreferencesPane.kt`, resources/translations, README and CI source guard; integrate reviewed A/C commits.
- **Chat B:** continue B101 toolbar work in issue #32, no overlapping migration changes.

**Release gate:** source/privacy/owner checks → clean A/C exact-SHA local handoffs → coordinator-only integration → exact-SHA GitHub Kotlin tests/lint/full-ABI APK CI → real device save/reopen/export/legacy-snapshot tests. No mass conversion, push to `developer` or removal of legal notices.

## Verdict
**Do not rename the native ZIP format.** Ship `.ntcanvas` as a new *external suffix* while reading and preserving all `.xcanvas` files. The safe-label and Save As wiring can land independently; native extension migration requires the coordinated A/C implementation and exact APK verification.

**Not checked:** Every vendor source line, generated Android/NDK build products, actual Gradle compiler execution on this checkpoint, Galaxy M13 behavior, runtime SAF edge cases. This audit is a source/metadata review only.
