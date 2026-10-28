# Samsung Notes-style Canvas and Precision Eraser Upgrade

**Research/inspection:** 2026-10-28
**Project:** NotesTaking App; native Kotlin / Jetpack Compose, existing paged and infinite canvases
**Local folder:** `docs/canvas/`
**Status:** IMPLEMENTATION PLAN ONLY - no application code changed, no APK tested
**Linked:** `MASTER_CANVAS_UPGRADE_PLAN.md`, `FEATURE_BACKLOG.md`, `RESEARCH_AND_SOURCE_MAP.md`, `CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md`. The UI discoverability plan CV-101–CV-120 is responsible for keeping the eraser mode/size controls visible in a pinned, contextual toolbar. **Do not treat already having AREA mode in code as a complete user-facing fix**.

## 0. User's actual requirement

> When I erase handwriting, I do NOT want touching any part of a pen stroke to delete that whole stroke. I want a circular eraser whose size I can change. It must remove **only the exact written ink under the eraser's moving footprint**, while keeping all ink outside that footprint. Like Samsung Notes.

This is the P1 deliverable. A broader Samsung-Notes-inspired canvas polish is planned after that. **Do not copy Samsung assets/icons/UI screens or its proprietary code.** Reproduce the interaction logic and make our own branded UI.

## 1. Samsung Notes research: grounded UX reference

Samsung's official support pages document:
- Eraser types: **Stroke eraser** removes a touched line, **Area eraser** removes only the area touched, with a size slider.
- Some versions also show **Erase highlighter only** and **Erase all handwriting**, and allow holding the S Pen button for temporary erasing and returning to the previous pen.
- The source docs explicitly warn that even a small area eraser may not erase perfectly. We should aim to surpass imprecise behavior with correct geometric clipping, not promise pixel-perfect performance before validating it.
- Samsung also offers separate pen/highlighter type, thickness and color controls, favorite pens, handwriting selection, handwriting conversion, shapes and PDF writing workflows.

Official sources:
- https://www.samsung.com/us/support/answer/ANS10002888/ (FAQ: default 'Erase by line'; choose area and size)
- https://www.samsung.com/us/support/answer/ANS10003634/ (Stroke/Area/Highlighter-only + S Pen button)
- https://www.samsung.com/ae/support/mobile-devices/samsung-notes-composing-notes-with-handwriting/ (handwriting tools, eraser modes, selection, conversion)
- https://insights.samsung.com/2025/03/27/10-tips-for-increased-productivity-with-samsung-notes/ (favorite pens, eraser/toolbar UX)

**Product design mandate:** inspired by usability, not a one-to-one Samsung Notes visual clone. Avoid Samsung branding.

## 2. Actual current state and root cause (read from local source)

| Source location | Verified behavior | Deficiency relative to user request |
|---|---|---|
| `core/tools/ToolConfig.kt:12-18,63-64` | `EraseMode.STROKE`, `EraseMode.AREA`; default STROKE | Entire line deleted by default |
| `core/tools/ToolConfig.kt:126,173-175` | Eraser default `baseWidth = 24.0`, slider range 1..80 | `baseWidth` is actually used as a **radius**, UI caption says 'size'; clarify dimension and zoom |
| `ui/Popups.kt:919-961` | `EraserConfigPopup` already has Stroke/Area picker, width slider, Scale, Switch back, Markups, Scribble erase | Buried after selecting tool and tapping armed icon again; no obvious mode status at first tap |
| `ui/Toolbar.kt:288-327`, `ui/InfiniteToolbar.kt:95-125` | Same settings popup for both canvases | Opportunity to create consistent quick mode/size affordance |
| `canvas/InteractionController.kt:1495-1655` | Paged note supports area erase and per-gesture history/undo; erase happens against every cursor sample | Need continuous sweep, visible circle sizing, correctness with PDF pages and multi-page |
| `canvas/InteractionController.kt:953-967` | `PointerMode.ERASE -> eraseAt(vx,vy)` only for **current** MotionEvent point | Historical MotionEvent locations not replayed in paged mode; fast sweeps can skip ink |
| `canvas/InteractionController.kt:1178-1195` | Handwriting drawing DOES process historical positions | Reuse this pattern for eraser; avoid changing drawing pipeline |
| `core/model/Stroke.kt:683-739` | `erasedBy` deletes any stored sample falling within a circle, partitions surviving sample runs | Sparse samples give inaccurate cuts, clipping doesn't account for rendered ink half-width/end caps |
| `core/model/Stroke.kt:698-700` | `straight` stroke loses its **whole segment** at any touched point even in AREA mode | Definite mismatch; segment clipping and boundary interpolation needed |
| `core/infinite/EraseSession.kt:48-109` | Infinite canvas has area/stroke modes, spatial index, one undoable command per drag | Preserve indexed performance and shared semantics when geometry is upgraded |
| `ui/InfiniteInteraction.kt:1826-1842` | Infinite erase includes historical touch points | Still needs swept-segment rather than discontinuous point-circles |
| `settings/Settings.kt:356-400` | `erase_mode`, `base_width` and scale setting persisted | Must explicitly handle upgrade/migration and size units |
| `core/model/EraseRule.kt:6-46` | Text/images locked; dropped fills/tape erase whole; some filled shapes lose fill entirely | Define which objects are editable ink; no accidental deletion of text/images/PDF |
| `core/model/Stroke.kt`, `platform/CanvasPdf.kt`, codecs | Stroke fragments are editable model objects, then exported | Visually accurate live canvas / save / reopen / exported PDF required |
| `app/src/test/java/com/notestaking/app/core/model/StrokeEraseTest.kt` | Existing unit tests cover point-sample splitting and an invariant with `intersectsCircle` | Must update/add tests for segment clipping and page/zoom/cancel |

