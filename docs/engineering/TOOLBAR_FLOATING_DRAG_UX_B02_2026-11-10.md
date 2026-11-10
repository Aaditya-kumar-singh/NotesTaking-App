# NotesTaking App — Movable floating toolbar, UI batch B02

Date: 2026-11-10
Branch: `feature/editor-floating-toolbar-drag-b02`
Base: B01 commit `9811900`
Status: Locally implemented. Kotlin build, unit test execution, Android screenshots, and real stylus/phone/tablet acceptance are still pending.

## User experience

- In **Settings → Toolbar → Floating**, an isolated six-dot drag grip appears alongside the existing toolbar.
- Drag **only the grip** to reposition the bar. Pen strokes, eraser controls, color swatches and the canvas retain their separate handlers.
- The bar's location follows the active pane. Movement is clamped inside the pane with an 8dp edge margin; a wide floating horizontal bar scrolls its existing tools and is capped at 480dp, while a vertical bar is capped at 600dp.
- On very narrow phones, the existing full toolbar can occupy nearly all horizontal space, limiting sideways movement; vertical movement remains available. A smaller independent pen-favorites dock is a separate follow-up rather than hiding important existing actions.
- Its X/Y values are fractions of the **available free travel** after subtracting the toolbar's measured size. Reopening, window resizing and orientation changes therefore keep the toolbar within bounds instead of reusing absolute pixel positions.
- The drag persists once **after release**; dragging does not write settings or invalidate the ink engine on every movement. Canceling the gesture restores its starting position.
- The grip is at least 48dp. TalkBack actions provide Move left/right/up/down and Reset position. Unavailable moves return false.
- Changing the edge choice in Settings resets the saved floating offset, while switching Docked/Floating preserves it.
- Existing placements without saved coordinates still anchor to Top/Bottom/Left/Right as before. Existing notes and settings JSON remain compatible.

## Source mapping

| File | Change |
|---|---|
| `ui/FloatingToolbar.kt` | Measured, clamped, touch-draggable overlay with accessible grip |
| `ui/ToolbarFrame.kt` | Reuses original toolbar within the movable overlay |
| `MainActivity.kt` | Wires both paged and infinite editors to the same save handler |
| `ui/Editor.kt` | Immediately persists a completed move without repainting the document |
| `settings/ToolbarLook.kt` | Optional normalized X/Y with fallbacks for each edge |
| `settings/Preferences.kt` | Round-trip X/Y; reject missing, invalid and out-of-range coordinates |
| `ui/PreferencesPane.kt` | Manual edge choice resets free-placement coordinates |
| `res/values*/strings.xml` | English and Simplified Chinese movement descriptions |
| `SettingsTest.kt` | Position/legacy/malformed JSON regression cases |

## Verification / acceptance

- [ ] GitHub Actions Kotlin compilation, tests and lint on the exact pushed SHA.
- [ ] Android phone: Floating bar moves vertically; grips do not turn into ink strokes; no clipping near system bars.
- [ ] Android tablet: Floating bar moves horizontally and vertically and remains accessible in either orientation.
- [ ] Resizing split panes and switching to multi-window clamp the bar safely.
- [ ] All original tool actions (Pen, Highlighter, Eraser, Lasso, Undo/Redo, Add Page, More) remain reachable and clickable when floating.
- [ ] Page and infinite-canvas color swatch drag/drop still works when the toolbar is moved.
- [ ] Saved X/Y and manual toolbar edge selections restore after app restart; corrupted settings fall back.
- [ ] TalkBack custom movement actions work and communicate unavailable directions.
- [ ] Light/dark/OLED themes, large system font, keyboard and stylus are checked on device.
- [ ] No changes to note serialization, drawing, PDF, or backups.

## Deferred deliberately

- A separate small pen-only favorites palette with four/eight configurable presets and brush live previews.
- A collapse/hide animation and per-document independent palette positions.
- Home/Library and New Note full visual redesign.

The earlier B01 build has not been verified by GitHub CI yet; this is a dependent local branch and should be published as a reviewed batch only after the preceding checkpoint is reconciled.
