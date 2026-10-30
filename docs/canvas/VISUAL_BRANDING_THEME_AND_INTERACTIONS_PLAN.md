# NotesTaking App — Visual Identity, Full Theme, Alerts, Popups & Motion Plan

**Created:** 2026-10-28
**Status:** Master design plan. First-pass launcher/loader artwork, colors and shared popup styling are implemented on `feature/modern-paper-ink-foundation` (see `docs/engineering/BRAND_FOUNDATION_2026-10-29.md`). BR-3 popup/destructive actions and BR-4 canvas-menu/toolbar/settings visual fixes are also partially implemented (see `docs/engineering/BRAND_DIALOG_CANVAS_BATCH_2026-10-29.md`); full BR-2 through BR-5 completion and device QA are still pending.
**Workspace:** C:\Users\WHITE-SNAKE\Desktop\xnotes-android
**Related:** MASTER_CANVAS_UPGRADE_PLAN.md, SAMSUNG_NOTES_STYLE_ERASER_PLAN.md, FEATURE_BACKLOG.md, BRAND_UI_AUDIT_MATRIX.md, CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md, CANVAS_FEATURE_VISIBILITY_MATRIX.md
**App:** Native Kotlin 2.2.21 / Jetpack Compose / Material 3, minSdk 26, targetSdk 36
**Brand:** NotesTaking App (working public-facing name); do not modify applicationId, package names, document extensions or copyright/license notices as part of visual rebrand.

## 1. Design brief and defaults

**Goal:** Give the full application a cohesive professional visual identity from launcher icon through file picker/home, PDF/notes editor, infinite canvas, panels, empty/loading/error states, alerts, popup menus, export progress and settings. Tablet-first handwriting UX, fully usable on Android phones. Improve confidence, organization and visual quality without distracting from writing or altering user note content.

**Direction selected for planning:** *Modern Paper & Ink* — premium, quiet, precise, tactile, clean but not sterile. Use restrained layered surfaces, subtle depth, clear typography and smooth short motion. The app's functionality is already extensive: do not fill the interface with decorative gradients, heavy frosted glass, uncontrolled neumorphism or animations that delay handwriting.

**Product constraints:** Offline-first; original PDF appearance preserved; no cloud login prerequisite; respect users' current light/dark/OLED/dynamic/custom theme preferences, font choices, written notes, undo and canvas ink. Phone/tablet/keyboard/stylus/talkback. English UI baseline; respect any supported localization.

**Not a Samsung Notes clone:** Borrow ergonomics (fast tool access, clear eraser picker, favorite pens, minimalist writing stage) without copying their icons, layout screenshots, logo, proprietary assets, sounds or code.

## 2. Grounded baseline: what is already implemented

