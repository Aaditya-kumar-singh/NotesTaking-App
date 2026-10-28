# Oversized image and clipboard import safety

**Branch:** `fix/bounded-image-clipboard-imports` (independent of import-archive and backup branches)
**Date:** 2026-10-28
**Status:** Implemented for CI validation; not device-tested or production-certified.

## Root cause

Image picker, sticker picker, camera photo, and both canvas clipboard paths previously read untrusted URI/file data using unbounded `readBytes()`. A malicious provider or oversized media file could allocate excessive heap; picker and sticker reads ran on the UI thread. Plain SVG clipboard text also converted directly to bytes with no cap. Existing rendering already has sampled bitmap decoding, but that does not protect the initial encoded-byte allocation.

## Implementation

- `platform/BoundedBytes.kt` reads streams incrementally while enforcing a 32 MiB maximum and file-size preflight when available. Provider metadata is never trusted as the only limit. Rejects excess *actual* bytes, including providers that report no content length; closes streams at call sites.
- `platform/SystemClipboard.kt` uses bounded URI reads and enforces a separate 2 MiB encoded SVG text limit.
- `MainActivity.kt` applies bounded reads for picked images, multi-select stickers, camera photos and infinite-canvas image imports. Image and sticker picker I/O has moved to `Dispatchers.IO`, returning to the main coroutine context for editor mutations.
- `ui/Editor.kt` applies bounded clipboard/sticker reads and refuses over-limit byte arrays at insertion points; `ui/InfiniteEditor.kt` also refuses oversized encoded arrays directly.
- `BoundedBytesTest.kt` covers exact-sized reads, empty streams, known file preflight, a large provider stream with no declared length, multibyte SVG sizing and over-limit input.
- Existing image decode pipeline still probes native size and down-samples files for rendering, but this change does not replace the need for malformed-image decoder testing.

## Compatibility / tradeoffs

The current upper bound is **32 MiB per encoded image**, with **2 MiB for SVG text from the clipboard**. Images above this bound will be rejected rather than causing an unbounded heap allocation. This cap must be benchmarked with realistic high-resolution camera photographs and tablet memory constraints; a file with small encoded bytes could still describe enormous dimensions, so decoder-level pixel, SVG complexity and CPU limits remain a separate safety task.

## Validation gates

1. Run `python tools/ci/check_project.py`, `git diff --check`, and GitHub `testDebugUnitTest / lintDebug / assembleDebug`.
2. Test Android ContentResolver with streams that return unknown size, huge payloads, IOException midway, revoked grants and 0-byte files.
3. Test pasted SVG, external clipboard image, camera, screenshot, gallery picker and sticker multi-select on a real low-memory tablet.
4. Check the UI shows an appropriate read/import failure; do not silently place partial images.
5. Audit remaining unbounded reads for fonts, theme files, settings backups, audio, general file imports and PDF; those are not claimed fixed by this patch.
6. Do not merge on red CI or publish a production APK before device testing.

**No user notes, library files, branches or releases were deleted.**
