# NotesTaking App — Page export workflow, UI batch B07

Date: 2026-11-10
Branch: `feature/pdf-export-dialog-ux-b07`
Parent: B06 `0d7f5a7` (depends on locally committed B01–B05)
Status: Implemented locally; **not published, Kotlin-compiled, device-verified or merged**.

## What changed

- The **Pages** sidebar has a visible **Export pages** action. The existing per-page **Save as** and **Share** menu entries, and the multi-page selection export action, open one consistent Material 3 dialog instead of separate nested PDF/PNG menus.
- **Format:** PDF document (one combined file; vector/text preserved when supported) or PNG images (one file per page).
- **Scope:** This page (the tapped thumbnail), Selected pages (when a selection exists), All pages, or Custom range. Range input accepts e.g. `1,3-5`, is one-based, sorts pages in document order, removes duplicates and rejects out-of-bounds/reversed/malformed values.
- **Destination:** Save locally through the existing Android Storage Access Framework pickers, or Share via Android's existing share intents. The dialog explains the distinction and shows the number of pages to be exported.
- **Validation:** The action is disabled for blank, invalid, zero-page, or out-of-range selections; incorrect custom ranges display a localized inline error, and no export callback runs until the selection is valid.
- **Entry defaults:** Page sidebar opens with All pages / Save / PDF. A page context menu opens with that page / chosen Save or Share / PDF. Multi-selection opens with Selected pages / chosen destination / PDF. Users can change any option before confirming.
- All chips have a minimum 48dp touch height, the dialog scrolls on small phones, and Android Back/cancel dismisses without exporting. English and Simplified Chinese strings are provided.
- The dialog captures selected page indices on opening so changes behind it cannot silently change what gets exported.

## Existing behavior preserved

`MainActivity` still owns PDF rendering, progress cancellation, temporary file cleanup, SAF destination selection, PNG output, and sharing:

| Choice | Existing callback |
|---|---|
| Save as PDF | `onSavePagesAsPdf(indices)` |
| Save as PNG | `onSavePagesAsImages(indices)` |
| Share PDF | `onSharePages(indices, true)` |
| Share PNG | `onSharePages(indices, false)` |

No PDF flattening/quality/watermark/background toggles are presented because the current exporter does not expose them as selectable settings. No changes to `.xnote`, `.xcanvas`, `.xtemplate`, note save, PDF/PNG writer, strokes, annotation positioning, backups, or file permissions.

## Changed files

- `ui/PageExportDialog.kt` — reusable dialog and choices (new).
- `ui/SidePanel.kt` — route sidebar, page menus and selection export through one dialog; preserve original callbacks.
- `core/util/ExportPageRange.kt` — defensive page range parser (new).
- `app/src/test/java/com/notestaking/app/core/util/ExportPageRangeTest.kt` — parsing/invalid-range unit tests (pending CI).
- `app/src/main/res/values/strings.xml` and `values-b+zh+Hans/strings.xml` — localized UI text.

## Local source verification

- [x] Project source/resource inventory check.
- [x] Backup/privacy check.
- [x] Git whitespace check.
- [x] Resource XML parsing and unique new keys in English and Simplified Chinese.
- [x] Source-only assertions that all four format/destination callback paths remain connected and no legacy nested menus remain.
- [x] Page-range unit tests authored (not executed locally).

## Acceptance pending GitHub Actions + Android hardware

- [ ] Exact pushed SHA Kotlin compilation, tests, lint and APK checks pass in GitHub CI.
- [ ] 320dp and 360dp phones: dialog scrolls without clipped buttons when the keyboard is up; system Back cancels.
- [ ] Tablet portrait, landscape and multi-window: dialog fits; page thumbs, selection and existing editor are undisturbed.
- [ ] Export This, Selected, All, and `1,3-5`; check page order, correct page count and content in the resulting PDF and PNGs.
- [ ] Invalid ranges `0`, `6-2`, beyond last page, malformed separators and massive numeric tokens do not export.
- [ ] Save one PNG via CreateDocument and many PNGs to an OpenDocumentTree folder; cancel each picker without losing selection or files.
- [ ] Share one/multiple PNGs and PDF; verify filenames, MIME types, temporary-file cleanup, and export cancellation.
- [ ] Cover pages, PDF background, real stroke/vector content, bookmarks and annotations appear correctly without altering stored notes.
- [ ] Light/dark/OLED, Chinese, font scaling, TalkBack focus/order and 48dp touch targets verified on real device.

The untracked `.kotlin/` directory stays untouched. No GitHub push is part of B07's local checkpoint.