| Surface | Observed file(s) | Existing code capability / upgrade need |
|---|---|---|
| App name / launcher metadata | app/src/main/AndroidManifest.xml:26-39, res/values/strings.xml | app label, icon and round icon already mapped; refine branding without changing activity/file URIs |
| Adaptive launcher icon | res/mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml | Adaptive background + foreground + monochrome present; **redesign art**, not mechanism |
| Bitmap legacy launcher assets | res/mipmap-mdpi ... xxxhdpi/ic_launcher*.png | Existing densities; regenerate all together from single master |
| Splash | res/values-v31/themes.xml, res/drawable/splash_blank.xml, res/drawable/notestaking_loader.xml; res/drawable-nodpi/notestaking_frame_00..23.png | Android 12+ system splash presently has blank icon + animation after; test first-frame consistency, dark/light flash, size and asset weight |
| Brand color inconsistency | res/values/colors.xml:3-6; settings/Preferences.kt:205 | Existing launch navy (#07142A), accent resource #00E676 and default material seed RGB(154,124,66) are not one deliberate identity; verify where each is used |
| Material 3 surface tokens | ui/theme/Palette.kt, MaterialColors.kt, NotesTakingTheme.kt | Already robust material seeded scheme, OLED and surfaces; refine source tokens and comprehensive preview tests, **do not replace engine** |
| Dynamic color | ui/theme/DynamicColors.kt, settings prefs | Existing Android 12+ wallpaper dynamic color support; retain as opt-in or established user choice |
| Shapes | ui/theme/Shape.kt, settings/MaterialThemeOptions.kt | Rounded and alternative corner styles supported; unify component sizes and responsive behavior |
| System theme and prefs | settings/Preferences.kt, ui/PreferencesPane.kt | system, light, dark, OLED, contrast/color/corner controls; preserve existing choices, add curated branding preset |
| App iconography | ui/icons/NotesTakingIcons.kt | Existing manually constructed 24dp round-line Feather/Lucide-derived vectors; extend consistently with license notices, avoid duplicate icon libraries |
| Dialog wrapper | ui/Popups.kt:1191-1218 | Shared Material AlertDialog wrapper already adds palette surface/border; **reuse it**, migrate direct uncategorized popups carefully |
| Popup controls | ui/Popups.kt, ui/Toolbar.kt, ui/InfiniteToolbar.kt, ui/Tooltip.kt | Tool/paper/eraser/color/zoom menus are established; standardize placement and sizes without changing behavior |
| Dialogs / destructive actions | ui/Backstage.kt, TrashPane.kt, SidePanel.kt, FolderPicker.kt, MergeDialog.kt, CanvasMenus.kt | Multiple existing confirmation, file and markup dialogs; review language, button order, dismiss semantics |
| Progress + feedback | MainActivity.kt:1642+, ui/Editor.kt:653+, NewNoteScreen.kt, AboutPane.kt | PDF import/export/saving dialogs, SnackbarHost, isolated Toast exist; standardize message severity, loading and retries |
| Navigation / explorer | ui/Backstage.kt, ExplorerTiles.kt | Folder navigation, file tiles, home, recent, preferences, trash; preserve workflows and keyboard/split-pane behavior |
| PDF / page / canvas UI | ui/Editor.kt, Toolbar.kt, SidePanel.kt, InfiniteEditor.kt, CanvasMenus.kt | Dense toolbar and popups support advanced tools; simplify discoverability, not delete tools |
| Logo in About | ui/AboutPane.kt | Text branding and license credits; update consistently while retaining MIT upstream and 3rd-party attribution |
| Fonts bundled | app/src/main/assets/fonts/inter, roboto, poppins, lora, etc. | Reuse existing license-cleared local families; avoid new web fonts or file redistribution |
| Notifications | res/drawable/ic_notification_mic.xml, platform/RecordingService.kt | Functional microphone/recording notification icon; keep contrast/legibility and update branding only where appropriate |

The user's existing design controls are features; do not hard-code a global palette that makes them stop working. Verify current runtime screenshots before marking baseline findings as visual defects.

## 3. Brand identity: logo + app icon specification

### Recommended identity (subject to visual approval)
Name remains **NotesTaking App** for this plan. Brand promise: a reliable place for notes, ideas and learning. Concept: **a single unfolding page with one fluid ink curve** suggesting both handwriting and PDF markup. Should be recognizable without text at 24–48dp, not another generic AI-gradient notebook icon.

### Three original icon exploration directions (make mockups in implementation phase)
1. **Folded Page / Ink Trail — preferred.** Rounded folded page silhouette with deliberate single ink curve/pencil stroke. Elegant and specific to note-taking; ensure the shape remains legible at small scale.
2. **Open Notebook / Spark of Idea.** Two simple leaf/page planes separated by a subtle writing line. Friendly but potentially generic; simplify.
3. **N-Page Monogram.** Abstract letter N built from folding paper and a pen stroke. Distinctive and scalable, may need brand testing.

The logo is a SYMBOL + optional wordmark family, not merely cropped application screenshots. Create primary full-color, monochrome, light-on-dark, dark-on-light and small favicon-sized variants. Main app icon has **no tiny text** and no busy details. Ensure no similarity to Samsung Notes, Goodnotes, Notion, OneNote or previous XNotes asset trademarks.

### Android asset deliverables and exact engineering constraints
- Original editable master drawing (SVG/vector source or documented Figma/Canva vector artifact) with usage and ownership/rights. A generated raster image alone is insufficient as source for crisp adaptive/monochrome icons.
- Adaptive layers foreground, background and monochrome targeting 108x108dp canvas; foreground essential logo within safe center 66x66dp, margin reserved for masks. Test circular, squircle, rounded square, launcher animation and badges.
- Generate `ic_launcher` and `ic_launcher_round` XML, color tokens, mipmap raster fallbacks for every existing density mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi and correct Android 13+ monochrome layer.
- No baked shadow in the foreground, because Android launcher supplies masking/depth; keep transparent edges and crisp colors.
- Build and verify on API 26, 28, 33, 36, themed icon enabled/disabled, light/dark wallpaper and launcher alternatives.
- Play Store 512x512 listing icon and 1024x500 feature graphic are separate *future store assets*, not the Android launcher resource; screenshots must be original.
- Keep recording notification small icon separate: a monochrome notification glyph should remain readable at system sizes.
- Update splash frame/asset palette from the approved symbol; do NOT animate system splash endlessly or delay first interactive screen.

Android icon source: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
Android themed icon guide: https://developer.android.com/distribute/aep/aep-req-theme-app-icons

## 4. Cohesive visual theme and tokens

### Proposed brand seeds for **art direction**, not production palette until contrast-tested

| Role | Proposed hex | Usage |
|---|---|---|
| Brand ink navy | #17243B | Brand mark/key text, main dark accent reference |
| Primary blue | #526DDF | Selected drawing tools, key actions and links |
| Secondary teal | #188D88 | Support/secondary action; not automatic success semantic |
| Warm paper | #F8F7F3 | Light UI inspiration; not forced on PDF background or user paper settings |
| Dark UI slate | #202631 | Dark Material scheme base reference |
| Primary high contrast | Generated by Material 3 tonal roles | Must pass on-primary/on-surface contrast tests, not literal hex everywhere |

**Critical implementation:** Seed the existing `MaterialColors.seeded(...)` and `Palette.material(...)` machinery with the approved brand preset. Define named semantic roles for `brand`, `success`, `warning`, `error`, `informational`, `focus`, `selection`, `divider`, `overlay`, `paperFrame`, `inkColorPreview`. Keep existing `LocalPalette` and `MaterialTheme.colorScheme` in sync; centralize any exceptional foreground/background values. Don't use hex literals in individual dialogs and don't confuse pen *ink colors* with UI theme accent colors.

**Theme modes:** Follow System, Light, Dark, OLED, optional Wallpaper Dynamic (existing), and Custom Accent/Preset. No accidental resets during upgrade. Offer an explicitly labeled "NotesTaking Original" or "Paper & Ink" curated preset instead of overwriting customized preferences. Confirm current behavior of dynamic material `materialStyle`, `cornerStyle`, contrast and dual-seed options before changes.

**Type scale** (UI only; typed note/document fonts unchanged):
- Titles 24–28sp semibold; section titles 18–20sp semibold; body 14–16sp regular; supporting text 12–14sp; button text ~14sp medium; fine labels >=12sp when practical.
- Prefer bundled `inter` for modern UI, or system `Roboto` if metrics/performance are better. Use at most two UI families; set `MaterialTheme.typography` explicitly (currently `NotesTakingTheme.kt` passes colors and shapes, but not typography).
- Use system font-scale/layout, avoid ellipsis for destructive actions. Preserve font selection in actual documents and code editor.

**Shape/spacing/elevation:**
- Standard spacing increments: 4, 8, 12, 16, 24, 32dp. E.g. 12–16dp dialog internal padding, 16–24dp tablet panel, 8–12dp tool groups; responsive rather than fixed in narrow screens.
- Small/medium/large corners from the existing `CornerStyle` system, with a baseline around 8/12/16/24dp; respect users choosing square vs round.
- Hairline borders only where needed for surface separation; subtle, capped shadow and tonal elevation; no aggressive gloss/glass blur inside ink area.
- Touch target goal >=48dp for important actions, especially pen/eraser/undo/cancel, with tooltips and semantics.
- Tool active state uses foreground + filled/tonal chip + optional underline/edge, never color alone.

**Theme contract:** Paper/ink/PDF import/export appearance must **not change** when UI chrome accent/theme changes; only user-defined note/page style changes paper/ink. System window bar contrast and dark/light icons must track actual background. Edge-to-edge must respect navigation bars, gesture insets, cutouts and IME. User content is the visual focus.

Official Material 3 source: https://developer.android.com/develop/ui/compose/designsystems/material3
Custom Compose theme guidance: https://developer.android.com/develop/ui/compose/designsystems/custom

## 5. Complete component library and visual patterns

Build shared reusable components on top of Compose Material3; preserve the existing `ui/Popups.kt` AlertDialog wrapper and `NotesTakingTheme.kt` system.

### Alerts: which surface and why
- **Destructive confirmation dialog** (Delete file, empty trash, discard unsaved note): title states object, body states consequence and whether undo/trash recovery is possible; primary destructive action danger-toned, secondary "Cancel"; no auto-dismiss on outside tap for truly destructive flows; proper keyboard/Back accessibility.
- **Normal confirmation dialog** (Rename, Save as, Change page size): compact standard dialog with labeled fields; validate inline and disable confirmation until valid; moving focus correctly.
- **Nonblocking snackbar** (Saved, copied, moved, small success): bottom safe-area above toolbar/keyboard, brief and readable; optional Undo or Retry if action is safe and available. No extra modal for trivial events.
- **Banner / inline error** (Failed to open PDF, storage permission, OCR model missing): persistent context-specific message and precise next action; don't hide errors behind short toasts.
- **Progress modal or progress sheet** (Import 300-page PDF, Export, Backup, OCR indexing): cancellable if engine truly supports cancellation; progress stage, count, honest failure result. No fake percentages; background work never blocked by ornamental progress animation.
- **Tool anchored popover** (pen settings, eraser size, color palettes, quick page actions): anchored to tapped tool, bounds-safe and keyboard/pointer accessible. Avoid dialogs that cover the writing region.
- **Bottom sheet** (phone page templates, multi-step selection, large option sets): not for every simple tool action. On tablet use an anchored panel/sidebar with equivalent information.
- **Context menu** (file tile, selected ink, PDF markup): predictable item order, text/icon labels and separator use; selection menu should not hide the active content.
- **Tooltip / hint**: concise action + optional shortcut, distinct from actionable popovers, dismiss on tap and respect reduce-motion.

### Shared component contracts (suggested)
- `BrandDialog` / `ConfirmActionDialog`: title, description, severity, icon?, primary/secondary, destructive flag, busy flag, dismiss policy, accessibility.
- `BrandSnackbarHost`: severity, optional 1 action, queue/coalescing, safe positioning.
- `BrandPopupSurface` / `ToolSettingsPopover`: width limits, shape, shadow, anchor/side, focus/mouse/stylus/back handling.
- `BrandBottomSheet`: type-safe result/cancel state, phone/tablet adaptive layout, IME.
- `BrandProgress`: determinate/indeterminate, truthful cancel support, background retry.
- `BrandEmptyState`: own lightweight vector glyph, title, body, one useful action.
- `BrandIconButton`, `BrandSegmentedControl`, `BrandSlider`, `BrandSwatch`, `BrandTextField`, `BrandCard`, `BrandListRow`, `BrandSectionHeader`, `BrandTooltip`.
- `BrandSaveStatus`: unsaved, saving, saved, failed; never display 'Saved' from a suppressed exception. Link to CV-001/CV-002 before claiming reliable.
- `SemanticColorTokens`, `BrandDimensions`, `BrandTypography`, `BrandMotion`: core tokens, no duplicate magic numbers across modules.

Don't make a generic giant UI framework if a small extension of existing Compose components suffices. Prefer centralized defaults and thin wrappers. Avoid changing icon drawing style or stroke engine from the theme layer.

## 6. Screen-by-screen visual rollout

| Screen / workflow | Target design | Most relevant code |
|---|---|---|
| Android launcher and splash | Recognizable icon, themed monochrome, smooth branded first frame, no blank gap | mipmap resources, splash_blank, NotesTakingLoader, MainActivity |
| Home/dashboard and navigation | Clear create/open/import options, clean heading, recent notes hierarchy, coherent app logo | Backstage.kt, ExplorerTiles.kt |
| Folder/file explorer | Consistent tiles/list, selected/hover/drag states, filename and file type badges, empty folders, trash | Backstage.kt, ExplorerTiles.kt, TrashPane.kt |
| New note and templates | Step clarity, template previews, branded sizes/selection, no excessive modal chains | NewNoteScreen.kt, PageSizeControls.kt |
| PDF reader and paged editor | Quiet paper stage, clear page index, readable toolbar/sidebar, floating tools without blocking writing | Editor.kt, Toolbar.kt, SidePanel.kt, CanvasMenus.kt |
| Infinite canvas | Same toolbar and popup tokens, clear canvas controls/minimap, separate paper color | InfiniteEditor.kt, InfiniteToolbar.kt, CanvasStylesPopup.kt |
| Precision eraser/pen settings | Fast one/two tap flow, accurate ring, distinct modes and width/opacity indicators | Popups.kt, Toolbar.kt, SAMSUNG_NOTES_STYLE_ERASER_PLAN.md |
| Color picker/fonts/style | Swatches with legible selected outline, numeric color entry, no jump on popup | ColorPicker.kt, Popups.kt, TextToolConfigPopup.kt |
| PDF import/export and batch | Honest progress, errors, retry/cancel, safe document name and page-count summaries | MainActivity.kt dialogs, platform PDF routines |
| Search, bookmarks and multi-selection | Search result emphasis, clear selected state, tool-accessible menu and keyboard | SearchTab.kt, SidePanel.kt, CanvasMenus.kt |
| Preferences and personalization | Curated theme presets, live previews, grouping into Appearance, Drawing, Documents, Accessibility | PreferencesPane.kt, settings/Preferences.kt |
| Recording/audio | Consistent player, permission explanation, real recording status and notification semantics | AudioChip.kt, platform/RecordingService.kt |
| About, help and permissions | Same typography/logo, correct MIT/upstream credits and license links | AboutPane.kt, res/values/strings.xml |
| All error/empty/loading dialogs | Replace inconsistent shapes/margins/severity with shared component patterns | Popups.kt, Backstage, MainActivity, folders/dialog files |

### Canvas toolbar and discoverability integration

The user has reported that the existing app includes features which cannot be easily found or understood inside the canvas. **This is a navigation/component UX problem, separate from logo color polish.** Coordinate BR-2..4 (tokens, shared popovers, menus, responsive toolbar) with **CV-101–CV-120**, described in `CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md`. Do not render a beautiful but functionally misleading toolbar: Pen/Marker/Eraser/Select/Undo/Redo must be pinned, AREA/Stroke and size directly apparent, +Page visible for PDF/paged notes, and All Tools reveals all actual implemented actions. Infinite canvas uses Minimap/Saved Views in place of page controls. Every visible button must invoke a real editor method, not a mock. Retain user-customizable toolbar layouts and disabled action states.

## 7. Motion & feedback design

Functional motion (not decoration):
- Icon/button press: ~80–120ms; chip selection: 120–180ms; anchored popover enter: 150–220ms; bottom sheet: 220–300ms. These are proposed design durations, tune after device testing.
- Navigation page transitions and file tile states: subtle opacity/position; no large zoom causing motion fatigue. Predictive back should work where supported by Android and not collide with unsaved changes.
- Save/complete: a discreet status check, no full-screen celebration or confetti.
- Destructive action: confirmation clarity plus optional undo where technically correct, not distracting vibration loops.
- Import/export: simple progress, cancel affordance, no indefinite fake indicators.
- Respect system animation scale, TalkBack, reduced-motion preference; remove ornamental movement when reduced.
- CRITICAL: keep animations on UI chrome, **not** on drawing/rendering or stylus input pipelines; don't degrade ink latency or block pen hover.

## 8. UX content and copy system

- Plain English, specific outcomes. 'Delete 3 notes?' / 'Move to trash' rather than 'Are you sure?'. Label 'Save failed — choose a location and try again' rather than a silent toast.
- Text styles: sentence case for labels; 'Cancel' on nondestructive exit; 'Delete permanently' only when actually permanent; 'Keep editing' in unsaved note flows; 'Undo' for reversible operations.
- Error template: what failed, what was kept, next action. Never claim save or export completed before IO verified.
- Standard loading/empty copy for no notebooks, no PDF pages, searching, blank canvas, low storage, permission denied and OCR model missing (future).
- Keep all text from Android string resources with correct localization/plural support; visual brand doesn't change document text.

## 9. Accessibility / adaptive quality gates

- WCAG-ish contrast target: >=4.5:1 for normal text, >=3:1 for large text/icons and meaningful controls; test selected/disabled, light/dark/OLED/custom wallpaper accent.
- Target >=48dp tap regions; hit slop where visual glyph smaller. TalkBack semantics, accessibility focus order, alert titles and roles. Screen reader should announce the current pen and eraser size/mode.
- Phone widths 320dp and 360dp, 8-inch and 11-inch tablets, portrait/landscape, split-screen, IME, Android display-size and font scaling up to 200%; keyboard/mouse/trackpad and stylus hover.
- Avoid drawing sheet behind system bars or clipping menus near right/left edges; test inset/predictive back behaviors on target SDK.
- Motion off for accessibility, no ghost overlays under zoom. Test high-contrast palette and color-blind distinguishability without color-only status.
- Theme mismatch tests: selecting Dark/OLED must not invert source PDF or change written ink colors. Export should match document styles.

## 10. Implementation roadmap + dependencies

**Phase BR-0 — Inventory, safe branch, design approval (CV-076–078)**
- Capture actual screenshots at fixed devices/themes. Count and classify dialogs, popups, errors, menus; track each screen. Back up docs/native files.
- Create three original logo concepts with light/dark/mono Android launcher mockups; select one before generating production artwork.
- Approve design token spec and theme migration contract. Ensure no existing user customization lost.

**Phase BR-1 — Launcher icon, logo, splash (CV-079–082)**
- Produce editable approved master vector source (owned/license cleared), adaptive icon layers, monochrome icon and raster fallbacks.
- Align system splash and 24-frame loader visual styling; reduce visual gaps and asset footprint if possible.
- Update About app logo/wordmark only, with license attribution intact. Test mask/theme/API variants.

**Phase BR-2 — Core theme and primitives (CV-083–087)**
- Integrate brand seed with existing MaterialColors and Preference system, ensure dynamic wallpaper/custom seeds remain.
- Explicit Material Typography, spacing, shape and elevation tokens.
- Brand icon style catalog and reusable control wrappers.
- Build a **development-only component gallery** with light/dark/OLED/dynamic/large text previews and screenshot goldens; don't ship a hidden production debug menu.
- Verify theme/color contrast and PDF ink independence.

**Phase BR-3 — Alerts, popups, snackbars, progress (CV-088–092)**
- Standardize shared dialog, destructive confirmation, popover, tool menus, snackbars, loading and errors.
- Migrate one high-impact screen at a time. No blanket automated replacement that breaks callback/dismiss/focus semantics.
- Integrate save status with real file IO correctness (CV-001), cancel with actual cancellable jobs.

**Phase BR-4 — Home, explorer, editors, personalization (CV-093–097)**
- Update home/file explorer, create-note/templates, PDF editor, infinite canvas and Tool/Eraser settings consistently.
- Preserve all original functions and stylus interactions. Keep toolbar density adaptive; never replace full note workspace with a flashy dashboard.
- Polish preferences theme selector, color previews, availability of settings.

**Phase BR-5 — Motion, testing and release (CV-098–100)**
- Finalize motion tokens, accessible behaviors and predictable focus/keyboard input.
- Device/UI screenshot audits with API26/28/33/36, light/dark/OLED, font scaling, long PDF notes, low RAM.
- CI unit/UI/lint, APK/resource validation, signed release checks separately. Do not commit asset source files with license restrictions.

Do not couple progress in BR phases to OCR, PDF recognition or pen engine rewrites; these are independent backlogs. Coordinate BR-4 eraser UI with the precision eraser work CV-061–066.

## 11. Acceptance criteria

| ID | Release-quality check |
|---|---|
| BR-A01 | All launcher resolutions and adaptive masks render correctly; monochrome themed icon recognizable |
| BR-A02 | Splash never flashes incorrect theme or waits on heavy animations; cold/warm launch verified |
| BR-A03 | Standard screens share the same logo, text scale, semantic colors, icon treatment, corners and spacing |
| BR-A04 | Existing user custom material seeds, corners, light/dark/OLED and toolbar layouts survive upgrade |
| BR-A05 | Changing UI accent never changes existing PDF page/ink or affects PDF export appearance |
| BR-A06 | Delete/move/discard alert states have unambiguous outcome, cancel, disabled and Back handling |
| BR-A07 | Save/export error shown accurately, no false success; progress/cancel only where supported |
| BR-A08 | Popups anchor safely around viewport, stylus, IME and split-screen, do not hide ink under pen |
| BR-A09 | TalkBack labels, contrast, visible focus, 48dp touch zones, 200% font scale and reduce motion pass |
| BR-A10 | One-handed phone, 8-11in tablet, dark/OLED and landscape scenarios preserve functionality |
| BR-A11 | 60/90/120Hz writing tests show no regression in strokes/render latency after UI polish |
| BR-A12 | Android resources build and lint; original licenses/attribution retained; debug/release icon test passes |
| BR-A13 | Icon and notification not confused; unreadable badge/notification variants corrected |
| BR-A14 | Themed app icon remains legible over different wallpaper palettes |
| BR-A15 | No unintended API/backend dependencies, account requirements or document-format migrations |

## 12. Source and inspiration index

1. Adaptive icons / safe zones / monochrome: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
2. Material Design 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
3. Custom design systems in Compose: https://developer.android.com/develop/ui/compose/designsystems/custom
4. Android 16 behavior / themed icon / back: https://developer.android.com/about/versions/16/behavior-changes-all
5. Themed launcher icons: https://developer.android.com/distribute/aep/aep-req-theme-app-icons
6. Material 3 theme and dynamic color example: https://developer.android.com/codelabs/jetpack-compose-theming

These are official design/system references, not external copyable assets. The current icon/launcher and UI baseline is taken from inspected LocalMCP source files; no emulator/screenshots captured during this planning pass. Implementation remains pending.