**No external eraser library needed for the first two phases.** The app already owns the vector stroke model, page and history transactions. Adding a bitmap-clear effect over PDF is WRONG (e.g. opaque white paint would cover the PDF background). Keep the eraser as an actual edit to annotation geometry.

## 3. Immediate workaround in existing app, before upgrades

- Tap **Eraser** in the editor toolbar.
- Tap the already-selected **Eraser** icon again to show its settings.
- Select **Area** instead of **Stroke**.
- Adjust the **Size** slider. This setting is persisted in tool preferences.
- Supported on both paged notes (including imported PDF annotations) and infinite canvas.
- Limitation: it remains sample-based; sparse or straight strokes are not precise. This is **not** the final fix.

## 4. Exact desired interaction, Samsung-inspired but branded

### Quick eraser strip
- First eraser tap: activate **Area Eraser** (the preferred default). Show a thin ring corresponding to eraser diameter at the cursor/hover point.
- Second tap on the active eraser or long-press: open a small panel, not a full settings screen.
- At top of panel: **Erase by Area** (default), **Erase Whole Stroke** (optional), **Highlighter Only** (optional, Phase B).
- Size: slider plus named chips **Fine / Medium / Large**, with a live circular preview and user-friendly diameter label. Consider 2..72dp diameter and 4/12/24/48dp presets; these are *proposed UI values*, not existing app units.
- Show `Area • 12dp` in a small transient indicator when settings change; don't block writing.
- Subsequent eraser use remembers last size/mode; provide a clear way to switch intentionally to whole stroke.
- Option **Switch back to previous pen** after a gesture; stylus side button should be temporary and restore pen automatically where supported.
- Pen hovering should show cursor location and effective size; finger needs touch indicator but not a persistent obstructing menu.
- Accessible names: 'Erase only touched ink', 'Erase whole stroke', 'Erase only highlighter', 'Eraser size'. Avoid raw developer names.
- Phone toolbar and tablet toolbar share one `EraserConfigPopup` data source, adapted for available width and left handed layouts.

### How default migration should behave
- For **new installations**, `ToolDefaults.configFor(Tool.ERASER).eraseMode = AREA`.
- Existing users may already have stored `erase_mode=stroke` because it was the default, even if they never chose it. The old format does NOT record whether the user explicitly selected STROKE. Therefore a migration cannot perfectly preserve their intent.
- Proposed rollout: a **one-time, versioned preference migration** switches the prior default-stroke setting to Area, shows a small 'Precision eraser now selected; tap again for whole stroke' hint and immediately saves the new preference; users can switch back. Record the migration key to avoid re-applying it after the user deliberately chooses Stroke again. Product owner should approve this trade-off before shipping.
- Never reinterpret old persisted `base_width` as a diameter without migration. It currently acts as radius in content coordinates; preserve apparent legacy size or migrate numerically to the new size representation with visual regression tests.

## 5. Precision erase algorithm - technical specification

