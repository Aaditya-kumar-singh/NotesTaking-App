# Canvas Upgrade - Prioritized Implementation Backlog

Created: 2026-10-28
Most items are PLANNED; CV-101/103/105/108/112/113/115/118 have a partial local UI implementation as of 2026-10-28, not APK/device-verified. See CANVAS_FIRST_PASS_IMPLEMENTATION_STATUS.md. All other planned work remains pending.
Legend: Existing = source path confirmed; Enhance = improve existing; New = build; Research = feasibility check.
Priority P0 = data safety, P1 = must-have, P2 = useful after P1, P3 = optional.
Cross-reference MASTER_CANVAS_UPGRADE_PLAN.md; use one issue per ticket in GitHub. Branding work CV-076–CV-100 is detailed in VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md and BRAND_UI_AUDIT_MATRIX.md. Canvas discoverability and UI integration work CV-101–CV-120 is detailed in CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md and CANVAS_FEATURE_VISIBILITY_MATRIX.md.

| ID | Priority | Item | Current baseline | Target components | Acceptance / dependency |
|---|---|---|---|---|---|
| CV-001 | P0 | Save returns explicit success/failure | Enhance | MainActivity, Editor.save | No false success; tested with null/failing SAF stream |
| CV-002 | P0 | Atomic session write/restore | Enhance | SessionStore, CanvasSessionStore, JsonStore | Old copy survives write/rename failure |
| CV-003 | P0 | SAF root/path traversal checks | Enhance | AppStorageDocumentsProvider | Canonical containment and invalid name tests |
| CV-004 | P0 | Bound ZIP extraction & XML entities | Enhance | DocumentCodec, CanvasCodec, FlowXml | Hostile document safe rejection |
| CV-005 | P0 | Baseline device and golden output tests | New | app/src/test, proposed androidTest | Save/reopen/PDF/vector comparisons on target devices |
| CV-006 | P1 | Prominent Add Page button in PDF canvas | Enhance | Toolbar, SidePanel, MainActivity | Visible without nested long-press |
| CV-007 | P1 | Insert Before/After/At End choices | Enhance | Editor, SidePanel | Undoable insertion at exact location |
| CV-008 | P1 | Insert N blank pages | New | Editor, history, page picker UI | Single undo transaction |
| CV-009 | P1 | Page template picker at insert | Enhance | PageStyle, TemplateLibrary, SidePanel | Blank/ruled/dots/grid/custom supported |
| CV-010 | P1 | PDF-sized inserted sheet defaults | Enhance | Document.blankPageSize, Page | Can select match PDF or custom size |
| CV-011 | P1 | Clear PDF vs paper thumbnail badges | Enhance | SidePanel | Correct source and logical numbering |
| CV-012 | P1 | Mixed PDF + blank page export QA | Enhance | CanvasPdf, Editor export, PdfSource | Correct page count and ink positions |
| CV-013 | P1 | Undo/redo page insert + PDF insert safely | Enhance | Editor, history, PdfJoin | No lost original, accurate redo after reload |
| CV-014 | P1 | 4-8 pen favorite presets | New | Toolbar, Preferences, ToolConfig | Persist/restore tool/color/width |
| CV-015 | P1 | Ballpoint preset | Enhance | StrokeEngine, ToolConfig | Crisp consistent handwriting |
| CV-016 | P1 | Gel preset | New | StrokeEngine, brush palette | Dense smooth ink with defined width |
| CV-017 | P1 | Fountain/fineliner presets | New | ToolConfig, StrokeEngine | Controlled tilt/pressure or constant line |
| CV-018 | P1 | Marker/highlighter preset variants | Enhance | ToolConfig, shaders | Correct translucent compositing |
| CV-019 | P1 | Color palette: recent, custom hex, favorites | Enhance | ColorPicker, InkPalette, Preferences | Reuse across notes and app restart |
| CV-020 | P1 | Pen settings preview & thickness | Enhance | Popups, Toolbar | Live preview; no accidental edits |
| CV-021 | P1 | Quick pen/eraser switch | Enhance | StylusButtons, ToolConfig, Toolbar | Switch while pressed; return on release |
| CV-022 | P1 | Eraser stroke/area clarity & preview (parent CV-061–CV-070) | Enhance | ToolConfig, InteractionController, Popups | Area default, adjustable size, vector accuracy and full undo after subtask completion |
| CV-023 | P1 | Real tablet gesture regression suite | New | CanvasView, InteractionController, androidTest | Stylus writes, finger pans, palms ignored |
| CV-024 | P1 | Legacy note + template backward compatibility | Enhance | DocumentCodec, CanvasCodec | Old fixtures round-trip |
| CV-025 | P2 | Mechanical pencil | New | stroke rendering, ink material | Fine pressure-controlled graphite |
| CV-026 | P2 | HB/2B/6B textured pencils | New | stroke rendering, shaders | Differentiated grain/softness without blur |
| CV-027 | P2 | Brush pen & advanced calligraphy | Enhance | ToolConfig, StrokeEngine | Pressure/direction and taper fidelity |
| CV-028 | P2 | Chalk/crayon/charcoal styles | New | texture/brush renderer | Original/lawfully licensed textures |
| CV-029 | P2 | Pressure and tilt response curves | New | input samples, config | Graceful fallback on unsupported hardware |
| CV-030 | P2 | Width measured consistently across zoom | Enhance | ToolConfig, Stroke/renderer | Same perceived size when selected |
| CV-031 | P2 | Smart shape snap after hold | Enhance | ShapeRecognizer, controller | Reversible; no surprise line changes |
| CV-032 | P2 | Lasso rotate/recolor/copy between pages | Enhance | InteractionController, history | Object transforms and undo correct |
| CV-033 | P2 | Highlighter-only / object selective eraser | New | controller, EraseRule | PDF underlying text untouched |
| CV-034 | P2 | Batch page management and grid view | Enhance | SidePanel, Editor | Reliable multi-select/reorder |
| CV-035 | P2 | 2-page reading, 4-page overview | New | canvas layout, SidePanel | Editing points match page transforms |
| CV-036 | P2 | Left-handed floating/docked toolbar | Enhance | Toolbar, Settings | Comfortable in portrait & landscape |
| CV-037 | P2 | Layers UI for ink/text/images | Research | PageLayers, SidePanel | Confirm model/export support first |
| CV-038 | P2 | Bookmarks and page outline UX | Enhance | PdfBookmarks, SidePanel | Jump + labels stable after insertion |
| CV-039 | P2 | Margin note / sticky note | New | CanvasItem/Tool | Moveable/editable and exported correctly |
| CV-040 | P2 | Ruler and grid snap | Enhance | Ruler, shapes, templates | Mm angle/coordinate behavior consistent |
| CV-041 | P2 | Laser pointer/presentation mode | New | Toolbar, controller | Transient ink never saved |
| CV-042 | P2 | Expand template catalog | Enhance | assets/templates, TemplateLibrary | Cornell, checklist, planners, engineering |
| CV-043 | P2 | Eyedropper and palette management | Enhance | ColorPicker, InkPalette | Pick precise color, undo not relevant |
| CV-044 | P2 | Color/accessibility and reduced motion | Enhance | Toolbar/theme | Tool labels and large targets, screen reader |
| CV-045 | P2 | Studio performance telemetry local-only | New | CanvasView, GlRenderer, tests | Opt-in local benchmark log, no privacy leak |
| CV-046 | P3 | Watercolor and textured blending | Research | GPU brush architecture | No mismatch between live/save/export |
| CV-047 | P3 | OCR and handwriting recognition research epic (CV-051–CV-060) | Research | PdfiumDocument, core/search, new isolated module | Reuse native text first; English/Hindi OCR privacy, model sizes, device benchmarks |
| CV-048 | P3 | Editable PDF annotations interoperability | Research | Pdfium/PDFBox, export | Acrobat/Xodo roundtrip tests |
| CV-049 | P3 | DOCX/EPUB import via conversion pipeline | Research | platform import subsystem | No claim of native DOCX editing |
| CV-050 | P3 | Optional study tools: stickers/flashcards | New | canvas items/templates | Offline, fast, editable |

