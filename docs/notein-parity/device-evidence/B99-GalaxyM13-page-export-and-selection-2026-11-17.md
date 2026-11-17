# B99 Galaxy M13 - selection toolbar and page-range export checks

**Date:** 2026-11-17 (Asia/Kolkata).

**Status:** Real-phone smoke evidence; not full NT-048/NT-054+ or REL-11/12/16 signoff.

**Related:** [Issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29) (selection toolbar), [B100 UI issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28), [next universal APK plan](../../github/NEXT_UNIVERSAL_APK_UPDATE_PLAN_2026-11-17.md).

## Exact device and APK

- Samsung Galaxy M13, model `SM-M135F`, Android 14 / API 34, CPU ABI `armeabi-v7a`.
- Installed debug package `com.notestaking.app.debug`, version `0.1.0-debug`, B99 source `b299cbb8972267eebe103493fa093f7e5cfa6f70`, [CI #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156).
- Installed APK SHA-256 confirmed again: `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`.
- Used only the synthetic `QA_Imported_B99_20261116` two-page PDF note, `QA_GalaxyM13_20261116`, `QA_Canvas_M13_20261116` and uniquely named `QA_B99_*` exports.
- Immutable original PDF `QA_NotesTaking_B99_DeviceTest_20261116.pdf`: 13,770 bytes, **2 A4 pages**, SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. Hash verified unchanged after tests.

## Observed test results

| Test | Observation | Classification |
|---|---|---|
| Selection-bar Open side by side, fresh reselect | Long-pressed the QA imported PDF and selected QA standard note. Enabled `Open side by side` appeared. ADB tapped its accessible center `(469,316)`, waited 3s; selection remained active, no pane opened. | **Reproduced injected-tap failure**; physical finger/hit-test cause not yet proven |
| Clear Selection, fresh reselect | ADB tap `(124,315)` left selection mode active. Android Back cleared selection. | **Reproduced injected-tap failure**; finger-tap reproduction still needed |
| Export selected Page 1 to PDF | In `Export pages`, chose Shareable PDF, Page 1, Save; Android SAF wrote unique `QA_B99_Page1_Only_20261117.pdf`, **11,357 bytes**, SHA-256 `61bfa52da4d4b33fbe9b2089a41e7618a63cd5fc2b65725874bd034678ff65ed`. Independent PyMuPDF parse confirmed **exactly 1 A4 page** (595.3 x 841.9 pt), 1,872 green-ink pixels. | **PASS, smoke** |
| Export selected Page 1 to PNG | Chose PNG images and Page 1; SAF saved unique `QA_B99_Page1_Only_20261117.png`, **30,655 bytes**, SHA-256 `5631e09951ce2a9a56bfa6851546234403256c9e184fdaa88f299a47b689e4ab`. PNG signature, Pillow full decode and 1240 x 1753 px canvas verified; 7,129 green-ink pixels. | **PASS, smoke** |
| Invalid custom page range | Entered `3` where the dialog explicitly stated valid pages `1 to 2`; tapped Save. Dialog remained, invalid `3` stayed in field, and no Android Save picker opened. | **PASS, range validation** |
| Valid custom page range | Replaced invalid value with `2`; tapping Save opened Android SAF picker. | **PASS, valid range accepted into Save workflow** |
| Existing Page 2 PDF artifact | Before saving another file, `QA_B99_Page2_CustomRange_20261117.pdf` was **already present** on device. Did not overwrite. Read-only PyMuPDF inspection found **1 A4 page**, 7,595 bytes, SHA-256 `89f7350ae970adc8f1a0d21e40689c78f85cd042e199f2d0dff290f97c342086`, 656 green-ink pixels. | **VALID FILE, CREATION PROVENANCE UNVERIFIED**: do not count as an assistant-confirmed custom-range export |
| Source integrity | Re-read initial QA 2-page PDF after exports; exact SHA-256 remained `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. | **PASS** |
| Final app state | Returned from imported QA note to library using its Home button; app process running and original QA content retained. | **PASS, bounded** |

**Important:** Export validation used the actual device and independent PDF/PNG decoders. Pixel counts are evidence of rendered green marks, **not** a precision test for pen geometry or editability. The already-existing Page 2 PDF's creation path must not be inferred from its filename or content.

## Pending decisions and full acceptance

- Investigate [issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29): a physical finger tap, overlay/hit-box trace, two-pane open, focus and independent undo in split view. B100's new top filename and chevron tools must not conceal selection actions.
- Test multiple non-adjacent pages on a larger synthetic fixture, PNG batches/ZIP, true editable PDF ink, PNG resolution scaling, SAF cancellation/provider failure, precise 25-400% zoom and rotation landmark alignment.
- 200-page PDF memory/Perfetto, TalkBack hardware interaction, S Pen tilt/pressure/palm rejection and production signing **not tested**.
- No source implementation, Android build, APK install, or protected branch mutation was done. The three QA notes and exports were retained for inspection. Original non-QA documents were not opened or edited.
