# Precision Area Eraser — geometry and fast-sweep correction (2026-10-29)

**Branch:** `fix/precise-area-eraser-clipping` (based on the green `main` baseline, independent of still-running security, PDF and undo builds).
**Scope:** Both paged notebook ink and infinite-canvas ink. Not a rebrand or complete Samsung Notes UI redesign.

## Verified cause

`Stroke.erasedBy()` previously tested only raw sample centers. Straight-line mode stores just two endpoints and its old Area-mode branch **erased the entire segment on any contact**. Long, sparsely sampled freehand segments also remained uncut through their middles. Both canvases call this same stroke method.

Both canvas controllers treated erasing as independent disks at observed pointer locations. The paged controller omitted MotionEvent history altogether; infinite-canvas collected historical points but did not bridge gaps. Fast drags could skip ink between events.

## Implemented

1. **Shared segment clipping:** `core/model/Stroke.kt` now solves the line-segment/circle intersections analytically, classifies inside/outside intervals and inserts cut-boundary samples. Surviving fragments keep original tool, configuration, speed/smoothing scales, pressure, sample timestamps and group/sync/lock metadata. It retains the original if untouched, removes it only when fully covered, or creates ordered fragments for actual gaps. It applies to straight two-point strokes and ordinary handwriting.
2. **Nib safety margin:** the geometric cut is expanded by half the configured pen width to prevent the fragments' round caps protruding inside the nominal eraser footprint. Variable-pressure brushes, smoothed ribbons, neon and complex non-circular nibs still require visual verification on an actual device; the margin is a conservative approximation, not a pixel-perfect mask.
3. **Continuous fast sweep:** `core/geometry/EraserSweep.kt` interpolates overlap-spaced cursor samples between consecutive viewport positions at the active zoom. Both controllers use the same pure-Kotlin helper, and paged mode now consumes MotionEvent history. Start/end of a gesture clears the previous point so independent taps do not draw an unwanted eraser line. A 512-step/event safety cap avoids unbounded work on corrupted pointer coordinates; very large event jumps may therefore be approximated coarsely.
4. **Preserved undo structure:** existing `ReplacePageItems` and infinite `EraseSession` history paths are unchanged, so an eraser drag still records one undoable action. No image, PDF background, text or note file is deleted by this geometry code.

## Automated regression coverage

- `StrokeEraseTest`: full removal, misses, end trimming, two-piece mid cuts, multiple crossings, sparse ordinary strokes, two-point straight lines, off-axis cut geometry, pressure/time interpolation, copy independence, repeat erasing, configuration/group preservation and hit-test agreement.
- `EraserSweepTest`: tap, fast straight and diagonal drag, zero movement, reset between gestures and bounded pathological jumps.
- `EraseSessionTest`: straight-line area cut on infinite canvas plus undo/redo restoration of original item identity.
- Additional device acceptance: measure memory and latency for very long sampled handwriting, stylus hover/side button, fast diagonal strokes, crossing PDF annotations, different zoom levels, cancelled and multi-pointer gestures, heavy highlighter and dashed strokes, export and reopen.

## Limitations / next phase

The **default eraser mode** remains the prior stored setting; the legacy default is whole-stroke. Changing it requires a one-time user-preference migration or an explicit product decision so a user's intentional Stroke-mode choice is not silently overwritten. Users can select Area in the existing eraser popup.

The helper interpolates overlapping discs rather than calculating a single exact swept capsule from pointer A to B. Device rendering still smooths freehand strokes after clipping; the visible ribbon and its dynamic pressure/velocity can differ slightly from the raw polyline used for cutting. Pixel-exact area-mask clipping and renderer pressure-width parity are separate tasks.

The branch must pass GitHub unit tests, lint and APK assembly. It has **not** been physically validated on an Android tablet or stylus.
