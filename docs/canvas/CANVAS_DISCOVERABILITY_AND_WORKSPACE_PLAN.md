# NotesTaking App — Canvas UX, Feature Discovery & Samsung-Inspired Toolbar Plan

**Created:** 2026-10-28
**Scope:** existing native Android paged notebook/PDF editor and infinite canvas; Kotlin + Jetpack Compose; NO rewrite of document or rendering engines
**Status:** Initial in-app discoverability pass implemented locally on 2026-10-28; full redesign remains a PLAN. No screenshot-based device review, full precision eraser fix, APK validation, commit or push yet. See CANVAS_FIRST_PASS_IMPLEMENTATION_STATUS.md.
**Related:** `MASTER_CANVAS_UPGRADE_PLAN.md`, `FEATURE_BACKLOG.md`, `SAMSUNG_NOTES_STYLE_ERASER_PLAN.md`, `VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md`, `CANVAS_FEATURE_VISIBILITY_MATRIX.md`.

## 0. The user problem we are solving

The app already implements many useful capabilities but their **settings and actions are not visible or discoverable in the canvas**. Example: Area Eraser and size are in the settings for an **already active** eraser; initial eraser mode is Whole Stroke; current partial eraser algorithm has incomplete geometry. A feature can exist in core Kotlin without providing a useful, obvious user experience.

**Goal:** make the first screen of a note or imported PDF immediately usable like a polished stylus note-taking app: pen, highlighter, precision eraser, lasso, undo/redo and **+ Page** visible; secondary options reached in one deliberate tap; advanced tools not lost. Fix faulty behavior rather than only moving icons.

**Principle:** *Existing + hidden -> expose it; existing + broken -> repair then expose; not implemented -> identify as proposed, don't show dead controls.* Do not claim Samsung Notes equivalence without device testing. Do not copy proprietary UI assets/branding.

## 1. Code-backed findings and current feature inventory

