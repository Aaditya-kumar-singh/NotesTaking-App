# B78 — source review decisions and QA gates (15 November 2026)

**Repository:** Aaditya-kumar-singh/NotesTaking-App. **Integration base:** developer at `6f2f1578f90975788972dae19658d4134ce4570a`. This report records source review and QA candidates, not a release or physical-device verification.

## Live source-review ledger at preparation

- **67 local_ready**, **0 review**, **63 pending**, **0 verified**, **0 device_verified**, **0 deferred**.
- The original Chat A, Chat B and Chat C worker histories and sessions remain intact. The optional B task NP-014 is reserved for tests-only work; C has NP-036 reserved. These allocations are not worker claims.
- The following six handoffs have been source-reviewed in the shared triad ledger. These are **not** marked `verified`.

| Task | Original worker handoff | Resolution or current scope | Remaining acceptance |
|---|---|---|---|
| NP-084 | A `21cf5928f5bd` | B76 `a6d235a`: validate streamed note/canvas manifest format and JSON, verify manifest ZIP CRC, fail closed before metadata save | Force-stop, storage pressure, actual provider writes |
| NP-074 | A `ac08b4ffd745` | B76 `a6d235a`: PdfBox-Android `PDAnnotationMarkup` with Ink subtype and opt-in UI through page Save/Share | Acrobat/Xodo edit, save and reopen; only eligible simple trailing pen strokes |
| NP-098 | B `b6f4fe3d6b56` | B76 `a6d235a`: retain responsive toolbar and 48dp split-divider touch area with 2dp visible line | Tablet split resize, no stray ink, screen-reader testing |
| NP-100 | B `d0a52f4734e8` | B77 `2cc537e`: retain pinned render errors; reject PDF without header/EOF; verify saved SAF bytes via SHA-256 readback; fail closed and explain retry | Real provider error, interruption, cancellation and retry tests; other save/undo status |
| NP-012 | A `dfa9845699ed` | B77 `79c3fd0`: preserve original implementation `23a18ed`; sanitize portable names, avoid duplicates and occupied SAF targets | Provider naming, cancel, overwrite tests on device |
| NP-018 | A `7c7f6929da78` | B78 `e856370`: preserve original implementation `f66d293`; deduplicate copy/move/trash selections and report partial failure | Bulk provider operations, nested folders and physical failure recovery |

## GitHub CI sequence

The initial B75 combined and individual A/B runs reached Kotlin compilation and failed on exact source/API mismatches; these are retained for diagnosis. B76 QA commit `a6d235a` fixed the original compile mismatches but GitHub CI [#43716343221](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43716343221) exposed three unresolved `assertArrayEquals` calls in `SafeDocumentTreeTest.kt`. B77 commit `2cc537e` adds the missing import and provider status regressions; [B77 CI #43717129958](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43717129958) was running when this report was prepared.

B78 stages the additional reviewed NP-018 bulk selection implementation for a separate exact-SHA Android CI run. The tests include source/resources/privacy, JDK 17 JVM tests, Android lint, APK build and ABI validation. A passing CI build alone does not prove physical stylus, PDF-reader or Storage Access Framework behavior.

## Release gate

**Do not merge into `developer`, `main` or `master` or create a release** until the selected integrated exact SHA has successful GitHub CI and the required provider, note recovery, stylus, TalkBack and PDF reader/device acceptance is recorded. No A-Z batch completion checkbox is authorized by source review alone.
