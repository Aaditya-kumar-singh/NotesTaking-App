# Chat A — B39 save/recovery: local implementation handoff

- **Assignment:** B39 / G03 — P0 NP-084 (atomic local save and document recovery).
- **Branch:** `agent/chat-a-b39-save-recovery`
- **Worktree:** `.agent-worktrees/chat-a` under `xnotes-android`
- **Parent baseline:** `71c494d8879f504d1767b3d5d1458eab2bdda022` (dual-GPT coordinator, built from B38 `b2e38f3`).
- **Status:** LOCAL READY; **not pushed, not Kotlin CI-verified, not device-tested**.
- **Fix commit:** `04b1613dcab0853308beb3060bbbdd5923a1a5ca` — `fix(storage): reject empty atomic session writes`.

## Confirmed root cause and correction

`platform/SafeFileWriter.write()` previously considered a normal return from its caller successful even if the staged file contained **zero bytes**. It fsynced and atomically replaced `document.xnote`, `canvas.xcanvas` or settings metadata with this empty file, potentially discarding the last valid saved copy.

The guard now checks the staged file length before the atomic move. It rejects empty output, returns `false`, and removes the staged file in the pre-existing `finally` block without touching the prior valid destination. Nonempty normal writes and supported codecs keep existing behavior. This does not make arbitrary nonempty corrupt content valid; that needs separate codec verification.

**Owned changes:**
- `app/src/main/java/com/notestaking/app/platform/SafeFileWriter.kt`: empty staged-file rejection.
- `app/src/test/java/com/notestaking/app/platform/SafeFileWriterTest.kt`: two focused cases `emptySuccessfulWriteCannotReplaceLastGoodSnapshot` and `emptySuccessfulFirstSaveLeavesNoBrokenDocument`.

**Reviewed existing callers:** `SessionStore`, `CanvasSessionStore`, `JsonStore`, and `SafeFileWriter` tests. Valid JSON writes at least `{}` and native note codecs produce ZIP content; no caller intentionally requires a zero-length successful save.

## Verification actually run

- `python tools/agents/check_lane.py` — **PASS**, Chat A owns both changed paths.
- `python tools/ci/check_project.py` — **PASS**, 388 Kotlin application files, 226 Kotlin test files, 22 XML, 39 PNG; 0 problems.
- `python tools/ci/check_backup_privacy.py` — **PASS**, private app data excluded.
- `git diff --check` — **PASS**.
- Pre-commit ownership hook — **PASS**.

No local Gradle/JVM tests, Android lint/build, device install or instrumentation was run (repository policy). No new GitHub CI result exists for this B39 commit.

## Required subsequent acceptance

1. After serialized integration, obtain exact-SHA GitHub Kotlin unit tests, Android lint and debug APK CI pass.
2. On disposable notes, force-stop while a large save is running, reopen and compare the last committed note and media assets; test both `.xnote` and `.xcanvas`.
3. Exercise low-storage and faulty document-provider writes; verify the original file's hash remains unchanged when writing fails.
4. Ensure normal nonempty note saves, canvas saves, and JSON settings updates continue to work, including successful replace and reopen.
5. Check staged-file cleanup after failure. Report actual device model, Android version, fixture hashes, results and screenshots in coordinator's common tracker.

**Handoff to coordinator:** Review local `04b1613`, inspect all callers, then cherry-pick only the reviewed Chat A commits into the administrative coordination checkout after the preceding `developer` CI checkpoint is green. Update shared NP/NT trackers there, run checks, then push a single guarded fast-forward `developer` checkpoint. Do not publish directly from this worktree or touch Chat B / Gemini branches.