| Feature | Status in current source | Where it lives today / evidence | Main gap |
|---|---|---|---|
| Area vs Stroke eraser | IMPLEMENTED modes, but partial behavior imperfect | `core/tools/ToolConfig.kt:8-18,63-64`, `ui/Popups.kt:919-961`; tap active eraser again to open | STROKE default; user may never discover AREA; sparse/straight strokes not precisely clipped |
| Eraser size | IMPLEMENTED | `ui/Popups.kt:949-952`, `core/tools/ToolConfig.kt:175`; `InteractionController.kt:1499-1501` | Settings buried, units/ring confusing; cannot quickly preview |
| Other pen types (pen, dashed, calligraphy, speed, taper, highlight) | IMPLEMENTED | `core/tools/ToolbarLayout.kt:185-197`, `ui/Toolbar.kt:104-120` | Seven separate drawing icons create a long scrollable bar; favorites/defaults not simple |
| Tool pressure, line width, glow, highlighter strength | IMPLEMENTED | `ui/Popups.kt:93-203`; re-tap active tool | Advanced settings hidden; no always-visible contextual pen preview |
| Color swatches and palette | IMPLEMENTED | `ui/Toolbar.kt:273-284`, `ui/ColorPicker.kt` | Re-tap active swatch to edit, low discoverability |
| Lasso, selection, object operations | IMPLEMENTED | `ui/CanvasMenus.kt:180+`, `ui/Toolbar.kt:205-208`, `ui/InfiniteEditor.kt` | Many actions appear only on selection/context menu, no visible indicator of available tools |
| Add blank page / insert after page | IMPLEMENTED | `ui/Editor.kt:6572+,6646+,6656+,6679+`, `ui/SidePanel.kt:590` | Buried in page thumbnail three-dot menu; no persistent labeled +Page |
| Insert another PDF | IMPLEMENTED | `ui/SidePanel.kt:591`, `ui/Editor.kt:6690+` | Hidden in same page context menu |
| Page duplicate/reorder/delete/rotate | IMPLEMENTED | `ui/SidePanel.kt:592-605`, `ui/Editor.kt` | More menu is comprehensive; often not obvious |
| Page styles, templates, paper color | IMPLEMENTED | `ui/Popups.kt:205+`, `ui/Toolbar.kt:244`, `core/model/PageStyle.kt` | Styles menu needs plain "Paper" identity and quick templates |
| Double-page and vertical scroll modes | IMPLEMENTED SETTINGS, test on devices | `ui/Popups.kt:675-760` (ViewingMode.SINGLE, DOUBLE, COVER) | Buried in View menu. Do NOT list double page as wholly unimplemented |
| PDF filters, rotate, fit, zoom lock | IMPLEMENTED | `ui/Popups.kt:675-760`, `ui/Toolbar.kt:248-269` | Technically visible only if scrolled to these icons; group under one View entry |
| PDF text highlight/underline/strike | IMPLEMENTED only when PDF source | `ui/Toolbar.kt:213-215`, `ui/Popups.kt:1051+` | Only appear with a PDF; label clearly vs freehand highlighter |
| Page thumbnails, contents, bookmarks, search | IMPLEMENTED | `ui/SidePanel.kt:94-156` | Four icon-only tabs, no text in compact panel |
| Infinite canvas minimap / saved waypoints | IMPLEMENTED | `ui/InfiniteToolbar.kt:164-175` | Deep in its distinct scrolling toolbar |
| Toolbar position/floating/size/customization | IMPLEMENTED | `ui/ToolbarFrame.kt:78-198`, `ui/ToolbarCustomizer.kt:52-59`, `core/tools/ToolbarLayout.kt:67-245` | Customizer in Preferences; long bar scrolls and tools can be hidden |
| Current toolbar layout | IMPLEMENTED, user configurable | `core/tools/ToolbarLayout.kt:181-213` | More tools than fit screen; no explicit persistent pinned row+overflow affordance |
| Precise partial vector geometry | NEEDS FIX, not just UI | `core/model/Stroke.kt:683-721` and `canvas/InteractionController.kt:953-967` | Sample-point clipping; 2-point straight strokes fully erased; paged erase history missing |
| Easy Writing Pad | NOT VERIFIED as existing dedicated tool | Prior backlog CV-073 | Research/prototype only; do not promise in initial toolbar |
| OCR/handwriting-to-text | RESEARCH ONLY | `OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md` | No OCR shipped; do not add actionable conversion button before model/device tests |
| Pencil/graphite texture | PLANNED only | Prior backlog CV-025–028 | Keep placeholders out of production until supported renderer exists |
| Highlighter-only eraser | PLANNED | Prior backlog CV-067 | Must work before offering a clickable mode |
| "Magic Wand" toggle | IMPLEMENTED toggle; semantics need validation | `ui/Toolbar.kt:217-218`, `ui/InfiniteToolbar.kt:130-135` | Avoid misleading renaming to "Laser Pointer" without checking exact functionality |

**Truthful status labels in our backlog:** SOURCE-CONFIRMED, UI-REACHABLE IN CODE, DEVICE-VERIFIED, NEEDS-UX, NEEDS-FIX, NOT-IMPLEMENTED. The first two are not evidence of smooth on-device behavior. A runtime screenshot audit is still outstanding.

## 2. Samsung Notes-derived interaction principles and legal boundary

Official Samsung Notes documentation shows: visible switching between pen/highlighter/eraser; favorites; pen color/thickness; Stroke/Area eraser with size; temporary S Pen button erasing; lasso/selected handwriting editing and Easy Writing Pad. These behaviors are reference patterns, not borrowed assets.
- Samsung Business (favorites and convenient toolbar): https://insights.samsung.com/2025/03/27/10-tips-for-increased-productivity-with-samsung-notes/
- Samsung India (favorites and Easy Writing Pad): https://www.samsung.com/in/support/mobile-devices/how-to-use-samsung-notes/
- Samsung eraser instructions: https://www.samsung.com/us/support/answer/ANS10003634/
- Samsung PDF import/export: https://www.samsung.com/us/support/answer/ANS10002404/
- Samsung handwriting editing: https://www.samsung.com/ae/support/mobile-devices/samsung-notes-composing-notes-with-handwriting/
- Community critique of toolbar customization/positioning: https://www.reddit.com/r/GalaxyTab/comments/pui0tm/ — anecdote, not a survey or precise current Samsung behavior.

