# B99 Galaxy M13 - NT-060 occupied-target SAF collision controls

**Date:** 2026-11-17 (Asia/Kolkata). **Classification:** **PASS for ordinary Android Downloads filename collision (native .xnote and PDF); NT-060 remains P0 PARTIAL / NOT FULLY VERIFIED.** This is separate follow-up to [NT-060 SAF cancellations](B99-GalaxyM13-NT060-SAF-cancel-2026-11-17.md).

## Safe disposable setup

- Physical Samsung Galaxy M13 (SM-M135F), Android 14, authorized ADB serial `RZ8W103G6EL`; existing B99 `com.notestaking.app.debug` APK. No rebuild, install or settings modification.
- Used synthetic existing `QA_NT054_TEXT_TOP_20261117` as the source of new saves and uniquely named `QA_NT060_*` target files. Intentional **valid** occupied destinations were created only by copying existing synthetic QA documents into Downloads. These are test-only files, not personal notes.
- Pre-test destination hash was captured before asking Android's DocumentsUI Save picker to save a new output **with exactly the occupied filename**.

## Results

| Target type | Existing occupied target before save | New output after save | Outcome |
|---|---|---|---|
| Native `.xnote` | `/sdcard/Download/QA_NT060_OCCUPIED_COPY_20261117.xnote`, 73,610 bytes, SHA-256 `6f441b89771fb96cc5761786ff6c5ab760086133a997d23768d7a1518369040c` copied from existing mixed-content QA note | Android Downloads provider created separate **`QA_NT060_OCCUPIED_COPY_20261117 (1).xnote`**, 789 bytes, SHA-256 `388d706c9ce47b6094d4ef65d3a7b90e264aec3636adbbf1deda10c2d40c5005` (case-insensitive). Independent ZIP CRC verified clean, members `flow.xml` and `manifest.json`; `flow.xml` contains the known QA marker `QA_NT054_IME_CONFIRMED_20261117`. | **PASS - original hash unchanged; valid independent new note** |
| Shareable PDF via library `Export to PDF` | `/sdcard/Download/QA_NT060_OCCUPIED_PDF_20261117.pdf`, 7,595 bytes, SHA-256 `9d7f593a0b3a1d52b7a98180ae59c7340ef408bfcc2ef5b9ffacae5b32a6b228` copied from freshly exported Page 2 QA PDF | Android Downloads provider created **`QA_NT060_OCCUPIED_PDF_20261117 (1).pdf`**, 1,757 bytes, SHA-256 `608b0e016d3c790303a7c0a75692540c664eb8ddd58eee87727217ba504537bb`. PyMuPDF independently parsed one A4 page. This new PDF is blank because it comes from the QA text fixture affected by separate issue #30. | **PASS - original hash unchanged; new valid PDF** |

**Important:** These checks show standard Android Downloads/DocumentsUI **auto-renaming of occupied filenames**, not application-level verification of arbitrary existing destination streams. They did **not** simulate a lying or unknown-size provider, zero-byte occupied metadata, revoked URI, null output, interrupted writes, low storage, cloud provider stalls, failed PNG batch or required metadata-sidecar failures. **Do not infer NT-060 full signoff.**

The independent original two-page synthetic PDF remained hash `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`; NotesTaking remained active at My library. No source code, personal files or release artifacts were changed.
