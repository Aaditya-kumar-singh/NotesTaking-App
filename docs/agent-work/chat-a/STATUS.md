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

## Subsequent status: B39 integration and B41 bulk-write performance follow-up

- B39 original fix `04b1613` was reviewed and integrated on `developer` as `fc7298e`, with tracker commit `e6b73d8`. [B39 Android CI #43397349803](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43397349803) was still **running** at this check; device recovery QA remains pending.
- **B41 assignment:** Address unnecessary per-byte writing in the existing atomic save stream wrapper, without touching save format, crash recovery, or any Chat B UI files.
- **B41 local code commit:** `cd55fef3c45b25b89b272f184e369612533035c3` — `perf(storage): preserve bulk writes in atomic save wrapper`.
- **Files changed:** `app/src/main/java/com/notestaking/app/platform/SafeFileWriter.kt`, `app/src/test/java/com/notestaking/app/platform/SafeFileWriterTest.kt`.
- **Cause:** `FilterOutputStream.write(byte[],off,len)` otherwise sends bytes individually, adding overhead to large ZIP/native-note asset writes. `SafeFileWriter.nonClosingStream()` now forwards byte-array writes directly to the original stream, preserving its non-closing `close()` behavior.
- **Regression added:** `borrowedStreamUsesBulkWriteAndLeavesDescriptorOpen`: a 256 KB payload must be passed in one bulk write, in original byte order, without closing the underlying destination.
- **Validation:** Chat A ownership guard passed; source checker passed (388 Kotlin source / 226 test files, 0 problems); backup privacy check passed; `git diff --check` and pre-commit guard passed. **Kotlin test execution, Android build, and device performance measurements have not run.**
- **Handoff:** This B41 fix is a new independent local commit after the old B39 handoff on Chat A's existing guarded branch. Integrate **only `cd55fef` and its B41 status follow-up**, after the prior CI gates and B40 integration are resolved. Do not re-cherry-pick B39 commits `04b1613` or `29c1b14` (already integrated).
- **State:** B41 LOCAL READY after scoped review, **not pushed**, no CI/device success claimed.

## B42 — SAF provider stalled-read recovery (local-only)

- **Assignment:** NP-081 / NP-082: avoid endless copy loops and orphaned exports when a cloud/document provider repeatedly returns zero bytes without EOF.
- **Source evidence:** `ProviderCopy.save()` used `InputStream.copyTo()`, which has no explicit consecutive-zero-read safeguard. The existing first-party `BoundedCopy.copy()` already fails after 16 consecutive zero-length reads and accepts intermittent non-empty reads.
- **Fix:** Reuse `BoundedCopy.copy(input, output, Long.MAX_VALUE)` in `ProviderCopy.save()`, retaining the existing nonempty-output check, exception handling, destination cleanup, and no new export size limit.
- **Local fix commit:** `203eadcdb3527c1c82a829195920333aaea3ac46` — `fix(storage): stop stalled SAF source streams during copy`.
- **Owned files:** `app/src/main/java/com/notestaking/app/platform/ProviderCopy.kt`; `app/src/test/java/com/notestaking/app/platform/ProviderCopyTest.kt`.
- **Tests added (not executed locally):** `stalledProviderReadFailsAndCleansIncompleteDestination` and `intermittentEmptyReadsStillSaveExactProviderBytes` (128 KiB payload).
- **Verification actually performed:** Lane guard PASS; source checks PASS (388 Kotlin main, 226 tests, 0 problems); privacy PASS; diff/staged diff check PASS; pre-commit guard PASS.
- **Not verified:** Kotlin/JVM execution, Android build/lint, real SAF provider timeouts, device copy behavior and partial destination cleanup under revoked permissions.
- **Integration order:** B40 CI must pass before publishing B41. Then B41's own CI must pass before separately cherry-picking **only B42 `203eadc` and this handoff commit** to a fresh coordinator integration branch. Never re-cherry-pick old B39/B41 commits, merge directly from Chat A's old baseline, or touch Chat B/Gemini.
- **State:** B42 LOCAL READY, NOT PUSHED.
