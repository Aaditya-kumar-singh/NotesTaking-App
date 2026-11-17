# B99 Galaxy M13 - NT-056 third-party PDF opening and user-facing export hints

**Date:** 2026-11-17 (Asia/Kolkata). **Verdict:** **PASS for the bounded NT-056 phone acceptance scenario** (clear native-editability messaging and opening a synthetic exported PDF outside NotesTaking); **NOT** NT-057 third-party annotation editability or broader release sign-off.

## Device and fixture

- Physical Galaxy M13 (SM-M135F), Android 14, ADB serial `RZ8W103G6EL`; existing B99 `com.notestaking.app.debug` APK (source `b299cbb8972267eebe103493fa093f7e5cfa6f70`).
- Previously exported synthetic two-page `/sdcard/Download/QA_NotesTaking_B99_DeviceTest_20261116.pdf` (13,770 bytes; SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`); tested read-only, original bytes unchanged.

## Observations

1. Confirmed Android reports Google Drive PDF Viewer `com.google.android.apps.docs/com.google.android.apps.viewer.PdfViewerActivity` for the `application/pdf` VIEW intent. A direct ADB-constructed `content://com.android.externalstorage.documents/...` intent **failed due to shell URI grant restrictions**; this is not evidence of an NotesTaking export bug. Using the document's indexed MediaStore URI with a read grant opened the same QA PDF successfully in the Google Drive viewer.
2. The resumed activity was independently confirmed to be `com.google.android.apps.docs/com.google.android.apps.viewer.PdfViewerActivity`, not NotesTaking. The viewer's accessibility tree showed **PDF view** and **page 1 of 2**; after scrolling, **page 2 of 2**. **PASS: external open/navigation** (not pixel-level visual comparison).
3. In NotesTaking, reopened `QA_Imported_B99_20261116`, entered **Export pages**, and read the **Shareable PDF** hint on device: `One PDF for viewing and sharing. Drawings appear on the pages but cannot be edited as NotesTaking pen strokes in a PDF reader. Keep your original .xnote to continue editing.` The exact text also matches resource `R.string.page_export_pdf_hint`.
4. Toggled **Editable PDF ink (simple pen strokes)**. The displayed hint changed to: `Eligible pen strokes remain separately editable in compatible PDF readers. Keep the original .xnote for full editing.` The exact text matches `R.string.page_export_pdf_editable_hint`.
5. The wording is truthful for the existing evidence: earlier B99 testing independently parsed a real editable `/Ink` annotation only for an eligible pressure-off simple-pen stroke; pressure-enabled strokes remained flattened. The hint correctly qualifies both eligibility and compatible readers.

## Limits

- No Xodo/Acrobat annotation selection/edit/round-trip in this test. NT-057 stays **NOT VERIFIED**; Google Drive opening alone cannot establish native PDF annotation editing.
- No exhaustive visual diff, all-page/range matrix, alternate viewer, heavy PDF, or 200-page stress acceptance. NT-055 and release gates remain separate.
- No APK install/build, protected-branch mutation or personal document accessed. Phone returned to NotesTaking QA library after subsequent NT-054 checks.
