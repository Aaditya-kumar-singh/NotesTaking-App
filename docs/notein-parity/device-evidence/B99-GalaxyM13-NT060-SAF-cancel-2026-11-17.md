# B99 Galaxy M13 - NT-060 SAF cancellation smoke tests

**Test date:** 2026-11-17 (Asia/Kolkata).

**Scope:** Three safe real-device cancellation paths. **NT-060 remains PARTIALLY TESTED / NOT VERIFIED**; fault-injection requirements remain open. See [TEST_PLAN.md](../TEST_PLAN.md) NT-060, [release QA issue #18](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/18), and [NT-054 text-recovery issue #30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30).

## Baseline

- Actual phone: Samsung Galaxy M13, `SM-M135F`, Android 14, ADB `RZ8W103G6EL`.
- App: B99 `com.notestaking.app.debug`, exact installed APK SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`. No APK was installed or rebuilt.
- Only disposable `QA_*` data used, principally `QA_NT054_MIXED_NOTE_20261117`. No personal notes, microphone or system storage settings were touched.

## Recorded results

| Case | Procedure and observed evidence | Result |
|---|---|---|
| Native archive Save a Copy cancellation | In library, opened the exact QA note's More menu and selected **Save a copy**. Android Save picker opened. Replaced its proposed filename with unique `QA_NT060_CANCEL_NATIVE_20261117.xnote`; verified exact filename and enabled SAVE control; pressed Android **Back**, deliberately not SAVE. App returned to **My library**. Checked `/sdcard/Download/QA_NT060_CANCEL_NATIVE_20261117.xnote` absent both before and after. | **PASS - cancel without output** |
| PDF export cancellation | Opened the same exact QA note, `Export pages -> Shareable PDF -> All (1) -> Save`. Android Save picker appeared. Entered unique `QA_NT060_CANCEL_PAGE_20261117.pdf`, confirmed picker filename, pressed **Back** without saving. Picker closed, app returned to QA editor, destination remained absent. | **PASS - cancel without output** |
| Multi-PNG tree selection cancellation | From the same note chose `Export pages -> PNG images -> All (1) -> Save`. Android document-tree picker appeared. Pressed Back without selecting a folder or granting access; app returned to editor. Known existing QA folder `/sdcard/QA_B99_MultiPNG_Batch_20261117` had **identical `ls -la` listing before and after**. | **PASS - cancel without changing known QA destination** |
| Existing QA assets unchanged | After the three cancellations, SHA-256 of earlier QA .xnote copy = `6f441b89771fb96cc5761786ff6c5ab760086133a997d23768d7a1518369040c`; .xcanvas copy = `e20603dcfe90742523f32a60efb74be324b5fc4200ecf1c8143cf7a4ee78b606`; original synthetic two-page PDF = `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. All match their prior verified values. | **PASS - those output files unmodified** |
| App stability / final state | Note editor exited via Home and `My library` was observed. App process `1974` still running; no crash observed. | **PASS - bounded** |

## Outstanding NT-060 acceptance requirements

The complete NT-060 P0 test additionally requires null destinations, failed writes/cleanup, intermittent zero-byte reads vs stalled providers, URI revocation, corrupt/unknown metadata, existing or unknown-size destination collision protection, required color-sidecar failure versus clearing an absent color, multi-PNG partial-write rollback, and ZIP/native copy/PDF provider compatibility. None was simulated here. **Do not mark NT-060 verified or release-ready based on the three cancellation smoke checks.**

The older NT-054 note text issue #30 remains open: native `flow.xml` preserved text placed lower in the page, but the reopened editor and exported PDF omitted it. A subsequently attempted near-top text control was **inconclusive** because the marker did not enter the native note, despite Text-tool selection. See [NT-054 device evidence](B99-GalaxyM13-NT054-native-mixed-persistence-2026-11-17.md).

No source files, third-party personal data, production branches or GitHub release artifacts were modified by these tests.
