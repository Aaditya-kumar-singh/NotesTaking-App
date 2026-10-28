# NotesTaking App - Master Canvas and PDF Upgrade Plan

Research date: 2026-10-28
Repository: https://github.com/Aaditya-kumar-singh/NotesTaking-App
Local workspace: C:\Users\WHITE-SNAKE\Desktop\xnotes-android
Observed branch: master
Observed commit: facce2c
Document state: PROPOSAL ONLY - not implemented
Scope: native Android Kotlin/Jetpack Compose, bundled PDFium, existing OpenGL/CPU stroke renderer
Companions: RESEARCH_AND_SOURCE_MAP.md, FEATURE_BACKLOG.md, OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md, SAMSUNG_NOTES_STYLE_ERASER_PLAN.md, VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md, BRAND_UI_AUDIT_MATRIX.md, CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md and CANVAS_FEATURE_VISIBILITY_MATRIX.md in this folder

## 1. Product direction

Make the existing editor a tablet-first, offline-first notebook, drawing canvas and PDF study workspace. The experience should combine: (1) inserting blank writing pages into a PDF-backed note, (2) natural pen/pencil/brush tools and quick presets, (3) precise erasers and selection, (4) high-quality vector ink, (5) reliable editing, saving, reopening and exporting, (6) an understandable UI for both phones and tablets.

**Do not rewrite the existing canvas**, change technology stacks, require an account, or add a cloud API for these features. Keep .xnote, .xcanvas, .xtemplate backwards compatible. The user must retain ownership of local documents. The main PDF-backed note has an embedded source PDF plus logical note pages and annotations; it is not necessarily a rewritten source PDF.

## 2. Verified existing capabilities - avoid duplicating

Status labels: EXISTING = code path verified; ENHANCE = code exists but workflow or quality should improve; NEW = planned feature; VERIFY = behavior needs runtime or deeper investigation.

| Capability | Status | Current source evidence | Action |
|---|---|---|---|
| Blank page after current page / PDF-backed note | EXISTING | ui/Editor.kt:6568-6586, 6646-6681; core/model/Page.kt:11-14 | Make prominent and add before/template/batch choices |
| Import extra PDF into note | EXISTING | ui/Editor.kt:6687-6717; MainActivity.kt:537-539; ui/SidePanel.kt:590-591 | Improve preview, progress, rollback and placement |
| Page duplicate, delete, rotation, reorder, copy/paste | EXISTING | ui/Editor.kt:7090-7203; ui/SidePanel.kt:590-605 | Improve page thumbnails, multi-selection, undo safety |
| PDF background independent of annotation layers | EXISTING | core/model/Page.kt:11-20, core/model/Document.kt:27-32 | Preserve PDF source page indices and coordinate mapping |
| Pens (standard, dashed, calligraphy, speed, taper) | EXISTING | core/tools/Tool.kt:4-10; core/tools/ToolConfig.kt:118-125 | Add realistic graphite/gel/fountain/brush variants using current engine |
| Highlighter and PDF text markups | EXISTING | Tool.kt, ToolConfig.kt:21-31, 69-97 | Refine blending, no darkening at overlaps, text-aware strokes |
| Stroke and area erasers | EXISTING | ToolConfig.kt:8-18, 63-68 | Improve modes, selective content erasing, preview, shortcut |
| Color overrides, opacity and pressure controls | EXISTING | ToolConfig.kt:41-102; ToolConversions | Add favorite palette presets, hex input, recent history |
| Lasso, shapes, text, image, tape, markup | EXISTING | core/tools/Tool.kt | Improve discovery and cross-page selection, not reimplement |
| Page styles and templates | EXISTING | core/model/PageStyle.kt:7-12, 49-80; assets/templates | Expose template picker during page insertion |
| Infinite canvas and GL/wet ink | EXISTING | gl/, ui/InfiniteEditor.kt, canvas/CanvasView.kt | Preserve separate .xcanvas semantics and low-latency pipeline |
| Autosave / session restore | EXISTING, RISK | ui/Editor.kt:2865-2933; platform/SessionStore.kt:53-70 | Fix documented reliability issues before feature work |
| Export as PDF/PNG | EXISTING | ui/Editor.kt:2619, 7254; MainActivity.kt:385-417 | Add explicit export guarantees, original-PDF preservation modes |

