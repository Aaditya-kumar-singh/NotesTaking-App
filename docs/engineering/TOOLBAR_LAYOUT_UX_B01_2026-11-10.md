# NotesTaking App — Toolbar placement and sizing, Batch B01

Date: 2026-11-10
Branch: `feature/editor-toolbar-placement-size-b01`
Base: `1ebf442` (`release/universal-test-beta2`)
Status: Implemented locally. Android compilation, CI and real-device visual QA remain unverified.

## What changed

- New installations choose **Auto** toolbar placement: bottom for an active window whose shorter side is below 600dp, top at 600dp and above. Using the current window's dimensions also handles rotation and tablet multi-window.
- Existing installations with no saved toolbar position remain **Top**. An explicit choice of Top/Bottom/Left/Right is persisted even when it matches an old default, so those preferences are not silently replaced by Auto.
- Preferences provide Auto, Top, Bottom, Left and Right chips, wrapping on narrow phones. Docked/Floating mode and Compact/Regular/Comfortable sizing remain available.
- Both paged note and infinite canvas share one toolbar metrics implementation.
- Compact: 20dp icon, 48dp action, 56dp bar; Regular: 24dp icon, 48dp action, 56dp bar; Comfortable: 26dp icon, 56dp action, 64dp bar.
- Buttons and color swatches use a touch surface of at least 48dp, with 4dp side padding on each action (8dp separation between adjacent actions). Color swatch circles stay visually smaller.
- All existing tools, drag-to-fill color, user-customized toolbar sections, edit actions and data formats are retained.

## Migration and implementation notes

Existing JSON without `toolbar_position` means the legacy Top choice; new defaults explicitly serialize `toolbar_position: "auto"`. Saved explicit edge positions round-trip unchanged. `ToolbarPosition.resolve(shortSideDp)` is pure and covered by a unit test.

The implementation reuses `ToolbarFrame`, `ToolbarIcon`, `Swatch`, `ToolbarAround`, `Preferences` and `PreferencesPane`. No parallel toolbar renderer or new dependencies were added.

## Acceptance criteria — pending GitHub CI and device QA

- [ ] Android Kotlin compilation, unit tests and lint pass on the exact pushed commit.
- [ ] On a 360dp phone window, Auto docks the toolbar at the bottom above system/navigation areas; no writing canvas clipping.
- [ ] On a tablet 600dp+ shorter-side window, Auto docks below the top editor chrome.
- [ ] Tablet multi-window narrow pane selects bottom; returning to full screen selects top without re-creating the document.
- [ ] TOP, BOTTOM, LEFT, RIGHT persist after restart and override Auto.
- [ ] Every primary icon and swatch presents a >=48dp actionable area; TalkBack announces its action and state.
- [ ] Colors can still be dragged from a swatch to a page to fill.
- [ ] No toolbar, floating popup, keyboard or system navigation overlaps a tappable tool.
- [ ] Test paged PDF, new blank note, infinite canvas, dark/OLED theme, landscape and split-editor modes.
- [ ] Ink/eraser, Undo/Redo, PDF positioning and document save/reopen remain unchanged.

## Not in this batch

- A user-draggable floating dock with saved X/Y coordinates, favorites presets and collapse animation.
- The broader Library/New Note visual redesign, PDF page manager and export-dialog redesign.
- Any changes to PDF rendering, drawing, note storage, document serialization or backup behavior.

These are separate follow-up design batches to avoid mixing UI geometry and data correctness.