| CV-051 | P1 | Identify PDFium text vs scanned-page recognition gate | Enhance | PdfiumDocument, PdfSource, core/search | Existing selectable PDF uses PDFium; scan threshold tested |
| CV-052 | P1 | Define cancellable OCR engine interface and metadata contract | New | platform/ocr (proposed), core/search | Engine independent, rotation/page transforms, no implicit upload |
| CV-053 | P2 | ML Kit bundled English+Hindi printed OCR prototype | Research | isolated Android OCR adapter | API 26+, image OCR, APK/ABI delta, offline on first launch |
| CV-054 | P2 | Tesseract4Android English+Hindi offline prototype | Research | isolated Android OCR adapter, traineddata | Apache license, engine serialized/thread-safe, model files integrity |
| CV-055 | P2 | Compare two printed OCR candidates on measured corpus | New | app/src/test, androidTest, benchmark fixtures | CER/WER, Hindi diacritics, layouts, latency, memory, APK cost measured |
| CV-056 | P2 | OCR bounding boxes and searchable note page index | New | core/search, PdfSource, new OCR metadata | Click search hit maps correctly on rotated/reordered pages |
| CV-057 | P2 | OCR privacy, opt-in, offline models and cache invalidation | New | settings, UI, OCR module | No cloud upload; model missing UI; delete/rebuild indexes |
| CV-058 | P3 | ML Kit Digital Ink recognizer for live stylus strokes | Research | core/model/Stroke, canvas input, new recognizer | Hindi/English, timing/stroke grouping, downloadable models validated |
| CV-059 | P3 | PaddleOCR mobile vs RapidOCR Android alternative spike | Research | experimental branch, native/ONNX adapters | Real APK builds all supported ABIs, model accuracy and license gates |
| CV-060 | P3 | Searchable exported PDF text layer feasibility | Research | platform PDF export, OCR text positions | Acrobat/Android searchable text and line geometry verified |

