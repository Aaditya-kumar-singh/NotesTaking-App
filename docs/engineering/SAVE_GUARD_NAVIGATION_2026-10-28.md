# Save / Save As and guarded navigation — implementation checkpoint

**Date:** 2026-10-28
**Branch:** `fix/guarded-save-navigation`
**Scope:** explicit paged-note and infinite-canvas Save/Save As, unsaved-change confirmation, autosave-before-close, and leaving/switching editors. Not a substitute for process-death recovery or SAF provider transactional guarantees.

## Source findings

1. `MainActivity.EditorScreen` CreateDocument callback previously treated `runCatching` success as successful saving even when `ContentResolver.openOutputStream(uri, "wt")` returned null; `Editor.save()` also swallowed writer exceptions. The guarded continuation could close a note after no bytes were written.
2. The unsaved-changes Save button ignored `saveToThen`'s Boolean result.
3. `guarded()` allowed folder-autosaved documents to navigate immediately, while `goHome()` / `flushThen()` closed unconditionally. Earlier writers optimistically clear dirty while writes are in flight; a clean flag isn't proof of a completed save.
4. `guarded()` used the paged note's dirty flag for infinite-canvas navigation, and a previous Save As did not choose the .xcanvas format.
5. An active text/flow editor may hold uncommitted text at the moment navigation is requested.

## Design and implementation

- **One-shot Save As continuation:** `GuardedSaveFlow` consumes the navigation action when its file-picker result arrives, including cancellation. Only a confirmed successful save runs the action, at most once.
- **SAF output is fallible:** `ContentResolver.openOutputStream` is documented to return null after provider failure and throws on other errors. Paged Save As now routes through the existing `writeNoteSafely` staging-and-copy path and Boolean result; no direct ZIP encoding on the UI thread and no success after null output.
- **Canvas Save As:** added `saveCanvasToThen` and a separate `.xcanvas` CreateDocument contract. A canvas is never written through the `.xnote` codec. Both existing-document and new Save As flows wait for the on-disk writer.
- **Visible document identity:** `visibleDocumentUri` and `visibleDocumentTitle` select the active infinite canvas rather than the hidden paged buffer.
- **Navigation decision:** `saveNavigationDecision` checks `savingNote` before dirty; if auto-bound it waits for the write; if not auto-bound and dirty it prompts Save/Discard/Cancel.
- **Live text:** `commitPendingEdits` commits an active paged text/flow session or infinite-canvas text before the save/navigation decision.
- **Close on success only:** `flushThen`, `flushCanvasThen` and `goHome` use acknowledged results. A failed write, changed document, or new edits while writing keeps the editor open.
- **Queued autosave:** the flush waits even if a writer previously cleared `dirty` optimistically; if that write fails it can attempt to save again.

## Tests and release gates

Automated JVM cases in `platform/GuardedSaveFlowTest.kt` cover: Save As success once, failure never continuing navigation, cancelled picker clearing old actions, late/duplicate callback isolation, in-flight writes blocking navigation, autosave needing flush, and dirty external file prompting.

Android emulator/real-device acceptance still required:
1. Start with an unsaved paged note, choose Save in the unsaved-changes dialog, cancel picker: original note stays open; next unrelated Save must not run the previous navigation.
2. Repeat with an empty/unavailable provider stream and permission revocation: note remains open and editable, success is not shown; partial new SAF file handling depends on the provider.
3. Existing note Save with an unavailable destination: stay in editor, show error, allow retry/Save As/Discard.
4. Autosaved note or canvas while a write is in progress: tapping Back/Home/open another file must not close early; a successful write allows navigation.
5. Autosave write error, low storage, cloud provider disconnect: editor remains open with unsaved changes and reports the error.
6. Type a final character in the text editor and immediately navigate: that character must be in the saved file.
7. New and existing infinite canvas Save/Save As: use `.xcanvas`, preserve strokes and bookmarks/waypoints after re-opening.
8. In a two-pane session, target the focused pane, save one pane, ensure the other stays intact.
9. Use Save/Discard/Cancel explicitly and inspect the resulting files; never infer file success solely from UI state.
10. Rotate, background/foreground, kill the process during provider writes; recover both documents as a separate release gate.

### Limitations and follow-up

- `ContentResolver` writes are provider-defined; app-level staging prevents codec failure from truncating a destination before encoding but **cannot promise cross-provider atomic replacement or rollback after a mid-stream I/O failure**. See Android [ContentResolver](https://developer.android.com/reference/android/content/ContentResolver#openOutputStream(android.net.Uri,%20java.lang.String)) documentation.
- This phase does not implement crash-safe recovery of two split-pane sessions, process-death restore for all paths, or large hostile PDF import limits. Keep their P0 tickets open.
- Android CI unit tests, lint and APK assembly must pass on the feature branch before changing `main`.
