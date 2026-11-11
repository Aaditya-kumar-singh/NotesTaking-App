# NotesTaking App — PDF reading and annotation controls, B08

Date: 2026-11-10
Local branch: `feature/pdf-reading-annotation-ux-b08`
Base: `developer` at `ac0508d` — Android CI #42820416013 succeeded for this exact base.
State: **Implemented and source-checked locally. Not pushed; Kotlin tests/build, visual and stylus QA not yet run for B08.**

## User-visible changes

- When the current note has an embedded PDF, a **Read / Annotate** action appears among the main editor's pinned toolbar actions. The button uses the existing 48dp/56dp toolbar sizing and indicates whether the current tool is **Pan (Read)** or an annotation tool; tapping switches between them.
- **Read** selects the existing `Tool.PAN` navigation input mode. **Annotate** restores the most recent PDF editing tool selected, such as Pen, Highlighter, Eraser, Lasso or PDF Text Markup. Pan, Screenshot and Image do not replace the remembered editing tool. This is *not* a permissions-based read-only lock: other UI controls, configured stylus side-button actions and editing commands remain available.
- The compact floating favorites dock also shows this switch for PDFs. It reserves one of its existing action slots for it, keeping the grip and expand control within bounds on narrow split panes.
- The existing **All Tools** overflow exposes a PDF-specific **Reading and annotation** section: Read (Pan), Annotate, PDF text markup, Search PDF text, Go to page, Fit page width and Fit whole page.
- Search uses the pre-existing PDF/document search sidebar and focuses the search field; page jump uses the existing validated number field. Fit-width and fit-page call the existing viewport methods. Existing page thumbnails, bookmarks and contents remain under the normal sidebar.
- Non-PDF notes do not display PDF-specific actions; infinite canvas retains its original toolbar and favorites.
- English and Simplified Chinese text and accessibility labels reflect the selected mode and the next tap action. No raw colors, new vector icon families, or external libraries.

## Implementation mapping

| File | What it does |
|---|---|
| `core/tools/PdfReadingToolChoice.kt` | Small non-serialized selection memory; Pan does not overwrite the last annotation tool |
| `core/tools/PdfReadingToolChoiceTest.kt` | Unit regression for restoring Pen, Highlighter and PDF text markup after navigating |
| `ui/Editor.kt` | Single `setPdfReadingMode(reading)` entrypoint, using the existing `selectTool` and Ink/Pan controller |
| `ui/CanvasQuickAccess.kt` | Contextual Read/Annotate button and shortcuts for PDF viewing |
| `ui/FavoriteToolDock.kt` | Reserve a compact dock slot for the same mode switch on PDF notes |
| `res/values/strings.xml`, `res/values-b+zh+Hans/strings.xml` | Localized mode, status and search descriptions |

No changes to PDF rendering, PDF link detection, document data, annotations, `.xnote`/`.xcanvas`/`.xtemplate` formats, undo history, save/recovery, fonts or storage permissions.

## Checks

### Source-only checks (local, allowed by AGENTS.md)
- [x] Project source/resource validator.
- [x] Backup/privacy source check.
- [x] Diff whitespace check.
- [x] Source references show PDF controls are guarded by `hasPdf` and delegate to existing editor functions.
- [x] Six localized strings unique and valid in both locale XML files.
- [x] Kotlin unit test written (not yet executed).

### Acceptance still required
- [ ] GitHub Android CI compiles exact B08 SHA, runs all Kotlin unit tests including `PdfReadingToolChoiceTest`, lint and APK verification.
- [ ] Phone in portrait and landscape: one-tap mode switching, existing pen and eraser gestures, scroll/zoom in Read, no toolbar clipping.
- [ ] Tablet and stylus: palm rejection and configured stylus button behavior in both modes; verify that Read is a Pan tool choice, not a security lock.
- [ ] Current tool changes via keyboard, main toolbar, compact favorites or PDF text markup correctly update the button and restored tool.
- [ ] PDF search, Contents, Bookmarks, Go to page, Fit width and Fit page work without resetting document or losing position.
- [ ] Switching to a plain note or infinite canvas hides PDF actions; returns cleanly when reopening PDF.
- [ ] Light, dark, OLED, large font, TalkBack descriptions, split-pane, 320dp phone width and 600dp+ tablet width reviewed with real screenshots.
- [ ] Saving/reopening PDF-backed notes, undo/redo and page export remain unchanged.

**Deferred:** Full distraction-free reading layout, configurable dedicated PDF reading preferences, richer PDF text selection/OCR or flattening switches. Those require separate behavior and device review; B08 reuses existing functionality.

No local Android build or device install was run. Keep the pre-existing untracked `.kotlin/` directory untouched.
