# NotesTaking App — B10 Saved pen styles

Date: 2026-11-11
Branch: `feature/saved-pen-styles-b10`
Parent: locally committed B09 `5072725`; inherits published B08 `5633d01`.
State: **Implemented and source-checked locally. Not pushed, Kotlin compiled, or device verified.**

## User-facing change

The existing stroke-tool settings popup now offers **Saved pen styles**. Each stroke tool (Pen, Dashed, Calligraphy, Speed, Taper, Highlighter) can save up to three independent configurations. The user can:

- Adjust color, width, pressure and tool-specific effects as before.
- Tap **Save current style** to store the *live* configuration for that tool. If it follows the toolbar's active swatch, save the current ink color as a fixed override so switching swatches later cannot silently change the preset.
- Tap any saved style to apply its complete configuration to that tool and close the popup. Both the paged notebook and infinite canvas reuse their existing `updateToolConfig`/stroke renderer paths.
- Delete any saved style using its own 48dp, accessibility-labeled delete button. When three styles are saved, explain the limit instead of presenting a nonworking save button.

Saved styles are separate from the selected/current pen settings. Saving a preset **does not** replace the active tool, change existing stroke objects, or change the note itself. Applying one updates the live tool for *future* strokes only.

## Persistence and compatibility

- New optional device-level settings field `pen_presets`, indexed by stroke-tool ID. Up to three `ToolConfig` entries are stored per stroke tool, reusing the existing JSON serializer/deserializer for the full tool properties.
- Missing field means no presets. Invalid element types are skipped and oversized arrays are capped on load; other tool IDs (e.g. Eraser) are not accepted as saved pen styles.
- The two editors use the existing process-shared `LiveSettings` and atomic `SettingsRepository`, so saving from either surface updates the same collection. The existing note and infinite-canvas serialization, settings defaults, current `tools` map, undo, eraser data, PDF input and backup exclusions are unchanged.
- English and Simplified Chinese UI copy. Existing popup theme roles, 48dp touch targets, and Material3 menu controls are preserved.

## Changed files

- `settings/Settings.kt` — optional preset field, bounded add/delete, JSON serialization.
- `ui/ToolPopupHost.kt` — three explicit methods for the shared pen popup.
- `ui/Editor.kt`, `ui/InfiniteEditor.kt` — host implementations using existing settings persistence.
- `ui/Popups.kt` — save/apply/delete controls and live color preview in the pen popup.
- `res/values/strings.xml`, `values-b+zh+Hans/strings.xml` — localized labels.
- `test/settings/SettingsTest.kt` — legacy load, save/load roundtrip, independent pen/highlighter presets, current config isolation, max-three policy, deletion and malformed oversized input.

## Verification

- [x] Project source check.
- [x] Backup/privacy source check.
- [x] English/Chinese XML parsed with exactly one copy of all five keys.
- [x] Source assertions for both tool hosts, existing current-config update paths, typed preset serialization and capacity.
- [x] Staged Git whitespace/diff check.
- [x] Regression tests written; not executed locally by repository policy.

### Still required before integration

- [ ] GitHub Android CI compiles **exact B10 SHA** and runs unit tests, lint and APK verification. Its B08/B09 ancestors must be reconciled with their respective CI checkpoints.
- [ ] On a phone and tablet, save three styles of Pen, delete the middle entry and save a replacement; verify order, color, pressure, width, and stroke rendering.
- [ ] Apply a saved style on a PDF-backed note, infinite canvas, and split view; close/reopen the app and verify persistence and no lost pen settings.
- [ ] Toggle active toolbar color after saving a style; confirm saved color remains as originally captured.
- [ ] Check the menu at 320dp, large system fonts, keyboard/Back, light/dark/OLED, and Simplified Chinese; ensure the menu scrolls and accessible targets can be reached.
- [ ] Save/reopen notes containing existing annotations, test eraser and undo/redo, and ensure no change to strokes already on the page.

No local Gradle/Android build/device install; the pre-existing untracked `.kotlin/` folder is untouched.