**Design rule:** preserve user workflow and clearly reveal capabilities without hiding essential tools in scrolling or a settings screen. Our logo/colors/icon artwork must be original. No Samsung code, screenshots or icons embedded in the app.

## 3. Proposed information architecture — two layers, not 30 equally weighted icons

### A. Sticky top document bar, all devices
- Back/Home; document title and Save state (Saving / Saved / Error);
- note pages: compact **Page N of M**, page thumbnails button and direct **+ Page** action;
- search; View; an obvious **All tools (...)** entry; more document actions only where needed.
- on infinite canvas: replace Page N/M and + Page with canvas title, **Minimap** and **Saved views**. Never show page insertion where no logical pages exist.
- on split-pane: each pane retains context identity; the top bar indicates which pane an action changes; no wrong-pane insert or erase.

### B. Pinned main writing tools — NEVER solely offscreen in a scrollable strip
Proposed minimum priority order:
1. **Pen** — armed pen, tap to draw, second tap or small chevron opens pen types and presets;
2. **Highlighter** — easy direct access, distinct from PDF-text highlight;
3. **Eraser** — AREA default, size ring + mode visible; chevron opens its panel;
4. **Lasso/Select** — one visible selection entry; picker chooses freehand lasso, object select, text select when PDF;
5. **Undo / Redo** — always findable and disabled visibly when unavailable;
6. **+ Page** (paged note/PDF only) — persistent labeled action on document bar or adjacent stable action cluster.
7. **Current Color + Width** — a contextual micro-strip for active writing tool, with 3-5 favorite presets; its presence is adaptive rather than crowding phone width.

Secondary tool categories behind **All Tools** (ONE visible entry, not an invisible horizontal-scroll-only overflow):
- **Write:** ballpoint, dashed, calligraphy, speed, taper, marker, favorite pens; categories use actual supported tools.
- **Select & edit:** lasso, object select, cut/copy/paste, duplicate, resize/rotate/lock etc. Show context-sensitive actions after selection.
- **Insert:** image, camera, sticker, audio/record, text/text box, shapes; **page insertion** in paged notes via +Page.
- **Pages & PDF:** add/duplicate/reorder/rotate pages, insert PDF, paper styles/templates, search, bookmarks, view options.
- **View & Canvas:** pan, fit/zoom, zoom lock, page layout (paged) OR minimap/waypoints (infinite).
- **More:** preferences, toolbar customization, shortcuts/help, experimental features **only once implemented**.

No dead disabled "coming soon" chips in normal toolbar. A separate *roadmap/help page* may explain future OCR, pens and Easy Writing Pad as future work.

## 4. Written wireframes — conceptual, not code/UI screenshots

### Tablet landscape: PDF / paged notebook

```text
+----------------------------------------------------------------------------+
| < NotesTaking | Biology.pdf             Saved | 12/64 | [Thumbnails] [+ Page] [Search] [View] |
+----------------------------------------------------------------------------+
| [Pen v] [Highlighter] [Eraser v] [Lasso v]  | [Undo] [Redo] | [Color] [Width] [All Tools] |
+----------------------------------------------------------------------------+
| PAGES             |                                                       |
| [Page 10]         |               SELECTABLE ORIGINAL PDF                |
| [Page 11]         |                  + editable ink overlay              |
| [Page 12] active  |                                                       |
| [Page 13]         |        a small, anchored eraser/pen popup             |
| [+ Add Page]      |           only while user configures tools           |
+----------------------------------------------------------------------------+
```

The actual UI keeps note content central; top navigation and tool rows may collapse on 8-inch devices. Sidebar can be hidden with a visible button. PDF original is never erased by the annotation eraser.

