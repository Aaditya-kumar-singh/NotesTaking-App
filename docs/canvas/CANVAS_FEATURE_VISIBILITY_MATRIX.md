# Canvas Feature Visibility Matrix — Implementation and UX Truth Table

**Updated:** 2026-10-28. Evidence: LocalMCP source inspection, NOT an emulator screenshot or user test. Base table reflects state before the first UI patch; CanvasQuickAccess.kt now exposes Eraser, +Page and All Tools on both applicable editors (see CANVAS_FIRST_PASS_IMPLEMENTATION_STATUS.md); real-device verification pending.
**Primary plan:** `CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md`
**Classification:** PRESENT + DEEP UI = source/dispatch exists but discoverability weak; NEEDS FIX = existing output defective; ALREADY VISIBLE = code suggests direct button, still device verify; RESEARCH = do not enable until built.
**Column "proposed visible entry" is a plan, not an implemented control.**

| Feature/action | Current code location & current access | Source status | Proposed visible entry | Tasks / acceptance |
|---|---|---|---|---|
| Pen | ui/Toolbar.kt:205-210 | ALREADY VISIBLE, long scroll possible | Pinned Pen + active preset/name | CV-103/107; tap starts drawing |
| Pen types | core/tools/ToolbarLayout.kt:185-197 has PEN/DASHED/CALLIGRAPHY/SPEED/TAPER | PRESENT but spread as many icons | Pen ▼ segmented list and favorites | CV-104/107, CV-014..020 |
| Pen thickness | ui/Popups.kt:176-179 after second tap | PRESENT + DEEP UI | Inline Width micro control | CV-104/107 |
| Pressure/glow/opacity | ui/Popups.kt:93-203 | PRESENT + DEEP UI | Pen ▼ > Advanced; clear preview | CV-104/107 |
| Swatch colors | ui/Toolbar.kt:273-284; ui/ColorPicker.kt | ALREADY VISIBLE but may scroll | Pin Color with recent choices and HEX | CV-104/107 |
| Highlighter | ui/Toolbar.kt:205-210; popups strength/straight line | ALREADY VISIBLE | Pinned Marker + intensity swatch | CV-103/104 |
| Area Eraser mode | core/tools/ToolConfig.kt:12-18 default STROKE; ui/Popups.kt:945-950 | PRESENT + NEEDS FIX | Pinned Eraser, Area default + clear label | CV-106 + CV-061..066 |
| Eraser size | ui/Popups.kt:949-952 | PRESENT + DEEP UI | Eraser size slider, Fine/Medium/Large | CV-106 + CV-062 |
| Precision partial erase | core/model/Stroke.kt:683-721 sample point split, straight whole | NEEDS FIX | Same Eraser control after geometry fix | CV-063..066 |
| Highlighter only erase | no distinct mode in ToolConfig enum | RESEARCH/NEW | Eraser ▼ after model supports it | CV-067 |
| S Pen button quick erase | canvas/InteractionController.kt:675+, ui/InfiniteInteraction.kt | PRESENT, DEVICE QA | Tool option and gesture hint | CV-117 + CV-068 |
| Undo/Redo | ui/Toolbar.kt:224-225 | ALREADY VISIBLE in scrolling bar | Pinned actions | CV-103 |
| Select/Lasso | ui/Toolbar.kt:205-208, CanvasMenus.kt | PRESENT | Pinned Select ▼, context bar on selection | CV-111 |
| Stroke style/selection re-color | ui/CanvasMenus.kt:180+ | PRESENT + CONTEXT MENU | Context bar 'Style' > Color/Width | CV-111 |
| Cut/Copy/Paste/Duplicate | ui/CanvasMenus.kt:180+, ui/SidePanel.kt:592-597 | PRESENT + CONTEXT MENU | Context bar / All Tools > Edit | CV-111/105 |
| Add blank page | ui/SidePanel.kt:590; ui/Editor.kt:6572,6679 | PRESENT + DEEP UI | **+ Page** always visible in paged note | CV-108/109 + CV-006..013 |
| Insert PDF after page | ui/SidePanel.kt:591, ui/Editor.kt:6690 | PRESENT + DEEP UI | + Page > Insert PDF | CV-109 |
| Duplicate/Rotate/Delete page | ui/SidePanel.kt:592-605 | PRESENT + CONTEXT MENU | Page overview > action row / More | CV-110 |
| Page templates/line/grid | ui/Popups.kt:221+, core/model/PageStyle.kt | PRESENT + DEEP UI | Paper sheet in + Page and Paper menu | CV-113 |
| Page thumbnails | ui/SidePanel.kt:94-156 | PRESENT but panel optional | Visible Pages button and optional footer | CV-110 |
| Contents/Bookmarks/Search | ui/SidePanel.kt:139-152 | PRESENT in icon-only tabs | Search header and readable navigation panel | CV-114 |
| Page counter/jump | ui/Toolbar.kt:227-242 | PRESENT in toolbar | Header Page N/M | CV-110 |
| Page view SINGLE/DOUBLE/COVER | ui/Popups.kt:705-715 | PRESENT + DEEP UI | View > one/two pages and scroll | CV-112; device validate double mode |
| Fit, zoom, zoom lock | ui/Toolbar.kt:248-269 | PRESENT in scrolling bar | View quick controls | CV-112 |
| PDF contrast/invert/brightness/rotation | ui/Popups.kt:719-737 | PRESENT + DEEP UI | View > PDF display only | CV-112 |
| PDF text markup | ui/Toolbar.kt:213-215, if(editor.hasPdf) | PRESENT conditional | PDF > Text Highlight (not freehand marker) | CV-105 |
| Image/Camera/Audio/Sticker | ui/Toolbar.kt:616+, ui/InfiniteToolbar.kt:136-144 | PRESENT through Insert | All Tools > Insert and optional + insert | CV-105 |
| Text box/typing | ui/Toolbar.kt:205-208 | PRESENT paged only | All Tools > Insert / Write text | CV-105 |
| Shapes and ruler | ui/Toolbar.kt:217-220, core/tools/ToolbarLayout.kt | PRESENT | All Tools > Drawing | CV-105 |
| Magic Wand | ui/Toolbar.kt:217-218 / InfiniteToolbar.kt:130-135 | PRESENT toggle, meaning not verified | All Tools; label after code/device audit | CV-101/105 |
| Infinite minimap | ui/InfiniteToolbar.kt:172-175 | PRESENT | Sticky canvas header Minimap | CV-115 |
| Infinite saved waypoints | ui/InfiniteToolbar.kt:168-170 | PRESENT | Canvas header Saved Views | CV-115 |
| Toolbar top/bottom/left/right & floating | ui/ToolbarFrame.kt:78-124 | PRESENT + SETTINGS UI | All Tools > Customize Toolbar / Preferences | CV-116 |
| Toolbar sections visibility/move | ui/ToolbarCustomizer.kt:52-59, ToolbarLayout.kt:67-245 | PRESENT + SETTINGS UI | Customize quick entry, Essentials & Reset | CV-116 |
| OCR handwriting conversion | OCR research plan only | RESEARCH | No production action yet | CV-051..060 |
| Easy Writing Pad | backlog CV-073 | RESEARCH | Optional experiment later | CV-073 |
| Graphite and textured brushes | backlog CV-025..028 | RESEARCH/NEW | No misleading icon until implemented | CV-025..028 |