Important: The existence of a method does NOT demonstrate that every UI state, PDF variant, device or output is correct. Confirm in the app before marking a task done. The new blank Page object has pdfPage=null, so it is a writing page between PDF-background pages in a .xnote document.

## 3. Desired end-user workflows

### A. Open PDF -> Insert writing sheet -> Write -> Export
1. Open PDF as a new note; keep PDF content unchanged and embedded in the native note.
2. Toolbar and page sidebar show a clear **+ Page** action with Add Before / Add After / End, plus a template picker (blank, lined, grid, dots, Cornell, custom).
3. Insert one or N pages after a PDF page; size should default to neighboring page dimensions; user can choose A4/A5/Letter/custom and portrait/landscape.
4. Show page thumbnails as PDF / Note sheets, preserving the exact page order.
5. Write with pen, pencil, highlighter, shapes and text, and undo any page insertion or pen edit.
6. Save .xnote for future editing; **Export PDF** produces all PDF-backed and new writing pages in visible order, with live PDF text preserved where technically possible.
7. Reopen both file types. A flattened PDF may not preserve editability: describe this clearly in export options.

### B. Fast note-taking with a pen
1. The user selects a favorite pen chip (e.g. black gel 0.35mm, blue ballpoint 0.5mm, graphite 2B, yellow marker).
2. One tap activates the preset. Tap again opens pen options, with a live preview stroke and controls.
3. Press stylus side-button (where supported) to temporarily erase; release returns to previous preset.
4. Fingers pan/zoom while stylus writes, with palm rejection, no duplicate dots, no accidental page creation.
5. Undo/redo and autosave remain correct when pen changes during drawing.

### C. Review PDF and study
PDF text selection/markup, color-coded highlighting, note page inserted after a lecture slide, sticky note, quick crop/excerpt to companion notebook or second pane, bookmarks and page search. Use existing split-pane/markup primitives before proposing additional engines.

### D. Advanced canvas
Vector lasso with resize/rotate/recolor and cross-page copy, layers for handwriting/images/shapes, alignment/ruler guides, shape snap, laser pointer, presentation mode, optional handwriting OCR and handwriting search after the core is reliable.

## 4. Feature inventory and scope

### 4.1 Paper / PDF page manager
P0/P1:
- Visible + Page action on PDF and notes, accessible without opening nested page menus.
- Insert before/after current page, insert N sheets, add at end; page type selection in one sheet.
- Smart default: use last selected template; otherwise choose a blank sheet matching adjacent PDF aspect ratio and dimensions.
- Blank, ruled, square grid, dot grid, graph, Cornell, music, checklist; use existing .xtemplate rendering and embedded template support.
- Page thumbnail badges: PDF, blank-note, selected; clear source PDF page number and document page number when they differ.
- Insert/duplicate/move/delete with one undoable history entry, scroll anchor preserved, progress and cancellation.
- Multi-select page actions with confirmation for deleting many pages. Never delete every page.
- Re-export mixed pages in correct visual order, exactly one page per logical document page.
P2:
- Inline insert affordance between page thumbnails; preview page templates, show mixed-size warnings, optionally add separator sheets.
- Multi-select import/extract page ranges from another PDF, page bookmarks/labels and jump to page.
- Optional document page numbering (user-facing), separate from original PDF printed numbers.
P3:
- Reversible PDF page crop, margin expansion and rotation without moving annotations unexpectedly.
- True PDF annotation object export (not flattening) as a separately scoped engineering investigation, never promised without interoperability tests.
Non-goal: general text editing inside existing PDF page content, DRM bypass, or universal support for malformed PDFs.

