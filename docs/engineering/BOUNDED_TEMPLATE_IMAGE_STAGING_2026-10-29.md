# Bounded image-to-template staging — 2026-10-29

**Branch:** `fix/bounded-template-image-staging` (based on `main` at `5a93bd7`). Independent of the font/theme and pen/palm branches awaiting GitHub verification.

## Problem

`Editor.templateFromImage` streams the bytes returned by Android's document provider into a temporary image file with `InputStream.copyTo`, with **no file-size limit**. A very large, corrupted, or malicious provider could fill temporary app storage before the image decoder or template package validator sees the file.

## Fix

- Introduced small pure `BoundedCopy.copy(input, output, maxBytes)` to copy only actual received bytes with a fixed 64 KiB buffer. Unlike reading into a byte array, it does not allocate memory proportional to the image. It rejects sources exceeding the existing app image-import budget **32 MiB** and prevents infinite looping on providers that repeatedly return zero bytes without progress.
- `Editor.templateFromImage` now uses `BoundedCopy` before passing the staged image to `TemplateLibrary.packageFromImage`. Its existing `finally` block removes the temporary image on **success, cancellation, or failure**.
- `BoundedCopyTest` covers exact-size copies, oversized unknown-length streams, zero-byte stream stalls, intermittent empty reads, empty images and provider I/O exceptions.

## Tradeoffs and remaining gates

- Images over 32 MiB are rejected as image-to-template candidates. Existing template packages still keep their own 16 MiB package validation; an encoded image can pass staging yet fail package validation.
- This protects only the import staging size, not decoder memory, compressed-image decompression bombs, or complex image rendering. Those need separate validation and real tablet stress tests.
- Full GitHub Android unit tests, lint and APK build must succeed before integration into `main`. No physical device testing or release was performed.
- Existing note data and the unrelated untracked `SKILLS/` folder were not modified.