## Evidence and risk notes
1. The current toolbar defaults have many items and `ToolbarFrame.kt:172-195` is horizontally/vertically scrollable. The user may not find an existing tool even if the default list includes it. The plan pins essentials and adds an explicit overflow sheet, not simply more icons.
2. `ui/Toolbar.kt:309-314` and `ui/InfiniteToolbar.kt:100-125` reveal configuration after re-tapping an active tool. This is a learnability problem; a chevron and one-tap contextual options are proposed.
3. `ui/SidePanel.kt:590-605` has page actions but only inside a menu. A persistent +Page is proposed; its page order and PDF export must be tested.
4. Eraser's AREA option does not mean its behavior is finished. `Stroke.erasedBy` center-point sampling and straight-line whole deletion need geometry upgrade.
5. `ViewingMode.DOUBLE` already exists in `ui/Popups.kt:705-715`; treat the old CV-035 as *make it discoverable and test*, not invent the whole feature.
6. Paged note and infinite canvas should share UX styling and core tools but have different page vs waypoint actions. Avoid unavailable controls and dead slots.
7. The usability target "<=2 taps" applies to discovery/activation of a currently supported action from canvas, not to completing complex multistep operations.
8. No user data or names should appear in UX traces/screenshots. Use synthetic PDF and note fixtures.
9. All source status requires Android screenshot/device validation before marked DEVICE-VERIFIED; no visual screenshot supplied in this audit.

## Proposed device evidence
- Minimum: Android API 26/28/33/36 phones (320–400dp width), 8–11 inch tablets portrait/landscape; stylus hover/eraser/side button; optional keyboard/mouse.
- Capture baseline and post-redesign PNG screenshots for toolbar first-load, More Tools, eraser settings, pen favorites, +Page from PDF, page thumbnails, double-page view, selection context, infinite minimap, color picker, dark/OLED and 200% font scale.
- Android app structural tests do not validate interaction: run testable Compose UI semantics, individual feature behavior and real-device screenshots.

Reference UI/behavior: https://www.samsung.com/in/support/mobile-devices/how-to-use-samsung-notes/ ; https://www.samsung.com/us/support/answer/ANS10003634/ ; https://www.samsung.com/us/support/answer/ANS10002404/ .