### 5.1 Swept capsule, not disconnected sample dots
Current API models each event as an eraser circle. Build an erase path from every historical and current stylus coordinate. For consecutive cursor positions A and B, the swept eraser region is a **capsule** (disc around A + thick segment A→B + disc around B), radius R. If the eraser moves fast, do not leave gaps. A tap erases the disc at point A. A long gesture is the union of capsules.

Proposed pure Kotlin utility, shared by paged and infinite canvases:
- `core/geometry/EraserPath.kt` (or `core/stroke/StrokeClipper.kt`) with `EraseSweep(from, to, radius)` and robust collision + clipping routines.
- Coordinates: transform the actual event X/Y into canvas/page-local coordinate space *at sampling time*. Respect zoom, rotated PDF pages, canvas offset and per-page transforms.
- UI size semantics: eraser **diameter in dp** is intuitive; implement constant on-screen size as the default; convert to canvas radius using device density / zoom (with correct coordinate transforms). Optional 'Scale with content' toggle only if a clear user need remains.
- Store previous cursor position; reset on down/up/cancel, pointer handoff, page changes or view transforms. Do not bridge between two unrelated gestures/pages.
- Broad-phase intersection by bounding boxes / spatial index; only clip affected ink.

### 5.2 Partial vector-ink clipping
For a pen stroke polyline with local samples (position, pressure, recorded time):
- Determine intersections of each **ink geometry segment** with the swept eraser footprint; do **not** merely test stored sample centers.
- Compute boundary intersections analytically where feasible; interpolate boundary samples to produce pieces on either side of the erased gap, preserving pressure/time, ink width and existing tool settings. Test round caps carefully so they do not intrude back into the erased path.
- Account for actual rendered stroke thickness and variable-pressure ribbon width. A centerline-only cut may leave remnants; use rendered geometry bounds and a clipping strategy that produces the intended visible intersection.
- Retain all untouched ink and its z-order. Keep metadata (group, sync, tool config, neon, highlighter etc.) on surviving fragments.
- Work for single dot, 2-point **straight line**, sparse path, highlighter, dashed and taper strokes, overlapping strokes, cross-page strokes. A line crossed at its middle must become TWO smaller lines, not disappear.
- First implementation could combine analytic segment-vs-capsule cuts with dense sampling around complex curves, but the final acceptance gate must compare against the actual rendered ribbon; if it fails, evaluate geometric Boolean difference at the *vector ribbon* level. Never fake erased spaces with opaque white pixels.
- Prevent fragment explosion: merge/compact fragments where safe and use bounding-box filtering; preserve one undoable edit per gesture.

### 5.3 Shared semantics and consistency
- Paged path: `canvas/InteractionController.kt` -> page-space `eraseAreaFromPage` -> updated `Stroke.erasedBySweep`.
- Infinite path: `ui/InfiniteInteraction.kt` -> `core/infinite/EraseSession.kt` -> same new pure Kotlin clipper.
- Keep `EraseMode.STROKE` behavior available without changing its explicit label.
- **Eraser affects only the user's overlays/drawing objects**, never the source PDF text, PDF vector graphics or embedded image content. PDF text **markup annotations** are independent and need an explicit setting. Highlighter-only mode should ignore regular ink entirely.
- Text boxes and inserted images should remain protected by default; deletion via selection. For shape paths/fills/tape, document separate select/delete behavior rather than surprisingly removing whole items during precision erasing.
- Cursor ring diameter must correspond to the actual hit radius. Popup preview, page canvas, infinite canvas and exported result must agree.
- Save native vector fragments (not just visual masks), with clean undo/redo and persistence through `.xnote` and `.xcanvas` and PDF export.