| CV-061 | P1 | Make Area Eraser default; versioned preference migration | Enhance | ToolConfig, Settings, UI | New/upgraded users start in area mode; Stroke remains selectable; migration once |
| CV-062 | P1 | Samsung-inspired quick eraser panel, sizes and cursor preview | Enhance | Popups, Toolbar, InfiniteToolbar | Fine/Medium/Large, numeric diameter, consistent ring radius, accessible |
| CV-063 | P1 | Swept capsule geometry and paged MotionEvent history | New | InteractionController, new EraserPath/StrokeClipper | Fast erase has no skipped areas, movement continuous and undoable |
| CV-064 | P1 | Exact partial vector clipping of pen/highlighter/straight strokes | Enhance | Stroke, stroke geometry, clipper | Erasing middle of sparse/straight line preserves both sides accurately |
| CV-065 | P1 | Shared partial eraser semantics across paged/infinite canvases | Enhance | InteractionController, InfiniteInteraction, EraseSession | Same output/radius at zoom and rotation, spatial indexing maintained |
| CV-066 | P1 | Precision eraser tests: undo/cancel/save/PDF/legacy/dense ink | New | StrokeEraseTest, EraseSessionTest, androidTest | 18 acceptance scenarios in SAMSUNG_NOTES_STYLE_ERASER_PLAN.md |
| CV-067 | P2 | Highlighter-only erase with underlying pen/PDF protection | New | EraseRule, Popup, controllers | Only marker overlay touched; PDF source always unchanged |
| CV-068 | P2 | S Pen quick erase/hover/side-button/palm compatibility | Enhance | InteractionController, StylusButtons | Temporary erase, ring, return-to-pen, cancellation safe |
| CV-069 | P2 | Selective shapes/fills/markup erase rules | Enhance | EraseRule, ShapeItem, ConnectorItem | No unintended deletion of text/image/PDF source |
| CV-070 | P2 | Eraser accessibility/left-hand/phone-tablet toolbar refinement | Enhance | Toolbar, Popups, theme | 48dp controls, spoken mode/size, responsive popup |
| CV-071 | P2 | Samsung-like pen favorites/preset dock, quick colors | Enhance | Toolbar, ToolConfig, Preferences | One-tap pen selection, no copy of external assets |
| CV-072 | P2 | Samsung-like lasso/straighten/style editing polish | Enhance | InteractionController, selection history | Move/resize/recolor with undo; reuse existing recognition |
| CV-073 | P3 | Easy Writing Pad feasibility/experimental magnified input area | Research | new Compose pad, CanvasState mapping | Accurate vector placement and non-blocking UX |
| CV-074 | P2 | PDF study flow: paper inserts, templates, markers, thumbnails | Enhance | Editor, SidePanel, PageStyle | PDF source protected; mixed-page export tested |
| CV-075 | P2 | Tablet canvas performance and inclusive interaction benchmark | New | CanvasView, GL renderer, testing | Real stylus and 60/90/120Hz latency measured; no UI stalls |