### Tablet: if eraser active

```text
[Pen] [Marker] [Eraser (Area, 12dp) v] [Lasso] [Undo] [Redo]
                     |
                     +---------------------------+
                     | ERASER                    |
                     | (*) Erase only touched ink|
                     | ( ) Erase entire stroke   |
                     | Size: [Fine] [Mid] [Large]|
                     | [------o------------]12dp |
                     | (circle preview)          |
                     | Switch back to pen [on]   |
                     +---------------------------+
```

**The popup must not be the only indicator of AREA/STROKE**: a mode label or icon badge should distinguish the active eraser behavior. Popup appearance is coordinated with brand plan CV-089 and precision eraser CV-061–070.

### Phone portrait: single stable tool row

```text
+--------------------------------+
| <  Biology.pdf   12/64  [+Page] |
| [Pen] [Marker] [Erase] [More]  |
| [Undo] [Redo] [Color] [Width]  |
+--------------------------------+
|                                |
|         PDF / PAPER            |
|                                |
|                                |
+--------------------------------+
| [Pages] [Search] [Tools] [View] |
+--------------------------------+
```

For narrow widths, use a *single adaptive primary row plus a contextual row* or an expanded bottom sheet; do not require the user to scroll through 30 icons to reach Eraser. The exact placement is a design proposal pending screenshot/real-device layout tests.

### Infinite canvas: no page actions
Top: Canvas title + save state + minimap toggle + saved views. Writing row reuses Pen/Marker/Eraser/Select/Undo/Redo/Color/Width/All Tools. Secondary tools hold grid/style/zoom/fit, images and audio, waypoint management. A paged PDF source can be an element on the infinite canvas, but the UI must not imply it has normal notebook page numbers.

## 5. Detailed user interaction contracts

### U1: Erase one character inside a handwritten line
1. Tap Eraser (always visible). Initial mode "Erase only touched ink" (AREA after explicitly designed preference migration).
2. Hover or touch shows cursor ring with actual effective radius. Tap chevron for Fine/Medium/Large/slider and mode.
3. Draw across just part of a character. Only touched vector ink disappears, not whole pen stroke.
4. Lift -> one undo transaction. Undo restores exactly; redo repeats.
5. PDF background, typed text and images unchanged. A sparse straight 2-point line must split. See CV-061–066; do not treat UI-only selection of AREA as completion.

### U2: Change pen color and width while writing
Tap current pen -> choose preset or current pen settings. Color, width and preview appear next to main tools. Change and resume writing without navigating to Preferences. Preserve current stroke while changing a preset; do not retroactively recolor existing writing unless a selection edit explicitly requests that.

### U3: Open PDF then add a writing sheet between pages
1. A visible **+ Page** action in the document header or thumbnail footer.
2. Bottom sheet/popover: **Before current**, **After current**, **At end**; select Blank / Ruled / Grid / Dots / custom saved page; count default 1.
3. Preview logical order PDF page 12 -> paper -> PDF page 13, with correct page sizes.
4. Confirm -> insert using existing Editor operations; single history action; update count and thumbnail immediately; auto-focus new page, retain scroll/zoom preference where appropriate.
5. Save/reopen; export yields correct combined page order; native PDF pixels/source untouched. See CV-006–013.

### U4: Find advanced features, even if user hid toolbar item
Tap All Tools -> search by feature name + category (filterable) -> tap (activates action / opens existing popup). Optional pin-to-toolbar affordance. No feature should become unreachable by toggling its ToolbarEntry.visible to false.
- Restore Essentials, Restore Default Layout, Customize Toolbar; prompt before overwriting custom layout.
- When a new tool is added after upgrade, don't force it visible in all customized layouts if it crowds. Ensure it appears in All Tools.
- Respect user-defined toolbar position (top/bottom/left/right), floating setting and size from current `ToolbarFrame.kt`.