### 4.2 Writing instruments (priority order)
Phase 1 quality + presets on existing engines: Ballpoint (constant crisp width); Gel (slight pressure, dense ink); Fountain (pressure and angle); Technical fineliner (constant); existing PEN, DASHED, CALLIGRAPHY, SPEED, TAPER; marker/highlighter; neon/highlighter variants.
Phase 2 brush renderer: Mechanical pencil (sharp graphite), HB pencil (light grain), 2B and 6B graphite (pressure-controlled grain/darkening), brush pen (directional pressure), chalk/crayon (textured dry brush), charcoal (soft grain), watercolor/paint brush (optional, costly, needs layering/render target).
Phase 3 utility: Laser pointer (temporary, no save), dotted line/handwriting ruling, smart shape hold, handwriting stabilizer modes, pressure/tilt sensitivity curve editor.
Each brush needs a defined rendering model, no arbitrary tool ID appended to serialized files until migration handling exists. Reuse ToolConfig, Stroke samples and StrokeEngine when feasible; consider a new BrushPreset descriptor which resolves to a compatible existing Tool and a versioned brush style. Store pressure curves as metadata only when every reader/writer/exporter can tolerate or preserve it.

### 4.3 Pens, size and color interaction
- 4 to 8 favorite pen preset slots (type, color, width, transparency, pressure curve, stabilizer); allow custom labels and ordering.
- Persistent per-tool colors and recent colors, palette collections (Study, Pastel, Dark, High Contrast), custom hex/RGBA picker, eyedropper sampling from canvas.
- Independent palettes for markers vs pens, controlled highlighter blending and overlap compositing.
- Width slider in physical/mm-like user units with display preview; ensure consistent look at zoom levels and DPI.
- Floating compact and docked toolbar modes, customizable tool order and left-handed option, long-press preset picker.
- If PDF colors are inverted for dark reading, preserve annotation colors in exported PDF unless the user explicitly chooses export theme.
- Accessible contrast indicators; distinguish tools with both icon and name, not color alone.

### 4.4 Eraser and selection
**P1 critical user request (CV-061–CV-066): Samsung Notes-style precision eraser.** Current source already has STROKE/AREA and 1..80 size slider, but STROKE is the default. Current AREA erases stored sample points; straight two-point lines are removed whole, and paged eraser drag uses only the current MotionEvent coordinate (see `core/model/Stroke.kt:683-721`, `canvas/InteractionController.kt:953-967`, `ui/Popups.kt:919-961`). Deliver a visible default **Area** setting and one-time preference migration; easy size slider/preview; swept eraser capsule based on historical + current cursor positions; geometry-aware clipping and interpolated surviving vector fragments, including straight lines; one undo per drag; save/reopen/PDF export correctness; real-device regression tests. Preserve separate user-selectable Whole Stroke mode. Detailed design, implementation and 18 tests: **SAMSUNG_NOTES_STYLE_ERASER_PLAN.md**.

**P2 (CV-067–CV-070):** Add Highlighter Only, explicit markup controls, eraser ring, S Pen shortcut QA and protected content semantics. Do **not** erase the PDF source, images or typed text boxes; do not claim painting white over PDFs is equivalent to removing ink.

**P2 Samsung-inspired canvas polish (CV-071–CV-075):** Favorites/preset dock, lasso and straighten, experimental Easy Writing Pad, clearer add-sheet PDF workflow and large-screen performance. Reuse existing core components, never copy Samsung proprietary assets or code.
P2: soft whiteout/redaction should NOT be represented as secure PDF redaction; a visual cover is not permanent sanitization.
P2: lasso cut, copy, paste, duplicate, rotate, scale, recolor, and move across pages/panes; keep transforms stable for PDF page zoom/rotation.
P3: erase using scribble gesture, pressure-dependent eraser radius, selection eraser, shape-only erase.

### 4.5 Canvas control, creativity, student workflows
- In page view: fit-width, fit-page, page turn, continuous vertical scroll, optional 2-page reading and 4-page overview.
- In infinite canvas: grid/minimap, origin/home, paper and canvas layouts; keep page/infinite modes separate to prevent accidental document transformations.
- Ruler, protractor, compass, symmetry guides and snap-to-grid (future).
- Smart shapes (hold last ~350-600ms and offer reversible snap), arrows, rectangle, circle, ellipse, polygons, fill and connectors.
- Sticky notes, reusable stamps/stickers, reusable page backgrounds.
- Table and math tools already exist in parts of core/UI; assess before duplicate implementation.
- Audio playback linked to strokes: preserve current recording behavior, later consider timeline markers.
- OCR/handwriting-to-text/search: **reuse existing PDFium searchable text first**. For scanned printed English/Hindi PDF pages prototype ML Kit Text Recognition v2 (bundled) and Tesseract4Android (fully open-source fallback); benchmark PaddleOCR PP-OCRv5 and RapidOCR on Android before considering their deployment. For the app's vector handwriting, prototype ML Kit Digital Ink with separate one-time model-download/offline behavior. All work requires a cancellable, privacy-preserving on-device adapter and tests; see OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md. Not required for the first canvas release.
- Optional side-by-side PDF and notes for students, using existing split-pane infrastructure; document context and selection focus must be safe.

