# NotesTaking App — PDF workspace / page manager UX, batch B06

Date: 2026-11-10
Branch: `feature/pdf-page-workspace-ux-b06`
Parent: B05 `fcb4c2e` (depends on local UI batches B01–B04)
Status: Implemented and source checked locally. **Not pushed, Kotlin compiled, or visually tested on Android.**

## Focus

Make PDF and note page management easier to find and operate from the existing Pages sidebar without introducing another page manager or changing the underlying note/PDF data.

## User-visible changes

- **Page context:** The Pages tab now displays a header showing the current page and total pages. Direct 48dp **Add page** and **Insert PDF** buttons reuse the existing editor/SAF callbacks. Both are hidden during multi-selection to keep selection mode focused.
- **Reordering help:** A short instruction explains the existing two-stage long-press gesture: hold to select, then hold again to drag. It is shown when more than one page exists. The drag-to-reorder gesture, scroll debounce and thumbnail caching logic are unchanged.
- **Accessible alternative to dragging:** Each regular page's context menu offers **Move page up/down** where that move is valid. Both invoke the existing undoable `Editor.movePages` command using the original-list gap conventions (`index - 1`, `index + 2`). The cover cannot be moved, and the first regular page cannot be moved ahead of the cover. The page snapshot for the menu is captured only when it is open, avoiding repeated full-document copies during long-PDF thumbnail scrolling.
- **Thumbnail readability:** Thumbnail captions say **Page N** at 12sp instead of bare padded numbers at 10sp. The current page is emphasized and accessibility services receive page labels and current/selected state. The per-page options target grows from 26dp to 48dp, with a 20dp icon.
- **Pages/Contents/Bookmarks/Search tabs:** Selected-tab background and accessibility state are explicit. Each tab button uses a 48dp target while preserving the existing sidebar width.
- **Multi-page selection:** The old compressed 52dp, six-control bar becomes two rows. The first row shows **N selected**, Clear and Select all. The second has **Save selected pages as PDF**, Delete and More, each with a 48dp target. Copy remains available in the More menu, alongside Cut, Duplicate, Rotate, Page Size, Erase, Extract, Move, Share, Save As.
- **Localization:** New copy uses English and Simplified Chinese resources; theme roles, system gestures and editor navigation are unchanged.

## Files

- `app/src/main/java/com/notestaking/app/ui/SidePanel.kt`: Header, thumbnail semantics/hit areas, move actions, selection toolbar hierarchy.
- `app/src/main/res/values/strings.xml` and `values-b+zh+Hans/strings.xml`: Six localized strings.
- `app/src/test/java/com/notestaking/app/core/history/HistoryTest.kt`: Unit coverage of menu reorder gap arithmetic (pending cloud execution).

No changes to `.xnote`, `.xcanvas`, `.xtemplate`, PDF decoding, thumbnail renderer, stylus input, page serialization, `Editor.movePages`, file picker callbacks, backup rules or application identifiers.

## Verification

- [x] Python project source check.
- [x] Python backup/privacy source check.
- [x] XML resources parsed in both languages; required keys unique.
- [x] `git diff --check` on the staged files.
- [x] Source wiring verified for page insertion, PDF import, per-page menu, selection export and reorder.
- [ ] **GitHub CI:** Kotlin compilation, full unit tests including new gap cases, lint and APK validation on the exact published SHA.
- [ ] **Real phone:** 360dp narrow screen, thumbnail menu/48dp targets, selection bar, clear/select all, file picker, PDF save/restore, Android Back.
- [ ] **Tablet:** portrait/landscape, split-pane and multi-window controls, drag reordering, keyboard/mouse, TalkBack, large text and accessible menu reorder.
- [ ] Verify page insertion position, imported PDF page order, cover boundary, undo/redo of moves, bookmarks after reordering, duplicate and multi-page save to PDF.
- [ ] Compare screenshots in Light, Dark/OLED and Chinese system language.

## Deferred

A separate full-screen page-manager route, a configurable PDF export format dialog, per-document page overview layout, and simultaneous multi-page move-up/down controls remain separate UX tasks. This batch extends the existing page panel rather than creating an unverified parallel workflow.

The unrelated untracked `.kotlin/` directory is left untouched.