### U5: After selecting handwriting, show actions
Tap Lasso -> draw around ink -> short floating context bar containing Move, Copy, Style, Delete; secondary More contains rotate, resize, group/ungroup etc where supported. Do not permanently display object actions with no selection. VoiceOver/TalkBack focus, keyboard Delete/Undo and landscape clipping tests.

### U6: Page search and navigation
Header always exposes page count and search; page icon opens Pages/Contents/Bookmarks/Search. Icon-only tabs need accessible labels, selected state and optional text in full panel. Next/Prev and page-jump remain accessible. Add page from active page or thumbnail is obvious without selecting a 3-dot menu.

### U7: Styles/view
The label **Paper** opens current/document template/color options; **View** opens single/double/cover/scroll/fit/rotate/filters; use progressive disclosure to avoid a monolithic 300dp settings menu. Do not change original PDF/pen ink colors when adjusting UI theme.

### U8: Pen hardware and touch
Stylus tip draws; supported eraser-end/side button temporary erase; palm contact ignored; finger pans by default unless finger drawing enabled. Preserve existing input logic; surface settings in "Drawing controls". Offer a first-use nonmodal one-time tip "Tap a selected tool again for options" if beneficial.

## 6. Implementation architecture: make discovery reliable

### 6.1 Tool/Action Registry (new, proposal)
Existing:
- `core/tools/Tool.kt` and `ToolbarItem` enumerate tools; `ToolbarLayout` stores section order and visibility.
- `ui/Toolbar.kt` / `ui/InfiniteToolbar.kt` separately `when` over layout. Direct handler call sites already work.

Introduce a **UI-layer registry** (not a replacement for Document or Tool):
- `ui/canvas/CanvasActionId` sealed ids or enums: ACTIVATE_PEN, ACTIVATE_ERASER, ERASER_SETTINGS, OPEN_PAGE_INSERT, OPEN_PAGE_NAV, OPEN_VIEW, OPEN_SEARCH, OPEN_TOOL_SETTINGS, OPEN_SETTINGS, etc.
- `CanvasActionDescriptor(id, titleRes, icon, group, priority, supportedContexts, availability, invoke)`.
- `CanvasContext(mode: PAGED_NOTE | PDF_NOTE | INFINITE_CANVAS, selection: ..., page, activePane, capabilityFlags)`.
- A single `CanvasActionRegistry` powering primary toolbar, All Tools, command palette/help, optional pin customization. **No copy of business actions in fake UI**; route to existing Editor or InfiniteEditor methods. Separate runtime handlers to avoid tightly coupling both editors.
- Descriptor context/availability is **derived from real editor state** (PDF source, selection, canUndo, read-only/loading, GL status); don't show a functional-looking button for unimplemented models.
- An action can be HiddenFromPrimary but not HiddenFromAllTools unless context truly inapplicable; context-inapplicable is explained with a short label and optional help, rather than a silent dead button.
- Add `CanvasFeatureStatus` for development audit only: SourceConfirmed, WiredReachable, DeviceVerified, NeedsFix, Proposed. Do not ship research-state labels in normal UI.

### 6.2 Adaptive layout (extension, not engine rewrite)
Add `CanvasToolbarScaffold` composed of `DocumentHeader`, `PinnedEssentialTools`, `ActiveToolOptions`, `AllToolsSheet`, `ContextualSelectionBar`. **Reuse** `ToolbarFrame.kt`, `ToolButton`, `Popups`, `InfiniteToolbar`, `ToolbarCustomizer`; avoid building two unrelated tool systems. Test Compose breakpoint/available width and input type; don't assume screen orientation alone determines tablet vs phone.

Keep existing `ToolbarLayout` and settings persisted IDs stable. Do **not** change all default toolbar positions/layouts for existing users without explicit, versioned migration and opt-in strategy. Offer **Simple** vs **Advanced** visible layout preset while retaining per-user customization. Essential actions and All Tools stay accessible regardless of preset.

