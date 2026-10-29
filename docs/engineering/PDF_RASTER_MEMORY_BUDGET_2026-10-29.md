# PDF raster memory budget — 2026-10-29

**Branch:** `fix/pdf-raster-memory-budget` (based on verified `main`, independent of pending combined feature integration).

## Root cause

`PdfSource.render()` could allocate a raw 4096×4096 ARGB bitmap **and** another one to apply PDF color filters: 128 MiB of Java/graphics pixel buffers in one render before PDFium's native cache, markup data or compositing. The unfiltered source bitmap was recycled only on a few successful/failed returns. If a PDFium call, markup renderer, bitmap allocation, or color-matrix painting failed, bitmaps could remain allocated. Large page requests were clamped independently on both dimensions and could distort the page aspect ratio.

## Implemented

1. `platform/PdfRasterBudget.kt` has a single **8 MiB-pixel (8,388,608 pixels)** budget and the existing 4096-pixel maximum side. For preview/full-page rendering, it scales both dimensions *together*, preserving page aspect ratio. A filtered render now needs no more than ~64 MiB for its pair of ARGB pixel buffers, rather than 128 MiB. Ordinary A4 and standard tablet renders are unaffected.
2. `PdfSource.renderPage()` applies that uniform size cap. `renderRegion()` continues using exact crop geometry and refuses any region over the budget instead of silently shrinking and misaligning it.
3. `PdfSource.render()` owns raw and filtered bitmap lifetimes with `try/finally`. On failure, both buffers are recycled. If rendering throws an allocation `OutOfMemoryError` or a `RuntimeException` from malformed page content, the render request fails gracefully instead of retaining buffers.
4. `PdfRasterBudgetTest.kt` checks normal sizes, preserving very wide/tall aspect ratios, max pixel count, pathological dimensions, and normal 4K viewport region eligibility.

## Limits and acceptance

- **Potential blank oversize viewport**: A single cropped region larger than 8,388,608 pixels now returns no raster. It is intentionally not stretched: downscaling the crop without adjusting viewport math would show wrong coordinates. A future tiled renderer should divide it into multiple smaller renders and retain high-resolution mapping. A 3840×2160 region remains allowed; 4096×4096 does not.
- The 64 MiB cap refers only to up to two ARGB_8888 pixel buffers created by one `PdfSource` rendering call. Concurrent renders, native PDFium caches, GPU textures, and the notebook's other bitmaps are not included. Memory pressure may still affect a low-RAM device.
- Full GitHub CI unit tests/lint/APK required before merge; actual tablet checks must cover portrait/landscape, low memory, 4K screens, rotated pages, zoom, PDF markup, night mode inversion, keep-original-image-colors, and export.
- This does not change note data, undo stacks, storage, or Android permissions. It is not a PDFium security fuzzing fix.

**Production APK release remains blocked on device testing.**
