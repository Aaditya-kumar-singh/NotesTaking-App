# SVG parser and raster failure safety — 2026-10-29

**Branch:** `fix/svg-render-failure-safety` based on `main`. Independent of the release-candidate and PDF-raster branches.

## Verified problems

`platform/ImageDecoder.kt` used `Bitmap.createBitmap(...).also { paintSlice(...) }` when rasterizing SVG files. When AndroidSVG threw after a bitmap had been allocated, the exception was swallowed by `runCatching` but the allocated bitmap was never explicitly recycled. Both its full-document and viewport-slice software paths shared the pattern.

The software SVG path parsed arbitrary on-disk SVG files without a file-size check. `VectorScenes` already had a separate 12 MiB pre-check, but read the complete file using unbounded `readBytes()` afterward. These files can be imported from untrusted content providers.

## Implemented

- Added `core/util/Svg.withinParseLimit` with an existing product-aligned **12 MiB** limit for DOM parsing on disk. Both software and GPU scene parsing use the same ceiling; their input streams are not parsed when a file is missing, empty, or too large.
- `VectorScenes.sceneFor` now calls the existing bounded byte reader, protecting the actual number of bytes read if a source file changes after a file-size check.
- Both `ImageDecoder` software rendering paths use one small bitmap-owner helper. It recycles the created bitmap if painting fails with a runtime exception or out-of-memory error, and transfers ownership only after the render succeeds.
- Added pure Kotlin regression tests for normal, empty, missing, and oversized SVG file handling, including the exact 12 MiB boundary.

## Gates and limitations

- GitHub Android CI must run unit tests, lint and APK build before merging. The generic structural scanner is **not** a Kotlin compiler.
- Android device testing is still required for malformed SVG, extreme path counts, rapid pan/zoom, hardware renderer behavior, bitmap allocation failures, document export, and caching.
- 12 MiB is a file-byte limit, **not** a guarantee of bounded SVG node count, recursion, vector drawing CPU or native GPU memory. Very complex XML can still take excessive parsing/rendering time within the limit. Future work should add complexity and time budgets if profiling warrants it.
- A legitimate SVG larger than 12 MiB will not be rendered; the original imported asset remains untouched and can still be exported.
- No note data, user settings, or release workflows were changed. The untracked local `SKILLS/` directory was left untouched.
