# PDF import resource safety — implementation checkpoint

**Date:** 2026-10-29
**Branch:** `fix/pdf-import-resource-guards` (independent of the combined security-integration branch).
**Status:** Implemented locally; GitHub Android CI and device verification are mandatory before merge.

## Source findings and decision

The native PDF import flow in `ui/Editor.kt` opens a SAF stream and copies its contents to a temporary local file before handing it to PDFium. This already avoids loading the full PDF into the Java heap but did **not** limit how many bytes a provider could stream. A malformed or oversized source could consume device storage. `PdfSource.allPageSizePoints()` allocates an array proportional to the PDF page count, so a hostile page count also needs an application-level bound. `PdfImporter.pages()` converted page dimensions without rejecting NaN, infinity or absurdly large MediaBox values.

## Implemented

1. `platform/PdfImportSafety.kt`: stream PDFs to an app-private temp using a 64 KiB buffer and a hard **256 MiB** actual-byte limit. Verify a `%PDF-` signature occurs in the initial 1,024 bytes; still rely on PDFium to validate PDF structure. Poll cancellation, refuse repeated zero-byte reads, synchronize completed output, and delete partial files on errors/cancellation or bad header. The source stream is always closed.
2. `ui/Editor.kt`: central `stageImport()` used by single, batch and inserted-PDF paths now uses the common resource gate.
3. `ui/Editor.kt`: reject zero, negative or over **5,000** pages before allocating per-page sizes or creating a note, including the PDF-insertion path.
4. `platform/Pdf.kt`: replace malformed/nonfinite or oversized PDF page geometry (outside 1–14,400 points) with the existing previous-page/A4 fallback rather than creating impossible page rectangles.
5. `PdfImportSafetyTest.kt`: exact byte-limit boundaries, over-limit streaming and temp cleanup, malformed/missing headers, accepted preambles, cancellation, mid-read provider failure, page-count and MediaBox constraints.

## Limits and compatibility

A source PDF larger than 256 MiB, a PDF with more than 5,000 pages, or a PDF without its signature in the first 1 KiB will be rejected. These are explicit protective limits rather than intrinsic PDF-format limits. User-visible error handling still uses the existing general import-failure flow and should be made more specific in a follow-up. For very large legitimate files, review benchmarks and the app's memory/storage budget before changing the limits.

## Still required

- Full GitHub Android CI: unit tests, lint and APK assembly.
- Device testing: malformed, password-protected, zero-page, disconnected provider, oversized/many-page PDFs, successful 500/1,000-page document, cancellation, low disk space, two-pane PDF annotations and insert-PDF.
- Separate rendering memory review: `PdfSource.render()` can allocate one or more ARGB_8888 bitmaps at 4,096 × 4,096 and PDFium/native decoding may consume additional memory. Native PDFium fuzzing, sanitizer tests, timeouts and hostile object-depth behavior are **not** solved by this import-stage cap.
- No new release or production-ready claim before Android device acceptance.

Reference: PDF format parsing and page count still depend on the existing PDFium native library; the app now adds resource gates *before* parsing and page-array materialization.
