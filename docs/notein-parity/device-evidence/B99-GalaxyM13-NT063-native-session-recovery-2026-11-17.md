# B99 Galaxy M13 - NT-063 native-session recovery and abrupt-stop control

**Test date:** 2026-11-17 (Asia/Kolkata). **Checklist:** `docs/notein-parity/TEST_PLAN.md` NT-063 (P0, NP-084). **Verdict:** **PARTIAL PASS / NOT FULLY VERIFIED.** Real-phone recovery of a complete paged-note session passed and last-good bytes survived the tested abrupt stop. **No staged write was observed being interrupted.** Infinite Canvas copy integrity passed; its cold-restart recovery was **not tested**.

## Device / scope

- Physical Samsung Galaxy M13 (`SM-M135F`, Android 14), ADB `RZ8W103G6EL` authorized.
- Existing B99 debug APK `com.notestaking.app.debug`, version `0.1.0-debug`, built from source `b299cbb8972267eebe103493fa093f7e5cfa6f70`. Prior installed APK SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`; not rebuilt or reinstalled during this test.
- Used **only disposable `QA_*` notes** and native copies. No microphone, storage settings, low-space simulation, personal documents or production branch modifications.
- In app My library, copied the known mixed-media QA note and renamed the **copy** `QA_NT063_RECOVERY_NOTE_20261117`; original `QA_NT054_MIXED_NOTE_20261117` was not edited.
- Native **pre-test copied-note baseline**, saved via app `Save a copy` to `/sdcard/Download/QA_NT063_BASELINE_20261117.xnote`: 73,583 bytes, SHA-256 `f5aef527dbce95c89878416d82f82a6a4100653d736e858088cace15a421535f`. ZIP CRC check **clean**; synthetic PNG and WAV match previous known asset hashes.

## Paged `.xnote` real-device procedure and results

| Check | Observed evidence | Classification |
|---|---|---|
| Save a complete working snapshot | Opened exact QA duplicate. Selected Pen and injected one page swipe; then pressed **Android Home** and allowed background save to complete. Debug app's internal **working-session** `files/session/session.json` explicitly referenced `QA_NT063_RECOVERY_NOTE_20261117.xnote`; `document.xnote` was **73,614 bytes** and ZIP CRC-clean. Snapshot SHA-256 `89f57710ddd842f6b5801a276b4697a3cedfba1c6aec63cbad22ce605864dbd2`. | **PASS: completed session exists with right document identity** |
| Verify synthetic assets within saved working session | Archive has one page and `format=xnote`, with original synthetic `assets/image-000.png` SHA-256 `ae51c78dd68508643ecc806ca41bf7f4b951a818da9d2c3b1df651f35ae3c5fe` and `assets/audio-000.wav` SHA-256 `c1f86a3eaaa86f4102fb6d1d7224183ed8bfa3ee7abe52a0bafb284aa106cea6`. | **PASS: native recovery media intact** |
| Force-stop only the app after snapshot completion; cold launch | ADB force-stopped `com.notestaking.app.debug` from launcher, relaunched it. UI accessibility tree showed the exact QA note title, and physical screenshot showed **green ink, the synthetic image, and its audio Play pill**, with app still running. | **PASS: cold-reopen smoke** |
| Abrupt stop immediately after a new editing gesture | With exact QA note title confirmed, selected Pen, recorded pre-stop snapshot SHA-256 `89f57710ddd842f6b5801a276b4697a3cedfba1c6aec63cbad22ce605864dbd2`, injected a swipe, and executed `adb shell am force-stop` immediately after. The app had **not been sent to Android Home for a completed save**. Subsequent native session bytes remained **73,614**, SHA-256 unchanged, ZIP CRC **clean** with both media assets intact. Relaunch recovered the correct QA note title; the native session SHA-256 remained identical. | **PASS for last-good snapshot surviving this abrupt-stop timing; NOT proof of interruption during native ZIP/atomic staged write.** Whether the injected stroke reached the model or completed autosave was not independently established. |

**Critical precision:** The first process kill occurred after Android Home/background save had completed; the second occurred directly after an ADB gesture. Neither run captured an actual `.staged` temp file, a fsync boundary, nor a kill between flush and atomic rename. Do not call native *mid-write* atomicity, data durability under power loss, or NT-063 fully verified based on these results. The original P0 text-rendering issue #30 remains separate and unresolved.

## Infinite Canvas `.xcanvas` read-only copy check

- Used library Copy/Paste on the existing synthetic `QA_NT054_MIXED_CANVAS_20261117`, then renamed the **duplicate** `QA_NT063_RECOVERY_CANVAS_20261117`.
- Targeted read of its specific app-storage file `.../files/Notes/QA_NT063_RECOVERY_CANVAS_20261117.xcanvas`: **73,278 bytes**, SHA-256 `b642f97f19f7cf8795444b7614f6fe85cf97e4647b832990ce9c3ef3a576b8c6`; ZIP CRC **clean**, `format=xcanvas`, **4 items** (`stroke`, `image`, `audio`, `stroke`).
- It is **byte-identical** to the app-storage original QA canvas at that moment (same SHA-256), and both embedded assets are byte-for-byte identical: PNG SHA-256 `ae51c78dd68508643ecc806ca41bf7f4b951a818da9d2c3b1df651f35ae3c5fe`; WAV SHA-256 `c1f86a3eaaa86f4102fb6d1d7224183ed8bfa3ee7abe52a0bafb284aa106cea6`.
- **Do not claim Canvas UI recovery**: before the planned reopen, the phone foreground view unexpectedly changed to `untitled_1` while search navigation was being attempted. To avoid altering an unrelated document, further UI input and process stops were deliberately halted. No fresh canvas cold-open or save-interruption acceptance was performed.

## Remaining NT-063 requirements / next test

- Catch a **real in-progress staged `.xnote` and `.xcanvas` save** when the process is killed; prove old valid or new valid snapshot, never a truncated/replaced archive, and confirm abandoned stages are cleaned.
- Test deliberately **zero-byte native slot**, native session slot replaced by directory, blocked metadata sidecar, failing provider and low storage **only with an isolated QA session** and a verified rollback/backup. Inspect JSON sidecar byte identity after failed metadata-only save, then full-save repair.
- Run a larger deterministic-attachment fixture and compare native asset hashes, restart time and file size against the same pre-fix fixture. Check explicit app behavior if a temporary edit was not committed.
- Repeat for a **fully cold-reopened Infinite Canvas**, then run complete exact-B99/build SHA release QA. Hardware/CI/publish/triad verify states remain unchanged.

## Protected data / final state

- Prior original QA exported `.xnote` SHA-256 remained `6f441b89771fb96cc5761786ff6c5ab760086133a997d23768d7a1518369040c`; original QA `.xcanvas` export SHA-256 remained `e20603dcfe90742523f32a60efb74be324b5fc4200ecf1c8143cf7a4ee78b606`; original synthetic two-page PDF remained `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`.
- No source-code changes, protected-branch updates, builds or release actions were performed, and original QA file hashes were preserved. The foreground unexpectedly showed `untitled_1` during navigation; that non-QA note was **not intentionally edited or inspected**, but incidental input before recognizing the screen change cannot be independently excluded. Automation stopped at that point. This report is a **QA observation**, not a fix or release sign-off.