### 6.3 Single editor action path
- UI actions use the existing `Editor`/ `InfiniteEditor` interfaces. Move only presentation into new components.
- Keep undo/history commands within the original document engine. UI popover should not write directly to native document.
- PDF + Page action calls `insertPageAfter`, `insertBlankPageAt` or a new transaction-friendly high-level API; checks page size PDF source and history semantics.
- Eraser control writes `ToolConfig.eraseMode` and `baseWidth` via current `ToolPopupHost`, after geometry fixes; don't build a parallel eraser state that drifts from saved preference.
- Selection context derives from selection state and action availability, not a separate stale menu boolean.
- Theme uses `LocalPalette` and `MaterialTheme` from the branding plan; paper/ink remains independent.

### 6.4 Instrument discoverability without collecting personal notes
Developer-only action tracing records optional UI event id, surface, activation path, test device profile—**never content/text/strokes/PDF names**. On-device usability metrics: taps needed to change eraser mode, insert PDF page, choose favorite pen; missing-action counts and re-opening popups. Do not ship analytics service without consent/privacy review.

## 7. Phased development with definitions of done

### Phase D0 — action inventory, screenshots, baseline (CV-101–CV-102)
Capture current phone/tablet screenshots and screen recordings (paged PDF, new note, infinite canvas), including settings and toolbar after scroll. For every feature write status SOURCE-CONFIRMED -> UI-REACHABLE -> DEVICE-VERIFIED, missing hardware/test caveats. Enumerate all `ToolbarItem`, menus, side panel contexts, settings. Prepare safe test PDF + handwritten ink.
**Exit:** no claims that code presence proves on-device success.

### Phase D1 — discoverable essentials, no engine rewrite (CV-103–CV-105)
Ship sticky primary Pen, Highlighter, Eraser, Selection, Undo, Redo and +Page for paged docs; active width/color strip and All Tools menu. Scaffold adapts to phone/tablet/toolbar location. A user can reach every already-supported action in <=2 direct UI interactions from canvas without blind horizontal scrolling. Standardize icon labels/menus and in-place state.
**Exit:** visible controls functional in both relevant canvases.

### Phase D2 — precision eraser and pen favorites (CV-106–CV-107)
Deliver real vector AREA eraser with size, hover ring, straight-line clipping, historical sweep, undo (CV-061–066). Quick pen preset picker and recent colors (CV-014–020). Highlighter-only erase only after CV-067 works.
**Exit:** handwritten letter trimmed exactly, 2-point line retained around gap; pen favorite selected with one tap.

### Phase D3 — PDF and page workspace (CV-108–CV-113)
Visible +Page, Before/After/End/template flow, thumbnails and PDF insertion. Contextual lasso menu. Simpler View/Paper controls with double page and search. Reuse current `Editor`, `SidePanel`, `Popups`; don't claim support for a mode that's untested on the device.
**Exit:** insert paper after PDF page 12 from visible action, edit, undo, save/reopen and export; selection and search work.

### Phase D4 — power tools, infinite canvas, customization (CV-114–CV-118)
Make sidebar search/bookmarks clear, infinite minimap/waypoints discoverable, customize/restore essentials, stylus gestures and focus/split context stable. Ensure a hidden entry cannot make feature inaccessible.
**Exit:** no dead actions, no mixed paged/infinite state and no loss of customization.

### Phase D5 — release and behavior QA (CV-119–CV-120)
Screenshot/gesture tests for relevant Android APIs, phone/tablet, low/high DPI, landscape, split, left-handed and screen readers. Check no inking frame-time regression, memory spikes or screen clipping. File write/safety prerequisites CV-001–005; ensure plan remains docs-only until development requested.
**Exit:** evidence-based feature discovery and functioning core flows; screenshots + device/build reports linked to tickets.

## 8. Acceptance tests / usability scripts

