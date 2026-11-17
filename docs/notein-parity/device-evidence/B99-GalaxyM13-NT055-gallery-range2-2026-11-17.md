# B99 Galaxy M13 - NT-055 PNG Gallery and fresh Page 2 range export

**Date:** 2026-11-17 (Asia/Kolkata). **Classification:** **PASS for two-page gallery and fresh single-page range controls; NT-055 remains PARTIAL / NOT FULLY VERIFIED.** See the earlier [range/selection report](B99-GalaxyM13-page-export-and-selection-2026-11-17.md) and [two-page PNG report](B99-GalaxyM13-multipage-PNG-editable-PDF-2026-11-17.md).

## Baseline

- Physical Samsung Galaxy M13, `SM-M135F`, Android 14, ADB `RZ8W103G6EL`. Existing B99 debug NotesTaking APK `com.notestaking.app.debug` (source `b299cbb8972267eebe103493fa093f7e5cfa6f70`); no APK rebuild or install.
- Only existing synthetic `QA_Imported_B99_20261116` PDF note and `QA_*` outputs were accessed. Original two-page QA PDF `/sdcard/Download/QA_NotesTaking_B99_DeviceTest_20261116.pdf` retained SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`.

## Real-phone checks

| Scenario | Observations | Classification |
|---|---|---|
| Open batch Page 1 PNG in independent gallery | Samsung Gallery `com.sec.android.gallery3d/.app.GalleryActivity` resumed with read-granted MediaStore image URI ID `1000039788`; screenshot independently showed multiple separated green pen strokes on white, no blank/corrupt rendering. Existing `QA_Imported_B99_20261116-p01.png`, 30,655 bytes, SHA-256 `5631e09951ce2a9a56bfa6851546234403256c9e184fdaa88f299a47b689e4ab`. | **PASS - Gallery Page 1** |
| Open batch Page 2 PNG in independent gallery | Samsung Gallery resumed for MediaStore image URI ID `1000039789`; independent screen showed a distinct near-vertical green pen mark, not Page 1's strokes. Existing `QA_Imported_B99_20261116-p02.png`, 18,727 bytes, SHA-256 `9491ab77c02a756de3e35b0ff1e428f511fbc5f1289fc9bbd45b61e7e599b2e9`. Both image hashes unchanged. | **PASS - Gallery Page 2** |
| Fresh custom range (2) to PDF | Opened `QA_Imported_B99_20261116` in NotesTaking, `Export pages` > `Shareable PDF` > `Custom range`; entered exactly `2` and used Android Downloads Save picker to create **new** `QA_NT055_FRESH_RANGE2_20261117.pdf`. File 7,595 bytes, SHA-256 `9d7f593a0b3a1d52b7a98180ae59c7340ef408bfcc2ef5b9ffacae5b32a6b228`. Independently PyMuPDF-parsed **one** A4 page (595.3 x 841.9 pt) with 643 green pixels in the tested RGB rasterization. | **PASS - confirmed fresh output** |
| Page 2 content identity | Rasterized both new PDF and previously existing `QA_B99_Page2_CustomRange_20261117.pdf` at 2x PyMuPDF scale (1191 x 1684 pixels); **rendered bytes matched exactly**, both raster SHA-256 `980eb25e224082abcf2b1d8c2b6f15ef5659f7ded14b5c67bf122c16883ceafd`. This validates new output's Page 2 appearance without asserting who previously created the older file. | **PASS - exact rendered match** |
| Non-destructive checks | Original two-page QA PDF retained SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`; app remained alive. | **PASS - original preserved** |

## Remaining NT-055 work

A two-page fixture does **not** verify multiple non-adjacent ranges such as `1,3-5`. Full NT-055 additionally requires a synthetic five-or-more-page fixture, This/Selected/All and PNG/range matrix, all gallery opens, precise page order/valid MIME and independent image integrity on each output. Do not mark entire NT-055 or release gates verified. No personal documents, application code, protected branches, network services or release artifacts were changed.
