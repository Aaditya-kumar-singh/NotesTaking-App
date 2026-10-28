# Session persistence safety — implementation record

**Date:** 2026-10-28
**Branch:** `fix/session-persistence-safety` (branched from `main`)
**Scope:** local working-session snapshots and JSON sidecars. This is **not** a rewrite of user-selected SAF document save/export, and does **not** close every earlier data-loss finding.

## Research and decision

Android's official [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile) documentation specifies that a durable replacement first writes and syncs a new file, then atomically replaces the original. It explicitly warns that file replacement does not serialize concurrent writers. This project targets Android API 26+, so `java.nio.file.Files.move` with `ATOMIC_MOVE` and `REPLACE_EXISTING` is available for app-private files. The implementation uses a small JVM-testable helper instead of an Android-only AtomicFile wrapper:

- create a **unique** same-directory staging file (so concurrent writes never share a `.tmp` name);
- write fully, close and `FileDescriptor.sync()`;
- atomically rename into place without first deleting the old session;
- on any failure, delete **only the new stage**; if atomic replacement is unavailable, **fail closed** and retain the committed file.

This is per-file atomicity, not a cross-file transaction between `.xnote` and `.json`. The two-file consistency and Android process death paths need instrumentation validation.

## Code changes

1. `platform/SafeFileWriter.kt` — reusable safe replacement for app-private files and sidecars, explicit Boolean result.
2. `platform/SessionStore.kt` — serializes reads/writes/clear, writes session safely, returns `false` if document or metadata write fails.
3. `platform/CanvasSessionStore.kt` — same replacement for infinite-canvas snapshot, preserving old file on failure.
4. `platform/JsonStore.kt` — saves atomically with a checked write method; existing best-effort settings method remains Unit-returning.
5. `ui/Editor.kt` — queued paged-session writes/clears use a coroutine Mutex; version watermark advances only after acknowledged success; a new document identity always forces a write even when its content-version number matches a previous document.
6. `.github/workflows/android-ci.yml` — CI trigger includes `fix/**`, so feature and safety branches undergo the same checks.
7. `.gitignore` — ignore connector-created `.mcp-backups/` without deleting local backups.

## New regression tests

- `SafeFileWriterTest`: initial write and replacement, partial writer exception preserving last good copy, first-write failure leaving no destination, concurrent writers producing whole noninterleaved files.
- `SessionStoreTest`: failure to replace a blocked document slot returns false without deleting the existing directory and without leaving staged files.
- Existing SessionStore roundtrip/view-state tests remain unchanged.

## Remaining safety gates

- Run Gradle `testDebugUnitTest`, `lintDebug`, `assembleDebug` on GitHub CI, then Android process-death/failing-SAF stream tests. The Windows host has JDK21 but no configured Android SDK/NDK; local Gradle download previously timed out.
- Follow-up: `MainActivity` unsaved-change dialogs and verified on-disk note saving (XN-001/015/024/025); correct both-pane session restoration (XN-037/038); move large canvas-session writes off UI thread; ensure metadata/payload cross-file consistency; test interrupted `clear()` and real low-storage conditions.
- Neither a passing source scan nor green CI is proof of zero data loss. Do not merge a failing branch or publish production APK until source, runtime and release acceptance gates pass.

## Links

- Repo: https://github.com/Aaditya-kumar-singh/NotesTaking-App
- Whole-app roadmap: `docs/APP_WIDE_AUDIT_AND_ACTION_PLAN_2026-10-28.md`
- Prior static audit: `docs/audit/findings.md` (locally excluded from Git)
