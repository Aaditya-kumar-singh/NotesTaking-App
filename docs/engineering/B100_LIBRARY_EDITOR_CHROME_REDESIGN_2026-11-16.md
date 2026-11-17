# B100 - Library and editor chrome redesign

**Date:** 2026-11-16

**Status:** B100 FAB, per-pane header, expandable tools and duplicate-control suppression integrated on a local coordinator branch; source-only keyboard routing added. Not cloud-built, device-tested, published or merged.

**User screenshots:** `image(20261116-130027).png` (library), `image(20261116-130147).png` (PDF editor)

**Tracking:** [GitHub issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28)

**Baseline:** B99 universal Android debug APK, `b299cbb8972267eebe103493fa093f7e5cfa6f70`, [CI #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156). Preserve the B99 device acceptance record separately.

## 1. Problem and intended result

The screenshots show the purple **New Note** pill overlapping the top-left library navigation/title area and, in the PDF editor, a truncated document filename occupying the bottom control strip beside split-pane and drawing actions. The user wants the filename **at the top**, New Note somewhere that does not crowd the library header, and a single **arrow button** to show/hide Pen, Pencil and all existing drawing/editing tools.

The layout should stay familiar to Android users: a clear library top app bar + one primary FAB; a persistent editor title bar; a compact tool rail when drawing tools are hidden; and a page-/pane-aware expandable tool drawer. Avoid re-theming the app or changing how notes are saved.

**Source-backed finding (2026-11-17):** `Backstage.kt` passes `Modifier.align(Alignment.BottomEnd).padding(20.dp)` to `CreateButton`, but the implementation originally attached that parent-alignment modifier to its inner `ExtendedFloatingActionButton`, not the outer `Tooltip` box. The parent explorer `Box` therefore could position the outer wrapper at its default top-start, explaining the misplaced purple pill without requiring a second FAB. On coordinator branch `integrate/b100-library-fab-anchor-20261117`, the smallest local source fix moves the existing modifier to `Tooltip` itself while preserving the same one-tap New Note / selection Export PDF action. **Source checks passed; Android build, actual FAB position, selection-bar hit testing and device QA remain unverified.** Do **not** add a duplicate button.

**Coordinator header source checkpoint (2026-11-17):** On a separate `integrate/b100-editor-header-20261117` branch, `MainActivity.kt` now renders a 56dp per-pane top row with Back to library, the live document title (one-line ellipsis/full name on tap), type and page count on wide panes, and a separate Split action. Split opens a system document picker for a distinct `.xnote` or `.xcanvas`, keeps the original editor instance in place, validates the returned note and requires persistable read/write permission; a split pane can be closed through its separate button. Paged-note title tap reuses `Editor.renameCurrentDocument` and its existing error reporting. Infinite-canvas title tap is **read-only**, because the current paged rename API does not rename the canvas model and no A-owned editor source may be altered here. The existing B-owned bottom toolbar TITLE/HOME/Close Pane controls remain in place until Chat B/coordinator integration removes duplicates. The header, picker and tests are **source-only; Kotlin/Compose compilation, CI and phone/split-pane acceptance have not run.**

### B100 integrated source checkpoint (2026-11-18)

Local coordinator branch `integrate/b100-toolbar-pdfcache-20261118` incorporates the reviewed Chat B B100 drawing-tools, header-deduplication and keyboard-contract commits, and the approved Chat C handwriting-input planner, rotation, text-flow and 241-page PDF-cache test sources. The existing 56dp `EditorPaneHeader` is always present, so the paged and infinite toolbar invocations pass `headerChromeVisible = true`: HOME, TITLE and duplicate Close Pane buttons are removed only from their displayed derived sections; customized persisted layouts remain unchanged. Toolbar labels now announce **Expand drawing tools** and **Collapse drawing tools** in the existing English and Simplified Chinese resources.

The editor-pane keyboard preview now checks whether the pane's own focus target is directly focused before handling unmodified editor shortcuts. A focused child toolbar, header or text control receives its own arrows and Space; Ctrl document shortcuts still route through the existing handler, and the focused floating grip retains its arrow-key exemption. A coordinator-owned pure routing test covers these cases. This is **not evidence of actual Compose focus propagation or Back precedence**: exact integrated-SHA GitHub Kotlin/JUnit/Compose CI and hardware keyboard, TalkBack, split-pane, IME, PDF and ink tests remain mandatory. The distinct Pencil brush, working handwriting-to-text model, canvas renaming and P0 persisted-but-invisible text repair are **not implemented or verified** by this integration.

## 2. Proposed screens

### A. Library - move New Note out of the header

```text
+------------------------------------------------+
| [Menu] My library                 [Search] [More]|
| Recent    Modified     All kinds       3 files |
|                                                |
|  [Note / PDF / Canvas cards, scrollable]        |
|                                                |
|                            [ + New Note ]      |
+------------------------------------------------+
```

- Keep the **My library** title and clickable storage/folder path visible; don't overlay the navigation icon, search, filters or cards.
- Place the single primary **New Note** FAB at bottom-right of the library content, above the OS navigation bar and keyboard inset. It should scroll with neither the list nor the header.
- On a very narrow screen, shrink extended label to an icon FAB if necessary, but maintain an accessible `New Note` name and one-tap creation. Display a tooltip **next to its real anchor**, not in the top-left.
- Preserve the existing **Export PDF** action when file-selection mode is active; retain New Canvas, Import PDF and New Folder in the overflow; do not introduce a second primary FAB.
- Check partial scrolling, recent/filter/search results, 320/360dp portrait, landscape and tablet split windows.

### B. Editor - document name at top

```text
+------------------------------------------------+
| [Back] QA_Imported_B99_20261116  [Split] [More] |
|        PDF - 2 pages                           |
+------------------------------------------------+
| [Pages sidebar] |                              |
| Page 1          |    PDF / note writing area   |
| Page 2          |                              |
+------------------------------------------------+
| [Chevron UP: Tools]   [Selected: Pen]          |
+------------------------------------------------+
```

- Move `visibleDocumentTitle` out of the cramped bottom strip and into a top app bar. **Back** remains first; title uses one line with ellipsis; tapping the title or its info action reveals the complete filename and Rename (with confirmation when needed).
- Show page count, type (PDF, note, infinite canvas) and unsaved/loading status as compact secondary text *when space allows*. Avoid taking two lines from narrow split panes.
- Move split-pane toggle to the right side of the header, separate from pens. It must remain available when the tool drawer is collapsed, and show the active pane's title.
- Bottom strip becomes contextual to drawing, not file identification. Preserve Android/system Back, the existing unsaved-changes prompt, and tool settings/focus.
- Keep page manager and PDF Read/Annotate mode accessible; no change to note rendering, page geometry or stored bytes.

### C. Expand/collapse drawing tools

**Collapsed (default on compact phones):**

```text
+-----------------------------------------------+
| [Chevron UP] Drawing tools       Pen [color]  |
+-----------------------------------------------+
```

**Expanded (tap the arrow, close by tapping Chevron DOWN):**

```text
+-----------------------------------------------+
| [Chevron DOWN] Hide tools                     |
| [Pen] [Highlighter] [Eraser] [Lasso] [Select] |
| [Pan] [Shapes] [Text] [Text box] [More...]     |
| Brush variants: Dashed / Calligraphy / Speed  |
|                 Taper / other supported tools |
+-----------------------------------------------+
```

- Reuse the existing tool registry and actions, rather than add a parallel drawing engine. Current `All tools` contains Write a note, Pen, Dashed, Calligraphy, Speed, Taper, Highlighter, Eraser, Pan, Select, Lasso, Screenshot, Shape, Text and Text box. Include image/audio via existing entry points where supported.
- The requested **Pencil** is a new *label/preset* only if a distinct pencil brush currently exists. Otherwise display Pen, track a separate Pencil implementation, and do not silently relabel tools.
- Arrow icon changes with state; announce **Expand drawing tools** / **Collapse drawing tools** to TalkBack; show the active tool clearly. Panel scrolls horizontally or wraps within the pane bounds; no clipping or accidental page interaction.
- Phone: close the expanded drawer after selecting a tool (unless the chosen tool has an essential settings subpanel). Tablet: an expanded dock may remain visible. Store expansion per editor pane while split mode is open, retaining drawing selection.
- Pressing Android Back while the palette is open collapses the palette first; the next Back follows established editor navigation. Do not intercept system Back when another modal/IME has priority.
- Keep current toolbar placement preferences (Auto/Top/Bottom/Left/Right, floating/docked, Compact/Regular/Comfortable) working. Integrate with existing `ToolbarFrame`, `ToolbarAround` and `FloatingToolbar` state; avoid a wholly separate toolbar renderer.

## 3. Interaction / accessibility acceptance

| ID | Scenario | Expected result |
|---|---|---|
| B100-01 | Open My library | Title, menu, search and filters unobstructed; one New Note action; no top-left stray tooltip |
| B100-02 | Tap New Note | Existing creation flow opens in one tap; the new note is writable and survives save/reopen |
| B100-03 | Select multiple library files | Primary action changes to Export PDF, still one visible FAB |
| B100-04 | Open long-name PDF | Filename at top; full name accessible; no truncated duplicate bottom label |
| B100-05 | Tap arrow in collapsed bar | Tool drawer reveals existing, labeled tools without covering important content |
| B100-06 | Tap Pen, Eraser, Lasso / undo | Existing tool commands work; active state and saved ink are unchanged |
| B100-07 | Collapse tools | A compact selected-tool indicator remains; split toggle stays visible |
| B100-08 | Open two documents in split view | Each pane shows correct filename, active tool and independent drawer state |
| B100-09 | Press Back / tap outside / open IME | Dismissal order follows Android behavior; no loss of unsaved note state |
| B100-10 | Font scale 1.3x and 2x / TalkBack | Controls at least 48x48dp; names, expand state and focus read correctly |
| B100-11 | 180/240/320dp panes and landscape | No overlapping title, palette, floating toolbar or system navigation |
| B100-12 | Switch dark/OLED, scroll library, rotate PDF | No clipped popup; no unwanted page shifts; no change to ink data |
| B100-13 | New universal APK after source changes | Exact-SHA GitHub JUnit/lint/all-ABI build and APK integrity pass |
| B100-14 | Real phone verification | New screenshot comparison and use of disposable notes on Galaxy M13; sign off defects explicitly |

## 4. Implementation ownership and sequence

| Owner | Allowed work | Dependency |
|---|---|---|
| Coordinator | Design approval; library FAB/tooltip in `ui/Backstage.kt`; shared editor header in `MainActivity.kt`; shared strings/resources; QA integration and GitHub publication | Sole shared writer/publisher |
| Chat B | Existing dock/floating toolbar components, expand/collapse UI state and accessibility tests in B-owned `ui/Toolbar*.kt`, `InfiniteToolbar.kt`, `FloatingToolbar.kt`, `Tooltip.kt` and `test/ui/**` | Exclusive issue scope before implementation |
| Chat A | Read-only document-title review, or minimal A-owned `ui/Editor.kt` display-state helper and tests if coordinator explicitly authorizes | Avoid overlapping NP-004 source work |
| Chat C | Read-only geometry/selection regression review unless independently authorized | NP-102 remains C's primary claim |
| QA operator / Coordinator | GitHub exact-SHA universal APK, 320/360dp phone and split tests, 2x font, accessibility and PDF, ink-save verification | Only after code and CI are ready |

**Do not start concurrent source edits based solely on this document.** GitHub issue #28 is a supplementary UI issue outside the fixed 130-card A-Z catalog; grant exclusive file scopes first. Preserve reviewed commits and the current successful B99 universal debug APK for comparison. No production release, merge to `developer` or `main` without normal gates.

## 5. Development checklist

- [ ] Confirm the top-left `New Note` pill's runtime cause (FAB position vs tooltip anchoring) on the B99 APK.
- [ ] Approve revised library and editor hierarchy.
- [ ] Assign and register coordinator / A / B exclusive paths; preserve Chat C's NP-102.
- [ ] Fix one-tap New Note FAB/tooltip placement and keep Export PDF selection behavior.
- [ ] Implement persistent top filename and separate split-pane action.
- [ ] Implement arrow-driven, labeled expandable tool palette using existing tools.
- [ ] Add pure state/layout/regression tests: narrow panes, long filenames, Back/focus, drawer selection and note-state preservation.
- [ ] Run source/privacy/catalog/ownership checks and review exact worker SHAs locally.
- [ ] Publish a new QA-only B100 branch with locked coordinator, trigger GitHub JDK/Android CI and universal APK; do not update protected branches yet.
- [ ] Test on a real phone/tablet with before/after screenshots and log issues for any failures.
- [ ] Reconfirm note persistence, eraser undo/redo, PDF annotations, library search and file import/export with the redesigned UI.

## 6. Out of scope

No new note/canvas file format, sync/cloud backend, new pencil brush algorithm, permanent tool preference migration, unapproved visual rebrand, bulk data modification, keyboard shortcut redesign outside UI focus, or changes to existing signed/released APKs. Keep native format and current keyboard/SAF safety work intact.
