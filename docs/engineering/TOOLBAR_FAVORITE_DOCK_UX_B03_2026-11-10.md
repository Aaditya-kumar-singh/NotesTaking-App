# NotesTaking App — Favorite drawing tools dock, UI batch B03

Date: 2026-11-10
Branch: `feature/editor-favorite-pen-dock-b03`
Parent commit: B02 `1ba28f8` (itself depends on B01 `9811900`)
Status: Implemented and source-checked locally. **Not pushed, built, or tested on Android.**

## What users see

- With **Settings → Toolbar → Floating**, the existing draggable toolbar starts as a compact set of favorite drawing tools instead of forcing the long scrolling bar onto the writing stage.
- The default **phone dock** includes Pen, Highlighter, Eraser, Lasso. Wider tablet panes can display additional configured favorites, up to eight total.
- Phone/compact panes can show fewer than four tools when physically necessary to keep the 48dp drag grip and **Show all tools** control reachable.
- **Show all tools** expands the entire existing toolbar in place. **Show favorite tools** returns to the compact dock. Docked mode remains unchanged.
- The B02 drag grip is still independent of tool taps and still persists X/Y at gesture release.
- Tap a different favorite to select it. Tap an already selected configurable pen/eraser/select tool to open the **same** tool config popup used in the original toolbar, including pressure, width, color override, eraser mode, etc.
- Small colored stroke-width markers are previews of the current tool configuration; they are not new rendering or style controls.
- In **Settings → Toolbar**, users select up to eight favorites in selection order. Selected items can be removed; if eight are already selected, other choices are disabled until one is removed. At least one remains selected.
- Favorited tool IDs are saved in the existing global preferences, not in documents. Legacy settings load the default list. Duplicates, unsupported tool IDs and lists beyond eight are normalized.

## Sizing and responsiveness

- Compact and Regular: 48dp button hit regions, with icon glyphs 20dp / 24dp respectively.
- Comfortable: 56dp button hit regions, with 26dp glyphs.
- The toolbar's existing 4dp horizontal or vertical action padding supplies 8dp spacing between consecutive tool actions.
- Drag grip: 48dp. Expand and collapse actions: same accessible toolbar button sizing.
- The compact dock can occupy up to 600dp on a spacious tablet. Expanded full toolbar retains its previous 480dp horizontal cap and scrolling tool access. Comfortable 56dp buttons may limit a single row to seven favorites even on a large tablet; the rest remain available in the expanded toolbar.
- Short side of the actual pane determines max favorites: below 600dp up to four, at least 600dp up to eight. Available width/height can reduce the visible count, leaving room for drag grip and expand action.
- Left/Right toolbar positions use a vertical list of favorites; Top/Bottom use a horizontal row.
- Existing floating X/Y fractions and manual edge positioning are reused. The dock does not add a separate drag/save mechanism.

## Implementation, scope and safety

| File | Change |
|---|---|
| `settings/ToolbarLook.kt` | Defaults, supported favorite tool list and pure visible-count calculation |
| `settings/Preferences.kt` | Optional favorite ID array, sanitized on load |
| `ui/FavoriteToolDock.kt` | Shared paged-note/infinite-canvas favorites; existing tool selection and popup handlers |
| `ui/FloatingToolbar.kt` | Compact/expanded state, sizing, original drag grip |
| `ui/ToolbarFrame.kt`, `MainActivity.kt` | Pass one dock for each editor to the shared floating host |
| `ui/PreferencesPane.kt` | Configurable favorites chips with disabled state at eight |
| `res/values*/strings.xml` | English and Simplified Chinese text for favorites and expand/collapse |
| `SettingsTest.kt` | JSON compatibility, invalid/duplicate IDs, default order and phone/tablet sizing |

No changes were made to `.xnote`, `.xcanvas`, `.xtemplate`, PDF export, note saving, stroke geometry or input routing. No external dependencies or new icons were introduced. Existing untracked `.kotlin/` remains untouched.

## Acceptance criteria — pending GitHub CI and device QA

- [ ] Compile Kotlin/Compose and run the test suite and lint in GitHub Actions on the exact pushed SHA.
- [ ] On a 360dp phone, the default four buttons, 48dp drag grip and expand action are visible; on narrower screens the count reduces without clipping.
- [ ] On a 600dp+ tablet pane, up to eight configured tools fit, and in a narrow split pane the count reduces.
- [ ] Pen, Highlighter, Eraser and Lasso actually select the expected tool on a real device, without interfering with drawing gestures.
- [ ] Re-tapping an active pen or eraser opens the same configurable popup as the main toolbar, with live colors and widths.
- [ ] The small color/width preview reflects tool configuration, including overrides and current swatch.
- [ ] The full toolbar expands and collapses without losing focus, clearing a note, reinitializing the canvas or hiding essential actions.
- [ ] The dock can be dragged while collapsed/expanded and survives orientation change, split-screen, restart and theme changes.
- [ ] Favorites persist across app restarts; malformed JSON falls back without data loss.
- [ ] TalkBack, larger font, mouse and real stylus input work; contrast and hit targets pass on phone and tablet.

## Intentionally not included

A separate pen-preset engine, pressure-simulation graphics, per-document toolbar preference, full Home/Explorer redesign and New Note creation redesign. Those are independent feature/visual batches. No Android build was run on this Windows machine, per AGENTS.md and the repository's batched CI policy.