## 5. UI information architecture

**Tablet landscape:** slim top document bar (name, save state, search, page N/M, export); left thumbnails/bookmark sidebar; center content; compact bottom/side adjustable tool rail. Pen preset row appears next to the selected tool, not over the writing line. '+' is visible at bottom of page/sidebar and in page counter menu.

**Phone portrait:** document bar + 1-row essentials (pen preset, marker, eraser, lasso, undo/redo, +Page); overflow opens categorized tool sheet; thumbnails as a bottom sheet; never force a tablet-size full toolbar on a phone.

**Pen settings sheet:** preview scribble at top, preset name, pen type tiles, color swatches/custom hex, width, opacity, pressure curve, smoothing and Save preset. Secondary tuning behind Advanced; main action remains one tap.

**Insert page sheet:** Before / After / End segmented choice; template thumbnails; page count stepper; orientation/size under More; show quick preview of resulting PDF/blank/PDF ordering.

**Visual style and complete app rebranding:** see VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md and BRAND_UI_AUDIT_MATRIX.md. New work CV-076–CV-100 covers original icon/logo and adaptive/monochrome launcher resources, coherent Material 3 Light/Dark/OLED/theme customization, typography and icons, shared normal/destructive/progress alerts, tool popovers, bottom sheets, snackbars, motion, home/explorer/editors and accessibility. Proposed direction: calm Modern Paper & Ink, 48dp touch goals, subdued surface depth, reduced-motion support and no intrusive modal on every pen switch. Reuse existing Palette/MaterialColors/NotesTakingTheme/NotesTakingIcons/Popups wrappers; do **not** change PDF/page paper/ink colors when chrome theme changes. Animations must not touch the inking render thread.

## 6. Proposed technical architecture (incremental)

| Area | Reuse / extend | Design change |
|---|---|---|
| Document pages | core/model/Document.kt, Page.kt, PageStyle.kt | Define explicit page origin (pdf vs paper), preserve pdfPage link; avoid fragile index assumptions; versioned metadata only if essential |
| Page commands | core/history/Command.kt; ui/Editor.kt | Shared insert/move/duplicate/delete transactions with correct undo/redo and book/page anchor updates |
| PDF import/export | platform/PdfSource.kt, PdfJoin.kt, CanvasPdf.kt, PdfiumDocument.kt; format/DocumentCodec.kt | Mixed-page export contract, page count/size/order invariants, temp-file bounds, document source preservation |
| Tool definitions | core/tools/Tool.kt, ToolConfig.kt, InkPalette.kt | Versioned BrushPreset metadata; map presets to existing renderable types; separate color/preset UI state |
| Ink model/geometry | core/model/Stroke.kt, core/stroke/StrokeEngine.kt, StrokeGeometry.kt, WetRibbon.kt | Pressure and optional tilt-aware renderer, low-allocation sample processing, texture engine only for new pencil variants |
| Canvas input | canvas/CanvasView.kt, InteractionController.kt, StylusButtons.kt | Tablet stylus side button, TOOL_TYPE_ERASER, cancel/palm handling and mode transitions |
| Rendering | gl/GlRenderer.kt, gl/InkShader.kt, gl/GlWetPad*.kt; platform/AndroidRenderer.kt | Align wet-ink, dry-ink, thumbnails and PDF export; benchmark CPU vs GPU before changes |
| UI | ui/Toolbar.kt, Popups.kt, ColorPicker.kt, SidePanel.kt, NewNoteScreen.kt | Branded adaptable rail, insert-sheet, presets manager, tool preview, thumbnail status |
| Preferences | settings/Preferences.kt, SettingsRepository.kt, format codecs | JSON migration, defaults and backups for presets; never silently reinterpret old tool IDs |
| Tests | app/src/test/java/com/notestaking/app | Pure Kotlin page/brush/migration tests; add androidTest for SAF, pen, JNI and PDF-specific integration |