### 5.4 Undo, cancel and performance
- A complete pointer-down → move → up gesture is **one undo step**, even if it touches ten strokes or two pages. Redo must yield identical visual fragments.
- An Android `ACTION_CANCEL` from palm rejection or system interruption must safely rollback changes (or follow the app's existing explicitly tested cancellation policy) without corrupting snapshots. No runaway eraser after lift; release of side-button hover mode ends its transaction.
- No per-move full-document scans in infinite canvas; use `InfiniteDocument.itemsIn` spatial index; paged editor limit to visible/intersecting pages, invalidate the affected render region rather than whole page where possible.
- Track p95 erase processing time and allocations while erasing dense strokes; use incremental sweeps and bounded work. Benchmark on real S Pen, generic USI and finger devices, low-end Android tablet and fast 120Hz tablet.
- Avoid refitting untouched ink or changing its look after the erase.

## 6. Broader Samsung Notes-inspired canvas plan (reuse existing code)

| Samsung-like capability | Current app baseline (confirmed vs needs testing) | Target priority |
|---|---|---|
| Pen selector, calligraphy and pressure tools | Existing Tool/ToolConfig; UI already has pen popup | P1 polish: favorites and easy size/color/pressure controls |
| Eraser area/stroke/size | Existing but Stroke is default and area geometry imprecise | **P1 immediate** fix |
| Highlighter-only eraser | Not present as distinct mode in current model | P2 add, vector ink only |
| Stylus side-button quick erase | Existing controller/pen-button tool | P1 validate temporary and explicit mode |
| Handwriting resize/recolor/move | Existing lasso, transforms and selection infrastructure | P2 polish object editing |
| Paper styles/template and PDF notes | Existing PageStyle/Page, insert-after PDF | P1 + Page action visible and sheet variants |
| Handwriting straighten, smart shapes | Existing shape recognition/ruler & shape tools | P2 targeted interaction QA |
| Easy writing pad (magnified writing strip) | Not verified as a dedicated Samsung-like mode | P3 feasibility feature |
| Handwriting-to-text | Existing strokes, no installed recognition engine | P3 Digital Ink R&D per OCR plan |
| Separate handwriting/PDF annotation layers | PDF background vs ink layers partially present | P2 layer UI discovery; avoid duplicate data models |
| Pen favorites/preset chips | Existing pen types, preferences; dedicated favorite pen slots proposed | P1 |
| PDF text markup/highlighting | Existing PdfSource text/markup | P1 avoid eraser accidentally altering original PDF |
| Audio recording inside notes | Existing audio integration | P3 improve ease of use |
| Document sync/cloud and Galaxy AI | Not currently a requirement; offline-first | Out of scope, not necessary for Samsung-like writing quality |

The user asks for Samsung Notes-like **functionality**, not to copy its exact screen, assets or services.

## 7. Implementation milestones and exact issue IDs

### Eraser Phase A (P1, critical UX fix)
- **CV-061**: Make Area the default for newly installed and upgraded app, with a one-time preference migration and visible mode indicator.
- **CV-062**: Rework single shared eraser popup: precise mode wording, size slider with numeric diameter and preview, Fine/Medium/Large presets, live cursor feedback.
- **CV-063**: Implement swept capsule paths and historical MotionEvent processing in **paged** view; integrate with current InfiniteInteraction historical sampling.
- **CV-064**: Replace sample-point deletion with robust visible geometry clipping for normal strokes and 2-point straight lines.
- **CV-065**: Use the same shared eraser geometry in both paged and infinite canvases; verify coordinate transforms/zoom.
- **CV-066**: Add comprehensive erase undo/redo, cancellation, save/reopen, legacy format, dense-stroke and PDF-overlay tests.
Exit criterion: Open a PDF, draw one continuous long handwritten stroke, choose fine/medium/large erase, erase its center in one swipe; only the path under the cursor vanishes, left/right ink stays, undo restores original, save/reopen and PDF export match. Repeat in infinite canvas.

### Eraser Phase B (P2, finishing behavior)
- **CV-067**: Add highlighter-only erasing and protect ordinary ink; define PDF text markup option separately.
- **CV-068**: Stylus eraser tip/side-button, hover ring, finger/palm rejection, and return-to-previous-tool; validate hardware.
- **CV-069**: Selective erase for shape outlines; keep inserted images/text and PDF source protected; clarify fills/tape behavior.
- **CV-070**: Refine visible eraser cursor, 48dp touch affordances, shortcuts, left-handed popup and screen reader labels.

### Samsung-style canvas refinements (reuse current backlog; avoid duplicate features)
- **CV-071**: Refine pen preset dock/picker, instant width and color switching using CV-014..020.
- **CV-072**: Confirm and improve handwriting lasso, resize, recolor, straightening and auto-shapes using CV-031/032.
- **CV-073**: Easy Writing Pad experimental UI with zoomed handwriting strip; preview and place line accurately without sacrificing existing direct ink.
- **CV-074**: Samsung-inspired PDF handwriting study workflow audit: add sheets, templates, markers, source PDF locked, thumbnail navigation and export (CV-006..013).
- **CV-075**: Tablet performance and accessibility polish: 8-11 inch devices, 60/90/120Hz pen latency, responsive menus and no drawing thread blocking.

## 8. Regression test matrix

| ID | Exact test | Expected |
|---|---|---|
| ER-01 | New install selects eraser | Mode = Area |
| ER-02 | Existing settings migration | One-time switch strategy, user can choose Stroke and choice persists |
| ER-03 | One continuous cursive line, erase short middle segment | Two retained ink fragments, only touched part removed |
| ER-04 | Straight 2-point line, erase in middle | Two surviving segments, not whole deletion |
| ER-05 | Sparse 2-point ordinary pen | Real segment intersection cuts instead of checking only endpoints |
| ER-06 | Tiny eraser over thick pressure ribbon | Eraser removes the visibly contacted ink without leaving cap/fringe artifacts |
| ER-07 | Fast single MOVE event with history points | Continuous erased sweep with no gaps |
| ER-08 | Erase over overlapping blue pen and yellow marker in highlighter-only mode | Marker erased only; pen unchanged |
| ER-09 | Erase on imported PDF with ink | PDF underlying text/vector/photo unchanged, overlay edited correctly |
| ER-10 | Change size while zoomed, rotate PDF, work across pages | Preview ring and actual hit area agree in both coordinate spaces |
| ER-11 | Undo/redo a 15-stroke sweep | One undo restores all stroke(s) faithfully; one redo repeats |
| ER-12 | Cancel from palm rejection or app suspend | Consistent, safe rollback/commit policy; no lost unrelated strokes |
| ER-13 | Save .xnote/.xcanvas, reopen, PDF export | Same ink appearance and fragment locations; PDF page order maintained |
| ER-14 | Legacy notes/older eraser preference | No altered existing ink, no broken serializer/geometry |
| ER-15 | Dense 10k-stroke canvas and heavy PDF | Measured good latency without OOM/ANR; target after baseline |
| ER-16 | Text box, inserted image, locked item, native PDF content under ring | Never deleted in precision handwriting mode |
| ER-17 | Stylus tip, barrel button, eraser-end and fingers | Same mode semantics, correct pointer cleanup |
| ER-18 | Thin, dashed, neon, taper, highlighter and pencil variants | No visual mismatch between retained fragments and original material |

## 9. Code touch points, scope and safety

Likely files to modify:
1. `core/tools/ToolConfig.kt`, `settings/Settings.kt`: new default, size meaning, migration, persisted modes.
2. `ui/Popups.kt`, `ui/Toolbar.kt`, `ui/InfiniteToolbar.kt`: shared Samsung-inspired eraser settings, quick chips and cursor feedback.
3. `core/model/Stroke.kt` and new `core/stroke/StrokeClipper.kt` (or `core/geometry/EraserPath.kt`): geometry-aware partial clipping, interpolation and metadata.
4. `canvas/InteractionController.kt`, `ui/InfiniteInteraction.kt`, `core/infinite/EraseSession.kt`: path history, swept erase and grouping.
5. `core/model/EraseRule.kt`, `ShapeItem.kt`, `ConnectorItem.kt`: selective erasing without destructive surprises.
6. `app/src/test/java/com/notestaking/app/core/model/StrokeEraseTest.kt`, `core/infinite/EraseSessionTest.kt` plus new test fixtures; `androidTest` for device inputs.
7. `format/DocumentCodec.kt` / `CanvasCodec.kt` only if the new fragment representation requires serialization changes. **Prefer the existing Stroke object structure** to avoid changes to old file formats.

**Dependencies:** No third-party dependency is required for precision erasing. Existing Kotlin geometry/stroke/history code is the starting point. Official Android MotionEvent historical-event API supplies the path input. Jetpack Ink is a separate benchmark candidate but not necessary to solve the eraser problem.

**Safety/release gate:** Fix or isolate the previously found false-save-success and session-replacement risks (CV-001/CV-002) before shipping any destructive vector editing. Back up test notes before validation. Device tests and clean GitHub CI are mandatory. Do not claim this feature is currently fixed merely because documentation exists.

## 10. Implementation decisions summary
1. Choose **Area** as the user-facing default; preserve access to **Whole Stroke**.
2. Add a **clearly sized, zoom-correct eraser ring** and easy quick-settings panel.
3. Compute the **swept path** between motion points, including historical points and fast pointer motion.
4. Do **real vector clipping** of visible handwriting, including straight lines and sparse strokes; never cover PDFs with white paint.
5. Reuse both canvases' existing history/undo systems; keep original PDF contents untouched.
6. Measure QA on real hardware before calling it Samsung-Notes-quality.
7. Keep broader pen favorites, easy writing pad, OCR and design upgrades in their existing separate work streams.