| CV-076 | P1 | Screenshot/code inventory of all visual surfaces and current themes | Research | Backstage, Editor, Popup, app assets | Before screenshots, dialog catalog, settings and baseline QA |
| CV-077 | P1 | Explore 3 original app logo concepts and approve one | New | design master assets | Folded page/ink, notebook, N-page; clear approval |
| CV-078 | P1 | Define final semantic design tokens and preference migration rules | New | ui/theme, Preferences | Theme role/contrast matrix, existing user custom values unchanged |
| CV-079 | P1 | Prepare original editable brand logo/icon master vector | New | design asset source | Legible small, owned/licensed art, full-color/mono |
| CV-080 | P1 | Regenerate Android launcher adaptive/round/monochrome and densities | Enhance | res/mipmap*, drawable | API26-36 launcher masks and themed icons verified |
| CV-081 | P1 | Rebrand system splash and 24-frame loader, reduce startup mismatch | Enhance | res/values-v31, drawable, NotesTakingLoader | No wrong-color flashes, responsive cold start |
| CV-082 | P1 | Align wordmark/About and system notification icon | Enhance | AboutPane, strings, notification resource | Consistent app name/identity, copyright credits retained |
| CV-083 | P1 | Implement Paper & Ink brand preset using existing Material seed engine | Enhance | MaterialColors, Palette, PreferencesPane | Light/dark/OLED/dynamic/custom modes preserved |
| CV-084 | P1 | Add unified MaterialTheme UI typography using bundled fonts | Enhance | NotesTakingTheme, bundled Inter/Roboto | UI text scales, document font and note text unchanged |
| CV-085 | P1 | Standardize UI spacing, shapes, elevations and content insets | Enhance | ui/theme/Shape, shared components | Responsive phone/tablet, user corner preference honored |
| CV-086 | P1 | Standardize icon family, active/disabled/hover/hit target semantics | Enhance | NotesTakingIcons, Toolbar, drawable icons | Clear 24dp icons, 48dp tap regions and accessibility |
| CV-087 | P1 | Create debug-only component preview gallery and theme snapshot tests | New | ui/preview/tests | Theme matrix and component goldens, not shipped to production |
| CV-088 | P1 | Standardize normal/destructive/rename AlertDialog variants | Enhance | ui/Popups, Backstage, TrashPane, others | Correct severity/cancel/keyboard/back, no accidental deletes |
| CV-089 | P1 | Unify anchored tool popovers, context menus and responsive sheets | Enhance | Popups, ColorPicker, Tooltip, CanvasMenus | Safe bounds near stylus, phone/tablet consistent |
| CV-090 | P1 | Standard snackbar, lightweight success and Undo/retry feedback | Enhance | Editor, MainActivity, NewNoteScreen, AboutPane | No duplicate modals for trivial status |
| CV-091 | P1 | Improve PDF import/export/save loading and error dialogs | Enhance | MainActivity progress UI, Editor | Honest cancellable progress, real retry/fail state |
| CV-092 | P1 | QA destructive alert behavior and accessibility/focus | New | ui tests, dialogs | Screen-reader, focus, keyboard, irreversible state verified |
| CV-093 | P2 | Redesign home and file explorer chrome with brand hierarchy | Enhance | Backstage, ExplorerTiles, TrashPane | Keep folders, notes, selection and actions intact |
| CV-094 | P2 | Modernize create-note flow, template and empty states | Enhance | NewNoteScreen, PageSizeControls | Clear steps and compact cards without excess dialogs |
| CV-095 | P2 | Polish PDF/notes and infinite canvas chrome responsively | Enhance | Editor, InfiniteEditor, Toolbars, SidePanel | Paper remains visually independent, no inking latency regression |
| CV-096 | P2 | Design consistent pen/eraser/selection popup UI | Enhance | Popups, Toolbar, InfiniteToolbar | Coordinate with CV-061–070 precision eraser |
| CV-097 | P2 | Reorganize preferences appearance and customization UI | Enhance | PreferencesPane, settings/Preferences | Live previews, branded theme preset, preserve old choices |
| CV-098 | P2 | Apply motion tokens, haptics and reduced-motion support | Enhance | Toolbar, Tooltip, Backstage, motion tokens | 80-300ms restrained motion, no drawing thread work |
| CV-099 | P2 | Accessibility and device screenshot verification matrix | New | androidTest, screenshot artifacts | Light/dark/OLED, TalkBack, 200% fonts, phone/tablet |
| CV-100 | P1 | Final Android launcher, theme and dialog build release regression | New | CI, Android unit/UI tests, resources | No resource/lint errors, signed/icon variants QA before release |