Architectural invariants:
1. A PDF-backed logical page references immutable source PDF page and an editable overlay; a newly inserted paper page has no PDF reference. Reordering logical pages must not mutate PDF source indexing.
2. Render/export must use the same canvas coordinate -> PDF coordinate mapping and identical stroke appearance (including stroke widths, rotation, pressure response, opacity).
3. Every page operation is undoable and remains correct after save/reopen; undoing PDF-append must also reconcile source file ownership/lifecycle (currently a risk area to investigate).
4. No unbounded ZIP inflation; no unsafe XML external entities; stream large assets to bounded storage; stage writes and keep last good copy.
5. .xnote/.xcanvas/.xtemplate round-trips; unknown future fields should not silently corrupt documents.
6. No copyrighted brush bitmaps or third-party app assets copied; originals or correctly licensed resources only.

### Reuse-first OCR and third-party engine policy

New OCR research (2026-10-28) is documented in **OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md** with live GitHub popularity/maintenance snapshots, licensing, Android/ABI fit, Hindi/English coverage, reproducible mobile inference concerns and benchmarks. **Do not choose an OCR library on stars alone**. Existing PDFium searchable text is the zero-dependency default. Priority decision: ML Kit bundled text recognition for the easiest optional Android printed OCR integration; Tesseract4Android as the open-source Android alternative; PaddleOCR/RapidOCR as proof-of-concept challengers; ML Kit Digital Ink for ordered stylus strokes rather than images. No engine has been added yet. Keep third-party adapters isolated from saved ink and document formats; preserve no-network privacy and PDF/source page coordinate mapping.

### Jetpack Ink evaluation, not a mandatory migration
Android's Jetpack Ink API offers in-progress low-latency rendering and finalized stroke objects; official documentation says minSdk 26, matching this app. The current app already has a custom front/wet GL rendering path. Run a **spike with metrics** (latency, GL compatibility, pressure/tilt, erasing, export parity, ABI and APK size) before adding dependency; keep current engine if the spike regresses behavior or complicates portability.

### Canvas discoverability and feature visibility — immediate product priority

**2026-10-28 implementation checkpoint:** Added `ui/CanvasQuickAccess.kt`, pinned Eraser + All Tools in both editors, and pinned +Page Before/After/End in paged notes, with new localized labels. Source structurally checked only; Kotlin/Gradle build and device visuals are still unverified because Java is unavailable on the local shell. See CANVAS_FIRST_PASS_IMPLEMENTATION_STATUS.md. This is **not** a full canvas redesign or precise eraser fix.

See **CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md** and **CANVAS_FEATURE_VISIBILITY_MATRIX.md** (CV-101–CV-120). Main user complaint: capabilities are *implemented but hard to find in the real canvas*. Avoid adding more isolated tool icons. Instead update the existing paged PDF/note editor and infinite canvas via: (1) pinned Pen, Marker, Eraser, Select, Undo, Redo; (2) contextual Color/Width and settings with a visible mode; (3) a **clearly labeled + Page** action for paged/PDF notes; (4) an **All Tools** sheet/command list reachable with one tap, including tools previously hidden by toolbar customizer; (5) responsive phone/tablet/left-hand/split-pane layout; (6) accurate precision eraser implementation CV-061–066, not simply selecting the existing AREA mode. Reuse `ToolbarFrame`, `ToolbarLayout`, `ToolbarCustomizer`, `Popups`, `SidePanel`, and `Editor` methods; add a shared UI action registry rather than a second rendering engine. All capabilities need real device verification before claimed complete.

