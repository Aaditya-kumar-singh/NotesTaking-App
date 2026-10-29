# NotesTaking App — Brand/UI Screen and Component Audit Matrix

**Created:** 2026-10-28. **Document state:** Planning audit with partial BR-1 / BR-3 / BR-4 source changes on `feature/modern-paper-ink-foundation`; no device screenshot validation yet. See `docs/engineering/BRAND_DIALOG_CANVAS_BATCH_2026-10-29.md`.
**Companion:** VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md
**Tag meanings:** EXISTING = source evidence; REVIEW = visually unverified/needs QA; UPGRADE = proposed consistency work.

## 1. Component coverage inventory

| ID | Component / interaction | Current file | Current evidence | Proposed change | Owner phase |
|---|---|---|---|---|---|
| UI-01 | App launcher icon + round | res/mipmap-anydpi-v26/*.xml; res/mipmap-*/ic_launcher*.png | EXISTING: adaptive foreground/background/mono | New original approved icon + regenerate all densities | BR-1 |
| UI-02 | System splash and loader | res/values-v31/themes.xml; drawable/splash_blank.xml; drawable/notestaking_loader.xml; drawable-nodpi frames | EXISTING: system splash blank + 24-frame custom loader | Cohesive brand entry and cold-start flash/performance QA | BR-1 |
| UI-03 | Brand wordmark/About | ui/AboutPane.kt; res/values/strings.xml | UPDATED IN SOURCE: shared vector mark in About and navigation headers, original MIT credits retained | Verify tablet/phone screenshot, small-size legibility and TalkBack | BR-1 |
| UI-04 | Material palette generator | ui/theme/Palette.kt; MaterialColors.kt | EXISTING: tonal material scheme | Approved seed, semantic roles, contrast matrix | BR-2 |
| UI-05 | Appearance modes / wallpapers | ui/theme/DynamicColors.kt; settings/Preferences.kt | EXISTING: System, Light, Dark, OLED, dynamic support | Keep user choice, curated brand preset, no change to actual paper | BR-2 |
| UI-06 | Material typography | ui/theme/NotesTakingTheme.kt | EXISTING: color + shapes only in Theme wrapper | Explicit typography scale using bundled licensed fonts | BR-2 |
| UI-07 | Shape/radius tokens | ui/theme/Shape.kt; settings/MaterialThemeOptions.kt | EXISTING: corner style variants | Consistent cross-components, user choice retained | BR-2 |
| UI-08 | Toolbar icons | ui/icons/NotesTakingIcons.kt; res/drawable/ic_stroke*.xml | EXISTING: drawn 24dp stroke/icon family | Consistent icon stroke/size/active/hover/touch | BR-2 |
| UI-09 | Shared AlertDialog wrapper | ui/Popups.kt:1191-1218 | EXISTING: styled Material dialog | Semantic standard variant and destructive handling | BR-3 |
| UI-10 | File name dialog | ui/Backstage.kt:2275+, ui/Toolbar.kt:344+ | EXISTING | Shared validation/error, IME, focus, button order | BR-3 |
| UI-11 | Trash/delete dialogs | ui/TrashPane.kt, Backstage.kt | EXISTING | Permanent/trash distinction, Undo where correct, warning color | BR-3 |
| UI-12 | Folder picker/merge | ui/FolderPicker.kt, MergeDialog.kt | EXISTING | Adaptive dialog/sheet, keyboard/touch/accessibility | BR-3 |
| UI-13 | PDF page-size and page actions | ui/PageSizeControls.kt, SidePanel.kt | EXISTING | Consistent control hierarchy and confirmation | BR-3/4 |
| UI-14 | Tool settings popovers | ui/Popups.kt, Toolbar.kt, InfiniteToolbar.kt | EXISTING: anchored popups | Shared compact labels, sliders, preview, boundaries | BR-3/4 |
| UI-15 | Color picker | ui/ColorPicker.kt | EXISTING | Swatch focus/contrast/cursor/hex states | BR-3 |
| UI-16 | Tooltip/help | ui/Tooltip.kt | EXISTING | Consistent delay, placement, shortcut text, reduce-motion | BR-3 |
| UI-17 | Feedback snackbar and toast | ui/Editor.kt, MainActivity.kt, NewNoteScreen.kt, AboutPane.kt | EXISTING: snackbar and one isolated Toast | Unified lightweight status/Undo/Retry | BR-3 |
| UI-18 | PDF import/export progress | MainActivity.kt:1642+, 1714+, 1749+, 1792+ | EXISTING: progress modal classes | Honest progress/cancel/error states, visual consistency | BR-3 |
| UI-19 | Home + sidebar | ui/Backstage.kt | EXISTING: navigation/create/folders | Stronger hierarchy and spacing, same routes | BR-4 |
| UI-20 | File explorer cards/tiles | ui/ExplorerTiles.kt, Backstage.kt | EXISTING | Shared hover/selection/thumbnail/metadata states | BR-4 |
| UI-21 | Empty state | ui/Backstage.kt, NewNoteScreen.kt, search | REVIEW | BrandEmptyState with one key action, no huge illustrations | BR-4 |
| UI-22 | New note screen | ui/NewNoteScreen.kt | EXISTING | Clear creation steps and template cards | BR-4 |
| UI-23 | PDF and note editor | ui/Editor.kt, Toolbar.kt, SidePanel.kt | EXISTING | Quiet chrome/paper separation, page navigation, tools | BR-4 |
| UI-24 | Infinite canvas controls | ui/InfiniteEditor.kt, InfiniteToolbar.kt | EXISTING | Match note editor token hierarchy without GL changes | BR-4 |
| UI-25 | Eraser picker and preview | ui/Popups.kt:919+, controllers | EXISTING: Stroke/Area/size slider | See CV-061–070, live radius and better preset controls | BR-4 |
| UI-26 | Pen favorites/palette | ui/Toolbar.kt, Popups.kt, PreferencesPane.kt | EXISTING: pens and swatches | User-preserved new favorite chips and sizes | BR-4 |
| UI-27 | PDF text selection and menus | ui/CanvasMenus.kt, SearchTab.kt | EXISTING | Source text legibility, accessible options, focus | BR-4 |
| UI-28 | Audio/recording player | ui/AudioChip.kt, platform/RecordingService.kt | EXISTING | Consistent progress/state and contrast; notification safe | BR-4 |
| UI-29 | Settings personalizations | ui/PreferencesPane.kt; ui/MaterialColourPicker.kt; ui/ToolbarCustomizer.kt | UPDATED IN SOURCE: theme preview and reset confirmation; toolbar customizer chips now expose accessible visibility states with 48dp targets | Verify theme persistence, draggable sections, chip toggles and delete on phone/tablet with TalkBack | BR-4 |
| UI-30 | Motion/interaction | ui/Toolbar.kt, Tooltip.kt, Backstage.kt, SidePanel.kt | EXISTING: Compose animations | Token durations, reduce-motion, no inking pipeline cost | BR-5 |
| UI-31 | App notification small icon | res/drawable/ic_notification_mic.xml | EXISTING | Preserve system monochrome legibility; separate from app icon | BR-5 |
| UI-32 | Accessibility/focus/IME | All UI screens; ui/Toolbar.kt; ui/ToolbarCustomizer.kt | PARTIAL SOURCE FIX: drawing tools and ink colors expose selection; toolbar-customization chips expose checkbox state and TalkBack toggle action; section controls have 48dp touch areas | Check TalkBack chip toggles, section drag/arrange, switch/accessibility state, focus, contrast and large text on devices | BR-5 |
| UI-33 | License/source notice | ui/AboutPane.kt, LICENSE, NOTICE | EXISTING | Do not remove original project attribution | ALL |

