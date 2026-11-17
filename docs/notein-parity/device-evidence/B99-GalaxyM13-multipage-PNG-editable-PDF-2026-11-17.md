# B99 Galaxy M13 — multi-page PNG and editable PDF ink verification

**Date:** 2026-11-17 (Asia/Kolkata).

**Status:** Real-phone QA smoke and independent file inspection. Not full NT, REL-11/12/16, hardware or release acceptance.

**Tracking:** [B99 GitHub Actions #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156); [release QA issue #18](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/18); [next update UX #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28); [selection taps issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29).

## Device, build and fixture

- **Phone:** Samsung Galaxy M13 (`SM-M135F`), Android 14, 32-bit ARM (`armeabi-v7a`).
- **Installed APK:** B99 `b299cbb8972267eebe103493fa093f7e5cfa6f70`, `com.notestaking.app.debug`, version `0.1.0-debug`. Verified installed base APK SHA-256: `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`.
- **Only QA data:** Two-page `QA_Imported_B99_20261116`, its original synthetic exported PDF, and a new uniquely named `QA_B99_MultiPNG_Batch_20261117` folder at the root of shared device storage.
- **Original exported PDF:** `/sdcard/Download/QA_NotesTaking_B99_DeviceTest_20261116.pdf`, two A4 pages, 13,770 bytes, immutable SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. Rechecked after tests; no change.

## Case results

| Case | Observed evidence | Classification |
|---|---|---|
| Multi-page PNG export | PDF Pages panel → Export pages → PNG images → All (2) → Save opened Android OpenDocumentTree; root forbidden, a new empty `QA_B99_MultiPNG_Batch_20261117` folder was created. Following the folder permission confirmation, exactly **two** page PNGs appeared. We observed permission dialog but **did not independently observe who confirmed ALLOW**; file creation provenance is established by contents and timing, not by a directly observed final tap. | **PASS for output and file integrity; access-approval tap attribution unverified** |
| Page 1 batch PNG | `QA_Imported_B99_20261116-p01.png`, **30,655 bytes**, 1240x1753px, **7,129** green-ink pixels, SHA-256 `5631e09951ce2a9a56bfa6851546234403256c9e184fdaa88f299a47b689e4ab`. Exact byte hash **matches** earlier independently saved single-page Page 1 PNG. | **PASS** |
| Page 2 batch PNG | `QA_Imported_B99_20261116-p02.png`, **18,727 bytes**, 1240x1753px, **2,387** green-ink pixels, SHA-256 `9491ab77c02a756de3e35b0ff1e428f511fbc5f1289fc9bbd45b61e7e599b2e9`. | **PASS** |
| Default-pressure Editable PDF ink export | Export pages → Shareable PDF → All (2) → checked `Editable PDF ink (simple pen strokes)` → Save as `QA_B99_EditableInk_20261117.pdf` (in QA folder). File: **19,709 bytes**, 2 A4 pages, SHA-256 `2701e5ca7b7d7c4c18ae9a4a091e1c691380c5273c42c02e3b0fbe96993bcfb1`; PyMuPDF found **zero** `/Ink` annotations, but green pen marks remain visible. The installed Pen tool announced **Pressure Sensitivity 72%** (enabled). | **EXPECTED RESTRICTION**, not proof of export failure: pressure-enabled strokes are not eligible for editable Ink |
| Pressure-off simple-pen test | In the QA imported PDF's active Pen settings, switched Pressure **Off**; confirmed switch `checked=false` and toolbar description `Pressure Off`. Added one clearly separate green horizontal QA pen stroke on Page 1 (ROI green pixels 0 → **2,322**). | **PASS** |
| Editable PDF export with eligible stroke | Checked Editable PDF ink and All (2), saved `QA_B99_EditableInk_SimplePen_20261117.pdf` to same QA folder: **21,350 bytes**, SHA-256 `0f04cb5bdcc96dbf985d3bbfe0704191c2e543551dfadb60cdcbfcf85c8afeb3`. Independently parsed two A4 pages with PyMuPDF. **Page 1 has one genuine PDF `/Ink` annotation** consisting of one path with **43 points**; Page 2 has zero. | **PASS for actual PDF `/Ink` object and valid output** |
| Pen configuration restoration | Reopened Pen settings and switched Pressure back **On**. Confirmed `checked=true` and original toolbar description **`Pen, Colour #00e676, Width 3.0, Pressure Sensitivity 72%`**. | **PASS, setting restored** |
| Note saving and integrity | Returned from QA PDF editor via Home to library. `notestaking.save` reported successful `true` write to synthetic note, app process remained alive. Original exported source PDF SHA-256 remained unchanged. | **PASS for bounded path** |

**Source-grounded explanation:** `platform/PdfInkAnnotations.kt` documents opt-in Ink objects and implements `eligible`: `Tool.PEN`, 2–8192 samples, `pressureEnabled=false`, no taper/neon/direction/speed, fully opaque and valid width. `trailing` selects only **trailing** eligible strokes to preserve z-order. `platform/Pdf.kt` uses `PdfInkAnnotations.trailing` and writes `/Ink` annotations when requested. Existing `PdfInkAnnotationsTest.kt` covers the eligible/ineligible distinctions. Thus a pressure-enabled brush may render visibly but remain flattened in an ostensibly editable export.

## Limits and next work

- Positive result proves real PDF `/Ink` objects exist; it **does not** prove every third-party PDF editor can edit them, preserve styling or round-trip the markups.
- The default pressure-enabled Pen yields **no** editable PDF ink annotations despite an opt-in checkbox. UX could explain the restricted supported brush style in the export dialog or highlight when the page has zero eligible strokes; track as optional scope during B100 review rather than claiming the export encoder is broken.
- No bulk 50/200-page PNG export, interrupted provider write, low-space denial, TalkBack, true multi-touch zoom, physical S Pen, signed APK or full 79-case acceptance was performed.
- [Issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29) remains unresolved: library Split and Clear Selection unresponsive to prior ADB-injected taps, manual finger input required. No new source patch or APK build in this QA pass.
- All generated data remains in isolated `QA_B99_*` files/folder on device. No personal files or system configuration were changed; original pressure sensitivity was restored before leaving editor.
