# NotesTaking App — B23: Native "Save a Copy" stream safety

Date: 2026-11-11
Base: verified `developer` SHA `090e860` (Android CI #43000500557 passed)
Branch: `fix/native-save-copy-stream-safety-b23-20261111`
Scope: NP-071 (editable native note integrity), NP-081 (SAF provider failures and truthful completion).

## Bug verified in source

`MainActivity.saveCopyLauncher` synchronously opened an Android output stream on the UI thread and used `?.use` without checking for a null result. When a provider denied an output stream, `runCatching` succeeded with `null`, so the error message was never displayed. `Editor.copyFileTo`, also used for sharing copies, silently returned with no bytes if the source provider returned no input stream. Creating a native note copy from a slow SAF provider could block the main thread.

## Scoped correction

1. Add `ProviderInput.read`: fail closed on a missing readable stream, and close it after success or an exception.
2. Use this guard in the shared `Editor.copyFileTo` method. Existing clipboard/share native-note copy callers now receive an exception on a missing source instead of silently receiving an empty file.
3. In `saveCopyLauncher`, check source **before** opening the new destination, use the existing `ProviderOutput.write` for output validation and close, and copy on `Dispatchers.IO`. Report errors back on the UI coroutine.
4. Do not alter `.xnote`/`.xcanvas` formats, note content, document tree permissions, current editor state, or successful copy bytes.

## Regression checks

- `ProviderInputTest.absentSourceCannotSilentlyCreateAnEmptyCopy` confirms a missing source throws without invoking the writer.
- `sourceIsCopiedAndClosedAfterSuccessfulRead` verifies content and close.
- `sourceIsClosedWhenDestinationWriteFails` verifies close and propagated IO error.
- [x] Source checker, privacy checker, staged whitespace and exact caller assertions.
- [ ] GitHub exact-SHA Kotlin tests, Android lint, APK build.
- [ ] Android providers: local file, Google Drive/other cloud SAF, intentionally inaccessible source, unavailable destination, low storage and slow copy (using disposable note files).
- [ ] Verify the UI remains responsive, error messages are shown and original source is untouched. A provider may leave a partially written newly selected destination on a failed write; that remaining cleanup/retry behavior is separate from this batch.

No Gradle/Android build or device installation on the Windows machine. The pre-existing `.kotlin/` directory is untouched.