| CV-101 | P1 | Re-audit feature visibility and capture screenshots | Research | Toolbar, Popups, SidePanel | Source review complete; device screenshots pending |
| CV-102 | P1 | Shared context-aware canvas action registry | New | ui/canvas action descriptors | Partial direct action menu shipped; robust registry still planned |
| CV-103 | P1 | Pin essential tools on phone/tablet canvas | Enhance | ToolbarFrame, Toolbar, InfiniteToolbar | Eraser pinned; all other essential tool pins/layout QA pending |
| CV-104 | P1 | Contextual pen/eraser color/width/status UI | Enhance | Popups, ColorPicker, Toolbars | Eraser mode accessible and surfaced in accessibility label; richer strip pending |
| CV-105 | P1 | All Tools menu independent of hidden custom layout | Enhance/New | CanvasQuickAccess, Toolbar, InfiniteToolbar | Initial working direct actions wired; grouping/search/complete audit pending |
| CV-106 | P1 | True precise Area Eraser geometry and UI | Enhance | Stroke, Controller, Popups | Depends on CV-061–066, not yet fixed by UI work |
| CV-107 | P1 | Quick pen favorites and width/color preview | Enhance | ToolConfig, Toolbar, Preferences | Pending CV-014–020 |
| CV-108 | P1 | Persistent + Page entry for PDF/paged notes | Enhance | CanvasQuickAccess, Editor | Initial button and Before/After/End entry wired; real device QA pending |
| CV-109 | P1 | Page template picker, insert PDF and grouped history | Enhance | Editor, SidePanel, PageStyle | Pending, existing basic page transaction reused |
| CV-110 | P1 | Clear page number, thumbnails and quick jump | Enhance | SidePanel, Toolbar | Pages menu entry wired; persistent counter polish pending |
| CV-111 | P2 | Lasso floating selection actions | Enhance | CanvasMenus, controllers | Pending |
| CV-112 | P1 | Discoverable View, existing double page, PDF filters | Enhance | Popups, Toolbar | Initial All Tools > View wired; device verification pending |
| CV-113 | P2 | Paper/template picker directly accessible | Enhance | Popups, SidePanel | Initial All Tools > Styles wired; richer picker pending |
| CV-114 | P2 | Search/Bookmarks/Contents navigation clarity | Enhance | SidePanel, SearchTab | Pending |
| CV-115 | P2 | Infinite canvas Minimap, Waypoints and zoom access | Enhance | InfiniteToolbar, CanvasQuickAccess | Initial All Tools entries wired; UX/device QA pending |
| CV-116 | P1 | Simple/Advanced and restore toolbar layouts | Enhance | ToolbarLayout, ToolbarCustomizer, Preferences | Existing layout preserved; presets pending |
| CV-117 | P2 | Discoverable hardware pen shortcuts and gesture help | Enhance | StylusButtons, Tooltip, Preferences | Pending |
| CV-118 | P1 | Correct paged/PDF/infinite/split-pane action context | Enhance | Editor, InfiniteEditor, QuickAccess | Separate paged/infinite menu wired; split-pane QA pending |
| CV-119 | P1 | Canvas UI usability/accessibility device benchmark | New | androidTest, screenshots | Pending, JDK missing locally |
| CV-120 | P1 | Final connected canvas release/undo/save/PDF gate | New | CI, Android tests, docs | Pending |

### Starter GitHub issue sequence
1. CV-001 to CV-005 are release-blocking safety and test prerequisites.
2. CV-006 to CV-013 are the PDF extra-page experience.
3. CV-014 to CV-024 are the pen/palette/eraser workflow and compatibility.
4. CV-025 to CV-050 cover advanced brushes/study features; OCR epic CV-047 is expanded by CV-051 to CV-060, which may be researched in parallel after safety gates. No OCR dependency is installed.
5. CV-051–CV-060 follow OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md; the chosen engine must win a measured Android device evaluation, not a star contest.
6. **Next canvas priority: CV-061–CV-066 precision area eraser**: default Area, live size preview, continuous swept stroke clipping, one undo/save/reopen. CV-067–CV-075 extend Samsung Notes-like user workflows. See SAMSUNG_NOTES_STYLE_ERASER_PLAN.md.
7. **Full brand/UI modernization: CV-076–CV-100**. Start visual inventory and icon concept approval CV-076–078; shared theme and visual primitives CV-083–087; dialogs/popups/feedback CV-088–092; editor/home/screens CV-093–097; motion, QA and release CV-098–100. Detailed handoff: VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md and BRAND_UI_AUDIT_MATRIX.md. All are PLANNED.

### Backlog management rules
- Initial status of every item: PLANNED; do not mark existing capabilities as newly delivered.
- For each ticket record: design decision, current/changed files, unit tests, emulator/device verification, compatibility, screenshots, PR link and follow-up risks.
- Review next phase only after safety/data-loss issues are addressed and CI passes.