| Test | Procedure | Pass |
|---|---|---|
| DC-01 | New user opens PDF with zero training | Can find pen, highlighter, eraser, +Page without scrolling/searching settings |
| DC-02 | Pick AREA, change size, erase one letter segment | Only contacted ink deleted; rest retained; works at different zoom |
| DC-03 | Tap selected eraser again / chevron | Settings opens predictably, mode and size visible before gesture |
| DC-04 | Draw with pen then switch to saved favorite | One tap; active color/thickness preview accurate |
| DC-05 | Long toolbar on 320–360dp screen | Essential controls pinned; clear All Tools; no invisible action |
| DC-06 | User hides all advanced toolbar items | Still available through All Tools and searchable by name |
| DC-07 | Old customized layout loaded after upgrade | Existing layout preserved or explicit opt-in migration, can restore essentials |
| DC-08 | PDF page 12, + Page > After > Ruled | New editable sheet between PDF pages, thumbnails/order accurate |
| DC-09 | PDF +Page > Insert PDF | Source PDF joined/inserted without overwriting existing note, Undo once |
| DC-10 | Search and bookmark while annotating | Accessible from header/sidebar and correct page result |
| DC-11 | Lasso selected stroke | Context bar appears within screen, correct edit/undo |
| DC-12 | Enter infinite canvas | No page insertion; Minimap/Waypoints and tools accessible |
| DC-13 | Return to paged note, then split-screen | Correct active-pane label, page insertion affects intended pane only |
| DC-14 | Toggle light/dark/OLED, paper background | UI theme changes, original PDF/ink doesn't |
| DC-15 | Stylus side button / eraser end, palm nearby | No accidental whole-stroke deletion or panning |
| DC-16 | Zoom, rotate page, use Eraser and Lasso | Ring/selection geometry correct, exported PDF matches |
| DC-17 | Large PDF and 10k-stroke canvas | Tool interaction stays responsive with bounded allocations |
| DC-18 | TalkBack, 200% font and keyboard | Actions named, focus order correct, no clipped controls |
| DC-19 | Save error or unsupported action | Honest status, clear recovery path; never "Saved" falsely |
| DC-20 | Phone/tablet portrait/landscape screenshot parity | All key actions visible and UI doesn't cover writing or system bars |

## 9. Source files for engineering tasks

- **Action model / defaults:** `core/tools/ToolbarLayout.kt`, `core/tools/Tool.kt`, `settings/Settings.kt`, `settings/Preferences.kt`.
- **Common pinned/overflow/customization:** `ui/ToolbarFrame.kt`, `ui/Toolbar.kt`, `ui/InfiniteToolbar.kt`, `ui/ToolbarCustomizer.kt`, `ui/PreferencesPane.kt`.
- **Paged PDF + Page:** `ui/Editor.kt`, `ui/SidePanel.kt`, `ui/PageSizeControls.kt`, `core/model/PageStyle.kt`, `platform/PdfSource.kt`.
- **Pen/eraser/selection UI:** `ui/Popups.kt`, `ui/ColorPicker.kt`, `ui/CanvasMenus.kt`, `ui/Toolbar.kt`, `ui/InfiniteToolbar.kt`.
- **Ink correctness:** `core/model/Stroke.kt`, `canvas/InteractionController.kt`, `ui/InfiniteInteraction.kt`, `core/infinite/EraseSession.kt` (see eraser plan).
- **View/search/infinite:** `ui/Popups.kt`, `ui/SidePanel.kt`, `ui/SearchTab.kt`, `ui/CanvasWaypointsPopup.kt`, `ui/CanvasStylesPopup.kt`.
- **Styling & alerts:** `ui/theme/Palette.kt`, `NotesTakingTheme.kt`, `ui/icons/NotesTakingIcons.kt`; coordinate with CV-076–100.
- **Tests:** `app/src/test/.../tools/ToolbarLayoutTest.kt` or current equivalent; `core/model/StrokeEraseTest.kt`, `core/infinite/EraseSessionTest.kt`, paged layout/view tests and new Android Compose device tests.

All changes are **enhancements to one existing app and two existing edit surfaces**, not a new canvas library, a new backend or a Samsung Notes clone. Device screenshots, Compose preview, APK builds and production feature fixes remain future implementation tasks.