Implementation order: P0 save/data safety and fixtures (CV-001–005); **Phase D0/D1 make essentials visible CV-101–105**; concurrent precision eraser fixes CV-061–066 and shared styling BR CV-083–092; PDF insert and navigation CV-108–114; infinite canvas/customization CV-115–118; real device QA CV-119–120. Do not show unimplemented OCR, Easy Writing Pad, pencil effects or highlighter-only eraser as functioning buttons. A previous line calling double-page viewing 'new' is superseded: the mode already exists in `Popups.kt` and needs usability/device validation.

### Visual branding project — independent but coordinated

The complete rebranding and component unification plan is in **VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md**, with 33 individually grounded UI surfaces in BRAND_UI_AUDIT_MATRIX.md. Work CV-076–CV-100 is staged to avoid losing current theme customizations and does not need any rewriting of PDFium, canvas serialization, or the native drawing engine. Coordinate the new eraser popup (CV-061–070) with the shared popover/alert token redesign (CV-088–092); retain the existing Material 3 / palette architecture. No logo has been generated or approved in this planning pass.

## 7. Reliability blockers and release gates

Before creating release builds based on new editor features, triage the existing audit:
- ui/Editor.kt:3628-3641 swallows save exceptions while MainActivity.kt:360-362 can report success.
- platform/SessionStore.kt:53-70 replaces stored session with a delete-and-retry fallback and suppresses failures.
- platform/AppStorageDocumentsProvider.kt:62-103 uses unvalidated paths/names; copyTree can recurse into its own destination.
- format/DocumentCodec.kt:733-802 has no aggregate decompression/entry budget and reads flow XML without a cap.
- format/FlowXml.kt:317-325 default XML factory lacks explicit secure parser flags.
- Current audit docs record 45 findings. These findings were from the older baseline; retest against current-source hashes and do not call them resolved without a fix and regression test.

Do NOT test security using real user notes. Build fixtures and isolated SAF provider tests. Back up .xnote/.xcanvas before schema changes.

## 8. Milestone roadmap (priority and dependency based, estimates deferred)

**Phase 0 - safety, baseline & test fixtures (P0).**
- Baseline Android CI and real-device behavior; sample notebook, 300-page PDF, mixed-size PDFs, corrupt ZIP fixture, password PDF, large image/long handwriting.
- Correct save outcome, atomic replacement, ZIP/XML budgets, DocumentsProvider containment.
- Baseline memory/latency benchmark, screenshot and PDF visual-golden harness, verify .xnote round-trip and legacy fixture.
Exit: reliable save/reopen; repeatable CI; no P0 data-loss repro; no data corruption.

**Phase 1 - PDF sheet insertion and tool presets (P1).**
- High-visibility Add Page workflow (before/after/end, template, one/N pages), page menu UI, thumbnails.
- Add 4-8 favorites and presets; ballpoint/gel/technical/fountain styles on current stroke engine.
- **First visible canvas upgrade:** implement precision AREA eraser CV-061–CV-066 (default Area, resizable diameter/preview, continuous swept geometry, straight line clipping, undo/save/zoom); one-tap Whole Stroke remains optional. Then improve recent colors, size/opacity and stylus quick switch.
- Preserve PDF background/annotations in mixed-page PDF export; all operations undoable.
Exit: end-to-end PDF -> sheet -> ink -> save -> reopen -> export on API 26/36 devices.

**Phase 2 - natural graphite and advanced editing (P2).**
- Mechanical/HB/2B/6B pencil texture, calligraphy improvements, pressure/tilt curves, brush pen.
- Lasso object controls, copy between pages, highlighter-only eraser CV-067, adaptive toolbar and Samsung-style feature parity checklist (CV-070–CV-075).
- Page-grid preview and batch insertion; layer UI only after confirming core layer behavior.
Exit: visually matched ink in canvas, thumbnails and output; no frame-time regression; legacy strokes unchanged.

**Phase 3 - study and drawing utilities (P2/P3).**
- Two-page view, margin notes, page bookmarks, sticky notes, ruler/shape snap, laser/presentation modes.
- More templates and stamps; optional audio marker UX; accessibility and keyboard/pen shortcuts.
Exit: complete end-to-end student study workflow on 8-inch and 11-inch Android tablets.