## 2. Design deliverables / review pack

- Three icon concept sheets; approval record, source master vector, foreground/background/monochrome, previews on light/dark/wallpaper and round masks.
- Theme spec screenshot pack: Home, file tile, note editor, PDF editor, eraser popup, color picker, confirm dialog, progress, onboarding/create note, preferences, split-screen.
- Component kit visual snapshots: Button, IconButton, Chip, Segmented, Slider, TextField, Dialog normal/destructive, Popup/Tooltip, Snackbar, BottomSheet, Progress, Empty, FileCard, Toolbar.
- Appearance matrix: system/light/dark/OLED/custom/dynamic; narrow phone + landscape tablet, 200% font scale and TalkBack.
- Evidence for no data loss, no blocking pen interactions, no text/ink recoloring and save-flow correctness.

## 3. Critical no-regression boundaries

1. Never change exported PDF page backgrounds or pen stroke colors from app theme switches.
2. Never conflate dangerous action warnings with merely selected red tint; action text must communicate effect.
3. Preserve existing stored user preferences on first migration; avoid overwriting dynamic/color preferences even if new default is branded.
4. Logo changes have to update launcher densities and notification/splash as appropriate, but should not rename package or break existing .xnote file associations.
5. Do not remove current XNotes MIT license/third-party code attributions or embedded licensed fonts.
6. Avoid importing a huge icon/theme library just for visual polish; reuse Material 3, existing icon vectors and resources.
7. No UI animations on drawing or other latency-critical input; recording, autosave and export are functional and must remain correct.

## 4. Approval gates

- BR-0: select icon concept + theme direction; verify app screenshots of old and new.
- BR-1: launcher icon clean at 48dp + all adaptive masks.
- BR-2: semantic colors and typography survive light/dark/OLED/custom.
- BR-3: destructive/non-destructive/progress/error dialogs perform correct callbacks and focus rules.
- BR-4: screens use shared components, stylus/scroll/nav unchanged.
- BR-5: Android CI + real-device side-by-side screenshot QA, accessibility and release icon resources.

Some first-pass brand assets, popup styles and canvas chrome are implemented in source. The remaining redesign and all final visual/device acceptance gates are pending; no UI item is considered fully verified until Android CI and hardware review pass.