**Phase 4 - optional intelligence and interoperability (R&D).**
- OCR discovery begins earlier with CV-051–CV-055 research and isolated device spikes; integration is gated on documented Hindi/English benchmark results, model licensing, offline availability and memory/ABI costs.
- OCR architecture: reuse native PDF text extraction first, OCR only scanned pages/images, use vector Digital Ink recognition only for stylus handwriting (CV-056–CV-059). A searchable exported PDF needs a separate verified text-layer workflow (CV-060).
- On-device handwriting search, math recognition, better PDF annotation object export, EPUB or DOCX->PDF conversion investigation (not native DOCX editing).
- Sync/collaboration deliberately out of scope until offline document safety and conflict model are solved.

## 9. Quality/acceptance matrix

| ID | Test case | Expected |
|---|---|---|
| QA-01 | Import 300-page PDF and insert 3 ruled sheets after source page 12 | 303 note pages; original PDF pages retain rendering and text selection; page ordering stable |
| QA-02 | Reorder blank pages around rotated PDF page, undo/redo, save/reopen | Same visual page sequence, ink stays attached to correct page |
| QA-03 | Open mixed portrait/landscape PDF, insert A4 and PDF-sized paper | Explicit size behavior; no stretched/offset annotations |
| QA-04 | Pen preset changes mid-document, save/reopen | Earlier strokes retain stored style; new strokes use new preset |
| QA-05 | Pressure curve/tilt with device supporting each axis | Smooth predictable variation; fallback on no-pressure/no-tilt device |
| QA-06 | Pen/finger/palm events while pinch panning | No stray marks; cancellation correctly discards canceled stroke |
| QA-07 | Stroke/area/highlighter-only eraser, undo/redo | Only intended objects removed; original PDF remains unchanged |
| QA-08 | Export mixed-note PDF, compare to on-screen and reopen | Page count/order match and visible ink remains sharp at zoom |
| QA-09 | Force kill during autosave, reopen repeatedly | Latest committed state preserved or explicit recoverable draft; never corrupt last valid copy |
| QA-10 | Oversized hostile ZIP, XML with DTD, provider traversal | Input safely rejected, no external access, no runaway memory/disk |
| QA-11 | Long handwriting (e.g. 10k+ strokes), 300-page PDF with fast scrolling | No ANRs/OOMs; record p50/p95 stylus latency, memory, frame-time/device model |
| QA-12 | Existing .xnote, .xcanvas, .xtemplate from old XNotes | Open, edit, save, reopen with content and style unchanged |
| QA-13 | Switch phone/tablet, portrait/landscape, split screen | Toolbar remains accessible; no destructive reflow |
| QA-14 | Debug signed beta and actual release-signed build | Correct package, expected JNI .so ABIs, apksigner verification and device install |

Performance gates are **measurement targets to establish**, not claimed benchmark results. Track p50/p95 input-to-visible-ink, frame duration at 60/90/120Hz, heap peak, time-to-first-PDF-page, thumbnail cache hit rate and export time. Compare against baseline on the same hardware.

## 10. Execution process / GitHub workflow

One feature per branch and PR on top of current master. PR checklist: implementation, user-visible before/after, pure Kotlin tests, Android device smoke result, codec backward compatibility, JNI/ABI check when relevant, accessibility, screenshots, reviewer approval. First PR should focus on safety, second on PDF page UX, then pen presets. Use GitHub Issues IDs from FEATURE_BACKLOG.md. Maintain native app code as authoritative; docs research is **not proof** of runtime correctness.

The research inventory is in RESEARCH_AND_SOURCE_MAP.md. All examples must be independently implemented or evaluated for compatible licenses.

## 11. Decision log and assumptions
- This is a **planning-only** document. No existing source modified.
- Baseline assessed 2026-10-28 on master/facce2c via LocalMCP. Earlier docs/audit checkpoints predate current Git history and therefore need rebaselining.
- Keep internal PDF source embedded in .xnote to preserve portability; export PDF is a separate file, not overwrite by default.
- Prefer Kotlin/Jetpack Compose UI and current PDFium/C++ stack; do not switch to Flutter.
- Do not promise OCR, DOCX support, collaborative sync, secure PDF redaction, or fully editable exported PDF in Phase 1.
- Before any implementation, re-check the current branch and compare code to this document; update status if newer work has landed.
